package gov.nasa.jpl.view_repo.webscripts;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.PandocConverter;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.LogUtil;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HtmlConverterPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(HtmlConverterPost.class);

    private StringBuffer response = new StringBuffer();
    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();

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
        JsonObject postJson = null;
        JsonObject result = new JsonObject();
        try {
            postJson = JsonUtil.buildFromString(req.getContent().getContent());
            if (postJson.has("name")) {

                String docName = JsonUtil.getOptString(postJson, Sjm.NAME);
                String projectId = getProjectId(req);
                String refId = getRefId(req);
                EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);
                JsonObject project = emsNodeUtil.getProject(projectId);
                String siteName = JsonUtil.getOptString(project, "orgId");

                // Convert mimetype to extension string
                String rawFormat = postJson.get("Accepts").getAsString();
                MimeType mimeType = allTypes.forName(rawFormat);
                String format = mimeType.getExtension();

                String filename = String.format("%s%s", docName, format);
                postJson.addProperty("filename", filename);
                EmsScriptNode artifact = createDoc(postJson, docName, siteName, projectId, refId, format);

                result.addProperty(Sjm.NAME, docName);
                result.addProperty("filename", filename);

                if (artifact != null) {
                    result.addProperty("status", "Conversion succeeded.");
                } else {
                    result.addProperty("status", "Conversion failed.");
                }

            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Job name not specified");
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        appendResponseStatusInfo(instance);
        status.setCode(responseStatus.getCode());

        if (postJson == null) {
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

    private EmsScriptNode createDoc(JsonObject postJson, String filename, String siteName, String projectId,
        String refId, String format) {

        PandocConverter pandocConverter = new PandocConverter(filename, format);

        Path filePath = Paths.get(PandocConverter.PANDOC_DATA_DIR + "/" + pandocConverter.getOutputFile());

        EmsScriptNode artifact = null;

        try {
            // Convert HTML to Doc
            pandocConverter.convert(JsonUtil.getOptString(postJson, "body"));

            String artifactId = postJson.get(Sjm.NAME).getAsString() + System.currentTimeMillis() + format;
            artifact = NodeUtil.updateOrCreateArtifact(artifactId, filePath, format, siteName, projectId, refId);

            sendEmail(artifact);
            if (artifact == null) {
                logger.error(String.format("Failed to create HTML to %s artifact in Alfresco.", format));
            } else {
                File file = filePath.toFile();
                if (!file.delete()) {
                    logger.error(String.format("Failed to delete the temp file %s", filename));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return artifact;
    }

    protected void sendEmail(EmsScriptNode node) {
        String status = (node != null) ? "completed" : "completed with errors";
        String subject = String.format("HTML to PDF generation %s.", status);
        String msg = buildEmailMessage(node);
        ActionUtil.sendEmailToModifier(node, msg, subject, services);
        if (logger.isDebugEnabled())
            logger.debug("Completed HTML to PDF generation.");

    }

    protected String buildEmailMessage(EmsScriptNode pdfNode) {
        StringBuffer buf = new StringBuffer();
        HostnameGet hostnameGet = new HostnameGet(this.repository, this.services);
        String contextUrl = hostnameGet.getAlfrescoUrl() + "/share/page/document-details?nodeRef=";

        if (pdfNode == null) {
            buf.append(
                "HTML to PDF generation completed with errors. Please review the below link for detailed information.");
        } else {
            buf.append("HTML to PDF generation succeeded.");
            buf.append(System.lineSeparator());
            buf.append(System.lineSeparator());
            buf.append("You can access the PDF file at ");
            buf.append(contextUrl + pdfNode.getNodeRef().getStoreRef().getProtocol() + "://" + pdfNode.getNodeRef()
                .getStoreRef().getIdentifier() + "/" + pdfNode.getNodeRef().getId());
        }

        buf.append(System.lineSeparator());
        buf.append(System.lineSeparator());

        return buf.toString();
    }
}
