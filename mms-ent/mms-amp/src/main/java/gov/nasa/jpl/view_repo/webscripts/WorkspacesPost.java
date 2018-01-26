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

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.webscripts.util.SitePermission;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

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
        JsonObject json = null;
        JsonParser parser = new JsonParser();
        try {
            if (validateRequest(req, status)) {
                JsonElement reqJsonElement = parser.parse(req.getContent().getContent());
                JsonObject reqJson = reqJsonElement.isJsonNull() ? new JsonObject() :
                    reqJsonElement.getAsJsonObject();
                String projectId = getProjectId(req);
                JsonObject ozero = reqJson.get("refs").getAsJsonArray().get(0).getAsJsonObject();
                String sourceWorkspaceParam = JsonUtil.getOptString(ozero, "parentRefId");
                String newName = JsonUtil.getOptString(ozero, "name");
                String commitId = !JsonUtil.getOptString(ozero, "parentCommitId").equals("") ?
                        JsonUtil.getOptString(ozero, "parentCommitId") :
                        req.getParameter("commitId");

                json = createWorkSpace(projectId, sourceWorkspaceParam, newName, commitId, reqJson, user, status);
                statusCode = status.getCode();
            } else {
                statusCode = HttpServletResponse.SC_FORBIDDEN;
            }
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }
        if (json == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    json.addProperty("message", response.toString());
                }
                JsonObject resultRefs = new JsonObject();
                JsonArray refsList = new JsonArray();
                refsList.add(json);
                resultRefs.add("refs", refsList);
                model.put(Sjm.RES, resultRefs);
            } catch (JsonParseException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put(Sjm.RES, createResponseJson());
            }
        }
        status.setCode(statusCode);

        printFooter(user, logger, timer);

        return model;
    }

    public JsonObject createWorkSpace(String projectId, String sourceWorkId, String newWorkName, String commitId,
        JsonObject jsonObject, String user, Status status) throws JsonParseException {
        status.setCode(HttpServletResponse.SC_OK);
        if (Debug.isOn()) {
            Debug.outln(
                "createWorkSpace(sourceWorkId=" + sourceWorkId + ", newWorkName=" + newWorkName + ", commitId=" + commitId
                    + ", jsonObject=" + jsonObject + ", user=" + user + ", status=" + status + ")");
        }

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, sourceWorkId == null ? NO_WORKSPACE_ID : sourceWorkId);
        JsonObject project = emsNodeUtil.getProject(projectId);
        EmsScriptNode orgNode = getSiteNode(JsonUtil.getOptString(project, "orgId"));
        String date = TimeUtils.toTimestamp(new Date().getTime());

        if (orgNode == null) {
            log(Level.WARN, "Site Not Found", HttpServletResponse.SC_NOT_FOUND);
            status.setCode(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        String sourceWorkspaceId = null;
        String newWorkspaceId = null;
        JsonObject wsJson = new JsonObject();
        JsonObject srcJson = new JsonObject();
        String workspaceName = null;
        String desc = null;
        String elasticId = null;
        Boolean isTag = false;
        String permission = "read";  // Default is public read permission
        EmsScriptNode finalWorkspace = null;

        // If the workspace is supplied in the json object then get all parameters from there
        // and ignore any URL parameters:
        if (jsonObject != null) {

            JsonArray jarr = jsonObject.get("refs").getAsJsonArray();
            wsJson = jarr.get(0).getAsJsonObject();  // Will only post/update one workspace
            sourceWorkspaceId = JsonUtil.getOptString(wsJson, "parentRefId");
            srcJson = emsNodeUtil.getRefJson(sourceWorkspaceId);
            newWorkspaceId = JsonUtil.getOptString(wsJson, "id"); // alfresco id of workspace node

            workspaceName = JsonUtil.getOptString(wsJson, "name"); // user or auto-generated name, ems:workspace_name
            isTag = JsonUtil.getOptString(wsJson, "type", "Branch").equals("Tag");
            desc = JsonUtil.getOptString(wsJson, "description");
            permission = JsonUtil.getOptString(wsJson, "permission", "read");  // "read" or "write"
        } else {
            sourceWorkspaceId = sourceWorkId;
            workspaceName = newWorkName;   // The name is given on the URL typically, not the ID
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

            wsJson.addProperty(Sjm.SYSMLID, newWorkspaceId);
            wsJson.addProperty(Sjm.NAME, workspaceName);
            wsJson.addProperty(Sjm.COMMITID, commitId == null || commitId.isEmpty() ? emsNodeUtil.getHeadCommit() : commitId);
            wsJson.addProperty(Sjm.CREATED, date);
            wsJson.addProperty(Sjm.CREATOR, user);
            wsJson.addProperty(Sjm.MODIFIED, date);
            wsJson.addProperty(Sjm.MODIFIER, user);
            wsJson.addProperty("status", "creating");
            elasticId = emsNodeUtil.insertSingleElastic(wsJson);

            if (!NO_WORKSPACE_ID.equals(sourceWorkspaceId) && srcWs.getId() == null) {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Source workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } else {
                EmsScriptNode refContainerNode = orgNode.childByNamePath("/" + projectId + "/refs");
                EmsScriptNode dstWs = refContainerNode.createFolder(newWorkspaceId, null, null);

                if (dstWs != null) {
                    // keep history of the branch
                    String srcId = srcWs.getName().equals(NO_WORKSPACE_ID) ? NO_WORKSPACE_ID : srcWs.getId();
                    dstWs.setProperty("cm:title", dstWs.getId() + "_" + srcId);
                    dstWs.setProperty("cm:name", dstWs.getName());

                    CommitUtil.sendBranch(projectId, srcJson, wsJson, elasticId, isTag,
                        jsonObject != null ? JsonUtil.getOptString(jsonObject, "source") : null, commitId, services);

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
                finalWorkspace = existingRef;
            } else {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }

            wsJson.addProperty(Sjm.MODIFIED, date);
            wsJson.addProperty(Sjm.MODIFIER, user);
            elasticId = emsNodeUtil.insertSingleElastic(wsJson);
            emsNodeUtil.updateRef(newWorkspaceId, workspaceName, elasticId, isTag);
        }

        // Finally, apply the permissions:
        if (finalWorkspace != null) {
            finalWorkspace.setPermission("SiteManager", user);
            if (permission.equals("write")) {
                finalWorkspace.setPermission("SiteCollaborator", "GROUP_EVERYONE");
            } else {
                finalWorkspace.setPermission("SiteConsumer", String.format("Site_%s_SiteConsumer",orgNode.getName()));
            }
        }

        return wsJson;
    }

}
