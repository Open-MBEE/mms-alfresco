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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.webscripts.util.SitePermission;
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

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 *
 * @author cinyoung
 *
 */
public class ProjectGet extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(ProjectGet.class);

    public ProjectGet() {
        super();
    }

    public ProjectGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProjectGet instance = new ProjectGet(repository, getServices());
        return instance.executeImplImpl(req,  status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {
                String siteName = getSiteName( req );

                // get commitId if specified
                String commitId = req.getParameter("commitId");
                String projectId = getProjectId(req);
                String workspaceId = getRefId(req);

                json = handleProject(projectId, workspaceId, commitId);
                if (json != null && !Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n", e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            model.put("res", json);
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Get the project specified by the JSONObject
     *
     * @param projectId
     * @param workspaceId
     * @param commitId
     *
     *            Site project should reside in
     * @return HttpStatusResponse code for success of the POST request
     * @throws JSONException
     */
    private JSONObject handleProject( String projectId,
                                      String workspaceId, String commitId )
                                              throws JSONException {

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, workspaceId);
        JSONObject project = emsNodeUtil.getNodeBySysmlid(projectId);
        JSONArray projects = new JSONArray();
        projects.put(project);
        JSONArray filteredProjects = SitePermission.checkPermission(projects, projectId, workspaceId, null, SitePermission.Permission.READ, null).getJSONArray("allowedElements");
        if (filteredProjects.optJSONObject(0) != null && filteredProjects.optJSONObject(0).length() > 0) {
            return filteredProjects.getJSONObject(0);
        } else if (!project.has(Sjm.SYSMLID)) {
            log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "No projects found for site");
        } else {
            log(Level.ERROR, HttpServletResponse.SC_UNAUTHORIZED, "No permissions to read");
        }

        return new JSONObject();
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
        return checkRequestVariable(projectId, PROJECT_ID);
    }
}
