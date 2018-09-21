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

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author han
 */
public class ProjectPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProjectPost.class);

    public ProjectPost() {
        super();
    }

    public ProjectPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    private static final String REF_PATH = "refs";
    private static final String REF_PATH_SEARCH = "/" + REF_PATH;

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

        JsonArray success = new JsonArray();
        JsonArray failure = new JsonArray();

        try {
            if (validateRequest(req, status)) {

                JsonObject json = JsonUtil.buildFromString(req.getContent().getContent());
                JsonArray elementsArray = JsonUtil.getOptArray(json, "projects");
                String orgId = getOrgId(req);

                if (elementsArray.size() > 0) {
                    for (int i = 0; i < elementsArray.size(); i++) {
                        JsonObject projJson = elementsArray.get(i).getAsJsonObject();
                        String projectId = projJson.has(Sjm.SYSMLID) ? projJson.get(Sjm.SYSMLID).getAsString() : getProjectId(req);
                        if (validateProjectId(projectId)) {

                            if (orgId == null) {
                                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
                                orgId = emsNodeUtil.getOrganizationFromProject(projectId);
                            }

                            EmsScriptNode siteNode = getSiteNode(orgId);
                            if (siteNode != null) {
                                if (siteNode.checkPermissions(PermissionService.WRITE)) {
                                    CommitUtil.sendProjectDelta(projJson, orgId, user);
                                    if (projectId != null && !projectId.equals(NO_SITE_ID)) {
                                        responseStatus.setCode(updateOrCreateProject(projJson, projectId, orgId));
                                    } else {
                                        responseStatus.setCode(checkNoProject(siteNode));
                                    }
                                } else {
                                    responseStatus.setCode(HttpServletResponse.SC_FORBIDDEN);
                                }

                                if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
                                    success.add(projJson);
                                } else {
                                    failure.add(projJson);
                                }
                            } else {
                                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
                                // This should not happen, since the Organization should be created before a Project is posted
                                if (emsNodeUtil.orgExists(orgId)) {
                                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied");
                                } else {
                                    log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Organization does not exist");
                                }
                                failure.add(projJson);
                            }
                        } else {
                            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                                String.format("Invalid Project Id '%s' from client", projectId));
                            failure.add(projJson);
                        }
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request");
                }
            }
        } catch (IllegalStateException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "unable to get JSON object from request", e);
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        JsonObject response = new JsonObject();
        response.add(Sjm.PROJECTS, success);

        if (failure.size() > 0) {
            response.add("failed", failure);
        }

        status.setCode(responseStatus.getCode());

        if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
            model.put(Sjm.RES, response);
        } else {
            model.put(Sjm.RES, createResponseJson());
        }

        printFooter(user, logger, timer);

        return model;
    }

    public int checkNoProject(EmsScriptNode projectNode) {
        if (projectNode == null) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find project");
            return HttpServletResponse.SC_NOT_FOUND;
        }

        if (checkPermissions(projectNode, PermissionService.WRITE)) {
            log(Level.INFO, HttpServletResponse.SC_OK, "Project metadata updated.");
        }

        return HttpServletResponse.SC_OK;
    }

    /**
     * Update or create the project specified by the JSONObject
     *
     * @param jsonObject JSONObject that has the name of the project
     * @param projectId  Project ID
     * @return HttpStatusResponse code for success of the POST request
     */
    public int updateOrCreateProject(JsonObject jsonObject, String projectId, String orgId) {
        // see if project exists for workspace

        // make sure Model package under site exists
        SiteInfo orgInfo = services.getSiteService().getSite(orgId);
        if (orgInfo != null) {
            EmsScriptNode site = new EmsScriptNode(orgInfo.getNodeRef(), services);

            if (!checkPermissions(site, "Write")) {
                return HttpServletResponse.SC_FORBIDDEN;
            }

            EmsScriptNode projectContainerNode = site.childByNamePath(projectId);
            if (projectContainerNode == null) {
                projectContainerNode = site.createFolder(projectId, null);
                projectContainerNode.setProperty(Acm.CM_TITLE, JsonUtil.getOptString(jsonObject, Sjm.NAME));
                log(Level.INFO, HttpServletResponse.SC_OK, "Project folder created.\n");
            }

            EmsScriptNode documentLibrary = site.childByNamePath("documentLibrary");
            if (documentLibrary == null) {
                documentLibrary = site.createFolder("documentLibrary", null);
            }

            EmsScriptNode documentProjectContainer = documentLibrary.childByNamePath(projectId);
            if (documentProjectContainer == null) {
                documentProjectContainer = documentLibrary.createFolder(projectId, null);
                documentProjectContainer.setProperty(Acm.CM_TITLE, JsonUtil.getOptString(jsonObject, Sjm.NAME));
            }

            EmsScriptNode refContainerNode = projectContainerNode.childByNamePath(REF_PATH_SEARCH);
            if (refContainerNode == null) {
                refContainerNode = projectContainerNode.createFolder("refs", null);
            }

            EmsScriptNode branch = refContainerNode.childByNamePath(NO_WORKSPACE_ID);
            if (branch == null) {
                branch = refContainerNode.createFolder(NO_WORKSPACE_ID, null);
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
                JsonObject masterWs = new JsonObject();
                masterWs.addProperty("id", NO_WORKSPACE_ID);
                masterWs.addProperty("name", NO_WORKSPACE_ID);
                masterWs.addProperty("type", "Branch");
                // :TODO going to have to check that index doesn't exist if ES doesn't already do this
                emsNodeUtil.insertProjectIndex(projectId);
                String elasticId = emsNodeUtil.insertSingleElastic(masterWs, ElasticHelper.REF);
                emsNodeUtil.insertRef(NO_WORKSPACE_ID, NO_WORKSPACE_ID, elasticId, false);
            }

            if (branch == null) {
                log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST, "Projects must be created in master workspace.");
                return HttpServletResponse.SC_BAD_REQUEST;
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

    /**
     * Check project id for validity
     */
    protected boolean validateProjectId(String projectId) {
        Pattern p = Pattern.compile("^[\\w-]+$");
        Matcher m = p.matcher(projectId);
        return m.matches();
    }
}
