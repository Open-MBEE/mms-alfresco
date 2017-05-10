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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            //        {
            //            "id": documentId,
            //            "projectId": projectId of document,
            //            "name": name of document,
            //            "refId": refid of document
            //            "_views": [
            //            {"name": name of view, "id": id of view, "refid": refid of view, "projectId": projectId of view] }
            // :TODO
            // 1) filter out/ mounts logic -- this is already done by projectsGet
            // 2) getChildren in postgresHelper with edge type and depth (view or childview)
            // 3) Do this twice once for view and once for childview
            // 4) not reporting 404 on empty return
            // paginate results
            top.put("elements", filterByPermission(elementsJson, req));
            if (top.length() == 0) {
                responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
            } else {
                top.put("elements", filterByPermission(elementsJson, req));
            }
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
        Map<String, String> elasticResult = emsNodeUtil.search(json);

        Set<String> elementList = new HashSet<>();
        elasticResult.forEach((key, value) -> {
            elementList.add(key);
        });

        try {
            ModelsGet.handleMountSearch(emsNodeUtil.getProjectWithFullMounts(projectId, refId, null), true, 0L, elementList, elements);
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return emsNodeUtil.addExtraDocs(elements, new HashMap<>());
    }

}
