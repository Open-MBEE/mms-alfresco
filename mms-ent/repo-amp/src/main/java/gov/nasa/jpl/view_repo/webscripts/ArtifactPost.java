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

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.extensions.webscripts.servlet.FormData.FormField;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.EmsTransaction;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 * Posts artifacts.  Replaces artifact.post.js, as this version is workspace aware.
 *
 * @author gcgandhi
 */
public class ArtifactPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ArtifactPost.class);

    protected EmsScriptNode svgArtifact = null;
    protected EmsScriptNode pngArtifact = null;
    protected String artifactId = null;
    protected String extension = null;
    protected String content = null;
    protected String siteName = null;
    protected String path = null;
    protected WorkspaceNode workspace = null;
    protected Path pngPath = null;

    public ArtifactPost() {
        super();
    }

    public ArtifactPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }

    /**
     * Entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ArtifactPost instance = new ArtifactPost(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        JSONObject resultJson = null;
        String filename = null;
        Map<String, Object> model = new HashMap<>();

        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        //        String cs = req.getParameter("cs"); // Ignoring this b/c we calculate it from the data
        extension = req.getParameter("extension") != null ? req.getParameter("extension") : ".svg";

        if (!extension.startsWith(".")) {
            extension = "." + extension;
        }

        workspace = getWorkspace(req, AuthenticationUtil.getRunAsUser());

        // Get the content from the form data:
        FormData formData = (FormData) req.parseContent();
        for (FormField field : formData.getFields()) {
            if (field.getName().equals("content") && field.getIsFile()) {
                try {
                    content = field.getContent().getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Get the site name from the request:
        siteName = getSiteName(req);

        if (siteName != null && validateRequest(req, status)) {

            try {
                // Get the artifact name from the url:
                String artifactIdPath = getArtifactId(req);

                if (artifactIdPath != null) {
                    int lastIndex = artifactIdPath.lastIndexOf("/");

                    if (artifactIdPath.length() > (lastIndex + 1)) {

                        path = lastIndex != -1 ? artifactIdPath.substring(0, lastIndex) : "";
                        artifactId = lastIndex != -1 ? artifactIdPath.substring(lastIndex + 1) : artifactIdPath;
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
                                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                                                "Was not able to create the artifact!\n");
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
                                            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                                                            "Failed to convert SVG to PNG!\n");
                                        } else {
                                            synchSvgAndPngVersions(svgArtifact, pngArtifact);
                                            pngArtifact.getOrSetCachedVersion();
                                        }
                                        Files.deleteIfExists(svgPath);
                                        Files.deleteIfExists(pngPath);
                                    } catch (Throwable ex) {
                                        log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                                                        "Failed to convert SVG to PNG!\n");
                                    }
                                }
                            }
                        } else {
                            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId or no content!\n");
                            model.put("res", createResponseJson());
                        }
                    } else {
                        log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Invalid artifactId!\n");
                        model.put("res", createResponseJson());
                    }

                } else {
                    log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "artifactId not supplied!\n");
                    model.put("res", createResponseJson());
                }

            } catch (JSONException e) {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Issues creating return JSON\n");
                e.printStackTrace();
                model.put("res", createResponseJson());
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST,
                            "Invalid request, no sitename specified or no content provided!\n");
            model.put("res", createResponseJson());
        }

        status.setCode(responseStatus.getCode());
        if (!model.containsKey("res")) {
            model.put("res", resultJson != null ? resultJson : createResponseJson());
        }

        printFooter(user, logger, timer);

        return model;
    }

    protected Path saveSvgToFilesystem(String artifactId, String extension, String content) throws Throwable {
        byte[] svgContent = content.getBytes(Charset.forName("UTF-8"));
        File tempDir = TempFileProvider.getTempDir();
        Path svgPath = Paths.get(tempDir.getAbsolutePath(), String.format("%s%s", artifactId, extension));
        File file = new File(svgPath.toString());

        try (final InputStream in = new ByteArrayInputStream(svgContent);) {
            file.mkdirs();
            Files.copy(in, svgPath, StandardCopyOption.REPLACE_EXISTING);
            return svgPath;
        } catch (Throwable ex) {
            throw new Throwable("Failed to save SVG to filesystem. " + ex.getMessage());
        }
    }

    protected Path svgToPng(Path svgPath) throws Throwable {
        Path pngPath = Paths.get(svgPath.toString().replace(".svg", ".png"));
        try (OutputStream png_ostream = new FileOutputStream(pngPath.toString());) {
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

    protected void synchSvgAndPngVersions(EmsScriptNode svgNode, EmsScriptNode pngNode) {
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
}
