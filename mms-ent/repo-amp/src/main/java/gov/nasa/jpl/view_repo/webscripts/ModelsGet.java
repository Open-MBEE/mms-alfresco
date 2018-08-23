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
import java.util.*;

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
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.TimeUtils;
import gov.nasa.jpl.mbee.util.Timer;

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

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

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ModelsGet instance = new ModelsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();

        if (logger.isDebugEnabled()) {
            printHeader(user, logger, req);
        } else {
            printHeader(user, logger, req, true);
        }

        Timer timer = new Timer();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        Map<String, Object> model = new HashMap<>();
        Map<String, Set<String>> errors = new HashMap<>();
        JsonArray elementsJson = new JsonArray();
        JsonObject result = new JsonObject();

        try {

            if (validateRequest(req, status)) {
                try {
                    Long depth = getDepthFromRequest(req);
                    result =
                        (!req.getContent().getContent().isEmpty()) ? handleRequest(req, depth, "elements") : getAllElements(req);
                    elementsJson = JsonUtil.getOptArray(result, Sjm.ELEMENTS);
                } catch (IllegalStateException e) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "unable to get JSON object from request", e);
                } catch (JsonParseException e) {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Malformed JSON request", e);
                }
            }

            JsonObject top = new JsonObject();
            if (elementsJson.size() > 0) {
                JsonArray elements = filterByPermission(elementsJson, req);
                if (elements.size() == 0) {
                    log(Level.ERROR, HttpServletResponse.SC_FORBIDDEN, "Permission denied.");
                }
                top.add(Sjm.ELEMENTS, elements);
            } else {
                log(Level.INFO, HttpServletResponse.SC_NOT_FOUND, "No elements found");
                top.add(Sjm.ELEMENTS, new JsonArray());
            }

            if (!elementsToFind.isEmpty()) {
                errors.put(Sjm.FAILED, elementsToFind);
                log(Level.ERROR, HttpServletResponse.SC_OK, "Some elements not found.");
            }

            JsonArray errorMessages = parseErrors(errors);

            if (errorMessages.size() > 0) {
                top.add("messages", errorMessages);
            }

            if (prettyPrint || accept.contains("webp")) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                model.put(Sjm.RES, gson.toJson(top));
            } else {
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
     * Wrapper for handling a request for all elements in a project and ref and getting the appropriate JSONArray of
     * elements
     *
     * @param req
     * @return
     * @throws IOException
     */
    private JsonObject getAllElements(WebScriptRequest req) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));
        String commitId = req.getParameter(Sjm.COMMITID.replace("_", ""));

        JsonObject extendedElements = new JsonObject();
        JsonObject result = new JsonObject();

        if (commitId == null) {
            List<String> elementsToFindJson = emsNodeUtil.getModel();
            JsonArray elements = emsNodeUtil.getJsonByElasticIds(elementsToFindJson, false);
            result.add(Sjm.ELEMENTS, elements);
        } else {
            result = emsNodeUtil.getModelAtCommit(commitId);
        }
        if (extended && commitId == null) {
            extendedElements
                .add(Sjm.ELEMENTS, emsNodeUtil.addExtendedInformation(result.get(Sjm.ELEMENTS).getAsJsonArray()));
            return extendedElements;
        }
        return result;
    }
}

