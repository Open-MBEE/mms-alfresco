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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.SerialJSONObject;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.util.TempFileProvider;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;

// ASSUMPTIONS & Interesting things about the new model get
// 1. always complete json for elements (no partial)
// 2. out of order json or different serialization may cause extra objects to
// be saved and updates to be made. this will happen until we have a better
// diff algorithm to understand whether an update was actually made or not

// TODO
// check for conflicts


public class ModelPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ModelPost.class);

    protected String artifactId = null;
    protected String extension = null;
    protected String content = null;
    protected String path = null;
    protected EmsScriptNode workspace = null;

    private final String NEWELEMENTS = "newElements";

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
        printHeader(user, logger, req, true);
        Timer timer = new Timer();

        Map<String, Object> result;
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();


        result = handleElementPost(req, status, user, contentType);


        printFooter(user, logger, timer);

        return result;
    }

    protected Map<String, Object> handleElementPost(final WebScriptRequest req, final Status status, String user,
        String contentType) {
        JSONObject newElementsObject = new JSONObject();
        SerialJSONObject results;
        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));
        boolean withChildViews = Boolean.parseBoolean(req.getParameter("childviews"));
        boolean overwriteJson = Boolean.parseBoolean(req.getParameter("overwrite"));

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        Map<String, Object> model = new HashMap<>();

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        try {
            SerialJSONObject postJson = new SerialJSONObject(req.getContent().getContent());
            this.populateSourceApplicationFromJson(postJson);
            Set<String> oldElasticIds = new HashSet<>();


            String comment = postJson.optString(Sjm.COMMENT);

            results = emsNodeUtil
                .processPostJson(postJson.getJSONArray(Sjm.ELEMENTS), user, oldElasticIds, overwriteJson,
                    this.requestSourceApplication, comment, Sjm.ELEMENT);

            String commitId = results.getJSONObject("commit").getString(Sjm.ELASTICID);

            if (CommitUtil
                .sendDeltas(results, projectId, refId, requestSourceApplication, services, withChildViews, false)) {
                if (!oldElasticIds.isEmpty()) {
                    emsNodeUtil.updateElasticRemoveRefs(oldElasticIds);
                }
                Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitId);

                if (withChildViews) {
                    for (int i = 0; i < results.getJSONArray(NEWELEMENTS).length(); i++) {
                        SerialJSONObject childViews = emsNodeUtil.addChildViews(results.getJSONArray(NEWELEMENTS).getJSONObject(i));
                        results.getJSONArray(NEWELEMENTS).replace(i, childViews);
                    }
                }

                newElementsObject.put(Sjm.ELEMENTS, extended ?
                    emsNodeUtil.addExtendedInformation(filterByPermission(results.getJSONArray(NEWELEMENTS), req)) :
                    filterByPermission(results.getJSONArray(NEWELEMENTS), req));
                if (results.has("rejectedElements")) {
                    newElementsObject.put(Sjm.REJECTED, results.getJSONArray("rejectedElements"));
                }
                newElementsObject.put(Sjm.COMMITID, commitId);
                newElementsObject.put(Sjm.TIMESTAMP, commitObject.get(Sjm.TIMESTAMP));
                newElementsObject.put(Sjm.CREATOR, user);

                if (prettyPrint) {
                    model.put(Sjm.RES, newElementsObject.toString(4));
                } else {
                    model.put(Sjm.RES, newElementsObject);
                }

                status.setCode(responseStatus.getCode());
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                    "Commit failed, please check server logs for failed items");
                model.put(Sjm.RES, createResponseJson());
            }

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
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
