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
import gov.nasa.jpl.view_repo.util.*;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author han
 */
public class UserPreferencesPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(OrgPost.class);

    public UserPreferencesPost() {
        super();
    }

    public UserPreferencesPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        UserPreferencesPost instance = new UserPreferencesPost(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        //Map<String, String> templateArgs = req.getServiceMatch().getTemplateVars();
        String username = req.getServiceMatch().getTemplateVars().get(USERNAME);

//        printHeader(user, logger, req);
        Map<String, Object> model = new HashMap<>();
        try {
            SerialJSONObject postJson = new SerialJSONObject(req.getContent().getContent());
            SerialJSONArray toCommit = new SerialJSONArray();
            toCommit.put(postJson);
            // if(user.equals(username))
            CommitUtil.indexProfile(toCommit, null, false, "mms", "profiles");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //        Timer timer = new Timer();
//        boolean restoredOrg = false;
//
//        Map<String, Object> model = new HashMap<>();
//
//        JSONArray success = new JSONArray();
//        JSONArray failure = new JSONArray();
//
//        try {
//            if (validateRequest(req, status)) {
//
//                JSONObject json = (JSONObject) req.parseContent();
//                JSONArray elementsArray = json != null ? json.optJSONArray("orgs") : null;
//                if (elementsArray != null && elementsArray.length() > 0) {
//                    for (int i = 0; i < elementsArray.length(); i++) {
//                        JSONObject projJson = elementsArray.getJSONObject(i);
//
//                        String orgId = projJson.getString(Sjm.SYSMLID);
//                        String orgName = projJson.getString(Sjm.NAME);
//
//                        SiteInfo siteInfo = services.getSiteService().getSite(orgId);
//                        if (siteInfo == null) {
//                            SearchService searcher = services.getSearchService();
//                            ResultSet result =
//                                searcher.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, "fts-alfresco", "name:" + orgId);
//
//                            if (result != null && result.length() > 0) {
//                                log(Level.INFO, HttpServletResponse.SC_OK, "Organization Site restored.\n");
//                                services.getNodeService().restoreNode(result.getRow(0).getNodeRef(), null, null, null);
//                            } else {
//                                String sitePreset = "site-dashboard";
//                                String siteTitle =
//                                    (json != null && json.has(Sjm.NAME)) ? json.getString(Sjm.NAME) : orgName;
//                                String siteDescription = (json != null) ? json.optString(Sjm.DESCRIPTION) : "";
//                                if (!ShareUtils
//                                    .constructSiteDashboard(sitePreset, orgId, siteTitle, siteDescription, false)) {
//                                    log(Level.INFO, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//                                        "Failed to create site.\n");
//                                    logger.error(String
//                                        .format("Failed site info: %s, %s, %s", siteTitle, siteDescription, orgId));
//                                    failure.put(projJson);
//                                } else {
//                                    log(Level.INFO, HttpServletResponse.SC_OK, "Organization " + orgName + " Site created.\n");
//                                }
//                            }
//
//                            if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
//                                JSONObject res = CommitUtil.sendOrganizationDelta(orgId, orgName, projJson);
//                                success.put(res);
//                            }
//
//                        } else {
//                            JSONObject res = CommitUtil.sendOrganizationDelta(orgId, orgName, projJson);
//                            if (res != null && res.optString(Sjm.SYSMLID) != null) {
//                                log(Level.INFO, HttpServletResponse.SC_OK, "Organization " + orgName + " Site updated.\n");
//                                success.put(res);
//                            } else {
//                                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
//                                    "Organization " + orgName + " Site update failed.\n");
//                                failure.put(res);
//                            }
//                        }
//                    }
//                } else {
//                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request");
//                }
//            }
//        } catch (JSONException e) {
//            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
//        } catch (Exception e) {
//            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
//        }
//
//        JSONObject response = new JSONObject();
//        response.put(Sjm.ORGS, success);
//
//        if (failure.length() > 0) {
//            response.put("failed", failure);
//        }
//
//        status.setCode(responseStatus.getCode());
//        model.put(Sjm.RES, response);
//
//
//        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }

}
