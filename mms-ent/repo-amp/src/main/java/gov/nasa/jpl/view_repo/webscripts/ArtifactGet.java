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

import java.io.IOException;
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
 * @author lauram
 */
public class ArtifactGet extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(ArtifactGet.class);

    private static final String ARTIFACTID = "artifactId";
    private static final String COMMITID = "commitId";
    private static final String CONTENTTYPE = "contentType";

    public ArtifactGet() {
        super();
    }

    public ArtifactGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        if (refId != null && refId.equalsIgnoreCase(NO_WORKSPACE_ID)) {
            return true;
        } else if (refId != null && !emsNodeUtil.refExists(refId)) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Reference with id, %s not found", refId);
            return false;
        }
        return true;
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ArtifactGet instance = new ArtifactGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap();
        JsonObject top = new JsonObject();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Accept: %s", accept));
        }
        Boolean isCommit = req.getParameter(COMMITID) != null && !req.getParameter(COMMITID).isEmpty();
        // :TODO refactor to handle the response consistently
        try {
            if (isCommit) {
                JsonArray commitJsonToArray = new JsonArray();
                JsonObject commitJson = handleCommitRequest(req);
                commitJsonToArray.add(commitJson);
                if (commitJson.size() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "No elements found.");
                }
                if (commitJson != null && commitJson.size() > 0) {
                    top.add(Sjm.ARTIFACTS, filterByPermission(commitJsonToArray, req));
                }
                if (top.has(Sjm.ARTIFACTS) && top.get(Sjm.ARTIFACTS).getAsJsonArray().size() < 1) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }
                status.setCode(responseStatus.getCode());
                if (prettyPrint || accept.contains("webp")) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    model.put(Sjm.RES, gson.toJson(top));
                } else {
                    model.put(Sjm.RES, top);
                }
            } else {
                model = handleArtifactGet(req, status, accept);
            }
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        printFooter(user, logger, timer);

        return model;
    }

    private JsonObject handleCommitRequest(WebScriptRequest req) {
        // getElement at commit
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACTID);
        String commitId = req.getParameter(COMMITID);
        if (emsNodeUtil.getArtifactById(artifactId, true) != null) {
            JsonObject artifact = emsNodeUtil.getArtifactByArtifactAndCommitId(commitId, artifactId);
            if (artifact == null || artifact.size() == 0) {
                // :TODO I don't think this logic needs to be changed at all for Artifact
                //this is true if element id and artifact id are mutually unique
                return emsNodeUtil.getElementAtCommit(artifactId, commitId);
            } else {
                return artifact;
            }
        }

        JsonObject mountsJson = new JsonObject();
        mountsJson.addProperty(Sjm.SYSMLID, projectId);
        mountsJson.addProperty(Sjm.REFID, refId);
        // convert commitId to timestamp
        String commit = emsNodeUtil.getCommitObject(commitId).get(Sjm.CREATED).getAsString();

        JsonArray result = new JsonArray();
        Set<String> elementsToFind = new HashSet<>();
        elementsToFind.add(artifactId);

        try {
            EmsNodeUtil.handleMountSearch(mountsJson, false, false, 0L, elementsToFind, result, commit, "artifacts");
            if (result.size() > 0) {
                return JsonUtil.getOptObject(result, 0);
            }
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find artifact %s at commit %s", artifactId,
                commitId);
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonObject();
    }

    protected Map<String, Object> handleArtifactGet(WebScriptRequest req, Status status, String accept) {

        Map<String, Object> model = new HashMap<>();

        if (validateRequest(req, status)) {
            JsonObject result = null;
            String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACTID);
            String projectId = getProjectId(req);
            String refId = getRefId(req);
            if (artifactId != null) {
                JsonObject mountsJson = new JsonObject();
                mountsJson.addProperty(Sjm.SYSMLID, projectId);
                mountsJson.addProperty(Sjm.REFID, refId);

                JsonArray results = new JsonArray();
                Set<String> elementsToFind = new HashSet<>();
                elementsToFind.add(artifactId);
                try {
                    EmsNodeUtil
                        .handleMountSearch(mountsJson, false, false, 0L, elementsToFind, results, null, "artifacts");
                    if (results.size() > 0) {
                        result = JsonUtil.getOptObject(results, 0);
                    }
                } catch (IOException e) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                    logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                }
                if (result != null) {
                    JsonObject r = new JsonObject();
                    JsonArray array = new JsonArray();
                    array.add(result);
                    r.add("artifacts", array);
                    model.put(Sjm.RES, r);
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                }
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "ArtifactId not supplied!\n");
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid request!");
        }
        status.setCode(responseStatus.getCode());
        if (!model.containsKey(Sjm.RES)) {
            model.put(Sjm.RES, createResponseJson());
        }
        return model;
    }
}
