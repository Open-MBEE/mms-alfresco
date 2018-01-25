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
import java.util.Map;
import java.util.NavigableMap;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
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

        Map<String, Object> model;

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Accept: %s", accept));
        }

        model = handleArtifactGet(req, status, accept);

        printFooter(user, logger, timer);

        return model;
    }

    protected Map<String, Object> handleArtifactGet(WebScriptRequest req, Status status, String accept) {

        Map<String, Object> model = new HashMap<>();

        if (validateRequest(req, status)) {

            try {
                String artifactId = req.getServiceMatch().getTemplateVars().get(ARTIFACTID);
                String projectId = getProjectId(req);
                String refId = getRefId(req);
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
                String commitId = (req.getParameter(COMMITID) != null) ? req.getParameter(COMMITID) : null;
                String contentType = (req.getParameter(CONTENTTYPE) != null) ? req.getParameter(CONTENTTYPE) : null;
                if (artifactId != null && commitId == null) {
                    //Long commitTimestamp = emsNodeUtil.getTimestampFromElasticId(commitId);
                    JSONObject mountsJson = new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId);
                    JSONObject result = null;
                    try {
                        result = handleMountSearch(mountsJson, artifactId);
                    } catch (SQLException e) {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                        logger.error(String.format("%s", LogUtil.getStackTrace(e)));
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

    /**
     * Get the depth to recurse to from the request parameter.
     *
     * @param req WebScriptRequest object
     * @return Depth < 0 is infinite recurse, depth = 0 is just the element (if no request
     * parameter)
     */
    public Long getDepthFromRequest(WebScriptRequest req) {
        Long depth = null;
        String depthParam = req.getParameter("depth");
        if (depthParam != null) {
            try {
                depth = Long.parseLong(depthParam);
                if (depth < 0) {
                    depth = 100000L;
                }
            } catch (NumberFormatException nfe) {
                // don't do any recursion, ignore the depth
                log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST, "Bad depth specified, returning depth 0");
            }
        }

        // recurse default is false
        boolean recurse = getBooleanArg(req, "recurse", false);
        // for backwards compatiblity convert recurse to infinite depth (this
        // overrides
        // any depth setting)
        if (recurse) {
            depth = 100000L;
        }

        if (depth == null) {
            depth = 0L;
        }

        return depth;
    }

    protected JSONObject getVersionedArtifactFromParent(EmsScriptNode siteNode, String projectId, String refId,
        String filename, Long timestamp, EmsNodeUtil emsNodeUtil) {
        EmsScriptNode artifactNode = siteNode.childByNamePath("/" + projectId + "/refs/" + refId + "/" + filename);
        Pair<String, Long> parentRef = emsNodeUtil.getDirectParentRef(refId);
        Version nearest = null;
        if (artifactNode != null) {
            if (timestamp != null) {
                // Gets the url with the nearest timestamp to the given commit
                NavigableMap<Long, org.alfresco.service.cmr.version.Version> versions =
                    artifactNode.getVersionPropertyHistory();
                nearest = emsNodeUtil.imageVersionBeforeTimestamp(versions, timestamp);
                if (nearest != null) {
                    return new JSONObject().put("id", artifactNode.getSysmlId()).put("url",
                        "/service/api/node/content/versionStore/version2Store/" + String
                            .valueOf(nearest.getVersionProperty("node-uuid") + "/" + filename));
                }
            } else {
                // Gets the latest in the current Ref
                String url = artifactNode.getUrl();
                if (url != null) {
                    return new JSONObject().put("id", artifactNode.getSysmlId())
                        .put("url", url.replace("/d/d/", "/service/api/node/content/"));
                }
            }
        }
        if (parentRef != null && nearest == null) {
            // check for the earliest timestamp
            Long earliestCommit =
                (timestamp != null && parentRef.second.compareTo(timestamp) == 1) ? timestamp : parentRef.second;
            // recursive step
            return getVersionedArtifactFromParent(siteNode, projectId, parentRef.first, filename, earliestCommit,
                emsNodeUtil);
        }

        return null;
    }

    protected JSONObject handleArtifactMountSearch(JSONObject mountsJson, String filename, Long commitId) {
        String projectId = mountsJson.getString(Sjm.SYSMLID);
        String refId = mountsJson.getString(Sjm.REFID);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        EmsScriptNode siteNode = EmsScriptNode.getSiteNode(orgId);
        if (siteNode != null) {
            JSONObject parentImage =
                getVersionedArtifactFromParent(siteNode, projectId, refId, filename, commitId, emsNodeUtil);
            if (parentImage != null) {
                return parentImage;
            }
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID), null);
        }
        JSONArray mountsArray = mountsJson.getJSONArray(Sjm.MOUNTS);
        for (int i = 0; i < mountsArray.length(); i++) {
            JSONObject result = handleArtifactMountSearch(mountsArray.getJSONObject(i), filename, commitId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    protected JSONObject handleMountSearch(JSONObject mountsJson, String rootSysmlid) throws SQLException, IOException {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID));
        JSONObject tmpElements = emsNodeUtil.getArtifactById(rootSysmlid);
        if (tmpElements.length() > 0) {
            return tmpElements;
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID), null);
        }
        JSONArray mountsArray = mountsJson.getJSONArray(Sjm.MOUNTS);

        for (int i = 0; i < mountsArray.length(); i++) {
            tmpElements = handleMountSearch(mountsArray.getJSONObject(i), rootSysmlid);
            if (tmpElements.length() > 0) {
                return tmpElements;
            }
        }
        return new JSONObject();
    }
}
