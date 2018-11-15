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

import gov.nasa.jpl.view_repo.db.DocStoreInterface;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

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

        JsonArray success = new JsonArray();
        JsonArray failure = new JsonArray();

        try {
            if (validateRequest(req, status)) {
                JsonObject reqJson = JsonUtil.buildFromString(req.getContent().getContent());
                String projectId = getProjectId(req);
                JsonArray refsArray = reqJson.get("refs").getAsJsonArray();
                if (refsArray != null && refsArray.size() > 0) {
                    for (int i = 0; i < refsArray.size(); i++) {
                    	JsonObject o = refsArray.get(i).getAsJsonObject();
                        String sourceWorkspaceParam = JsonUtil.getOptString(o, "parentRefId");
                        String newName = JsonUtil.getOptString(o, "name");
                        String commitId = !JsonUtil.getOptString(o, "parentCommitId").isEmpty() ?
                                JsonUtil.getOptString(o, "parentCommitId") : req.getParameter("commitId");

                        json = createWorkSpace(projectId, sourceWorkspaceParam, newName, commitId, o, user, status);
                        statusCode = status.getCode();
                        if (statusCode == HttpServletResponse.SC_OK) {
                            success.add(json);
                        } else {
                            failure.add(json);
                        }
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request");
                }
            } else {
                statusCode = HttpServletResponse.SC_FORBIDDEN;
            }
        } catch (IllegalStateException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Unable to get object from JSON request");
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }
        if (success.size() == 0 && failure.size() == 0) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    if (json == null) {
                        json = new JsonObject();
                    }
                    json.addProperty("message", response.toString());
                }
                JsonObject resultRefs = new JsonObject();
                resultRefs.add("refs", success);
                if (failure.size() > 0) {
                    resultRefs.add("failed", failure);
                }
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
        JsonObject jsonObject, String user, Status status) {
        status.setCode(HttpServletResponse.SC_OK);
        if (logger.isDebugEnabled()) {
            logger.debug(
                "createWorkSpace(sourceWorkId=" + sourceWorkId + ", newWorkName=" + newWorkName + ", commitId=" + commitId
                    + ", jsonObject=" + jsonObject + ", user=" + user + ", status=" + status + ")");
        }

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, sourceWorkId == null ? NO_WORKSPACE_ID : sourceWorkId);
        JsonObject project = emsNodeUtil.getProject(projectId);
        EmsScriptNode orgNode = getSiteNode(JsonUtil.getOptString(project, "orgId"));
        String date = TimeUtils.toTimestamp(new Date().getTime());

        if (orgNode == null) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Site Not Found");
            status.setCode(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }

        String sourceWorkspaceId = null;
        String newWorkspaceId = null;
        JsonObject wsJson = new JsonObject();
        JsonObject srcJson = new JsonObject();
        String workspaceName = null;
        String desc = null;
        JsonArray keywords = new JsonArray();
        JsonObject metadata = new JsonObject();
        String elasticId = null;
        boolean isTag = false;
        String permission = "read";  // Default is public read permission
        EmsScriptNode finalWorkspace = null;

        // If the workspace is supplied in the json object then get all parameters from there
        // and ignore any URL parameters:
        if (jsonObject != null) {

            wsJson = jsonObject;
            sourceWorkspaceId = JsonUtil.getOptString(wsJson, "parentRefId");
            srcJson = emsNodeUtil.getRefJson(sourceWorkspaceId);
            newWorkspaceId = JsonUtil.getOptString(wsJson, Sjm.SYSMLID, null); // alfresco id of workspace node

            workspaceName = JsonUtil.getOptString(wsJson, "name"); // user or auto-generated name, ems:workspace_name
            isTag = JsonUtil.getOptString(wsJson, "type", "Branch").equals("Tag");
            desc = JsonUtil.getOptString(wsJson, "description");
            keywords = JsonUtil.getOptArray(wsJson, "keywords");
            metadata = JsonUtil.getOptObject(wsJson, "metadata");
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
            if (desc != null && !desc.isEmpty()) {
                wsJson.addProperty(Sjm.DESCRIPTION, desc);
            }
            if (keywords.size() > 0) {
                wsJson.add(Sjm.KEYWORDS, keywords);
            }
            if (metadata.size() > 0) {
                wsJson.add(Sjm.METADATA, metadata);
            }
            wsJson.addProperty(Sjm.CREATED, date);
            wsJson.addProperty(Sjm.CREATOR, user);
            wsJson.addProperty(Sjm.MODIFIED, date);
            wsJson.addProperty(Sjm.MODIFIER, user);
            elasticId = emsNodeUtil.insertSingleElastic(wsJson, DocStoreInterface.REF);

            if (!NO_WORKSPACE_ID.equals(sourceWorkspaceId) && srcWs == null) {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Source workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } else {
                EmsScriptNode refContainerNode = orgNode.childByNamePath("/" + projectId + "/refs");
                EmsScriptNode dstWs = refContainerNode.createFolder(newWorkspaceId, null);

                if (dstWs != null && srcWs != null) {
                    // keep history of the branch
                    String srcId = srcWs.getName();
                    dstWs.setProperty("cm:title", newWorkspaceId + "_" + srcId);
                    dstWs.setProperty("cm:name", dstWs.getName());

                    CommitUtil.sendBranch(projectId, srcJson, wsJson, elasticId, isTag,
                        JsonUtil.getOptString(jsonObject, "source", null), commitId, services);

                    if (wsJson.get("status").getAsString().equalsIgnoreCase("rejected")) {
                        status.setCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    }

                    finalWorkspace = dstWs;
                }
            }
        } else {
            // Workspace was found, so update it:
            if (existingRef.getId() != null) {
                log(Level.INFO, HttpServletResponse.SC_OK, "Workspace is modified");
                finalWorkspace = existingRef;
            } else {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Workspace not found.");
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
                return null;
            }

            JsonObject existingWsJson = emsNodeUtil.getRefJson(newWorkspaceId);

            if (wsJson.get(Sjm.COMMITID) != null) {
                wsJson.remove(Sjm.COMMITID);
            }
            if (wsJson.get("parentRefId") != null) {
                wsJson.remove("parentRefId");
            }
            if (wsJson.get("parentCommitId") != null) {
                wsJson.remove("parentCommitId");
            }

            wsJson.addProperty(Sjm.MODIFIED, date);
            wsJson.addProperty(Sjm.MODIFIER, user);
            wsJson.addProperty(Sjm.ELASTICID, existingWsJson.get(Sjm.ELASTICID).getAsString());
            elasticId = emsNodeUtil.updateSingleElastic(wsJson, DocStoreInterface.REF);
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
