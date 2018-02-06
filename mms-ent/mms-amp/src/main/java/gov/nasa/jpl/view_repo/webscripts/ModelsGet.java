/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * @author cinyoung
 */
public class ModelsGet extends ModelGet {
    static Logger logger = Logger.getLogger(ModelsGet.class);

    public ModelsGet() {
        super();
    }

    public ModelsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    String timestamp;
    Date dateTime;

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // get timestamp if specified
        timestamp = req.getParameter("timestamp");
        dateTime = TimeUtils.dateFromTimestamp(timestamp);

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
        ModelsGet instance = new ModelsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        Map<String, Object> model = new HashMap<>();
        JsonArray elementsJson = new JsonArray();
        JsonArray errors = new JsonArray();
        JsonObject result = new JsonObject();

        try {

            if (validateRequest(req, status)) {
                try {
                    Long depth = getDepthFromRequest(req);
                    result = handleRequest(req, depth);
                    elementsJson = JsonUtil.getOptArray(result, Sjm.ELEMENTS);
                } catch (JsonParseException e) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request", e);
                }
            }

            JsonObject top = new JsonObject();
            if (elementsJson.size() > 0) {
                JsonArray elements = filterByPermission(elementsJson, req);
                if (elements.size() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }

                top.add(Sjm.ELEMENTS, elements);

                JsonArray errorMessages = new JsonArray();
                for (String level : Sjm.ERRORLEVELS) {
                    errors = JsonUtil.getOptArray(result, level);
                    if (errors.size() > 0) {
                        for (int i = 0; i < errors.size(); i++) {
                            JsonObject errorPayload = new JsonObject();
                            errorPayload.addProperty("code", HttpServletResponse.SC_NOT_FOUND);
                            errorPayload.add(Sjm.SYSMLID, errors.get(i));
                            errorPayload.addProperty("message", String.format("Element %s was not found", errors.get(i).getAsString()));
                            errorPayload.addProperty("severity", level);
                            errorMessages.add(errorPayload);
                        }
                    }
                }

                if (errorMessages.size() > 0) {
                    top.add("messages", errorMessages);
                }

                if (prettyPrint || accept.contains("webp")) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    model.put(Sjm.RES, gson.toJson(top));
                } else {
                    model.put(Sjm.RES, top);
                }
            } else {
                log(Level.INFO, HttpServletResponse.SC_OK, "No elements found");
                top.add(Sjm.ELEMENTS, new JsonArray());
                model.put(Sjm.RES, top);
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

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
    private JsonObject handleRequest(WebScriptRequest req, final Long maxDepth)
        throws JsonParseException, IOException, SQLException {
        JsonParser parser = new JsonParser();
        JsonElement requestJson = parser.parse(req.getContent().getContent());
        if (!requestJson.isJsonNull() && requestJson.getAsJsonObject().has(Sjm.ELEMENTS)) {
            JsonArray elementsToFindJson = requestJson.getAsJsonObject().get(Sjm.ELEMENTS).getAsJsonArray();

            String refId = getRefId(req);
            String projectId = getProjectId(req);
            boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

            JsonObject mountsJson = new JsonObject();
            mountsJson.addProperty(Sjm.SYSMLID, projectId);
            mountsJson.addProperty(Sjm.REFID, refId);

            JsonArray found = new JsonArray();
            JsonObject result = new JsonObject();

            Set<String> uniqueElements = new HashSet<>();
            for (int i = 0; i < elementsToFindJson.size(); i++) {
                uniqueElements.add(elementsToFindJson.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
            }
            EmsNodeUtil.handleMountSearch(mountsJson, extended, false, maxDepth, uniqueElements, found);
            result.add(Sjm.ELEMENTS, found);
            JsonUtil.addStringSet(result, Sjm.WARN, uniqueElements);
            return result;
        } else {
            return new JsonObject();
        }
    }
}

