/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S.
 * Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. - Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.Sjm;
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

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbEdgeTypes;
import gov.nasa.jpl.view_repo.db.PostgresHelper.DbNodeTypes;

/**
 * Model search service that returns a JSONArray of elements
 *
 * @author cinyoung
 */
public class ModelSearch extends ModelPost {
    static Logger logger = Logger.getLogger(ModelSearch.class);

    public ModelSearch() {
        super();
    }

    public ModelSearch(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelSearch instance = new ModelSearch(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();


        try {
            JSONObject top = new JSONObject();
            JSONArray elementsJson = executeSearchRequest(req, top);
            top.put("elements", elementsJson);

            if (!Utils.isNullOrEmpty(response.toString())) {
                top.put("message", response.toString());
            }
            model.put("res", top.toString());
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not create the JSON response");
            model.put("res", createResponseJson());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private JSONArray executeSearchRequest(WebScriptRequest req, JSONObject top) throws JSONException, IOException {

        JSONArray elements = new JSONArray();

        String projectId = getProjectId(req);
        String refId = getRefId(req);

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JSONObject json = (JSONObject) req.parseContent();
        boolean checkIfPropOrSlot = Boolean.parseBoolean(req.getParameter("checkType"));
        try {
            JSONArray elasticResult = emsNodeUtil.search(json);
            elasticResult = filterByPermission(elasticResult, req);
            Map<String, JSONArray> bins = new HashMap<>();
            for (int i = 0; i < elasticResult.length(); i++) {
                JSONObject e = elasticResult.getJSONObject(i);

                if(checkIfPropOrSlot){
                    if(e.getString(Sjm.TYPE).equals("Property")){
                        e = getJsonBySysmlid(projectId, refId, e.getString(Sjm.OWNERID));
                    } else if (e.getString(Sjm.TYPE).equals("Slot")){
                        e = getGrandOwnerJson(projectId, refId, e.getString(Sjm.OWNERID));
                    }
                    elasticResult.put(i,e);
                }

                String key = e.getString(Sjm.PROJECTID) + " " +  e.getString(Sjm.REFID);

                if (!bins.containsKey(key)) {
                    bins.put(key, new JSONArray());
                }
            }
            for (Entry<String, JSONArray> entry: bins.entrySet()) {
                String[] split = entry.getKey().split(" ");
                projectId = split[0];
                refId = split[1];
                EmsNodeUtil util = new EmsNodeUtil(projectId, refId);
                util.addExtendedInformation(entry.getValue());
                util.addExtraDocs(entry.getValue(), new HashMap<>());
            }
            return elasticResult;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return elements;
    }

    /**
     * Returns the JSON of the specified ownerId
     * @param projectId ID of project
     * @param refId     ref ID -- ie: master
     * @param sysmlId   SysML ID
     * @return JSONObject
     */
    private JSONObject getJsonBySysmlid(String projectId, String refId, String sysmlId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JSONObject node = emsNodeUtil.getById(sysmlId).toJson();
        // Have to remove the _ because the node property for elasticId doesn't contain it for some reason.
        return emsNodeUtil.getElementByElasticID(node.getString(Sjm.ELASTICID.replace("_", "")));
    }

    /**
     *
     * @param projectId
     * @param refId
     * @param sysmlId
     * @return
     */
    private JSONObject getGrandOwnerJson(String projectId, String refId, String sysmlId) {
        return getJsonBySysmlid(projectId, refId, getJsonBySysmlid(projectId, refId, sysmlId).getString(Sjm.OWNERID));
    }
}
