package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WebScriptStatus;

import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This file is part of the Spring Surf Extension project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import gov.nasa.jpl.view_repo.webscripts.util.SitePermission;
import gov.nasa.jpl.view_repo.webscripts.util.SitePermission.Permission;

/**
 * Modified from Alfresco's DeclarativeWebScript so we can have one place to modify the cache
 * control headers in the response.
 * <p>
 * Script/template driven based implementation of an Web Script
 *
 * @author davidc
 */
public class DeclarativeJavaWebScript extends AbstractWebScript {
    // Logger
    private static final Log logger = LogFactory.getLog(DeclarativeJavaWebScript.class);

    static boolean cacheSnapshotsFlag = false;
    protected boolean editable = false;

    public static final String REF_ID = "refId";
    public static final String PROJECT_ID = "projectId";
    public static final String ORG_ID = "orgId";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String COMMIT_ID = "commitId";

    public static final String NO_WORKSPACE_ID = "master"; // default is master if unspecified
    public static final String NO_PROJECT_ID = "no_project";
    public static final String NO_SITE_ID = "no_site";

    /* (non-Javadoc)
     * @see org.alfresco.web.scripts.WebScript#execute(org.alfresco.web.scripts.WebScriptRequest, org.alfresco.web.scripts.WebScriptResponse)
     */
    @Override public final void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {

        // retrieve requested format
        String format = req.getFormat();

        try {
            // establish mimetype from format
            String mimetype = getContainer().getFormatRegistry().getMimeType(req.getAgent(), format);
            if (mimetype == null) {
                throw new WebScriptException("Web Script format '" + format + "' is not registered");
            }

            // construct model for script / template
            Status status = new Status();
            Cache cache = new Cache(getDescription().getRequiredCache());
            setCacheHeaders(req, cache); // add in custom headers for nginx caching

            Map<String, Object> model;

            String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
            Boolean perm = hasPermission(req, res);
            if (projectId == null || (perm != null && perm)) {
                model = executeImpl(req, status, cache);
            } else {
                if (perm == null) {
                    status.setMessage("Not Found!");
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    res.getWriter().write("{}");
                    return;
                }
                status.setMessage("Access denied!");
                res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                res.getWriter().write("{}");
                return;
            }

            if (model == null) {
                model = new HashMap<>(8, 1.0f);
            }
            model.put("status", status);
            model.put("cache", cache);

            try {
                // create model for template rendering
                Map<String, Object> templateModel = createTemplateParameters(req, res, model);
                // is a redirect to a status specific template required?
                if (status.getRedirect()) {
                    sendStatus(req, res, status, cache, format, templateModel);
                } else {
                    // render output
                    int statusCode = status.getCode();
                    if (statusCode != HttpServletResponse.SC_OK && !req.forceSuccessStatus()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Force success status header in response: " + req.forceSuccessStatus());
                            logger.debug("Setting status " + statusCode);
                        }
                        res.setStatus(statusCode);
                    }

                    // apply location
                    String location = status.getLocation();
                    if (location != null && location.length() > 0) {
                        if (logger.isDebugEnabled())
                            logger.debug("Setting location to " + location);
                        res.setHeader(WebScriptResponse.HEADER_LOCATION, location);
                    }

                    // apply cache
                    res.setCache(cache);

                    // render response according to requested format
                    if (templateModel.containsKey(Sjm.RES) && templateModel.get(Sjm.RES) != null) {
                        res.getWriter().write(templateModel.get(Sjm.RES).toString());
                        res.setHeader("Content-Type", "application/json;charset=UTF-8");
                    }
                }
            } finally {
                // perform any necessary cleanup
                executeFinallyImpl(req, status, cache, model);
            }
        } catch (Throwable e) {
            logger.error(String.format("Caught exception; decorating with appropriate status template : %s",
                            LogUtil.getStackTrace(e)));
            throw createStatusException(e, req, res);
        }
    }

    /**
     * Set the cache headers for caching server based on the request. This is single place
     * that we need to modify to update cache-control headers across all webscripts.
     *
     * @param req
     * @param cache
     */
    private boolean setCacheHeaders(WebScriptRequest req, Cache cache) {
        String[] names = req.getParameterNames();
        boolean cacheUpdated = false;
        // check if timestamp
        for (String name : names) {
            if (name.equals("timestamp")) {
                cacheUpdated = updateCache(cache);
                break;
            }
        }
        // check if configuration snapshots and products
        if (!cacheUpdated) {
            if (cacheSnapshotsFlag) {
                String url = req.getURL();
                if (url.contains("configurations")) {
                    if (url.contains("snapshots") || url.contains(Sjm.PRODUCTS)) {
                        cacheUpdated = updateCache(cache);
                    }
                }
            }
        }

        return cacheUpdated;
    }

    private boolean updateCache(Cache cache) {
        if (!cache.getIsPublic()) {
            cache.setIsPublic(true);
            cache.setMaxAge(31557000L);
            // following are true by default, so need to set them to false
            cache.setNeverCache(false);
            cache.setMustRevalidate(false);
        }
        return true;
    }

    /**
     * Execute custom Java logic
     *
     * @param req    Web Script request
     * @param status Web Script status
     * @return custom service model
     * @deprecated
     */
    @Deprecated protected Map<String, Object> executeImpl(WebScriptRequest req, WebScriptStatus status) {
        return null;
    }

    /**
     * Execute custom Java logic
     *
     * @param req    Web Script request
     * @param status Web Script status
     * @return custom service model
     * @deprecated
     */
    @Deprecated protected Map<String, Object> executeImpl(WebScriptRequest req, Status status) {
        return executeImpl(req, new WebScriptStatus(status));
    }

    /**
     * Execute custom Java logic
     *
     * @param req    Web Script request
     * @param status Web Script status
     * @param cache  Web Script cache
     * @return custom service model
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        // NOTE: Redirect to those web scripts implemented before cache support and v2.9
        return executeImpl(req, status);
    }

    /**
     * Execute custom Java logic to clean up any resources
     *
     * @param req    Web Script request
     * @param status Web Script status
     * @param cache  Web Script cache
     * @param model  model
     */
    private void executeFinallyImpl(WebScriptRequest req, Status status, Cache cache, Map<String, Object> model) {
        // This method left intentionally empty
    }

    private Boolean hasPermission(WebScriptRequest req, WebScriptResponse res) {
        String descriptionPath = getDescription().getDescPath();
        String methodType = getMethod(descriptionPath);
        Permission permissionType = getPermissionType(methodType);

        String refId = AbstractJavaWebScript.getRefId(req);
        String projectId = AbstractJavaWebScript.getProjectId(req);

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        String siteId = emsNodeUtil.getOrganizationFromProject(projectId);

        return SitePermission.hasPermission(siteId, projectId, refId, permissionType);
    }

    JSONArray filterProjectByPermission(JSONArray projects) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < projects.length(); i++) {
            JSONObject project = projects.optJSONObject(i);
            if (project != null && project.has("orgId")) {
                Boolean perm = SitePermission.hasPermission(project.getString("orgId"), project.getString(Sjm.SYSMLID), null, Permission.READ);
                if (perm != null && perm) {
                    result.put(project);
                }
            }
        }
        return result;
    }

    JSONArray filterOrgsByPermission(JSONArray orgs) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < orgs.length(); i++) {
            JSONObject org = orgs.optJSONObject(i);
            if (org != null && org.has("id")) {
                Boolean perm = SitePermission.hasPermission(org.getString("id"), null, null, Permission.READ);
                if (perm != null && perm) {
                    result.put(org);
                }
            }
        }
        return result;
    }

    JSONArray filterByPermission(JSONArray elements, WebScriptRequest req) {
        JSONArray result = new JSONArray();
        Map<String, Map<Permission, Boolean>> permCache = new HashMap<>();
        Map<String, String> projectSite = new HashMap<>();
        String refId = AbstractJavaWebScript.getRefId(req);
        String projectId = AbstractJavaWebScript.getProjectId(req);

        for (int i = 0; i < elements.length(); i++) {
            JSONObject el = elements.optJSONObject(i);
            String refId2 = el.has(Sjm.REFID) ? el.getString(Sjm.REFID) : refId;
            String projectId2 = el.has(Sjm.PROJECTID) ? el.getString(Sjm.PROJECTID): projectId;
            JSONObject element = filterElementByPermission(el, projectId2, refId2, Permission.READ,
                permCache, projectSite);
            if (element != null) {
                result.put(element);
            }
        }
        return result;
    }

    private JSONObject filterElementByPermission(JSONObject element, String projectId, String refId,
                     Permission permission, Map<String, Map<Permission, Boolean>> permCache,
                                                 Map<String, String> projectSite) {
        // temp fix to skip permission checking
        Boolean hasPerm;
        String siteId = null;
        if (projectSite.containsKey(projectId)) {
            siteId = projectSite.get(projectId);
        } else {
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            siteId = emsNodeUtil.getOrganizationFromProject(projectId);
            projectSite.put(projectId, siteId);
        }

        String cacheKey = projectId + "_" + refId;

        if (permCache.containsKey(cacheKey) && permCache.get(cacheKey).containsKey(permission)) {
            hasPerm = permCache.get(cacheKey).get(permission);
        } else {
            hasPerm = SitePermission.hasPermission(siteId, projectId, refId, permission);
            Map<Permission, Boolean> permMap = permCache.containsKey(cacheKey) ? permCache.get(cacheKey) : new HashMap<>();
            permMap.put(permission, (hasPerm == null) ? false : hasPerm);
            permCache.put(cacheKey, permMap);
        }

        if (hasPerm != null && hasPerm) {
            editable = true;
            if (permission != Permission.WRITE) {
                if (permCache.containsKey(cacheKey) && permCache.get(cacheKey).containsKey(Permission.WRITE)) {
                    editable = permCache.get(cacheKey).get(Permission.WRITE);
                } else {
                    editable = SitePermission.hasPermission(siteId, projectId, refId, Permission.WRITE);
                    Map<Permission, Boolean> writePermMap = permCache.containsKey(cacheKey) ? permCache.get(cacheKey) : new HashMap<>();
                    writePermMap.put(Permission.WRITE, editable);
                    permCache.put(cacheKey, writePermMap);
                }
            }
            element.put(Sjm.EDITABLE, editable);
            return element;
        }
        return null;
    }

    private String getMethod(String descriptionPath) {
        String method = "GET";
        if (descriptionPath.contains(".put")) {
            method = "PUT";
        } else if (descriptionPath.contains(".delete")) {
            method = "DELETE";
        } else if (descriptionPath.contains(".post")) {
            method = "POST";
        }
        return method;
    }

    private Permission getPermissionType(String method) {
        switch (method) {
            case "DELETE":
                return Permission.WRITE;
            case "POST":
                return Permission.WRITE;
            default:
                return Permission.READ;
        }
    }
}
