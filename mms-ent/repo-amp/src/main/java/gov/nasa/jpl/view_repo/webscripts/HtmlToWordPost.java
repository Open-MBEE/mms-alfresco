package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.PandocConverter;
import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.TempFileProvider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class HtmlToWordPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(HtmlToWordPost.class);

    protected String dirRoot = "/mnt/alf_data/temp/";
    protected String userHomeSubDirName; // format: docId_time
    protected EmsScriptNode nodeUserHomeSubDir;

    protected UUID guid = UUID.randomUUID();

    // filesystem working directory
    protected String fsWorkingDir; // format: dirRoot + "[USER_ID]/[GUID]

    private String user, storeName, nodeId, filename;

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

        JSONObject result = instance.getHtmlJson(req);
        String docName = req.getServiceMatch().getTemplateVars().get("documentName");

        if (result != null && result.has("name")) {

            String projectId = getProjectId(req);
            String refId = getRefId(req);
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            JSONObject project = emsNodeUtil.getProject(projectId);
            String siteName = project.optString("orgId", null);

            result.put("filename", String.format("%s.%s", docName, PandocConverter.OutputFormat.DOCX.getFormatName()));
            if (createWordDoc(result, docName, siteName, projectId, refId)) {
                result.put("status", "Conversion succeeded.");
            } else {
                result.put("status", "Conversion failed.");
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
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    result.put("message", response.toString());
                }
                model.put(Sjm.RES, result);
            } catch (JSONException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        printFooter(user, logger, timer);
        return model;
    }

    private JSONObject getHtmlJson(WebScriptRequest req) {
        JSONObject postJson = null;
        JSONObject reqPostJson = (JSONObject) req.parseContent();
        if (reqPostJson != null) {
            postJson = reqPostJson;
            if (reqPostJson.has("documents")) {
                JSONArray documents = reqPostJson.getJSONArray("documents");
                if (documents != null) {
                    postJson = documents.getJSONObject(0);
                }
            }
        }

        return postJson;
    }

    private boolean createWordDoc(JSONObject postJson, String filename, String siteName, String projectId,
        String refId) {

        PandocConverter pandocConverter = new PandocConverter(filename);
        String filePath = PandocConverter.PANDOC_DATA_DIR + "/" + pandocConverter.getOutputFile();
        boolean bSuccess = false;
        // Convert HTML to Word Doc
        try {
            pandocConverter.convert(postJson.optString("html"));
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
