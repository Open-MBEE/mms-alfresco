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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.webscripts.util.ShareUtils;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
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

        JsonArray success = new JsonArray();
        JsonArray failure = new JsonArray();
        
        try {
            if (validateRequest(req, status)) {

                JsonObject json = JsonUtil.buildFromString(req.getContent().getContent());
                JsonArray elementsArray = JsonUtil.getOptArray(json, "orgs");
                if (elementsArray.size() > 0) {
                    for (int i = 0; i < elementsArray.size(); i++) {
                        JsonObject projJson = elementsArray.get(i).getAsJsonObject();

                        String orgId = projJson.get(Sjm.SYSMLID).getAsString();
                        String orgName = projJson.get(Sjm.NAME).getAsString();

                        SiteInfo siteInfo = services.getSiteService().getSite(orgId);
                        if (siteInfo == null) {
                            SearchService searcher = services.getSearchService();
                            ResultSet result =
                                searcher.query(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, "fts-alfresco", "name:" + orgId);

                            if (result != null && result.length() > 0) {
                                log(Level.INFO, HttpServletResponse.SC_OK, "Organization Site restored.");
                                services.getNodeService().restoreNode(result.getRow(0).getNodeRef(), null, null, null);
                            } else {
                                String sitePreset = "site-dashboard";
                                String siteTitle =
                                    (json != null && json.has(Sjm.NAME)) ? json.get(Sjm.NAME).getAsString() : orgName;
                                String siteDescription = JsonUtil.getOptString(json, Sjm.DESCRIPTION);
                                if (!ShareUtils
                                    .constructSiteDashboard(sitePreset, orgId, siteTitle, siteDescription, false)) {
                                    log(Level.INFO, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                                        "Failed to create site.");
                                    logger.error(String
                                        .format("Failed site info: %s, %s, %s", siteTitle, siteDescription, orgId));
                                    failure.add(projJson);
                                } else {
                                    log(Level.INFO, HttpServletResponse.SC_OK,
                                        "Organization " + orgName + " Site created.");
                                }
                            }

                            if (responseStatus.getCode() == HttpServletResponse.SC_OK) {
                                JsonObject res = CommitUtil.sendOrganizationDelta(orgId, orgName, projJson);
                                success.add(res);
                            }

                        } else {
                            EmsScriptNode site = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
                            site.setProperty(Acm.ACM_NAME, orgName);
                            JsonObject res = CommitUtil.sendOrganizationDelta(orgId, orgName, projJson);
                            if (res != null && !JsonUtil.getOptString(res, Sjm.SYSMLID).isEmpty()) {
                                log(Level.INFO, HttpServletResponse.SC_OK,
                                    "Organization " + orgName + " Site updated.\n");
                                success.add(res);
                            } else {
                                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                                    "Organization " + orgName + " Site update failed.\n");
                                failure.add(res);
                            }
                        }
                    }
                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request");
                }
            }
        } catch (IllegalStateException e) {
            // get this when trying to turn JsonElement to a JsonObject, but no object was found
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "incorrect JSON from request");
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        JsonObject response = new JsonObject();
        response.add(Sjm.ORGS, success);

        if (failure.size() > 0) {
            response.add("failed", failure);
        }

        status.setCode(responseStatus.getCode());
        model.put(Sjm.RES, response);


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
