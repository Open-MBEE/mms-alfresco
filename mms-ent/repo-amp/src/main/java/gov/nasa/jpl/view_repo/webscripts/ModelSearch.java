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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.mbee.util.Utils;
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
            JSONArray elementsJson = executeSearchRequest(req);
            top.put("elements", elementsJson);

            if (!Utils.isNullOrEmpty(response.toString())) {
                top.put("message", response.toString());
            }
            model.put(Sjm.RES, top.toString());
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse the JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        status.setCode(responseStatus.getCode());
        if (model.isEmpty()) {
            model.put(Sjm.RES, createResponseJson());
        }
        printFooter(user, logger, timer);

        return model;
    }

    private JSONArray executeSearchRequest(WebScriptRequest req) throws JSONException, IOException {

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
            JSONArray finalResult = new JSONArray();
            Set<String> found = new HashSet<>();
            for (int i = 0; i < elasticResult.length(); i++) {
                JSONObject e = elasticResult.getJSONObject(i);

                if (checkIfPropOrSlot) {
                    String eprojId = e.getString(Sjm.PROJECTID);
                    String erefId = e.getString(Sjm.REFID);
                    JSONObject ownere = null;
                    if (e.getString(Sjm.TYPE).equals("Property")) {
                        ownere = getJsonBySysmlId(eprojId, erefId, e.getString(Sjm.OWNERID));
                    } else if (e.getString(Sjm.TYPE).equals("Slot")) {
                        ownere = getGrandOwnerJson(eprojId, erefId, e.getString(Sjm.OWNERID));
                    }
                    if (ownere != null && ownere.has(Sjm.SYSMLID) && !found.contains(ownere.getString(Sjm.SYSMLID))) {
                        finalResult.put(ownere);
                        String key = ownere.getString(Sjm.PROJECTID) + " " +  ownere.getString(Sjm.REFID);
                        if (!bins.containsKey(key)) {
                            bins.put(key, new JSONArray());
                        }
                        bins.get(key).put(ownere);
                        found.add(ownere.getString(Sjm.SYSMLID));
                    }
                }
                finalResult.put(e);
                found.add(e.getString(Sjm.SYSMLID));
                String key = e.getString(Sjm.PROJECTID) + " " +  e.getString(Sjm.REFID);
                if (!bins.containsKey(key)) {
                    bins.put(key, new JSONArray());
                }
                bins.get(key).put(e);
            }
            for (Entry<String, JSONArray> entry: bins.entrySet()) {
                String[] split = entry.getKey().split(" ");
                projectId = split[0];
                refId = split[1];
                EmsNodeUtil util = new EmsNodeUtil(projectId, refId);
                util.addExtendedInformation(entry.getValue());
                util.addExtraDocs(entry.getValue());
            }
            return finalResult;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return elements;
    }

    /**
     * Returns the JSON of the specified ownerId
     * @param projectId ID of project
     * @param refId ref ID -- ie: master
     * @param sysmlId of the Element to find grandowner of
     * @return JSONObject
     */
    private JSONObject getJsonBySysmlId(String projectId, String refId, String sysmlId) {
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        return emsNodeUtil.getNodeBySysmlid(sysmlId);
    }

    /**
     * Calls the method getJsonBySysmlId twice, once on the SysMLID of the owner, then again on the result ownerId.
     * Thus, returns the grandowner of the specified sysmlId.
     * @param projectId ID of project
     * @param refId ref ID -- ie: master
     * @param sysmlId of the Element to find grandowner of
     * @return JSONObject
     */
    private JSONObject getGrandOwnerJson(String projectId, String refId, String sysmlId) {
        return getJsonBySysmlId(projectId, refId, getJsonBySysmlId(projectId, refId, sysmlId).optString(Sjm.OWNERID));
    }
}
