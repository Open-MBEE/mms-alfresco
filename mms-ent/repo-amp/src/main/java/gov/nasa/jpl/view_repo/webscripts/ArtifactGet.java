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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.collections.map.HashedMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.Node;

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
        JSONObject top = new JSONObject();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Accept: %s", accept));
        }
        Boolean isCommit = req.getParameter(COMMITID) != null && !req.getParameter(COMMITID).isEmpty();
        // :TODO refactor to handle the response consistently
        try {
            if (isCommit) {
                JSONArray commitJsonToArray = new JSONArray();
                JSONObject commitJson = handleCommitRequest(req);
                commitJsonToArray.put(commitJson);
                if (commitJson.length() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "No elements found.");
                }
                if (commitJson != null && commitJson.length() > 0) {
                    top.put(Sjm.ARTIFACTS, filterByPermission(commitJsonToArray, req));
                }
                if (top.has(Sjm.ARTIFACTS) && top.getJSONArray(Sjm.ARTIFACTS).length() < 1) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }
                status.setCode(responseStatus.getCode());
                if (prettyPrint || accept.contains("webp")) {
                    model.put(Sjm.RES, top.toString(4));
                } else {
                    model.put(Sjm.RES, top);
                }
            } else {
                model = handleArtifactGet(req, status, accept);
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        printFooter(user, logger, timer);

        return model;
    }

    private JSONObject handleCommitRequest(WebScriptRequest req) {
        // getElement at commit
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACTID);
        String commitId = req.getParameter(COMMITID);
        if (emsNodeUtil.getArtifactById(artifactId, true) != null) {
            JSONObject artifact = emsNodeUtil.getArtifactByArtifactAndCommitId(commitId, artifactId);
            if (artifact == null || artifact.length() == 0) {
                // :TODO I don't think this logic needs to be changed at all for Artifact
                //this is true if element id and artifact id are mutually unique
                return emsNodeUtil.getElementAtCommit(artifactId, commitId);
            } else {
                return artifact;
            }
        }

        JSONObject mountsJson = new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId);
        // convert commitId to timestamp
        String commit = emsNodeUtil.getCommitObject(commitId).getString(Sjm.CREATED);

        JSONArray result = new JSONArray();
        Set<String> elementsToFind = new HashSet<>();
        elementsToFind.add(artifactId);

        try {
            EmsNodeUtil.handleMountSearch(mountsJson, false, false, 0L, elementsToFind, result, commit, "artifacts");
            if (result.length() > 0) {
                return result.optJSONObject(0);
            }
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find artifact %s at commit %s", artifactId,
                commitId);
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JSONObject();
    }

    protected Map<String, Object> handleArtifactGet(WebScriptRequest req, Status status, String accept) {

        Map<String, Object> model = new HashMap();

        if (validateRequest(req, status)) {

            try {
                JSONObject result = null;
                String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACTID);
                String projectId = getProjectId(req);
                String refId = getRefId(req);
                if (artifactId != null) {
                    JSONObject mountsJson = new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId);

                    JSONArray results = new JSONArray();
                    Set<String> elementsToFind = new HashSet<>();
                    elementsToFind.add(artifactId);
                    try {
                        EmsNodeUtil.handleMountSearch(mountsJson, false, false, 0L, elementsToFind, results, null, "artifacts");
                        if (results.length() > 0) {
                            result = results.optJSONObject(0);
                        }
                    } catch (IOException e) {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                        logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                    }
                    if (result != null) {
                        model.put(Sjm.RES, new JSONObject().put("artifacts", new JSONArray().put(result)));
                    } else {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "ArtifactId not supplied!\n");
                }
            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Issues creating return JSON\n");
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid request!\n");
        }
        status.setCode(responseStatus.getCode());
        if (!model.containsKey(Sjm.RES)) {
            model.put(Sjm.RES, createResponseJson());
        }
        return model;
    }
}
