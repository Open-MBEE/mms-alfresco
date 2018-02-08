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

import gov.nasa.jpl.view_repo.util.*;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.util.TempFileProvider;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.FilenameUtils;
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
import gov.nasa.jpl.mbee.util.Utils;

import org.springframework.extensions.webscripts.servlet.FormData;
//import org.alfresco.repo.forms.FormData;



public class ArtifactPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ArtifactPost.class);

    protected EmsScriptNode svgArtifact = null;
    protected EmsScriptNode pngArtifact = null;
    protected String filename = null;
    protected String artifactId = null;
    protected String extension = null;
    protected String content = null;
    protected String siteName = null;
    protected String path = null;
    protected EmsScriptNode workspace = null;
    protected Path pngPath = null;

    private final String NEWELEMENTS = "newElements";

    public static boolean timeEvents = false;

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
        String imageId = getArtifactId(req);

        FormData formData = (FormData) req.parseContent();
        FormData.FormField[] fields = formData.getFields();
        try {
            for (FormData.FormField field : fields) {
                logger.debug("field.getName(): " + field.getName());
                if (field.getName().equals("file") && field.getIsFile()) {
                    filename = field.getFilename();
                    Object binaryContent = field.getContent().getContent();
                    content = binaryContent.toString();
                    logger.debug("filename: " + filename);
                    logger.debug("content: " + content);
                } else {
                    String name = field.getName();
                    String value = field.getValue();
                    postJson.addProperty(name, value);
                    logger.debug("property name: " + name);
                }
            }
            if (imageId != null && !imageId.isEmpty()) {
                postJson.addProperty(Sjm.SYSMLID, imageId);
            }
            postJson.addProperty(Sjm.TYPE, "Artifact");
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        // Would ideally be a transaction, :TODO the image has to be successfully posted before the json is post to the db
        // maybe processArtifactDelta needs to be called from handleArtifactPost
        if(handleArtifactPost(req, status, user, postJson)) {
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
            postJson.addProperty(Sjm.LOCATION, path);
            postJson.addProperty(Sjm.CHECKSUM, EmsNodeUtil.md5Hash(content));
            delta.add(postJson);
            this.populateSourceApplicationFromJson(postJson);
            Set<String> oldElasticIds = new HashSet<>();
            results = emsNodeUtil
                .processPostJson(delta, user, oldElasticIds, false, this.requestSourceApplication, Sjm.ARTIFACT);
            String commitId = results.get("commit").getAsJsonObject().get(Sjm.ELASTICID).getAsString();
            if (CommitUtil.sendDeltas(results, projectId, refId, requestSourceApplication, services, false, true)) {
                if (!oldElasticIds.isEmpty()) {
                    emsNodeUtil.updateElasticRemoveRefs(oldElasticIds);
                }
                Map<String, String> commitObject = emsNodeUtil.getGuidAndTimestampFromElasticId(commitId);
                newElementsObject.add(Sjm.ARTIFACTS, filterByPermission(results.get(NEWELEMENTS).getAsJsonArray(), req));
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

    boolean handleArtifactPost(final WebScriptRequest req, final Status status, String user,
        JsonObject postJson) {

        JsonObject resultJson = null;
        Map<String, Object> model = new HashMap<>();
        boolean isSvg = false;
        String contentType = req.getContentType() == null ? "" : req.getContentType().toLowerCase();
        if (!contentType.isEmpty() && JsonUtil.getOptString(postJson, Sjm.CONTENTTYPE).isEmpty()) {
            postJson.addProperty(Sjm.CONTENTTYPE, contentType);
        }

        String projectId = getProjectId(req);
        String refId = getRefId(req);
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
        JsonObject project = emsNodeUtil.getProject(projectId);
        siteName = JsonUtil.getOptString(project, "orgId");

        if (!siteName.isEmpty() && validateRequest(req, status)) {
        	extension = FilenameUtils.getExtension(filename);
        	artifactId = postJson.get(Sjm.SYSMLID).getAsString();
        	path = projectId + "/refs/" + refId + "/" + artifactId + System.currentTimeMillis() + "." + extension;
        	if (contentType.contains("svg") || JsonUtil.getOptString(postJson, Sjm.CONTENTTYPE).contains("svg") || extension.contains("svg")) {
        		isSvg = true;
        	}
        	// Create return json:
        	resultJson = new JsonObject();
        	resultJson.addProperty("filename", filename);
        	// TODO: want full path here w/ path to site also, but Doris does not use it,
        	//		 so leaving it as is.
        	resultJson.addProperty("path", path);
        	resultJson.addProperty("site", siteName);

        	// Update or create the artifact if possible:
        	if (!Utils.isNullOrEmpty(artifactId) && !Utils.isNullOrEmpty(content)) {

        		svgArtifact = NodeUtil
        				.updateOrCreateArtifact(artifactId, extension, null, content, siteName, projectId, refId, null,
        						response, null, false);

        		if (svgArtifact == null) {
        			logger.error("Was not able to create the artifact!\n");
        			model.put(Sjm.RES, createResponseJson());
        		} else {
        			resultJson.addProperty("upload", svgArtifact.toString());
        			if (isSvg) {
        				try {
        					Path svgPath = saveSvgToFilesystem(artifactId, extension, content);
        					pngPath = svgToPng(svgPath);

        					try {
        						pngArtifact = NodeUtil
        								.updateOrCreateArtifactPng(svgArtifact, pngPath, siteName, projectId, refId,
        										null, response, null, false);
        					} catch (Throwable ex) {
        						throw new Exception("Failed to convert SVG to PNG!\n");
        					}

        					if (pngArtifact == null) {
        						logger.error("Failed to convert SVG to PNG!\n");
        					} else {
        						synchSvgAndPngVersions(svgArtifact, pngArtifact);
        					}
        					Files.deleteIfExists(svgPath);
        					Files.deleteIfExists(pngPath);
        				} catch (IOException e) {
        					if (logger.isDebugEnabled()) {
        						logger.debug("Failed to convert image", e);
        					}
        				} catch (Throwable ex) {
        					logger.error("Failed to convert SVG to PNG!\n");
        				}
        			}
        		}
        	} else {
        		logger.error("artifactId not supplied!\\n");
        		model.put(Sjm.RES, createResponseJson());
        	}

        } else {
            logger.error("Invalid request, no sitename specified or no content provided!\\n");
            model.put(Sjm.RES, createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        if (!model.containsKey(Sjm.RES)) {
            model.put(Sjm.RES, resultJson != null ? resultJson : createResponseJson());
        }

        return true;
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
        if (svgPath.toString().contains(".png")) {
            return svgPath;
        }
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
            pngNode.createVersion("creating the version history", false);
            pngVer = pngNode.getCurrentVersion();
            pngVerLabel = pngVer.getVersionLabel();
            pngVersion = Double.parseDouble(pngVerLabel);
            pngVerLen = pngNode.getEmsVersionHistory().length;
        }
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String elementId = req.getServiceMatch().getTemplateVars().get("elementid");
        if (elementId != null && !checkRequestVariable(elementId, "elementid")) {
            return false;
        }

        return checkRequestContent(req);
    }
}
