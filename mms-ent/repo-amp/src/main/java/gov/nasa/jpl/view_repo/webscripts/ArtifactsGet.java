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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

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

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * @author cinyoung
 */
public class ArtifactsGet extends ArtifactGet {
    static Logger logger = Logger.getLogger(ModelsGet.class);

    public ArtifactsGet() {
        super();
    }

    public ArtifactsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    String timestamp;
    Date dateTime;

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // get timestamp if specified
        timestamp = req.getParameter("timestamp");
        dateTime = TimeUtils.dateFromTimestamp(timestamp);

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        if (refId != null && refId.equalsIgnoreCase(NO_WORKSPACE_ID)) {
            return true;
        } else if (refId != null && !emsNodeUtil.refExists(refId)) {
            log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Reference with id, %s not found",
                refId + (dateTime == null ? "" : " at " + dateTime));
            return false;
        }

        return true;
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ArtifactsGet instance = new ArtifactsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        Map<String, Object> model = new HashMap<>();
        JSONArray elementsJson = new JSONArray();
        JSONArray errors = new JSONArray();
        JSONObject result = new JSONObject();

        try {

            if (validateRequest(req, status)) {
                try {
                    result = handleRequest(req);
                    elementsJson = result.optJSONArray(Sjm.ARTIFACTS);
                } catch (JSONException e) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request", e);
                }
            }

            JSONObject top = new JSONObject();
            if (elementsJson != null && elementsJson.length() > 0) {
                JSONArray elements = filterByPermission(elementsJson, req);
                if (elements.length() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }

                top.put(Sjm.ARTIFACTS, elements);

                JSONArray errorMessages = new JSONArray();
                for (String level : Sjm.ERRORLEVELS) {
                    errors = result.optJSONArray(level);
                    if (errors != null && errors.length() > 0) {
                        for (int i = 0; i < errors.length(); i++) {
                            JSONObject errorPayload = new JSONObject();
                            errorPayload.put("code", HttpServletResponse.SC_NOT_FOUND);
                            errorPayload.put(Sjm.SYSMLID, errors.get(i));
                            errorPayload.put("message", String.format("Element %s was not found", errors.get(i)));
                            errorPayload.put("severity", level);
                            errorMessages.put(errorPayload);
                        }
                    }
                }

                if (errorMessages.length() > 0) {
                    top.put("messages", errorMessages);
                }

                if (prettyPrint || accept.contains("webp")) {
                    model.put(Sjm.RES, top.toString(4));
                } else {
                    model.put(Sjm.RES, top);
                }
            } else {
                log(Level.INFO, HttpServletResponse.SC_OK, "No elements found");
                top.put(Sjm.ARTIFACTS, new JSONArray());
                model.put(Sjm.RES, top);
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
    private JSONObject handleRequest(WebScriptRequest req)
        throws JSONException, IOException, SQLException {
        JSONObject requestJson = (JSONObject) req.parseContent();
        if (requestJson.has(Sjm.ARTIFACTS)) {
            JSONArray elementsToFindJson = requestJson.getJSONArray(Sjm.ARTIFACTS);

            String refId = getRefId(req);
            String projectId = getProjectId(req);

            JSONObject mountsJson = new JSONObject().put(Sjm.SYSMLID, projectId).put(Sjm.REFID, refId);

            JSONArray found = new JSONArray();
            JSONObject result = new JSONObject();

            Set<String> uniqueElements = new HashSet<>();
            for (int i = 0; i < elementsToFindJson.length(); i++) {
                uniqueElements.add(elementsToFindJson.getJSONObject(i).getString(Sjm.SYSMLID));
            }
            //this gets elements, not artifacts
            EmsNodeUtil.handleMountSearch(mountsJson, false, false, 0L, uniqueElements, found);
            result.put(Sjm.ARTIFACTS, found);
            result.put(Sjm.WARN, uniqueElements);
            return result;
        } else {
            return new JSONObject();
        }
    }
}

