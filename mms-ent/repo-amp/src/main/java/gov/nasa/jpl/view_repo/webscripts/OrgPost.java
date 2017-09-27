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
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author han
 */
public class OrgPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(OrgPost.class);

    public OrgPost() {
        super();
    }

    public OrgPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        OrgPost instance = new OrgPost(repository, getServices());
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
                JSONArray elementsArray = json != null ? json.optJSONArray("orgs") : null;
                JSONObject projJson = (elementsArray != null && elementsArray.length() > 0) ?
                                elementsArray.getJSONObject(0) :
                                new JSONObject();

                String orgId = projJson.getString(Sjm.SYSMLID);
                String orgName = projJson.getString(Sjm.NAME);

                SiteInfo siteInfo = services.getSiteService().getSite(orgId);
                if (siteInfo == null) {
                    String sitePreset = "site-dashboard";
                    String siteTitle = (json != null && json.has(Sjm.NAME)) ? json.getString(Sjm.NAME) : orgName;
                    String siteDescription = (json != null) ? json.optString(Sjm.DESCRIPTION) : "";
                    if (!ShareUtils.constructSiteDashboard(sitePreset, orgId, siteTitle, siteDescription, false)) {
                        log(Level.INFO, HttpServletResponse.SC_OK, "Failed to create site.\n");
                        logger.error(String.format("Failed site info: %s, %s, %s", siteTitle, siteDescription, orgId));
                        statusCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    } else {
                        log(Level.INFO, HttpServletResponse.SC_OK, "Organization Site created.\n");
                        statusCode = HttpServletResponse.SC_OK;
                    }
                }

                if (statusCode == HttpServletResponse.SC_OK) {
                    CommitUtil.sendOrganizationDelta(orgId, orgName, user);
                }

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
        model.put(Sjm.RES, createResponseJson());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }

}
