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
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

/**
 * @author cinyoung
 */
public class ArtifactsGet extends ModelsGet {
    static Logger logger = Logger.getLogger(ArtifactsGet.class);

    public ArtifactsGet() {
        super();
    }

    public ArtifactsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
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
        JsonArray elementsJson = new JsonArray();
        JsonArray errors = new JsonArray();
        JsonObject result = new JsonObject();

        try {

            if (validateRequest(req, status)) {
                result = handleRequest(req);
                elementsJson = JsonUtil.getOptArray(result, Sjm.ARTIFACTS);
            }

            JsonObject top = new JsonObject();
            if (elementsJson != null && elementsJson.size() > 0) {
                JsonArray elements = filterByPermission(elementsJson, req);
                if (elements.size() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }

                top.add(Sjm.ARTIFACTS, elements);

                JsonArray errorMessages = parseErrors(result);

                if (errorMessages.size() > 0) {
                    top.add("messages", errorMessages);
                }

                if (prettyPrint || accept.contains("webp")) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    model.put(Sjm.RES, gson.toJson(top));
                } else {
                    model.put(Sjm.RES, top);
                }
            } else {
                log(Level.INFO, HttpServletResponse.SC_OK, "No elements found");
                top.add(Sjm.ARTIFACTS, new JsonArray());
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
    private JsonObject handleRequest(WebScriptRequest req)
        throws IOException, SQLException {
        JsonObject requestJson = JsonUtil.buildFromString(req.getContent().getContent());
        if (requestJson.has(Sjm.ARTIFACTS)) {
            JsonArray elementsToFindJson = requestJson.get(Sjm.ARTIFACTS).getAsJsonArray();

            String refId = getRefId(req);
            String projectId = getProjectId(req);
            String commitId = req.getParameter(Sjm.COMMITID.replace("_",""));

            JsonObject mountsJson = new JsonObject();
            mountsJson.addProperty(Sjm.SYSMLID, projectId);
            mountsJson.addProperty(Sjm.REFID, refId);

            JsonArray found = new JsonArray();
            JsonObject result = new JsonObject();

            Set<String> uniqueElements = new HashSet<>();
            for (int i = 0; i < elementsToFindJson.size(); i++) {
                uniqueElements.add(elementsToFindJson.get(i).getAsJsonObject().get(Sjm.SYSMLID).getAsString());
            }

            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            JsonObject commitObject = emsNodeUtil.getCommitObject(commitId);

            String timestamp =
                commitObject != null && commitObject.has(Sjm.CREATED) ? commitObject.get(Sjm.CREATED).getAsString() : null;

            EmsNodeUtil.handleMountSearch(mountsJson, false, false, 0L, uniqueElements, found, timestamp, "artifacts");
            result.add(Sjm.ARTIFACTS, found);
            JsonUtil.addStringSet(result, Sjm.FAILED, uniqueElements);
            return result;
        } else {
            return new JsonObject();
        }
    }
}

