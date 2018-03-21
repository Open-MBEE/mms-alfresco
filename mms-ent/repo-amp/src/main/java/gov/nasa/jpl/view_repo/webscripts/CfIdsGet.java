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
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY~
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.db.GraphInterface;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.Node;

public class CfIdsGet extends AbstractJavaWebScript {

    static Logger logger = Logger.getLogger(CfIdsGet.class);

    private static final String ELEMENTID = "elementId";

    public CfIdsGet() {
        super();
    }

    public CfIdsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        if (refId != null && refId.equalsIgnoreCase(NO_WORKSPACE_ID)) {
            return true;
        } else if (refId != null && !emsNodeUtil.refExists(refId)) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Reference with id, %s not found", refId);
            return false;
        }
        return true;
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        CfIdsGet instance = new CfIdsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = handleElementGet(req, status);
        printFooter(user, logger, timer);
        return model;
    }

    protected Map<String, Object> handleElementGet(WebScriptRequest req, Status status) {

        Map<String, Object> model = new HashMap<>();
        JsonObject top = new JsonObject();

        if (validateRequest(req, status)) {
            try {
                JsonArray elementsJson = null;
                String modelId = req.getServiceMatch().getTemplateVars().get(ELEMENTID);
                if (null == modelId) {
                    log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element %s", modelId);
                }  else {
                    elementsJson = handleElementHierarchy(modelId, req);
                }
                if (elementsJson != null) {
                    top.add("elementIds", elementsJson);
                }
                if (!Utils.isNullOrEmpty(response.toString())) {
                    top.addProperty("message", response.toString());
                }
            } catch (Exception e) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
            }
            status.setCode(responseStatus.getCode());
        }
        model.put(Sjm.RES, top);
        return model;
    }

    protected JsonArray handleElementHierarchy(String rootSysmlid, WebScriptRequest req)
        throws SQLException, IOException {
        // get timestamp if specified
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        JsonObject mountsJson = new JsonObject();
        mountsJson.addProperty(Sjm.SYSMLID, projectId);
        mountsJson.addProperty(Sjm.REFID, refId);
        Long depth = null;
        String depthParam = req.getParameter("depth");
        if (depthParam != null) {
            try {
                depth = Long.parseLong(depthParam);
                if (depth < 0) {
                    depth = 100000L;
                }
            } catch (NumberFormatException nfe) {
                // don't do any recursion, ignore the depth
                log(Level.WARN, HttpServletResponse.SC_BAD_REQUEST, "Bad depth specified, returning depth 0");
            }
        }
        if (depth == null) {
            depth = 5L;
        }
        return handleMountSearch(mountsJson, rootSysmlid, depth);
    }

    protected JsonArray handleMountSearch(JsonObject mountsJson, String rootSysmlid, Long depth)
        throws SQLException, IOException {
        JsonArray result = null;
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(mountsJson.get(Sjm.SYSMLID).getAsString(), 
                        mountsJson.get(Sjm.REFID).getAsString());
        Node n = emsNodeUtil.getById(rootSysmlid);
        if (n != null) {
            if (n.isDeleted()) {
                log(Level.ERROR, HttpServletResponse.SC_GONE, "Element %s is deleted", rootSysmlid);
                return new JsonArray();
            }
            return emsNodeUtil.getChildrenIds(rootSysmlid, GraphInterface.DbEdgeTypes.VIEW, depth);
        }
        if (!mountsJson.has(Sjm.MOUNTS)) {
            mountsJson = emsNodeUtil.getProjectWithFullMounts(
                            mountsJson.get(Sjm.SYSMLID).getAsString(), 
                            mountsJson.get(Sjm.REFID).getAsString(), null);
        }
        JsonArray mountsArray = mountsJson.get(Sjm.MOUNTS).getAsJsonArray();

        for (int i = 0; i < mountsArray.size(); i++) {
            result = handleMountSearch(mountsArray.get(i).getAsJsonObject(), rootSysmlid, depth);
            if (result != null) {
                return result;
            }
        }
        return result;

    }
}
