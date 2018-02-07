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
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.get.desc.xml
 *
 * @author cinyoung
 */
public class ModelGet extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(ModelGet.class);

    private static final String ELEMENTID = "elementId";
    private static final String COMMITID = "commitId";

    public ModelGet() {
        super();
    }

    public ModelGet(Repository repositoryHelper, ServiceRegistry registry) {
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
        ModelGet instance = new ModelGet(repository, getServices());
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

        if (accept.contains("image") && !accept.contains("webp")) {
            model = handleArtifactGet(req, status, accept);
        } else {
            model = handleElementGet(req, status, accept);
        }

        printFooter(user, logger, timer);

        return model;
    }

    protected Map<String, Object> handleElementGet(WebScriptRequest req, Status status, String accept) {

        Map<String, Object> model = new HashMap<>();
        JsonObject top = new JsonObject();

        if (validateRequest(req, status)) {
            Boolean isCommit = req.getParameter(COMMITID) != null && !req.getParameter(COMMITID).isEmpty();
            try {
                if (isCommit) {
                    JsonArray commitJsonToArray = new JsonArray();
                    JsonObject commitJson = handleCommitRequest(req);
                    commitJsonToArray.add(commitJson);
                    if (commitJson.size() == 0) {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "No elements found.");
                    }
                    if (commitJson != null && commitJson.size() > 0) {
                        top.add(Sjm.ELEMENTS, filterByPermission(commitJsonToArray, req));
                    }
                    if (top.has(Sjm.ELEMENTS) && top.get(Sjm.ELEMENTS).getAsJsonArray().size() < 1) {
                        log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                    }
                } else {
                    JsonArray elementsJson = handleRequest(req);
                    if (elementsJson.size() == 0) {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "No elements found.");
                    }
                    if (elementsJson.size() > 0) {
                        top.add(Sjm.ELEMENTS, filterByPermission(elementsJson, req));
                    }
                    if (top.has(Sjm.ELEMENTS) && top.get(Sjm.ELEMENTS).getAsJsonArray().size() < 1) {
                        log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                    }
                }
                if (!Utils.isNullOrEmpty(response.toString()))
                    top.addProperty("message", response.toString());
            } catch (Exception e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
            }

            status.setCode(responseStatus.getCode());
        }
        if (prettyPrint || accept.contains("webp")) {
        	Gson gson = new GsonBuilder().setPrettyPrinting().create();
            model.put(Sjm.RES, gson.toJson(top));
        } else {
            model.put(Sjm.RES, top);
        }

        return model;
    }

    protected Map<String, Object> handleArtifactGet(WebScriptRequest req, Status status, String accept) {

        Map<String, Object> model = new HashMap<>();

        String extensionArg = accept.replaceFirst(".*/(\\w+).*", "$1");

        if (!extensionArg.startsWith(".")) {
            extensionArg = "." + extensionArg;
        }

        String extension = !extensionArg.equals("*") ? extensionArg : ".svg";  // Assume .svg if no extension provided
        // Case One: Search on or before the commitId you branched from
        // Case Two: Search by CommitId

        if (!Utils.isNullOrEmpty(extension) && !extension.startsWith(".")) {
            extension = "." + extension;
        }

        if (validateRequest(req, status)) {

                String artifactId = req.getServiceMatch().getTemplateVars().get(ELEMENTID);
                String projectId = getProjectId(req);
                String refId = getRefId(req);
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
                if (artifactId != null) {
                    int lastIndex = artifactId.lastIndexOf('/');
                    if (artifactId.length() > (lastIndex + 1)) {
                        artifactId = lastIndex != -1 ? artifactId.substring(lastIndex + 1) : artifactId;
                        String filename = artifactId + extension;
                        String commitId = (req.getParameter(COMMITID) != null) ? req.getParameter(COMMITID) : null;
                        Long commitTimestamp = emsNodeUtil.getTimestampFromElasticId(commitId);
                		JsonObject proj = new JsonObject();
                		proj.addProperty(Sjm.SYSMLID, projectId);
                		proj.addProperty(Sjm.REFID, refId);
                        JsonObject result = handleArtifactMountSearch(proj, filename, commitTimestamp);
                        if (result != null) {
                        	JsonObject res = new JsonObject();
                        	JsonArray array = new JsonArray();
                        	array.add(result);
                        	res.add("artifacts", array);
                            model.put(Sjm.RES, res);
                        } else {
                            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                        }
                    } else {
                        log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId!\n");
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "ArtifactId not supplied!\n");
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
     * Wrapper for handling a request and getting the appropriate JSONArray of elements
     *
     * @param req WebScriptRequest object
     * @return JSONArray of elements
     */
    private JsonArray handleRequest(WebScriptRequest req) {
        // REVIEW -- Why check for errors here if validate has already been
        // called? Is the error checking code different? Why?
        try {
            String modelId = req.getServiceMatch().getTemplateVars().get(ELEMENTID);
            String projectId = getProjectId(req);
            String refId = getRefId(req);
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            if (null == modelId) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s", modelId);
                return new JsonArray();
            } else if (emsNodeUtil.isDeleted(modelId)) {
                log(Level.ERROR, HttpServletResponse.SC_GONE, "Element %s is deleted", modelId);
                return new JsonArray();
            }
            return handleElementHierarchy(modelId, req);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonArray();
    }

    private JsonObject handleCommitRequest(WebScriptRequest req) {
        // getElement at commit
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String elementId = req.getServiceMatch().getTemplateVars().get(ELEMENTID);
        String commitId = req.getParameter(COMMITID);

        if (emsNodeUtil.getById(elementId) != null) {
            JsonObject element = emsNodeUtil.getElementByElementAndCommitId(commitId, elementId);
            if (element == null || element.size() == 0) {
                return emsNodeUtil.getElementAtCommit(elementId, commitId);
            } else {
                return element;
            }
        }

        JsonObject mountsJson = new JsonObject();
        mountsJson.addProperty(Sjm.SYSMLID, projectId);
        mountsJson.addProperty(Sjm.REFID, refId);
        // convert commitId to timestamp
        String commit = emsNodeUtil.getCommitObject(commitId).get(Sjm.CREATED).getAsString();
        try {
            return handleMountSearchForCommits(mountsJson, elementId, commit);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s at commit %s",
                elementId, commitId);
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return new JsonObject();
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

    protected JsonObject getVersionedArtifactFromParent(EmsScriptNode siteNode, String projectId, String refId,
        String filename, Long timestamp, EmsNodeUtil emsNodeUtil) {
        EmsScriptNode artifactNode = siteNode.childByNamePath("/" + projectId + "/refs/" + refId + "/" + filename);
        Pair<String, Long> parentRef = emsNodeUtil.getDirectParentRef(refId);
        Version nearest = null;
        if (artifactNode != null) {
            if (timestamp != null) {
                // Gets the url with the nearest timestamp to the given commit
                NavigableMap<Long, org.alfresco.service.cmr.version.Version> versions = artifactNode.getVersionPropertyHistory();
                nearest = emsNodeUtil.imageVersionBeforeTimestamp(versions, timestamp);
                if (nearest != null) {
                	JsonObject node = new JsonObject();
                	node.addProperty("id", artifactNode.getSysmlId());
                	node.addProperty("url",
                        "/service/api/node/content/versionStore/version2Store/" 
                	+ String.valueOf(nearest.getVersionProperty("node-uuid") + "/" + filename));
                    return node;
                }
            } else {
                // Gets the latest in the current Ref
                String url = artifactNode.getUrl();
                if (url != null) {
                	JsonObject node = new JsonObject();
                	node.addProperty("id", artifactNode.getSysmlId());
                	node.addProperty("url", url.replace("/d/d/", "/service/api/node/content/"));
                    return node;
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

    protected JsonObject handleArtifactMountSearch(JsonObject mountsJson, String filename, Long commitId) {
        String projectId = mountsJson.get(Sjm.SYSMLID).getAsString();
        String refId = mountsJson.get(Sjm.REFID).getAsString();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        EmsScriptNode siteNode = EmsScriptNode.getSiteNode(orgId);
        if (siteNode != null) {
            JsonObject parentImage =
                getVersionedArtifactFromParent(siteNode, projectId, refId, filename, commitId, emsNodeUtil);
            if (parentImage != null) {
                return parentImage;
            }
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                		mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();
        for (int i = 0; i < mountsArray.size(); i++) {
            JsonObject result = handleArtifactMountSearch(mountsArray.get(i).getAsJsonObject(), filename, commitId);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Recurse a view hierarchy to get all allowed elements
     *
     * @param rootSysmlid Root view to find elements for
     * @param req         WebScriptRequest
     * @return JSONArray of elements
     * @throws SQLException SQL error
     * @throws IOException  IO error
     */

    protected JsonArray handleElementHierarchy(String rootSysmlid, WebScriptRequest req)
        throws SQLException, IOException {
        // get timestamp if specified
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        Long depth = getDepthFromRequest(req);

        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

        JsonObject mountsJson = new JsonObject();
        mountsJson.addProperty(Sjm.SYSMLID, projectId);
        mountsJson.addProperty(Sjm.REFID, refId);

        return handleMountSearch(mountsJson, extended, rootSysmlid, depth);
    }

    protected JsonArray handleMountSearch(JsonObject mountsJson, boolean extended, String rootSysmlid, Long depth)
        throws SQLException, IOException {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.get(Sjm.SYSMLID).getAsString(), 
        		mountsJson.get(Sjm.REFID).getAsString());
        JsonArray tmpElements = emsNodeUtil.getChildren(rootSysmlid, depth);
        if (tmpElements.size() > 0) {
            return extended ? emsNodeUtil.addExtendedInformation(tmpElements) : tmpElements;
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                		mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();

        for (int i = 0; i < mountsArray.size(); i++) {
            tmpElements = handleMountSearch(mountsArray.get(i).getAsJsonObject(), extended, rootSysmlid, depth);
            if (tmpElements.size() > 0) {
                return tmpElements;
            }
        }
        return new JsonArray();

    }

    protected JsonObject handleMountSearchForCommits(JsonObject mountsJson, String rootSysmlid, String timestamp)
        throws SQLException, IOException {

        if (!mountsJson.has(Sjm.MOUNTS)) {
            EmsNodeUtil emsNodeUtil =
                new EmsNodeUtil(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                		mountsJson.get(Sjm.REFID).getAsString());
            mountsJson = emsNodeUtil
                .getProjectWithFullMounts(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                		mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();
        for (int i = 0; i < mountsArray.size(); i++) {
            EmsNodeUtil nodeUtil = new EmsNodeUtil(mountsArray.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString(),
                mountsArray.get(i).getAsJsonObject().get(Sjm.REFID).getAsString());
            if (nodeUtil.getById(rootSysmlid) != null) {
                JsonArray commits = nodeUtil.getRefHistory(mountsArray.get(i).getAsJsonObject().get(Sjm.REFID).getAsString());
                JsonArray nearestCommit = nodeUtil.getNearestCommitFromTimestamp(timestamp, commits);
                if (nearestCommit.size() > 0) {
                    JsonObject elementObject =
                        nodeUtil.getElementAtCommit(rootSysmlid, 
                        		nearestCommit.get(0).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
                    if (elementObject.size() > 0) {
                        return elementObject;
                    }
                }
                return new JsonObject();
            }
            JsonObject el = handleMountSearchForCommits(mountsArray.get(i).getAsJsonObject(), rootSysmlid, timestamp);
            if (el.size() > 0) {
                return el;
            }
        }
        return new JsonObject();
    }
}
