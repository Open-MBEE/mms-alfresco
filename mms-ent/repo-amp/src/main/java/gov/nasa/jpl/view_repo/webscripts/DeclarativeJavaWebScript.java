package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import gov.nasa.jpl.view_repo.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WebScriptStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

    protected boolean editable = false;

    public static final String REF_ID = "refId";
    public static final String PROJECT_ID = "projectId";
    public static final String ORG_ID = "orgId";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String COMMIT_ID = "commitId";
    public static final String USERNAME = "username";

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

            Map<String, Object> model;

            String projectId = req.getServiceMatch().getTemplateVars().get(PROJECT_ID);
            Boolean perm = hasPermission(req, res);
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, HEAD, OPTIONS");
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
                res.setStatus(status.getCode());
                String location = status.getLocation();
                if (location != null && location.length() > 0) {
                    res.setHeader(WebScriptResponse.HEADER_LOCATION, location);
                }
                // apply cache
                res.setCache(cache);

                // render response according to requested format
                if (model.containsKey(Sjm.RES) && model.get(Sjm.RES) != null) {
                    res.setContentType("application/json");
                    res.setContentEncoding("UTF-8");
                    if (model.get(Sjm.RES) instanceof JsonObject) {
                        JsonObject json = (JsonObject) model.get(Sjm.RES);
                        Map<String, Object> jsonMap = new Gson().fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
                        JsonContentReader reader = new JsonContentReader(jsonMap);

                        // get the content and stream directly to the response output stream
                        // assuming the repository is capable of streaming in chunks, this should allow large files
                        // to be streamed directly to the browser response stream.
                        reader.getStreamContent(res.getOutputStream());
                    } else {
                        res.getWriter().write(model.get(Sjm.RES).toString());
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
     * Execute custom Java logic
     *
     * @param req    Web Script request
     * @param status Web Script status
     * @param cache  Web Script cache
     * @return custom service model
     */
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        return null;
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

    JsonArray filterProjectByPermission(JsonArray projects) {
        JsonArray result = new JsonArray();
        for (int i = 0; i < projects.size(); i++) {
            JsonObject project = JsonUtil.getOptObject(projects, i);
            if (project.has("orgId")) {
                String projectId = project.has(Sjm.SYSMLID) ? project.get(Sjm.SYSMLID).getAsString() : null;
                Boolean perm = SitePermission.hasPermission(
                                project.get("orgId").getAsString(),
                                projectId,
                                null,
                                Permission.READ);
                if (perm != null && perm) {
                    result.add(project);
                }
            }
        }
        return result;
    }

    JsonArray filterOrgsByPermission(JsonArray orgs) {
        JsonArray result = new JsonArray();
        for (int i = 0; i < orgs.size(); i++) {
            JsonObject org = JsonUtil.getOptObject(orgs, i);
            if (org.has("id")) {
                Boolean perm = SitePermission.hasPermission(org.get("id").getAsString(), null, null, Permission.READ);
                if (perm != null && perm) {
                    result.add(org);
                }
            }
        }
        return result;
    }

    JsonArray filterByPermission(JsonArray elements, WebScriptRequest req) {
        JsonArray result = new JsonArray();
        Map<String, Map<Permission, Boolean>> permCache = new HashMap<>();
        Map<String, String> projectSite = new HashMap<>();
        String refId = AbstractJavaWebScript.getRefId(req);
        String projectId = AbstractJavaWebScript.getProjectId(req);

        for (int i = 0; i < elements.size(); i++) {
            JsonObject el = JsonUtil.getOptObject(elements, i);
            String refId2 = el.has(Sjm.REFID) ? el.get(Sjm.REFID).getAsString() : refId;
            String projectId2 = el.has(Sjm.PROJECTID) ? el.get(Sjm.PROJECTID).getAsString() : projectId;
            JsonObject element =
                filterElementByPermission(el, projectId2, refId2, Permission.READ, permCache, projectSite);
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    private JsonObject filterElementByPermission(JsonObject element, String projectId, String refId,
        Permission permission, Map<String, Map<Permission, Boolean>> permCache, Map<String, String> projectSite) {
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
            Map<Permission, Boolean> permMap =
                permCache.containsKey(cacheKey) ? permCache.get(cacheKey) : new HashMap<>();
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
                    Map<Permission, Boolean> writePermMap =
                        permCache.containsKey(cacheKey) ? permCache.get(cacheKey) : new HashMap<>();
                    writePermMap.put(Permission.WRITE, editable);
                    permCache.put(cacheKey, writePermMap);
                }
            }
            if (element.has(Sjm.SYSMLID)) {
                element.addProperty(Sjm.EDITABLE, editable);
            }
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
