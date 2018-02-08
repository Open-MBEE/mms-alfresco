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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.Node;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.GraphInterface.DbNodeTypes;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * @author gcgandhi
 */
public class SiteGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(SiteGet.class);

    public SiteGet() {
        super();
    }

    public SiteGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        SiteGet instance = new SiteGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        if (checkMmsVersions) {
            if (compareMmsVersions(req, getResponse(), getResponseStatus())) {
                model.put(Sjm.RES, createResponseJson());
                return model;
            }
        }
        JsonObject json = null;

        try {
            if (validateRequest(req, status)) {
                String projectId = getProjectId(req);
                String refId = getRefId(req);

                JsonArray jsonArray = handleSite(projectId, refId);
                json = new JsonObject();
                json.add("groups", jsonArray);
            }
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }
        if (json == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            model.put(Sjm.RES, json);
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Get all the sites that are contained in the workspace, and create json with that info in it.
     *
     *
     * @return json to return
     *
     * @throws IOException
     */
    private JsonArray handleSite(String projectId, String refId)
                    throws IOException {

        JsonArray json = new JsonArray();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String orgId = emsNodeUtil.getOrganizationFromProject(projectId);
        ElasticHelper eh = new ElasticHelper();
        List<String> ids = new ArrayList<>();
        List<String> alfs = new ArrayList<>();

        try {
            List<Node> siteNodes = emsNodeUtil.getSites(true, true);
            List<Node> alfSites = emsNodeUtil.getSites(true, false);

            for (Node n : siteNodes) {
                ids.add(n.getElasticId());
            }
            for (Node n : alfSites) {
                alfs.add(n.getSysmlId());
            }
            JsonArray elements = eh.getElementsFromElasticIds(ids, projectId);

            if (logger.isDebugEnabled())
                logger.debug("handleSite: " + elements);

            for (int i = 0; i < elements.size(); i++) {
                JsonObject o = elements.get(i).getAsJsonObject();

                for (Node n : siteNodes) {
                    if (n.getSysmlId().equals(o.get(Sjm.SYSMLID).getAsString())) {
                        if (n.getNodeType() == DbNodeTypes.SITEANDPACKAGE.getValue()) {
                            String path = "path|/Sites/" + orgId + "/documentLibrary/" + projectId + "/" + n.getSysmlId();
                            String siteUrl = "/share/page/repository#filter=" + StringEscapeUtils.escapeHtml(path);
                            Set<DbNodeTypes> sites = new HashSet<>();
                            sites.add(DbNodeTypes.SITE);
                            sites.add(DbNodeTypes.SITEANDPACKAGE);
                            String parent = emsNodeUtil.getImmediateParentOfTypes(n.getSysmlId(),
                                DbEdgeTypes.CONTAINMENT, sites);
                            o.addProperty("_parentId", parent);
                            o.addProperty("_link", siteUrl);
                        } else {
                            o.addProperty("_parentId", "null");
                        }
                    }
                }

                if (!alfs.contains(o.get(Sjm.SYSMLID).getAsString())) {
                    json.add(o);
                }
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return json;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String id = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestContent(req) && checkRequestVariable(id, REF_ID);
    }
}
