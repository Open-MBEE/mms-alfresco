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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
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
import org.json.JSONArray;
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

    protected EmsScriptNode svgArtifact = null;
    protected EmsScriptNode pngArtifact = null;
    protected String artifactId = null;
    protected String extension = null;
    protected String content = null;
    protected String siteName = null;
    protected String path = null;
    protected WorkspaceNode workspace = null;
    protected Path pngPath = null;

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

        Map<String, Object> result = null;
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();

        if (contentType.contains("image") || contentType.contains("x-www-form-urlencoded")) {
            result = handleArtifactPost(req, status, user, contentType);
        } else {
            result = handleElementPost(req, status, user, contentType);
        }

        printFooter(user, logger, timer);

        return result;
    }

    protected Map<String, Object> handleElementPost(final WebScriptRequest req, final Status status, String user, String contentType) {
        JSONObject commit = new JSONObject();
        JSONObject newElementsObject = new JSONObject();
        boolean extended = Boolean.parseBoolean(req.getParameter("extended"));
        boolean withChildViews = Boolean.parseBoolean(req.getParameter("childviews"));
        WorkspaceNode myWorkspace = getWorkspace(req, user);

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        Map<String, Object> model = new HashMap<>();

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        try {

            JSONObject postJson = new JSONObject(req.getContent().getContent());
            this.populateSourceApplicationFromJson(postJson);
            //logger.debug(String.format("ModelPost processing %d elements.", elements.length()));

            Map<String, JSONObject> foundElements = new HashMap<>();
            Map<String, String> foundParentElements = new HashMap<>();

            JSONArray deletedElements = new JSONArray();
            JSONObject results = emsNodeUtil.insertIntoElastic(emsNodeUtil.processElements(emsNodeUtil.processImageData(emsNodeUtil.populateElements(postJson.getJSONArray(Sjm.ELEMENTS)), myWorkspace), user, foundElements, deletedElements), foundParentElements, deletedElements);

            JSONObject formattedCommit = emsNodeUtil.processCommit(results, user, foundElements, foundParentElements);
            // this logic needs to be fixed because emsNodesUtil does not pass a formatted commit
            emsNodeUtil.insertCommitIntoElastic(formattedCommit);
            String commitResults = formattedCommit.getJSONObject("commit").getString(Sjm.ELASTICID);

            //logger.debug(String.format("Processing finished\nIndexed: %s", results));

            results = emsNodeUtil.addCommitId(results, commitResults);

            // :TODO this object is not the formatted commit object
            commit.put("processed", results);
            commit.put(Sjm.CREATOR, user);

            if (CommitUtil.sendDeltas(commit, commitResults, projectId, refId, requestSourceApplication)) {

                Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitResults);

                if (withChildViews) {
                    for (int i = 0; i < results.getJSONArray("newElements").length(); i++) {
                        results.getJSONArray("newElements")
                            .put(i, emsNodeUtil.addChildViews(results.getJSONArray("newElements").getJSONObject(i)));
                    }
                }

                newElementsObject.put(Sjm.ELEMENTS, filterByPermission(extended ?
                    emsNodeUtil.addExtendedInformation(results.getJSONArray("newElements")) :
                    results.getJSONArray("newElements"), req));
                newElementsObject.put(Sjm.COMMITID, commitResults);
                newElementsObject.put(Sjm.TIMESTAMP, commitObject.get(Sjm.TIMESTAMP));
                newElementsObject.put(Sjm.CREATOR, user);
                // Timestamp needs to be ISO format

                if (prettyPrint) {
                    model.put("res", newElementsObject.toString(4));
                } else {
                    model.put("res", newElementsObject);
                }

                status.setCode(responseStatus.getCode());
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Commit failed, please check server logs for failed items");
                status.setCode(HttpServletResponse.SC_BAD_REQUEST);
                model.put("res", createResponseJson());
            }

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return model;
    }

    protected Map<String, Object> handleArtifactPost(final WebScriptRequest req, final Status status, String user, String contentType) {

        JSONObject resultJson = null;
        String filename = null;
        Map<String, Object> model = new HashMap<>();

        extension = contentType.replaceFirst(".*/(\\w+).*", "$1");

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        workspace = getWorkspace(req, AuthenticationUtil.getRunAsUser());

        try {
            Object binaryContent = req.getContent().getContent();
            content = binaryContent.toString();
        } catch(IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        // Get the site name from the request:
        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JSONObject project = emsNodeUtil.getProject(projectId);
        siteName = project.optString("orgId");

        if (siteName != null && validateRequest(req, status)) {

            try {
                // Get the artifact name from the url:
                String artifactIdPath = getArtifactId(req);

                logger.debug("ArtifactIdPath: " + artifactIdPath);
                logger.debug("Content: " + content);
                logger.debug("Header: " + req.getHeader("Content-Type"));

                if (artifactIdPath != null) {
                    int lastIndex = artifactIdPath.lastIndexOf("/");

                    if (artifactIdPath.length() > (lastIndex + 1)) {

                        path = projectId + "/refs/" + refId + "/" + (lastIndex != -1 ? artifactIdPath.substring(0, lastIndex) : "");
                        artifactId = lastIndex != -1 ? artifactIdPath.substring(lastIndex + 1) : artifactIdPath;
                        logger.error("artifactId: " + artifactId);

                        filename = extension != null ? artifactId + extension : artifactId;

                        // Create return json:
                        resultJson = new JSONObject();
                        resultJson.put("filename", filename);
                        // TODO: want full path here w/ path to site also, but Dorris doesnt use it,
                        //		 so leaving it as is.
                        resultJson.put("path", path);
                        resultJson.put("site", siteName);

                        // Update or create the artifact if possible:
                        if (!Utils.isNullOrEmpty(artifactId) && !Utils.isNullOrEmpty(content)) {

                            new EmsTransaction(this.services, this.response, this.responseStatus) {
                                @Override public void run() throws Exception {
                                    svgArtifact = NodeUtil.updateOrCreateArtifact(artifactId, extension, null, content,
                                        siteName, path, workspace, null, response, null, false);
                                }
                            };

                            if (svgArtifact == null) {
                                logger.error("Was not able to create the artifact!\n");
                                model.put("res", createResponseJson());
                            } else {
                                resultJson.put("upload", svgArtifact);
                                svgArtifact.getOrSetCachedVersion();
                                if (!NodeUtil.skipSvgToPng) {
                                    try {
                                        Path svgPath = saveSvgToFilesystem(artifactId, extension, content);
                                        pngPath = svgToPng(svgPath);

                                        new EmsTransaction(this.services, this.response, this.responseStatus) {

                                            @Override public void run() throws Exception {
                                                try {
                                                    pngArtifact = NodeUtil.updateOrCreateArtifactPng(svgArtifact,
                                                        pngPath, siteName, path, workspace, null, response,
                                                        null, false);
                                                } catch (Throwable ex) {
                                                    throw new Exception("Failed to convert SVG to PNG!\n");
                                                }
                                            }
                                        };

                                        if (pngArtifact == null) {
                                            logger.error("Failed to convert SVG to PNG!\n");
                                        } else {
                                            synchSvgAndPngVersions(svgArtifact, pngArtifact);
                                            pngArtifact.getOrSetCachedVersion();
                                        }
                                        Files.deleteIfExists(svgPath);
                                        Files.deleteIfExists(pngPath);
                                    } catch (Throwable ex) {
                                        logger.error("Failed to convert SVG to PNG!\n");
                                    }
                                }
                            }
                        } else {
                            logger.error("Invalid artifactId or no content!\n");
                            model.put("res", createResponseJson());
                        }
                    } else {
                        logger.error("Invalid artifactId!\\n");
                        model.put("res", createResponseJson());
                    }

                } else {
                    logger.error("artifactId not supplied!\\n");
                    model.put("res", createResponseJson());
                }

            } catch (JSONException e) {
                logger.error("Issues creating return JSON\\n");
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put("res", createResponseJson());
            }
        } else {
            logger.error("Invalid request, no sitename specified or no content provided!\\n");
            model.put("res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        if (!model.containsKey("res")) {
            model.put("res", resultJson != null ? resultJson : createResponseJson());
        }

        return model;
    }

    protected static Path saveSvgToFilesystem(String artifactId, String extension, String content) throws Throwable {
        byte[] svgContent = content.getBytes(Charset.forName("UTF-8"));
        File tempDir = TempFileProvider.getTempDir();
        Path svgPath = Paths.get(tempDir.getAbsolutePath(), String.format("%s%s", artifactId, extension));
        File file = new File(svgPath.toString());

        try (final InputStream in = new ByteArrayInputStream(svgContent)) {
            file.mkdirs();
            Files.copy(in, svgPath, StandardCopyOption.REPLACE_EXISTING);
            return svgPath;
        } catch (Throwable ex) {
            throw new Throwable("Failed to save SVG to filesystem. " + ex.getMessage());
        }
    }

    protected static Path svgToPng(Path svgPath) throws Throwable {
        Path pngPath = Paths.get(svgPath.toString().replace(".svg", ".png"));
        try (OutputStream png_ostream = new FileOutputStream(pngPath.toString())) {
            String svg_URI_input = svgPath.toUri().toURL().toString();
            TranscoderInput input_svg_image = new TranscoderInput(svg_URI_input);
            TranscoderOutput output_png_image = new TranscoderOutput(png_ostream);
            PNGTranscoder my_converter = new PNGTranscoder();
            my_converter.transcode(input_svg_image, output_png_image);
        } catch (Throwable ex) {
            throw new Throwable("Failed to convert SVG to PNG! " + ex.getMessage());
        }
        return pngPath;
    }

    protected static void synchSvgAndPngVersions(EmsScriptNode svgNode, EmsScriptNode pngNode) {
        Version svgVer = svgNode.getCurrentVersion();
        String svgVerLabel = svgVer.getVersionLabel();
        Double svgVersion = Double.parseDouble(svgVerLabel);

        Version pngVer = pngNode.getCurrentVersion();
        String pngVerLabel = pngVer.getVersionLabel();
        Double pngVersion = Double.parseDouble(pngVerLabel);

        int svgVerLen = svgNode.getEmsVersionHistory().length;
        int pngVerLen = pngNode.getEmsVersionHistory().length;

        while (pngVersion < svgVersion || pngVerLen < svgVerLen) {
            pngNode.makeSureNodeRefIsNotFrozen();
            pngNode.createVersion("creating the version history", false);
            pngVer = pngNode.getCurrentVersion();
            pngVerLabel = pngVer.getVersionLabel();
            pngVersion = Double.parseDouble(pngVerLabel);
            pngVerLen = pngNode.getEmsVersionHistory().length;
        }
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null) {
            // TODO - move this to ViewModelPost - really non hierarchical post
            if (!checkRequestVariable(elementId, "elementid")) {
                return false;
            }
        }

        return checkRequestContent(req);
    }
}
