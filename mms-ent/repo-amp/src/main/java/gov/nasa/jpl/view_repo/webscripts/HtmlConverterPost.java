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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HtmlConverterPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(HtmlConverterPost.class);

    public HtmlConverterPost() {
        super();
    }

    public HtmlConverterPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest reg, Status status) {
        return false;
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        HtmlConverterPost instance = new HtmlConverterPost(repository, services);
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        HtmlConverterPost instance = new HtmlConverterPost(repository, services);

        JSONObject result = instance.getHtmlJson(req);
        String docName = req.getServiceMatch().getTemplateVars().get("documentName");

        if (result != null && result.has("name")) {

            String projectId = getProjectId(req);
            String refId = getRefId(req);
            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
            JSONObject project = emsNodeUtil.getProject(projectId);
            String siteName = project.optString("orgId", null);

            String format = result.getString("format");
            result.put("filename", String.format("%s.%s", docName, format));
            if (createDoc(result, docName, siteName, projectId, refId, format)) {
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

    private boolean createDoc(JSONObject postJson, String filename, String siteName, String projectId, String refId,
        String format) {

        PandocConverter pandocConverter = new PandocConverter(filename, format);

        String filePath = PandocConverter.PANDOC_DATA_DIR + "/" + pandocConverter.getOutputFile();
        boolean bSuccess = false;

        // Convert HTML to Doc
        pandocConverter.convert(postJson.optString("body"));

        String encodedBase64;
        FileInputStream binFile;
        File file = new File(filePath);

        try {
            binFile = new FileInputStream(filePath);
            byte[] content = new byte[(int) file.length()];
            binFile.read(content);
            encodedBase64 = new String(Base64.getEncoder().encode(content));

            EmsScriptNode artifact = NodeUtil
                .updateOrCreateArtifact(filename, format, encodedBase64, null, siteName, projectId, refId, null,
                    response, null, false);

            if (artifact == null) {
                logger.error(String.format("Failed to create HTML to %s artifact in Alfresco.", format));
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
