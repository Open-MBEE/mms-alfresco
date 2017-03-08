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
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/*
 * Behavior of ViewGet: 1. single element is specified, just return the element JSON 2. single
 * element is specified with /elements then grab all the displayed elements also 3. single element
 * is specified with /elements and recurse, then grab all the displayed elements, recursively
 *
 */

public class ViewGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ViewGet.class);

    protected boolean gettingDisplayedElements = false;
    protected boolean gettingContainedViews = false;

    public ViewGet() {
        super();
    }

    public ViewGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String viewId = getIdFromRequest(req);
        if (!checkRequestVariable(viewId, "id")) {
            return false;
        }

        return true;
    }

    protected static String getRawViewId(WebScriptRequest req) {
        if (req == null || req.getServiceMatch() == null || req.getServiceMatch().getTemplateVars() == null) {
            return null;
        }
        String viewId = req.getServiceMatch().getTemplateVars().get("id");
        if (viewId == null) {
            viewId = req.getServiceMatch().getTemplateVars().get("modelid");
        }
        if (viewId == null) {
            viewId = req.getServiceMatch().getTemplateVars().get("elementid");
        }
        logger.debug(String.format("Got raw id = %s", viewId));
        return viewId;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ViewGet instance = new ViewGet();
        instance.setServices(getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> res = new HashMap<>();
        JSONObject model = new JSONObject();

        if (validateRequest(req, status)) {
            String viewId = getIdFromRequest(req);
            String projectId = getProjectId(req);
            WorkspaceNode workspace = getWorkspace(req);
            boolean gettingDisplayedElements = isDisplayedElementRequest(req);
            boolean recurse = getBooleanArg(req, "recurse", false);
            JSONArray elements = handleGetElement(viewId, projectId, workspace, gettingDisplayedElements, false, recurse);

            if (elements.length() > 0) {
                model.put("elements", filterByPermission(elements, req));
                res.put("res", model);
            } else {
                status.setCode(HttpServletResponse.SC_NOT_FOUND);
            }

        } else {
            model.put("views", new JSONArray());
        }

        res.put("res", model);

        printFooter(user, logger, timer);

        return res;
    }

    public static JSONArray handleGetElement(String viewId, String projectId, WorkspaceNode workspace, boolean gettingDisplayedElements,
                    boolean gettingContainedViews, boolean recurse) throws JSONException {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, workspace);
        JSONArray elements = new JSONArray();

        try {
            HashMap<String, Boolean> sysmlids = new HashMap<>();

            JSONObject element = emsNodeUtil.getNodeBySysmlid(viewId);
            if (element == null) {
                return new JSONArray();
            }

            sysmlids.put(viewId, true);
            elements.put(element);

            if (gettingContainedViews) {
                getContainedViews(element, emsNodeUtil, sysmlids, elements);
            } else if (gettingDisplayedElements) {
                getDisplayedViews(element, emsNodeUtil, sysmlids, elements, recurse);
            }

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        logger.debug(String.format("handleGetElement: %s", elements));
        return elements;
    }

    private static void getContainedViews(JSONObject viewJson, EmsNodeUtil emsNodeUtil,
                    HashMap<String, Boolean> sysmlids, JSONArray elements) throws IOException {
        if (viewJson.has("view2view")) {
            JSONArray view2view = viewJson.getJSONArray("view2view");
            for (int ii = 0; ii < view2view.length(); ii++) {
                JSONObject subview = view2view.getJSONObject(ii);
                String subviewId = subview.getString("id");
                JSONObject subviewJson = emsNodeUtil.getNodeBySysmlid(subviewId);
                sysmlids.put(subviewId, true);
                elements.put(subviewJson);
                if (subview.has("childrenViews")) {
                    JSONArray childrenViews = subview.getJSONArray("childrenViews");
                    for (int jj = 0; jj < childrenViews.length(); jj++) {
                        String childViewId = childrenViews.getString(jj);
                        JSONObject childViewJson = emsNodeUtil.getNodeBySysmlid(childViewId);
                        sysmlids.put(childViewId, true);
                        elements.put(childViewJson);
                    }
                }
            }
        }

        if (viewJson.has("childViews")) {
            JSONArray childViews = viewJson.getJSONArray("childViews");
            for (int jj = 0; jj < childViews.length(); jj++) {
                JSONObject childView = childViews.getJSONObject(jj);
                String childViewId = childView.getString("id");
                JSONObject childViewJson = emsNodeUtil.getNodeBySysmlid(childViewId);
                sysmlids.put(childViewId, true);
                elements.put(childViewJson);

                if (childViewJson.has("ownedAttribute")) {
                    getContainedViews(childViewJson, emsNodeUtil, sysmlids, elements);
                }
            }
        }
    }

    private static void getDisplayedViews(JSONObject viewJson, EmsNodeUtil emsNodeUtil,
                    HashMap<String, Boolean> sysmlids, JSONArray elements, Boolean recurse) throws IOException {
        if (viewJson.has("displayedElements")) {
            JSONArray displayedElements = viewJson.getJSONArray("displayedElements");
            for (int i = 0; i < displayedElements.length(); i++) {
                if (recurse) {

                    JSONArray elasticElements = emsNodeUtil.getChildren(displayedElements.getString(i));

                    for (int j = 0; j < elasticElements.length(); j++) {
                        JSONObject o = elasticElements.getJSONObject(j);
                        if (!sysmlids.containsKey(o.getString(Sjm.SYSMLID))) {
                            elements.put(o);
                            sysmlids.put(o.getString(Sjm.SYSMLID), true);
                        }
                    }

                } else {
                    if (!sysmlids.containsKey(displayedElements.getString(i))) {
                        elements.put(emsNodeUtil.getNodeBySysmlid(displayedElements.getString(i)));
                        sysmlids.put(displayedElements.getString(i), true);
                    }
                }
            }
        }
    }

}
