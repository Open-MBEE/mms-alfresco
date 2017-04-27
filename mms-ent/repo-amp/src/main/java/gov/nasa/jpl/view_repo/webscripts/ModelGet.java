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

        Boolean isCommit = req.getParameter("commitId") != null;
        try {
            if (isCommit) {
                JSONArray commitJsonToArray = new JSONArray();
                JSONObject commitJson = handleCommitRequest(req, top);
                commitJsonToArray.put(commitJson);
                if (commitJson.length() > 0) {
                    top.put(Sjm.ELEMENTS, filterByPermission(commitJsonToArray, req));
                    //top.put(Sjm.ELEMENTS, commitJsonToArray);
                }
            } else {
                JSONArray elementsJson = handleRequest(req, top, NodeUtil.doGraphDb);
                if (elementsJson.length() > 0) {
                    top.put(Sjm.ELEMENTS, filterByPermission(elementsJson, req));
                    //top.put(Sjm.ELEMENTS, elementsJson);
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

        if (prettyPrint || accept.contains("webp")) {
            model.put("res", top.toString(4));
        } else {
            model.put("res", top);
        }

        return model;
    }

    protected Map<String, Object> handleArtifactGet(WebScriptRequest req, Status status, String accept) {

        JSONObject resultJson = null;
        Map<String, Object> model = new HashMap<>();

        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String cs = req.getParameter("cs");
        String extensionArg = accept.replaceFirst(".*/(\\w+).*", "$1");

        if (!extensionArg.startsWith(".")) {
            extensionArg = "." + extensionArg;
        }

        String extension = !extensionArg.equals("*") ? extensionArg : ".svg";  // Assume .svg if no extension provided
        String commitId = req.getParameter("commitId");
        Map<String, String> commitAndTimestamp = emsNodeUtil.getGuidAndTimestampFromElasticId(commitId);
        String timestamp = !Utils.isNullOrEmpty(commitAndTimestamp) ? commitAndTimestamp.get(Sjm.TIMESTAMP) : null;
        if (timestamp == null) {
            Date today = new Date();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            timestamp = df.format(today);
        }

        if (!Utils.isNullOrEmpty(extension) && !extension.startsWith(".")) {
            extension = "." + extension;
        }

        WorkspaceNode workspace = getWorkspace(req, AuthenticationUtil.getRunAsUser());

        if (validateRequest(req, status)) {

            try {
                String[] idKeys = {"modelid", "elementid", "elementId"};
                String artifactIdPath = null;
                for (String idKey : idKeys) {
                    artifactIdPath = req.getServiceMatch().getTemplateVars().get(idKey);
                    if (artifactIdPath != null) {
                        break;
                    }
                }
                // Get the artifact name from the url:

                if (artifactIdPath != null) {
                    int lastIndex = artifactIdPath.lastIndexOf("/");

                    if (artifactIdPath.length() > (lastIndex + 1)) {

                        String artifactId = lastIndex != -1 ? artifactIdPath.substring(lastIndex + 1) : artifactIdPath;
                        String filename = artifactId + extension;

                        EmsScriptNode matchingNode = null;

                        // Search for artifact file by checksum (this may return nodes in parent workspaces):
                        if (!Utils.isNullOrEmpty(cs)) {
                            ArrayList<NodeRef> refs = NodeUtil
                                .findNodeRefsByType("" + cs, SearchType.CHECKSUM.prefix, false, workspace,
                                    TimeUtils.dateFromTimestamp(timestamp), false, false, services, false);
                            List<EmsScriptNode> nodeList =
                                EmsScriptNode.toEmsScriptNodeList(refs, services, response, status);

                            // Find the first node with matching name (just in case there is multiple artifacts with
                            // the same checksum but different names):
                            for (EmsScriptNode node : nodeList) {

                                if (node.getSysmlId().equals(filename)) {
                                    matchingNode = node;
                                    break;
                                }
                            }
                        } else {
                            // Otherwise, search by the id (this may return nodes in parent workspaces):
                            matchingNode = NodeUtil
                                .findScriptNodeById(filename, workspace, TimeUtils.dateFromTimestamp(timestamp), false,
                                    services, response);
                        }

                        // Create return json if matching node found:
                        if (matchingNode != null) {

                            resultJson = new JSONObject();
                            JSONArray jsonArray = new JSONArray();
                            JSONObject jsonArtifact = new JSONObject();
                            resultJson.put("artifacts", jsonArray);
                            jsonArtifact.put("id", matchingNode.getSysmlId());
                            String url = matchingNode.getUrl();
                            if (url != null) {
                                jsonArtifact.put("url", url.replace("/d/d/", "/service/api/node/content/"));
                            }
                            jsonArray.put(jsonArtifact);
                        } else {
                            String fileStr = "File " + filename;
                            String err = Utils.isNullOrEmpty(cs) ?
                                fileStr + " not found!\n" :
                                (fileStr + " with cs=" + cs + " not found!\n");
                            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, err);
                            model.put("res", createResponseJson());
                        }

                    } else {
                        log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Invalid artifactId!\n");
                        model.put("res", createResponseJson());
                    }

                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "artifactId not supplied!\n");
                    model.put("res", createResponseJson());
                }

            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Issues creating return JSON\n");
                e.printStackTrace();
                model.put("res", createResponseJson());
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid request!\n");
            model.put("res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        if (!model.containsKey("res")) {
            model.put("res", resultJson != null ? resultJson : createResponseJson());
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
            String[] idKeys = {"modelid", "elementid", "elementId"};
            String modelId = null;
            for (String idKey : idKeys) {
                modelId = req.getServiceMatch().getTemplateVars().get(idKey);
                if (modelId != null) {
                    break;
                }
            }

            if (null == modelId) {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s", modelId);
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
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        String elementId = req.getServiceMatch().getTemplateVars().get("elementId");
        String currentElement = emsNodeUtil.getById(elementId).getElasticId();
        // This is the commit of the version of the element we want
        String commitId = (req.getParameter("commitId").isEmpty()) ?
            emsNodeUtil.getById(elementId).getLastCommit() :
            req.getParameter("commitId");
        // This is the lastest commit for the element
        String lastestCommitId = emsNodeUtil.getById(elementId).getLastCommit();

        Boolean checkInProjectAndRef = emsNodeUtil.commitContainsElement(elementId, commitId);

        if ((req.getParameter("commitId").isEmpty() && checkInProjectAndRef) || (commitId.equals(lastestCommitId))) {
            return emsNodeUtil.getElementByElasticID(currentElement);
        } else if (checkInProjectAndRef) {
            return emsNodeUtil.getElementByElementAndCommitId(commitId, elementId);
        } else {
            return new JSONObject();
        }
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

    /**
     * Recurse a view hierarchy to get all allowed elements
     *
     * @param rootSysmlid Root view to find elements for
     * @param req   WebScriptRequest
     * @return JSONArray of elements
     * @throws JSONException JSON element creation error
     * @throws SQLException  SQL error
     * @throws IOException   IO error
     */

    protected JSONArray handleElementHierarchy(String rootSysmlid, WebScriptRequest req) throws JSONException, SQLException, IOException {
        // get timestamp if specified
        String commitId = req.getParameter("commitId");
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        Long depth = getDepthFromRequest(req);

        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        JSONArray tmpElements = emsNodeUtil.getChildren(rootSysmlid, depth);

        JSONArray elasticElements = extended ? emsNodeUtil.addExtendedInformation(tmpElements) : tmpElements;
        JSONArray elasticElementsCleaned = new JSONArray();

        for (int i = 0; i < elasticElements.length(); i++) {
            JSONObject o = elasticElements.getJSONObject(i);
            elasticElementsCleaned.put(o);
        }

        return elasticElementsCleaned;
    }

    /*
     * This method searched through the specified results and compare
     * the sysmlIds with the specified input parameter. If a match is
     * found, that JSONObject is returned; otherwise, a null JSONObject
     * is returned.
     *
     * NOTE: only the "updated" and "added" elements need to be searched
     * since all "deleted" and "moved" elements are listed as "updated".
     */
    private JSONObject findInResults(EmsNodeUtil emsNodeUtil, JSONObject results, String sysmlId) {
        JSONObject resObj = null;
        JSONObject jObj = null;
        boolean fResFound = false;

        JSONArray jArray = results.getJSONArray("updated");
        for (int i = 0; i < jArray.length(); i++) {
            jObj = jArray.getJSONObject(i);
            if (jObj != null) {
                String sID = jObj.getString(Sjm.SYSMLID);
                if (sID.equalsIgnoreCase(sysmlId)) {
                    String elasticID = jObj.getString(Sjm.ELASTICID);
                    System.err.println("Found SysmlID=" + sID + ", elasticID=" + elasticID);
                    resObj = emsNodeUtil.getCommit(elasticID);
                    break;
                }
            }
        }

        jArray = results.getJSONArray("added");
        for (int j = 0; j < jArray.length(); j++) {
            jObj = jArray.getJSONObject(j);
            if (jObj != null) {
                String sID = jObj.getString(Sjm.SYSMLID);
                if (sID.equalsIgnoreCase(sysmlId)) {
                    String elasticID = jObj.getString(Sjm.ELASTICID);
                    System.err.println("Found SysmlID=" + sID + ", elasticID=" + elasticID);
                    resObj = emsNodeUtil.getCommit(elasticID);
                    break;
                }
            }
        }

        return resObj;
    }

}
