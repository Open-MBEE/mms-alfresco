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

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lemmy
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
        String username = req.getServiceMatch().getTemplateVars().get(USERNAME);

        Timer timer = new Timer();
        printHeader(user, logger, req, true);

        JSONArray success = new JSONArray();
        JSONArray failure = new JSONArray();

        Map<String, Object> model = new HashMap<>();
        // :TODO Weak check for authorization
        if (user != null && username.equals(user)) {
            try {
                if (validateRequest(req, status)) {
                    // Post request is always a single object
                    SerialJSONObject postJson =
                        new SerialJSONObject(req.getContent().getContent()).getJSONObject(Sjm.PROFILES);
                    // creates a new document if one didn't exist, otherwise updates existing document
                    JSONObject res = CommitUtil.indexProfile(username, postJson, "mms");
                    if (res != null && res.length() > 0 ) {
                        success.put(res);
                    } else {
                        failure.put(res);
                    }
                }
            } catch (JSONException ex) {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", ex);
            } catch (IOException e) {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                    "Commit failed, please check server logs for failed items", e);
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to make this post");
        }

        JSONObject response = new JSONObject();
        response.put(Sjm.PROFILES, success);

        if (failure.length() > 0) {
            response.put("failed", failure);
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
