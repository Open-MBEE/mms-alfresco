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

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
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

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;

/**
 * @author cinyoung
 */
public class ModelsGet extends ModelGet {
    static Logger logger = Logger.getLogger(ModelsGet.class);

    public ModelsGet() {
        super();
    }

    public ModelsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    String timestamp;
    Date dateTime;

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // get timestamp if specified
        timestamp = req.getParameter("timestamp");
        dateTime = TimeUtils.dateFromTimestamp(timestamp);

        WorkspaceNode workspace = getWorkspace(req);
        boolean wsFound = workspace != null && workspace.exists();
        if (!wsFound) {
            String wsId = getRefId(req);
            if (wsId != null && wsId.equalsIgnoreCase("master")) {
                wsFound = true;
            } else {
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Workspace with id, %s not found",
                    wsId + (dateTime == null ? "" : " at " + dateTime));
                return false;
            }
        }

        return true;
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelsGet instance = new ModelsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONArray elementsJson = new JSONArray();

        try {

            if (validateRequest(req, status)) {
                try {
                    Long depth = getDepthFromRequest(req);
                    elementsJson = handleRequest(req, depth);
                } catch (JSONException e) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request");
                    e.printStackTrace();
                }
            }

            JSONObject top = NodeUtil.newJsonObject();
            if (elementsJson.length() > 0) {
                top.put(Sjm.ELEMENTS, filterByPermission(elementsJson, req));
                //top.put(Sjm.ELEMENTS, elementsJson);
                String[] accepts = req.getHeaderValues("Accept");
                String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";
                if (!Utils.isNullOrEmpty(response.toString()))
                    top.put("message", response.toString());
                if (prettyPrint || accept.contains("webp")) {
                    model.put("res", top.toString(4));
                } else {
                    model.put("res", top);
                }
            } else {
                log(Level.WARN, HttpServletResponse.SC_OK, "No elements found");
                model.put("res", createResponseJson());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of
     * elements
     *
     * @param req
     * @return
     * @throws IOException
     */
    private JSONArray handleRequest(WebScriptRequest req, final Long maxDepth)
        throws JSONException, IOException, SQLException {
        JSONObject requestJson = (JSONObject) req.parseContent();
        if (requestJson.has(Sjm.ELEMENTS)) {
            JSONArray elementsToFindJson = requestJson.getJSONArray(Sjm.ELEMENTS);

            String refId = getRefId(req);
            String projectId = getProjectId(req);
            boolean extended = Boolean.parseBoolean(req.getParameter("extended"));

            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            JSONObject mountsJson = emsNodeUtil.getProjectWithFullMounts(projectId, refId, null);

            JSONArray result = new JSONArray();
            Set<String> uniqueElements = new HashSet<>();
            for (int i = 0; i < elementsToFindJson.length(); i++) {
                uniqueElements.add(elementsToFindJson.getJSONObject(i).getString(Sjm.SYSMLID));
            }
            handleMountSearch(mountsJson, extended, maxDepth, uniqueElements, result);
            return result;
        } else {
            return new JSONArray();
        }
    }

    protected void handleMountSearch(JSONObject projectOb, boolean extended, final Long maxDepth, Set<String> elementsToFind, JSONArray result)
        throws JSONException, SQLException, IOException {

        if (elementsToFind.isEmpty()) {
            return;
        }
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectOb.getString(Sjm.SYSMLID), projectOb.getString(Sjm.REFID));
        JSONArray nodeList = emsNodeUtil.getNodesBySysmlids(elementsToFind);
        Set<String> foundElements = new HashSet<>();
        JSONArray curFound = new JSONArray();
        for (int index = 0; index < nodeList.length(); index++) {
            String id = nodeList.getJSONObject(index).getString(Sjm.SYSMLID);
            if (maxDepth != 0) {
                JSONArray children = emsNodeUtil.getChildren(id, maxDepth);
                for (int i = 0; i < children.length(); i++) {
                    String cid = children.getJSONObject(i).getString(Sjm.SYSMLID);
                    if (foundElements.contains(cid)) {
                        continue;
                    }
                    curFound.put(children.getJSONObject(i));
                    foundElements.add(cid);
                }
            } else {
                curFound.put(nodeList.getJSONObject(index));
                foundElements.add(id);
            }
        }
        curFound = extended ? emsNodeUtil.addExtendedInformation(curFound) : curFound;
        for (int i = 0; i < curFound.length(); i++) {
            result.put(curFound.get(i));
        }
        elementsToFind.removeAll(foundElements);
        JSONArray mountsArray = projectOb.getJSONArray(Sjm.MOUNTS);

        for (int i = 0; i < mountsArray.length(); i++) {
            handleMountSearch(mountsArray.getJSONObject(i), extended, maxDepth, elementsToFind, result);
        }
    }
}

