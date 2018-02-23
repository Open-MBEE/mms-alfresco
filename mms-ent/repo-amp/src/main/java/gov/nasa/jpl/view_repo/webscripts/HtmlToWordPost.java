package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.PandocConverter;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HtmlToWordPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(HtmlToWordPost.class);

    public HtmlToWordPost() {
        super();
    }

    public HtmlToWordPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest reg, Status status) {
        return false;
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        HtmlToWordPost instance = new HtmlToWordPost(repository, services);
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        HtmlToWordPost instance = new HtmlToWordPost(repository, services);

        JsonObject result = instance.getHtmlJson(req);
        String docName = req.getServiceMatch().getTemplateVars().get("documentName");

        if (result != null && result.has("name")) {

            String projectId = getProjectId(req);
            String refId = getRefId(req);
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            JsonObject project = emsNodeUtil.getProject(projectId);
            String siteName = JsonUtil.getOptString(project, "orgId");

            result.addProperty("filename", String.format("%s.%s", docName, PandocConverter.OutputFormat.DOCX.getFormatName()));
            if (createWordDoc(result, docName, siteName, projectId, refId)) {
                result.addProperty("status", "Conversion succeeded.");
            } else {
                result.addProperty("status", "Conversion failed.");
            }
            result.remove("html");

        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Job name not specified");
        }

        appendResponseStatusInfo(instance);
        status.setCode(responseStatus.getCode());

        if (result == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            if (!Utils.isNullOrEmpty(response.toString())) {
                result.addProperty("message", response.toString());
            }
            model.put(Sjm.RES, result);
        }

        printFooter(user, logger, timer);
        return model;
    }

    private JsonObject getHtmlJson(WebScriptRequest req) {
        JsonParser parser = new JsonParser();
        JsonObject postJson = null;
        try {
            JsonElement reqPostJsonElem = parser.parse(req.getContent().getContent());
            JsonObject reqPostJson = reqPostJsonElem.getAsJsonObject();
            if (reqPostJson != null) {
                postJson = reqPostJson;
                if (reqPostJson.has("documents")) {
                    JsonArray documents = reqPostJson.get("documents").getAsJsonArray();
                    if (documents != null) {
                        postJson = documents.get(0).getAsJsonObject();
                    }
                }
            }
        } catch (IllegalStateException e) { 
            logger.error(String.format("unable to get JSON object from request: %s", e.toString()));
        } catch (JsonParseException e) {
            logger.error(String.format("Could not parse JSON request: %s", e.toString()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return postJson;
    }

    private boolean createWordDoc(JsonObject postJson, String filename, String siteName, String projectId,
        String refId) {

        PandocConverter pandocConverter = new PandocConverter(filename);
        String filePath = PandocConverter.PANDOC_DATA_DIR + "/" + pandocConverter.getOutputFile();
        boolean bSuccess = false;
        // Convert HTML to Word Doc
        try {
            pandocConverter.convert(JsonUtil.getOptString(postJson, "html"));
        } catch (Exception e) {
            logger.error(String.format("%s", e.getMessage()));
            return false;
        }

        String encodedBase64;
        FileInputStream binFile;
        File file = new File(filePath);

        try {
            binFile = new FileInputStream(filePath);
            byte[] content = new byte[(int) file.length()];
            binFile.read(content);
            encodedBase64 = new String(Base64.getEncoder().encode(content));

            EmsScriptNode artifact = NodeUtil
                .updateOrCreateArtifact(filename, PandocConverter.OutputFormat.DOCX.getFormatName(), encodedBase64,
                    null, siteName, projectId, refId, null, response, null, false);

            if (artifact == null) {
                logger.error("Failed to create HTML to Docx artifact in Alfresco.");
            } else {
                binFile.close();
                bSuccess = true;
                if (!file.delete()) {
                    logger.error(String.format("Failed to delete the temp file %s", filename));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return bSuccess;
    }
}
