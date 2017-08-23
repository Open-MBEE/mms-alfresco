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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
import gov.nasa.jpl.view_repo.webscripts.util.SitePermission;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Debug;
import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;

/**
 * @author johnli
 */

public class WorkspacesPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(WorkspacesPost.class);

    public WorkspacesPost() {
        super();
    }

    public WorkspacesPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        return checkRequestContent(req) && SitePermission.hasPermissionToBranch(orgId, projectId, refId);
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        WorkspacesPost instance = new WorkspacesPost(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        int statusCode = HttpServletResponse.SC_OK;
        JSONObject json = null;
        try {
            if (validateRequest(req, status)) {
                JSONObject reqJson = (JSONObject) req.parseContent();
                String projectId = getProjectId(req);
                String sourceWorkspaceParam = reqJson.getJSONArray("refs").getJSONObject(0).optString("parentRefId");
                String newName = reqJson.getJSONArray("refs").getJSONObject(0).optString("name");
                String commitId =
                    reqJson.getJSONArray("refs").getJSONObject(0).optString("parentCommitId", null) != null ?
                        reqJson.getJSONArray("refs").getJSONObject(0).optString("parentCommitId") :
                        req.getParameter("commitId");

                json = createWorkSpace(projectId, sourceWorkspaceParam, newName, commitId, reqJson, user, status);
                statusCode = status.getCode();
            } else {
                statusCode = HttpServletResponse.SC_FORBIDDEN;
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "JSON malformed\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal stack trace:\n %s \n",
                e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (json == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    json.put("message", response.toString());
                }
                JSONObject resultRefs = new JSONObject();
                JSONArray refsList = new JSONArray();
                refsList.put(json);
                resultRefs.put("refs", refsList);
                model.put(Sjm.RES, resultRefs);
            } catch (JSONException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put(Sjm.RES, createResponseJson());
            }
        }
        status.setCode(statusCode);

        printFooter(user, logger, timer);

        return model;
    }

    public JSONObject createWorkSpace(String projectId, String sourceWorkId, String newWorkName, String commitId,
        JSONObject jsonObject, String user, Status status) throws JSONException {
        status.setCode(HttpServletResponse.SC_OK);
        if (Debug.isOn()) {
            Debug.outln(
                "createWorkSpace(sourceWorkId=" + sourceWorkId + ", newWorkName=" + newWorkName + ", commitId=" + commitId
                    + ", jsonObject=" + jsonObject + ", user=" + user + ", status=" + status + ")");
        }

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, sourceWorkId == null ? NO_WORKSPACE_ID : sourceWorkId);
        JSONObject project = emsNodeUtil.getProject(projectId);
        EmsScriptNode orgNode = getSiteNode(project.optString("orgId"));
        String date = TimeUtils.toTimestamp(new Date().getTime());

        if (orgNode == null) {
            log(Level.WARN, "Site Not Found", HttpServletResponse.SC_NOT_FOUND);
            status.setCode(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        String sourceWorkspaceId = null;
        String newWorkspaceId = null;
        JSONObject wsJson = new JSONObject();
        JSONObject srcJson = new JSONObject();
        String workspaceName = null;
        String desc = null;
        String elasticId = null;
        Boolean isTag = false;
        String permission = "read";  // Default is public read permission
        EmsScriptNode finalWorkspace = null;

        // If the workspace is supplied in the json object then get all parameters from there
        // and ignore any URL parameters:
        if (jsonObject != null) {

            JSONArray jarr = jsonObject.getJSONArray("refs");
            wsJson = jarr.getJSONObject(0);  // Will only post/update one workspace
            sourceWorkspaceId = wsJson.optString("parentRefId", null);
            srcJson = emsNodeUtil.getRefJson(sourceWorkspaceId);
            newWorkspaceId = wsJson.optString("id", null); // alfresco id of workspace node

            workspaceName = wsJson.optString("name", null); // user or auto-generated name, ems:workspace_name
            isTag = wsJson.optString("type", "Branch").equals("Tag");
            desc = wsJson.optString("description", null);
            permission = wsJson.optString("permission", "read");  // "read" or "write"
        } else {
            sourceWorkspaceId = sourceWorkId;
            workspaceName = newWorkName;   // The name is given on the URL typically, not the ID
        }

        if ((newWorkspaceId != null && newWorkspaceId.equals(NO_WORKSPACE_ID)) || (workspaceName != null
            && workspaceName.equals(NO_WORKSPACE_ID))) {
            log(Level.WARN, "Cannot change attributes of the master workspace.", HttpServletResponse.SC_BAD_REQUEST);
            status.setCode(HttpServletResponse.SC_BAD_REQUEST);
            return null;
        }

        // Only create the workspace if the workspace id was not supplied:
        EmsScriptNode existingRef =
            orgNode.childByNamePath("/" + projectId + "/refs/" + newWorkspaceId);
        if (newWorkspaceId == null || existingRef == null) {

            EmsScriptNode srcWs =
                orgNode.childByNamePath("/" + projectId + "/refs/" + sourceWorkspaceId);

            if (newWorkspaceId == null) {
                newWorkspaceId =
                    workspaceName.toLowerCase().replace("-", "_").replaceAll("\\s+", "").replaceAll("[^A-Za-z0-9]", "");
            }

            wsJson.put(Sjm.SYSMLID, newWorkspaceId);
            wsJson.put(Sjm.NAME, workspaceName);
            wsJson.put(Sjm.COMMITID, commitId == null || commitId.isEmpty() ? emsNodeUtil.getHeadCommit() : commitId);
            wsJson.put(Sjm.CREATED, date);
            wsJson.put(Sjm.CREATOR, user);
            wsJson.put(Sjm.MODIFIED, date);
            wsJson.put(Sjm.MODIFIER, user);
            wsJson.put("status", "creating");
            elasticId = emsNodeUtil.insertSingleElastic(wsJson);

            if (!NO_WORKSPACE_ID.equals(sourceWorkspaceId) && srcWs.getId() == null) {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Source workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } else {
                EmsScriptNode refContainerNode = orgNode.childByNamePath("/" + projectId + "/refs");
                EmsScriptNode dstWs = refContainerNode.createFolder(newWorkspaceId);

                if (dstWs != null) {
                    // keep history of the branch
                    String srcId = srcWs.getName().equals(NO_WORKSPACE_ID) ? NO_WORKSPACE_ID : srcWs.getId();
                    dstWs.setProperty("cm:title", dstWs.getId() + "_" + srcId);
                    dstWs.setProperty("cm:name", dstWs.getName());

                    dstWs.addAspect("ems:HasWorkspace");
                    dstWs.setProperty("ems:workspace", dstWs.getNodeRef());

                    dstWs.addAspect("ems:Workspace");
                    dstWs.setProperty("ems:workspace_name", newWorkspaceId);

                    CommitUtil.sendBranch(projectId, srcJson, wsJson, elasticId, isTag,
                        jsonObject != null ? jsonObject.optString("source") : null, commitId);

                    finalWorkspace = dstWs;
                }
            }
        } else {
            // Workspace was found, so update it:
            if (existingRef.getId() != null) {

                if (existingRef.isDeleted()) {

                    existingRef.removeAspect("ems:Deleted");
                    log(Level.INFO, HttpServletResponse.SC_OK, "Workspace undeleted and modified");

                } else {
                    log(Level.INFO, "Workspace is modified", HttpServletResponse.SC_OK);
                }

                // Update the name/description:
                // Note: allowing duplicate workspace names, so no need to check for other
                //       refs with the same name
                if (workspaceName != null) {
                    existingRef.createOrUpdateProperty("ems:workspace_name", workspaceName);
                }
                if (desc != null) {
                    existingRef.createOrUpdateProperty("ems:description", desc);
                }
                finalWorkspace = existingRef;
            } else {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }

            wsJson.put(Sjm.MODIFIED, date);
            wsJson.put(Sjm.MODIFIER, user);
            elasticId = emsNodeUtil.insertSingleElastic(wsJson);
            emsNodeUtil.updateRef(newWorkspaceId, workspaceName, elasticId, isTag);
        }

        // Finally, apply the permissions:
        if (finalWorkspace != null) {
            finalWorkspace.setPermission("SiteManager", user);
            if (permission.equals("write")) {
                finalWorkspace.setPermission("SiteCollaborator", "GROUP_EVERYONE");
            } else {
                finalWorkspace.setPermission("SiteConsumer", "GROUP_EVERYONE");
            }
            finalWorkspace.createOrUpdateProperty("ems:permission", permission);
        }

        return wsJson;
    }

}
