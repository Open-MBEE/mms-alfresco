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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.surf.util.Content;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;

import org.springframework.extensions.webscripts.servlet.FormData;

public class ArtifactPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ArtifactPost.class);

    protected EmsScriptNode artifact = null;
    protected String filename = null;
    protected String finalContentType = null;
    protected String artifactId = null;
    protected String extension = null;
    protected String content = null;
    protected String siteName = null;
    protected EmsScriptNode workspace = null;
    protected Path filePath = null;
    protected String mimeType = null;
    protected String encoding = null;

    private final String NEWELEMENTS = "newElements";

    public ArtifactPost() {
        super();
    }

    public ArtifactPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {

        ArtifactPost instance = new ArtifactPost(repository, services);
        instance.setServices(getServices());

        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(final WebScriptRequest req, final Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req, true);
        Timer timer = new Timer();

        Map<String, Object> result = new HashMap<>();
        JsonObject postJson = new JsonObject();

        FormData formData = (FormData) req.parseContent();
        FormData.FormField[] fields = formData.getFields();
        try {
            for (FormData.FormField field : fields) {
                if (logger.isDebugEnabled()) {
                    logger.debug("field.getName(): " + field.getName());
                }
                if (field.getName().equals("file") && field.getIsFile()) {
                    //String extension = FilenameUtils.getExtension();
                    //String filenameString = field.getFilename().substring(0, field.getFilename().lastIndexOf('.') - 1);
                    filename = field.getFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
                    Content tempContent = field.getContent();
                    mimeType = tempContent.getMimetype();
                    encoding = tempContent.getEncoding();

                    filePath = EmsNodeUtil.saveToFilesystem(filename, field.getInputStream());
                    content = new String(Files.readAllBytes(filePath));
                    finalContentType = Files.probeContentType(filePath);

                    if (logger.isDebugEnabled()) {
                        logger.debug("filename: " + filename);
                        logger.debug("mimetype: " + mimeType);
                        logger.debug("finalMimetype: " + finalContentType);
                        logger.debug("encoding: " + encoding);
                        logger.debug("content: " + content);
                    }
                } else {
                    String name = field.getName();
                    String value = field.getValue();
                    postJson.addProperty(name, value);
                    if (logger.isDebugEnabled()) {
                        logger.debug("property name: " + name);
                    }
                }
            }
            postJson.addProperty(Sjm.TYPE, "Artifact");
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Throwable t) {
            logger.error(String.format("%s", LogUtil.getStackTrace(t)));
        }

        // Would ideally be a transaction, :TODO the image has to be successfully posted before the json is post to the db
        // maybe processArtifactDelta needs to be called from handleArtifactPost
        if (handleArtifactPost(req, status, user, postJson)) {
            result = processArtifactDelta(req, user, postJson, status);
        }

        printFooter(user, logger, timer);

        return result;
    }

    protected Map<String, Object> processArtifactDelta(final WebScriptRequest req, String user, JsonObject postJson,
        final Status status) {
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        Map<String, Object> model = new HashMap<>();
        JsonObject newElementsObject = new JsonObject();
        JsonObject results;

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        try {
            JsonArray delta = new JsonArray();
            postJson.addProperty(Sjm.CHECKSUM, EmsNodeUtil.md5Hash(filePath.toFile()));
            delta.add(postJson);
            this.populateSourceApplicationFromJson(postJson);
            Set<String> oldElasticIds = new HashSet<>();
            results = emsNodeUtil.processPostJson(delta, user, oldElasticIds, false, this.requestSourceApplication,
                JsonUtil.getOptString(postJson, "comment"), Sjm.ARTIFACT);
            String commitId = results.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();
            if (CommitUtil.sendDeltas(results, projectId, refId, requestSourceApplication, services, false, true)) {
                if (!oldElasticIds.isEmpty()) {
                    emsNodeUtil.updateElasticRemoveRefs(oldElasticIds, "artifact");
                }
                Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitId);
                newElementsObject
                    .add(Sjm.ARTIFACTS, filterByPermission(results.get(NEWELEMENTS).getAsJsonArray(), req));
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
            return model;
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        return model;
    }

    boolean handleArtifactPost(final WebScriptRequest req, final Status status, String user, JsonObject postJson) {

        JsonObject resultJson = null;
        Map<String, Object> model = new HashMap<>();

        // Replace with true content type
        postJson.addProperty(Sjm.CONTENTTYPE, finalContentType);

        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JsonObject project = emsNodeUtil.getProject(projectId);
        siteName = JsonUtil.getOptString(project, "orgId");

        if (validateRequest(req, status) && !siteName.isEmpty()) {

            extension = FilenameUtils.getExtension(filename);
            artifactId = postJson.get(Sjm.SYSMLID).getAsString();

            // Create return json:
            resultJson = new JsonObject();
            resultJson.addProperty("filename", filename);
            // TODO: want full path here w/ path to site also, but Doris does not use it,
            //		 so leaving it as is.
            //resultJson.put("path", path);
            //resultJson.put("site", siteName);

            // Update or create the artifact if possible:
            if (!Utils.isNullOrEmpty(artifactId) && !Utils.isNullOrEmpty(content)) {
                String alfrescoId = artifactId + System.currentTimeMillis() + "." + extension;
                // :TODO check against checksum first, md5hash(content), if matching return the previous version

                if (filePath != null) {
                    artifact = NodeUtil
                        .updateOrCreateArtifact(alfrescoId, filePath, JsonUtil.getOptString(postJson, Sjm.CONTENTTYPE),
                            siteName, projectId, refId);
                }

                if (artifact == null) {
                    log(HttpServletResponse.SC_BAD_REQUEST, "Was not able to create the artifact!\n");
                    model.put(Sjm.RES, createResponseJson());
                } else {
                    String url = artifact.getUrl();
                    if (url != null) {
                        postJson.addProperty(Sjm.LOCATION, url.replace("/d/d/", "/service/api/node/content/"));
                    }
                }
            } else {
                log(HttpServletResponse.SC_BAD_REQUEST, "artifactId not supplied or content is empty!");
                model.put(Sjm.RES, createResponseJson());
            }
        } else {
            log(HttpServletResponse.SC_BAD_REQUEST, "Invalid request, no sitename specified or no content provided!");
            model.put(Sjm.RES, createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        if (!model.containsKey(Sjm.RES)) {
            model.put(Sjm.RES, resultJson != null ? resultJson : createResponseJson());
        }

        return true;
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null && !checkRequestVariable(elementId, "elementid")) {
            return false;
        }

        return checkRequestContent(req);
    }
}
