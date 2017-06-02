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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.webscripts.util.ConfigurationsWebscript;
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

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;

/**
 *
 * @author cinyoung
 */
public class ProjectPost extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(ProjectPost.class);

    public ProjectPost() {
        super();
    }

    public ProjectPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    private final String MODEL_PATH = "Models";
    private final String MODEL_PATH_SEARCH = "/" + MODEL_PATH;

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProjectPost instance = new ProjectPost(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        int statusCode = HttpServletResponse.SC_OK;

        try {
            if (validateRequest(req, status)) {

                JSONObject json = (JSONObject) req.parseContent();
                JSONArray elementsArray = json != null ? json.optJSONArray(Sjm.ELEMENTS) : null;
                JSONObject projJson = (elementsArray != null && elementsArray.length() > 0) ?
                                elementsArray.getJSONObject(0) :
                                new JSONObject();

                // We are now getting the project id form the json object, but
                // leaving the check from the request
                // for backwards compatibility:
                String siteName = getSiteName(req);
                String projectId = projJson.has(Acm.JSON_ID) ?
                                projJson.getString(Acm.JSON_ID) :
                                getProjectId(req, siteName);

                boolean delete = getBooleanArg(req, "delete", false);
                boolean createSite = getBooleanArg(req, "createSite", false);

                WorkspaceNode workspace = getWorkspace(req);

                SiteInfo siteInfo = services.getSiteService().getSite(siteName);
                if (siteInfo == null) {
                    String sitePreset = "site-dashboard";
                    String siteTitle = (json != null && json.has(Sjm.NAME)) ? json.getString(Sjm.NAME) : siteName;
                    String siteDescription = (json != null) ? json.optString(Sjm.DESCRIPTION) : "";
                    if (!ShareUtils.constructSiteDashboard(sitePreset, siteName, siteTitle, siteDescription, false)) {
                        // TODO: Add some logging for failed site creation
                        log("Site was not created");
                    } else {
                        log(Level.INFO, HttpServletResponse.SC_OK, "Model folder created.\n");
                        statusCode = 200;
                    }
                }

                if (siteName != null && !siteName.equals(NO_SITE_ID)) {
                    statusCode = updateOrCreateProject(projJson, workspace, projectId, siteName, createSite, delete);
                } else {
                    statusCode = updateOrCreateProject(projJson, workspace, projectId);
                }

                CommitUtil.sendProjectDelta(projJson, siteName, user);
            } else {
                statusCode = responseStatus.getCode();
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n",
                            e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(statusCode);
        model.put("res", createResponseJson());

        printFooter(user, logger, timer);

        return model;
    }

    public int updateOrCreateProject(JSONObject jsonObject, WorkspaceNode workspace, String projectId)
                    throws JSONException {
        EmsScriptNode projectNode = findScriptNodeById(projectId, workspace, null, true);

        if (projectNode == null) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find project\n");
            return HttpServletResponse.SC_NOT_FOUND;
        }

        String projectName = null;
        if (jsonObject.has(Acm.JSON_NAME)) {
            projectName = jsonObject.getString(Acm.JSON_NAME);
        }
        String projectVersion = null;
        if (jsonObject.has(Acm.JSON_SPECIALIZATION)) {
            JSONObject specialization = jsonObject.getJSONObject(Acm.JSON_SPECIALIZATION);
            if (specialization != null && specialization.has(Acm.JSON_PROJECT_VERSION)) {
                projectVersion = specialization.getString(Acm.JSON_PROJECT_VERSION);
            }
        }
        if (checkPermissions(projectNode, PermissionService.WRITE)) {
            String oldId = (String) projectNode.getProperty(Acm.ACM_ID);
            boolean idChanged = !projectId.equals(oldId);
            if (idChanged) {
                projectNode.createOrUpdateProperty(Acm.ACM_ID, projectId);
            }
            projectNode.createOrUpdateProperty(Acm.ACM_TYPE, "Project");
            boolean nameChanged = false;
            if (projectName != null) {
                projectNode.createOrUpdateProperty(Acm.CM_TITLE, projectName);
                String oldName = (String) projectNode.getProperty(Acm.ACM_NAME);
                nameChanged = !projectName.equals(oldName);
                if (nameChanged) {
                    projectNode.createOrUpdateProperty(Acm.ACM_NAME, projectName);
                }
            }
            if (idChanged || nameChanged) {
                projectNode.removeChildrenFromJsonCache(true);
            }
            if (projectVersion != null) {
                projectNode.createOrUpdateProperty(Acm.ACM_PROJECT_VERSION, projectVersion);
            }
            log(Level.INFO, HttpServletResponse.SC_OK, "Project metadata updated.\n");
        }

        return HttpServletResponse.SC_OK;
    }

    /**
     * Update or create the project specified by the JSONObject
     *
     * @param jsonObject JSONObject that has the name of the project
     * @param projectId  Project ID
     * @param siteName   Site project should reside in
     * @return HttpStatusResponse code for success of the POST request
     * @throws JSONException
     */
    public int updateOrCreateProject(JSONObject jsonObject, WorkspaceNode workspace, String projectId, String siteName,
                    boolean createSite, boolean delete) throws JSONException {
        // see if project exists for workspace
        EmsScriptNode projectNodeAll = findScriptNodeById(projectId, workspace, null, true, siteName);
        EmsScriptNode projectNode =
                        (projectNodeAll != null && NodeUtil.workspacesEqual(projectNodeAll.getWorkspace(), workspace)) ?
                                        projectNodeAll :
                                        null;

        boolean idChanged = false;
        boolean nameChanged = false;

        // only create site, models, and projects directory in master workspace
        if (workspace == null) {
            EmsScriptNode siteNode = getSiteNode(siteName, workspace, null);
            // make sure Model package under site exists
            EmsScriptNode modelContainerNode = null;
            if (siteNode != null) {
                modelContainerNode = siteNode.childByNamePath(MODEL_PATH_SEARCH, false, workspace, true);

                if (!checkPermissions(siteNode, "Write")) {
                    return HttpServletResponse.SC_FORBIDDEN;
                }
                if (modelContainerNode == null) {
                    modelContainerNode = siteNode.createFolder("Models");
                    if (modelContainerNode != null)
                        modelContainerNode.getOrSetCachedVersion();
                    siteNode.getOrSetCachedVersion();
                    log(Level.INFO, HttpServletResponse.SC_OK, "Model folder created.\n");
                }

                if (projectNode == null) {
                    projectNode = modelContainerNode.createFolder(projectId, Acm.ACM_PROJECT,
                                    projectNodeAll != null ? projectNodeAll.getNodeRef() : null);
                    modelContainerNode.getOrSetCachedVersion();
                }


                // if project node is still null here, means we're in branch and it
                // doesn't exist in master
                if (projectNode == null && workspace != null) {
                    EmsScriptNode pnode = findScriptNodeById(projectId, workspace.getParentWorkspace(), null, true, siteName);
                    //            if (pnode != null) {
                    //                projectNode = workspace.replicateWithParentFolders(pnode);
                    //            }
                }

                if (projectNode == null) {
                    log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST, "Projects must be created in master workspace.\n");
                    return HttpServletResponse.SC_BAD_REQUEST;
                }

                if (delete) {
                    projectNode.makeSureNodeRefIsNotFrozen();
                    projectNode.remove();
                    log(Level.INFO, "Project deleted.\n", HttpServletResponse.SC_OK);
                } else {
                    if (checkPermissions(projectNode, PermissionService.WRITE)) {
                        String projectName = null;
                        if (jsonObject.has(Acm.JSON_NAME)) {
                            projectName = jsonObject.getString(Acm.JSON_NAME);
                        }
                        String projectVersion = null;
                        if (jsonObject.has(Acm.JSON_SPECIALIZATION)) {
                            JSONObject specialization = jsonObject.getJSONObject(Acm.JSON_SPECIALIZATION);
                            if (specialization != null && specialization.has(Acm.JSON_PROJECT_VERSION)) {
                                projectVersion = specialization.getString(Acm.JSON_PROJECT_VERSION);
                            }
                        }

                        idChanged = projectNode.createOrUpdateProperty(Acm.ACM_ID, projectId);
                        projectNode.createOrUpdateProperty(Acm.ACM_TYPE, "Project");
                        if (projectName != null) {
                            projectNode.createOrUpdateProperty(Acm.CM_TITLE, projectName);
                            nameChanged = projectNode.createOrUpdateProperty(Acm.ACM_NAME, projectName);
                        }
                        if (projectVersion != null) {
                            projectNode.createOrUpdateProperty(Acm.ACM_PROJECT_VERSION, projectVersion);
                        }
                        log(Level.INFO, "Project metadata updated.\n", HttpServletResponse.SC_OK);

                    }
                }
                if (idChanged || nameChanged) {
                    projectNode.removeChildrenFromJsonCache(true);
                }
                projectNode.getOrSetCachedVersion();
            }
        }

        return HttpServletResponse.SC_OK;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }

}
