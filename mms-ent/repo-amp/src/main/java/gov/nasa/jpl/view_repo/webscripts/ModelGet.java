/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY~
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import gov.nasa.jpl.mbee.util.TimeUtils;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.get.desc.xml
 *
 * @author cinyoung
 */
public class ModelGet extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(ModelGet.class);

    protected static final String ELEMENTID = "elementId";
    protected static final String ARTIFACTID = "artifactId";

    protected Set<String> elementsToFind = new HashSet<>();
    protected JsonArray deletedElementsCache = new JsonArray();

    public ModelGet() {
        super();
    }

    public ModelGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // get timestamp if specified
        String timestamp = req.getParameter("timestamp");
        Date dateTime = TimeUtils.dateFromTimestamp(timestamp);

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        if (refId != null && refId.equalsIgnoreCase(NO_WORKSPACE_ID)) {
            return true;
        } else if (refId != null && !emsNodeUtil.refExists(refId)) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Reference with id, %s not found",
                refId + (dateTime == null ? "" : " at " + dateTime));
            return false;
        }
        return true;
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelGet instance = new ModelGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        if (logger.isDebugEnabled()) {
            printHeader(user, logger, req);
        } else {
            printHeader(user, logger, req, true);
        }
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        try {
            if (validateRequest(req, status)) {
                model = getModel(req);
            }
        } catch (IllegalStateException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "unable to get JSON object from request", e);
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request", e);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (model.isEmpty()) {
            model.put(Sjm.RES, createResponseJson());
        }
        status.setCode(responseStatus.getCode());
        printFooter(user, logger, timer);
        return model;
    }

    protected Map<String, Object> getModel(WebScriptRequest req) throws IOException {
        Long depth = getDepthFromRequest(req);
        JsonObject result = handleRequest(req, depth, Sjm.ELEMENTS);
        return finish(req, result, true, Sjm.ELEMENTS);
    }

    protected Map<String, Object> finish(WebScriptRequest req, JsonObject result, boolean single, String type) {
        Map<String, Set<String>> errors = new HashMap<>();
        JsonArray elementsJson = JsonUtil.getOptArray(result, type);
        JsonObject top = new JsonObject();
        JsonArray got = filterByPermission(elementsJson, req);
        if (elementsJson.size() > 0) {
            top.add(type, got);
            if (got.size() < 1) {
                responseStatus.setCode(HttpServletResponse.SC_FORBIDDEN);
            }
        } else if (!single) {
            top.add(type, got);
        }
        if (deletedElementsCache.size() > 0) {
            JsonArray deleted = filterByPermission(deletedElementsCache, req);
            if (deleted.size() > 0) {
                Set<String> deletedSet = new HashSet<>();
                for (int i = 0; i < deleted.size(); i++) {
                    deletedSet.add(deleted.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
                }
                elementsToFind.removeAll(deletedSet);
                errors.put(Sjm.DELETED, deletedSet);
                top.add(Sjm.DELETED, deleted);
                if (single) {
                    responseStatus.setCode(HttpServletResponse.SC_GONE);
                }
            }
        }
        if (!elementsToFind.isEmpty()) {
            errors.put(Sjm.FAILED, elementsToFind);
            if (single) {
                responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        JsonArray errorMessages = parseErrors(errors);
        if (errorMessages.size() > 0) {
            top.add("messages", errorMessages);
        }

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        Map<String, Object> model = new HashMap<>();
        if (prettyPrint || accept.contains("html")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            model.put(Sjm.RES, gson.toJson(top));
        } else {
            model.put(Sjm.RES, top);
        }
        return model;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of
     * elements
     *
     * @param req
     * @return
     * @throws IOException
     */
    protected JsonObject handleRequest(WebScriptRequest req, final Long maxDepth, String type) throws IOException {
        JsonObject requestJson = JsonUtil.buildFromString(req.getContent().getContent());
        String templateVar = type.equals(Sjm.ELEMENTS) ? ELEMENTID : ARTIFACTID;

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        String commitId = req.getParameter(Sjm.COMMITID.replace("_", ""));
        String elementId = req.getServiceMatch().getTemplateVars().get(templateVar);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JsonArray elementsToFindJson = new JsonArray();

        if (elementId != null) {
            if (commitId != null) {
                JsonObject element = emsNodeUtil.getByCommitId(elementId, commitId, type);
                if (element != null && element.size() > 0) {
                    JsonObject result = new JsonObject();
                    JsonArray elements = new JsonArray();
                    elements.add(element);
                    result.add(type, elements);
                    return result;
                }
            }
            JsonObject element = new JsonObject();
            element.addProperty(Sjm.SYSMLID, elementId);
            elementsToFindJson.add(element);
        } else if (requestJson.has(type)) {
            elementsToFindJson = requestJson.get(type).getAsJsonArray();
        } else {
            return new JsonObject();
        }

        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

        JsonObject mountsJson = new JsonObject();
        mountsJson.addProperty(Sjm.SYSMLID, projectId);
        mountsJson.addProperty(Sjm.REFID, refId);

        JsonArray found = new JsonArray();
        JsonObject result = new JsonObject();

        for (int i = 0; i < elementsToFindJson.size(); i++) {
            try {
                String currentId = elementsToFindJson.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString();
                elementsToFind.add(currentId);
            } catch (Exception e) {
                // Intentionally empty
            }
        }

        JsonObject commitObject = emsNodeUtil.getCommitObject(commitId);

        String timestamp = commitObject != null && commitObject.has(Sjm.CREATED) ?
            commitObject.get(Sjm.CREATED).getAsString() :
            null;

        if (commitId != null && commitObject == null) {
            elementsToFind = new HashSet<>();
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid commit id");
        } else {
            EmsNodeUtil.handleMountSearch(mountsJson, extended, false, maxDepth, elementsToFind, found, timestamp, type);
        }

        if (!elementsToFind.isEmpty()) {
            JsonArray deletedElementsJson;
            if (type.equals(Sjm.ELEMENTS)) {
                deletedElementsJson = emsNodeUtil.getNodesBySysmlids(elementsToFind, false, true);
            } else {
                deletedElementsJson = emsNodeUtil.getArtifactsBySysmlids(elementsToFind, true);
            }
            if (timestamp != null) {
                for (JsonElement check : deletedElementsJson) {
                    try {
                        Date created = EmsNodeUtil.df.parse(JsonUtil.getOptString((JsonObject) check, Sjm.CREATED));
                        Date commitDate = EmsNodeUtil.df.parse(timestamp);
                        if (!created.after(commitDate)) {
                            this.deletedElementsCache.add(check);
                        }
                    } catch (ParseException pe) {
                        if (logger.isDebugEnabled()) {
                            logger.debug(pe);
                        }
                    }
                }
            } else {
                this.deletedElementsCache = deletedElementsJson;
            }
        }
        result.add(type, found);
        return result;
    }

    /**
     * Get the depth to recurse to from the request parameter.
     *
     * @param req WebScriptRequest object
     * @return Depth < 0 is infinite recurse, depth = 0 is just the element (if no request
     * parameter)
     */
    Long getDepthFromRequest(WebScriptRequest req) {
        long depth = 0L;
        String depthParam = req.getParameter("depth");
        if (depthParam != null) {
            depth = parseDepth(depthParam);
        }

        return getBooleanArg(req, "recurse", false) ? 100000L : depth;
    }

    Long parseDepth(String depthParam) {
        long depth = 0L;
        try {
            depth = Long.parseLong(depthParam);
            if (depth < 0) {
                depth = 100000L;
            }
        } catch (NumberFormatException nfe) {
            // don't do any recursion, ignore the depth
            log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST, "Bad depth specified, returning depth 0");
        }
        return depth;
    }
}
