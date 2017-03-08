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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

// ASSUMPTIONS & Interesting things about the new model get
// 1. always complete json for elements (no partial)
// 2. out of order json or different serialization may cause extra objects to
// be saved and updates to be made. this will happen until we have a better
// diff algorithm to understand whether an update was actually made or not

// TODO
// check for conflicts


public class ModelPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ModelPost.class);

    public static boolean timeEvents = false;

    public ModelPost() {
        super();
    }

    public ModelPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        ModelPost instance = new ModelPost(repository, services);
        instance.setServices(getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(final WebScriptRequest req, final Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Object content;
        Map<String, Object> result = new HashMap<>();
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();
        boolean jsonNotK = !contentType.contains("application/k");
        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));
        String expressionString = req.getParameter("expression");
        WorkspaceNode myWorkspace = getWorkspace(req, user);
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        JSONObject commit = new JSONObject();
        JSONArray elements = null;
        JSONObject newElementsObject = new JSONObject();
        Date start = new Date();
        Date end = null;
        JSONArray ownersNotFound = new JSONArray();

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, myWorkspace);

        try {
            if (!jsonNotK) {
                content = req.getContent().getContent();
            } else {
                content = req.parseContent();
            }

            JSONObject postJson = getPostJson(true, content, expressionString);

            elements = emsNodeUtil.populateElements(postJson.getJSONArray("elements"));
            this.populateSourceApplicationFromJson(postJson);
            logger.debug(String.format("ModelPost processing %d elements.", elements.length()));

            // process elements to see which ones will go to the holding bin
            // decide which holding bin to place the element in, if there is no

            ownersNotFound = emsNodeUtil.getOwnersNotFound(elements, projectId);

            // some owners not found, bail with appropriate response
            if (ownersNotFound.length() > 0) {
                logger.debug(String.format("Owners not found: %s", ownersNotFound.toString()));
                JSONObject res = new JSONObject();
                res.put("ownersNotFound", ownersNotFound);
                res.put("message", response.toString());
                result.put("res", res);

                status.setCode(responseStatus.getCode());
                return result;
            }

            Map<String, JSONObject> foundElements = new HashMap<>();
            Map<String, String> foundParentElements = new HashMap<>();

            JSONArray processed = emsNodeUtil.processElements(elements, user, foundElements);
            processed = emsNodeUtil.processImageData(processed, myWorkspace);
            JSONObject results = emsNodeUtil.insertIntoElastic(processed, foundParentElements);
            JSONObject formattedCommit = emsNodeUtil.processCommit(results, user, foundElements, foundParentElements);
            // this logic needs to be fixed because emsNodesUtil does not pass a formatted commit
            emsNodeUtil.insertCommitIntoElastic(formattedCommit);
            String commitResults = formattedCommit.getJSONObject("commit").getString(Sjm.ELASTICID);
            logger.debug(String.format("Processing finished\nIndexed: %s", results));
            JSONArray newElements = results.getJSONArray("newElements");

            results = emsNodeUtil.addCommitId(results, commitResults);

            // :TODO this object is not the formatted commit object
            commit.put("workspace2", results);
            commit.put("_creator", user);


            CommitUtil.sendDeltas(commit, commitResults, projectId, myWorkspace == null ? null : myWorkspace.getId(),
                requestSourceApplication);

            Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitResults);

            newElementsObject.put("elements", extended ? emsNodeUtil.addExtendedInformation(newElements) : newElements);
            newElementsObject.put("commitId", commitResults);
            newElementsObject.put("_timestamp", commitObject.get("timestamp"));
            newElementsObject.put("_creator", user);
            // Timestamp needs to be ISO format
            result.put("res", newElementsObject.toString(4));

            status.setCode(responseStatus.getCode());

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        end = new Date();

        printFooter(user, logger, timer);

        return result;
    }

    private JSONObject getPostJson(boolean jsonNotK, Object content, String expressionString) throws JSONException {
        JSONObject postJson = null;

        if (!jsonNotK) {
            // TODO
        } else {
            if (content instanceof JSONObject) {
                postJson = (JSONObject) content;
            } else if (content instanceof String) {
                postJson = new JSONObject((String) content);
            }
        }
        if (postJson == null)
            postJson = new JSONObject();

        JSONArray jarr = postJson.optJSONArray("elements");
        if (jarr == null) {
            jarr = new JSONArray();
            postJson.put("elements", jarr);
        }

        return postJson;
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }
        }

        return true;
    }

}
