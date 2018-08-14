/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 *
 * U.S. Government sponsorship acknowledged. All rights reserved.
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
import java.util.*;
import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;

public class MovePost extends ModelPost {

    private final String NEWELEMENTS = "newElements";

    public MovePost() {
        super();
    }

    public MovePost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        MovePost instance = new MovePost(repository, services);
        instance.setServices(getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(final WebScriptRequest req, final Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();

        printHeader(user, logger, req, true);

        Timer timer = new Timer();

        Map<String, Object> result;
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();

        // call move logic

        result = createDeltaForMove(req, user, status);

        printFooter(user, logger, timer);

        return result;
    }

    protected Map<String, Object> createDeltaForMove(final WebScriptRequest req, String user, final Status status) {
        JsonObject newElementsObject = new JsonObject();
        JsonObject results;

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        Map<String, Object> model = new HashMap<>();
        boolean withChildViews = true;

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        try {
            JsonObject postJson = JsonUtil.buildFromStream(req.getContent().getInputStream()).getAsJsonObject();

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Post Data: '%s'", postJson));
            }

            Set<String> oldElasticIds = new HashSet<>();
            JsonArray moved = emsNodeUtil.processMove(postJson.get(Sjm.MOVES).getAsJsonArray());
            results = emsNodeUtil
                .processPostJson(moved, user, oldElasticIds, false, this.requestSourceApplication, "", Sjm.ELEMENT);
            String commitId = results.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();

            int added = JsonUtil.getOptArray(results, "addedElements").size();
            int updated = JsonUtil.getOptArray(results, "updatedElements").size();
            int deleted = JsonUtil.getOptArray(results, "deletedElements").size();

            if (added + updated + deleted == 0) {
                JsonObject rejected = new JsonObject();
                rejected.addProperty(Sjm.CREATOR, user);
                rejected.add(Sjm.ELEMENTS, new JsonArray());
                JsonArray rejectedElements = JsonUtil.getOptArray(results, "rejectedElements");
                rejected.add(Sjm.REJECTED, rejectedElements);
                model.put(Sjm.RES, rejected);
            } else if (CommitUtil
                .sendDeltas(results, projectId, refId, requestSourceApplication, services, withChildViews, false)) {
                if (!oldElasticIds.isEmpty()) {
                    emsNodeUtil.updateElasticRemoveRefs(oldElasticIds, "element");
                }
                Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitId);

                if (withChildViews) {
                    JsonArray array = results.get(NEWELEMENTS).getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        array.set(i, emsNodeUtil.addChildViews(array.get(i).getAsJsonObject()));
                    }
                }

                newElementsObject.add(Sjm.ELEMENTS, filterByPermission(results.get(NEWELEMENTS).getAsJsonArray(), req));
                if (results.has("rejectedElements")) {
                    newElementsObject.add(Sjm.REJECTED, results.get("rejectedElements"));
                }

                newElementsObject.addProperty(Sjm.COMMITID, commitId);
                newElementsObject.addProperty(Sjm.TIMESTAMP, commitObject.get(Sjm.TIMESTAMP));
                newElementsObject.addProperty(Sjm.CREATOR, user);

                if (prettyPrint) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    model.put(Sjm.RES, gson.toJson(newElementsObject));
                } else {
                    model.put(Sjm.RES, newElementsObject);
                }

                status.setCode(responseStatus.getCode());
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                    "Commit failed, please check server logs for failed items");
                model.put(Sjm.RES, createResponseJson());
            }
        } catch (IllegalStateException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Unable to parse JSON request");
            //model.put(Sjm.RES, createResponseJson
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            model.put(Sjm.RES, createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        return model;
    }



    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null && !checkRequestVariable(elementId, "elementid")) {
            return false;
        }

        return checkRequestContent(req);
    }
}
