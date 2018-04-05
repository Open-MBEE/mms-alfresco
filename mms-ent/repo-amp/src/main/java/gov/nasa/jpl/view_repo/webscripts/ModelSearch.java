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
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.db.ElasticHelper;


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
            boolean noprocess = Boolean.parseBoolean(req.getParameter("literal"));
            JsonObject json = JsonUtil.buildFromString(req.getContent().getContent());
            if (noprocess) {
                ElasticHelper eh = new ElasticHelper();
                JsonObject result = eh.searchLiteral(json);
                model.put(Sjm.RES, result.toString());
            } else {
                JsonObject top = executeSearchRequest(req, json);

                if (!Utils.isNullOrEmpty(response.toString())) {
                    top.addProperty("message", response.toString());
                }
                model.put(Sjm.RES, top.toString());
            }
        } catch (IllegalStateException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "unable to get JSON object from request", e);
        } catch (JsonParseException e) {
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

    private JsonObject executeSearchRequest(WebScriptRequest req, JsonObject json) {
        JsonObject top = new JsonObject();

        String projectId = getProjectId(req);
        String refId = getRefId(req);

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        boolean checkIfPropOrSlot = Boolean.parseBoolean(req.getParameter("checkType"));
        try {
            top = emsNodeUtil.search(json);
            JsonArray elasticResult = top.get("elements").getAsJsonArray();
            elasticResult = filterByPermission(elasticResult, req);
            Map<String, JsonArray> bins = new HashMap<>();
            JsonArray finalResult = new JsonArray();
            Set<String> found = new HashSet<>();
            for (int i = 0; i < elasticResult.size(); i++) {
                JsonObject e = elasticResult.get(i).getAsJsonObject();

                if (checkIfPropOrSlot) {
                    String eprojId = e.get(Sjm.PROJECTID).getAsString();
                    String erefId = e.get(Sjm.REFID).getAsString();
                    JsonObject ownere = null;
                    if (e.get(Sjm.TYPE).getAsString().equals("Property")) {
                        ownere = getJsonBySysmlId(eprojId, erefId, e.get(Sjm.OWNERID).getAsString());
                    } else if (e.get(Sjm.TYPE).getAsString().equals("Slot")) {
                        ownere = getGrandOwnerJson(eprojId, erefId, e.get(Sjm.OWNERID).getAsString());
                    }
                    if (ownere != null && ownere.has(Sjm.SYSMLID) && !found.contains(ownere.get(Sjm.SYSMLID).getAsString())) {
                        finalResult.add(ownere);
                        String key = ownere.get(Sjm.PROJECTID).getAsString() + " " +  ownere.get(Sjm.REFID).getAsString();
                        if (!bins.containsKey(key)) {
                            bins.put(key, new JsonArray());
                        }
                        bins.get(key).add(ownere);
                        found.add(ownere.get(Sjm.SYSMLID).getAsString());
                    }
                }
                finalResult.add(e);
                found.add(e.get(Sjm.SYSMLID).getAsString());
                String key = e.get(Sjm.PROJECTID).getAsString() + " " +  e.get(Sjm.REFID).getAsString();
                if (!bins.containsKey(key)) {
                    bins.put(key, new JsonArray());
                }
                bins.get(key).add(e);
            }
            for (Entry<String, JsonArray> entry: bins.entrySet()) {
                String[] split = entry.getKey().split(" ");
                projectId = split[0];
                refId = split[1];
                EmsNodeUtil util = new EmsNodeUtil(projectId, refId);
                util.addExtendedInformation(entry.getValue());
                util.addExtraDocs(entry.getValue());
            }
            top.add("elements", finalResult);
            return top;
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return top;
    }

    /**
     * Returns the JSON of the specified ownerId
     * @param projectId ID of project
     * @param refId ref ID -- ie: master
     * @param sysmlId of the Element to find grandowner of
     * @return JSONObject
     */
    private JsonObject getJsonBySysmlId(String projectId, String refId, String sysmlId) {
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
    private JsonObject getGrandOwnerJson(String projectId, String refId, String sysmlId) {
        return getJsonBySysmlId(projectId, refId,
        		JsonUtil.getOptString(getJsonBySysmlId(projectId, refId, sysmlId), Sjm.OWNERID));
    }
}
