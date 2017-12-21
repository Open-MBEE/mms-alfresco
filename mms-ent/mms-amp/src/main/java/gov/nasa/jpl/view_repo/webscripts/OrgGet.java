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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
//import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.JSONObject;
import gov.nasa.jpl.view_repo.util.JSONArray;

/**
 * @author han
 */
public class OrgGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(OrgGet.class);

    public OrgGet() {
        super();
    }

    public OrgGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        OrgGet instance = new OrgGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        if (checkMmsVersions && compareMmsVersions(req, getResponse(), getResponseStatus())) {
            model.put(Sjm.RES, createResponseJson());
            return model;
        }
        JSONObject json = null;
        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        try {
            if (validateRequest(req, status)) {
                String orgId = getOrgId(req);
                Boolean projects = req.getPathInfo().contains(Sjm.PROJECTS);
                JSONArray jsonArray;

                if (projects) {
                    String projectId = getProjectId(req);

                    if (projectId != null) {
                        jsonArray = handleProject(projectId);
                        json = new JSONObject();
                        json.put(Sjm.PROJECTS, filterProjectByPermission(jsonArray));
                        if (jsonArray.length() == 0) {
                            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Project does not exist\n");
                            responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
                        } else if (json.getJSONArray("projects").length() == 0) {
                            responseStatus.setCode(HttpServletResponse.SC_FORBIDDEN);
                        }
                    } else if (orgId != null) {
                        jsonArray = handleOrgProjects(orgId);
                        json = new JSONObject();
                        json.put(Sjm.PROJECTS, filterProjectByPermission(jsonArray));
                    } else {
                        jsonArray = handleProjects();
                        json = new JSONObject();
                        json.put(Sjm.PROJECTS, filterProjectByPermission(jsonArray));
                    }

                } else {
                    jsonArray = handleOrg(orgId);
                    json = new JSONObject();
                    json.put(Sjm.ORGS, filterOrgsByPermission(jsonArray));
                    if (orgId != null) {
                        if (jsonArray.length() == 0) {
                            responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
                        } else if (json.getJSONArray(Sjm.ORGS).length() == 0) {
                            responseStatus.setCode(HttpServletResponse.SC_FORBIDDEN);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create the JSON response", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        if (json == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            if (prettyPrint || accept.contains("webp")) {
                model.put(Sjm.RES, json.toString(4));
            } else {
                model.put(Sjm.RES, json);
            }
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Get all the sites that are contained in the workspace, and create json with that info in it.
     *
     * @return json to return
     *
     */
    private JSONArray handleOrg(String orgId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil();
        return emsNodeUtil.getOrganization(orgId);
    }

    private JSONArray handleOrgProjects(String orgId) {

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil();
        return emsNodeUtil.getProjects(orgId);
    }

    private JSONArray handleProjects() {

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil();
        return emsNodeUtil.getProjects();
    }

    private JSONArray handleProject(String projectId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil();
        JSONArray projects = new JSONArray();
        JSONObject project = emsNodeUtil.getProject(projectId);
        if (project != null) {
            projects.put(project);
        }
        return projects;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }
}
