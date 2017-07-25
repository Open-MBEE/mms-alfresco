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
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
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
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.view_repo.util.NodeUtil.SearchType;

/**
 * Descriptor in /view-repo/src/main/amp/config/alfresco/extension/templates/webscripts
 * /gov/nasa/jpl/javawebscripts/model.get.desc.xml
 *
 * @author cinyoung
 */
public class ModelGet extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(ModelGet.class);

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

            if (refId != null && refId.equalsIgnoreCase("master")) {
                return true;
            }
            else if (refId != null && !emsNodeUtil.refExists(refId)) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Reference with id, %s not found",
                    refId);
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

        Map<String, Object> model = new HashMap<>();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";
        logger.error("Accept: " + accept);

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
        JSONObject top = new JSONObject();

        if (validateRequest(req, status)) {
            Boolean isCommit = req.getParameter("commitId") != null;
            try {
                if (isCommit) {
                    JSONArray commitJsonToArray = new JSONArray();
                    JSONObject commitJson = handleCommitRequest(req, top);
                    commitJsonToArray.put(commitJson);
                    if (commitJson.length() > 0) {
                        top.put(Sjm.ELEMENTS, filterByPermission(commitJsonToArray, req));
                    }
                } else {
                    JSONArray elementsJson = handleRequest(req, top, NodeUtil.doGraphDb);
                    if (elementsJson.length() > 0) {
                        top.put(Sjm.ELEMENTS, filterByPermission(elementsJson, req));
                    }
                }
                if (top.length() == 0) {
                    responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
                }
                if (top.has(Sjm.ELEMENTS) && top.getJSONArray(Sjm.ELEMENTS).length() < 1) {
                    responseStatus.setCode(HttpServletResponse.SC_FORBIDDEN);
                }
                if (!Utils.isNullOrEmpty(response.toString()))
                    top.put("message", response.toString());
            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSONObject");
                logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
            }

            status.setCode(responseStatus.getCode());
        }
        if (prettyPrint || accept.contains("webp")) {
            model.put("res", top.toString(4));
        } else {
            model.put("res", top);
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
        String commitId = req.getParameter("commitId"); //need to support this

        if (!Utils.isNullOrEmpty(extension) && !extension.startsWith(".")) {
            extension = "." + extension;
        }

        if (validateRequest(req, status)) {

            try {
                String artifactId = req.getServiceMatch().getTemplateVars().get("elementId");
                String projectId = getProjectId(req);
                String refId = getRefId(req);
                if (artifactId != null) {
                    int lastIndex = artifactId.lastIndexOf("/");
                    if (artifactId.length() > (lastIndex + 1)) {
                        artifactId = lastIndex != -1 ? artifactId.substring(lastIndex + 1) : artifactId;
                        String filename = artifactId + extension;
                        JSONObject result = handleArtifactMountSearch(new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId), filename);
                        if (result != null) {
                            model.put("res", new JSONObject().put("artifacts", new JSONArray().put(result)));
                        } else {
                            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Artifact not found!\n");
                        }
                    } else {
                        log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId!\n");
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "ArtifactId not supplied!\n");
                }
            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Issues creating return JSON\n");
                e.printStackTrace();
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid request!\n");
        }
        status.setCode(responseStatus.getCode());
        if (!model.containsKey("res")) {
            model.put("res", createResponseJson());
        }
        return model;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of elements
     *
     * @param req   WebScriptRequest object
     * @param top   JSONObject for top level result
     * @param useDb Flag to use database
     * @return JSONArray of elements
     */
    private JSONArray handleRequest(WebScriptRequest req, final JSONObject top, boolean useDb) {
        // REVIEW -- Why check for errors here if validate has already been
        // called? Is the error checking code different? Why?
        try {
            String modelId = req.getServiceMatch().getTemplateVars().get("elementId");
            String projectId = getProjectId(req);
            String refId = getRefId(req);
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            if (null == modelId) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s", modelId);
                return new JSONArray();
            } else if (emsNodeUtil.isDeleted(modelId)) {
                log(Level.ERROR, HttpServletResponse.SC_GONE, "Element %s is deleted", modelId);
                return new JSONArray();
            }
            return handleElementHierarchy(modelId, req);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    private JSONObject handleCommitRequest(WebScriptRequest req, JSONObject top) {
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        JSONObject element;
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String elementId = req.getServiceMatch().getTemplateVars().get("elementId");
        String currentElement = emsNodeUtil.getById(elementId).getElasticId();

        // This is the commit of the version of the element we want
        String commitId = (req.getParameter("commitId").isEmpty()) ?
            emsNodeUtil.getById(elementId).getLastCommit() :
            req.getParameter("commitId");

        // This is the lastest commit for the element
        String lastestCommitId = emsNodeUtil.getById(elementId).getLastCommit();
        Boolean checkInProjectAndRef = false;
        try {
            checkInProjectAndRef = emsNodeUtil.commitContainsElement(elementId, commitId);
        } catch (Exception e){
            logger.warn(e.getMessage());
        }

        if (commitId.equals(lastestCommitId)) {
            return emsNodeUtil.getElementByElasticID(currentElement);
        } else if (checkInProjectAndRef) {
            return emsNodeUtil.getElementByElementAndCommitId(commitId, elementId);
        } else {

            element = emsNodeUtil.getElementAtCommit(elementId, commitId);
            if (element == null) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s at commit %s", elementId,
                    commitId);
            }
        }
        return element;
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

    protected JSONObject handleArtifactMountSearch(JSONObject mountsJson, String filename) {
        String projectId = mountsJson.getString(Sjm.SYSMLID);
        String refId = mountsJson.getString(Sjm.REFID);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        EmsScriptNode siteNode = EmsScriptNode.getSiteNode(orgId);
        if (siteNode != null) {
            EmsScriptNode artifactNode = siteNode.childByNamePath("/" + projectId + "/refs/" + refId + "/" + filename);
            if (artifactNode != null) {
                String url = artifactNode.getUrl();
                if (url != null) {
                    return new JSONObject().put("id", artifactNode.getSysmlId()).put("url", url.replace("/d/d/", "/service/api/node/content/"));
                }
            }
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil.getProjectWithFullMounts(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID), null);
        }
        JSONArray mountsArray = mountsJson.getJSONArray(Sjm.MOUNTS);
        for (int i = 0; i < mountsArray.length(); i++) {
            JSONObject result = handleArtifactMountSearch(mountsArray.getJSONObject(i), filename);
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
     * @throws JSONException JSON element creation error
     * @throws SQLException  SQL error
     * @throws IOException   IO error
     */

    protected JSONArray handleElementHierarchy(String rootSysmlid, WebScriptRequest req)
        throws JSONException, SQLException, IOException {
        // get timestamp if specified
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        Long depth = getDepthFromRequest(req);

        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

        JSONObject mountsJson = new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId);

        JSONArray elasticElements = handleMountSearch(mountsJson, extended, rootSysmlid, depth);
        return elasticElements;
    }

    protected JSONArray handleMountSearch(JSONObject mountsJson, boolean extended, String rootSysmlid, Long depth)
        throws JSONException, SQLException, IOException {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID));
        JSONArray tmpElements = emsNodeUtil.getChildren(rootSysmlid, depth);
        if (tmpElements.length() > 0) {
            return extended ? emsNodeUtil.addExtendedInformation(tmpElements) : tmpElements;
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil.getProjectWithFullMounts(mountsJson.getString(Sjm.SYSMLID), mountsJson.getString(Sjm.REFID), null);
        }
        JSONArray mountsArray = mountsJson.getJSONArray(Sjm.MOUNTS);

        for (int i = 0; i < mountsArray.length(); i++) {
            tmpElements = handleMountSearch(mountsArray.getJSONObject(i), extended, rootSysmlid, depth);
            if (tmpElements.length() > 0) {
                return tmpElements;
            }
        }
        return new JSONArray();

    }

}
