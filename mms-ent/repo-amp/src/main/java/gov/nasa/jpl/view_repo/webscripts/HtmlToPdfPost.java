package gov.nasa.jpl.view_repo.webscripts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.LogUtil;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.GUID;
import org.alfresco.util.exec.RuntimeExec;
import org.alfresco.util.exec.RuntimeExec.ExecutionResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.actions.ActionUtil;
import gov.nasa.jpl.view_repo.actions.HtmlToPdfActionExecuter;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class HtmlToPdfPost extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(HtmlToPdfPost.class);


    class DBImage {
        private String id;
        private String filePath;

        public String getId() {
            return id;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }


    protected String dirRoot = "/mnt/alf_data/temp/";
    protected String userHomeSubDirName; // format: docId_time
    protected EmsScriptNode nodeUserHomeSubDir;

    protected UUID guid = UUID.randomUUID();

    // filesystem working directory
    protected String fsWorkingDir; // format: dirRoot + "[USER_ID]/[GUID]

    private String user, storeName, nodeId, filename;

    public HtmlToPdfPost() {
        super();
    }

    public HtmlToPdfPost(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override protected boolean validateRequest(WebScriptRequest reg, Status status) {
        return false;
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        HtmlToPdfPost instance = new HtmlToPdfPost(repository, services);

        JsonObject result = instance.saveAndStartAction(req, status);
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

    protected void setUserHomeSubDir(EmsScriptNode userHomeFolder, String docId) throws Throwable {
        log("Creating 'user home' subdirectory...");
        this.userHomeSubDirName = String.format("%s_%s", docId, new DateTime().getMillis());
        try {
            this.nodeUserHomeSubDir = userHomeFolder.createFolder(this.userHomeSubDirName, null, null);
        } catch (Throwable ex) {
            log(String.format("ERROR: Failed to create user-home subdirectory! %s", ex.getMessage()));
            throw new Throwable();
        }
    }

    public EmsScriptNode convert(String docId, String tagId, String timeStamp, String htmlContent, String coverContent,
        String toc, String tof, String tot, String indices, String headerContent, String footerContent, String docNum,
        String displayTime, String customCss, Boolean disabledCoverPage) {
        EmsScriptNode pdfNode = null;
        String htmlFilename = String.format("%s_%s.html", docId, timeStamp).replace(":", "");
        String pdfFilename = String.format("%s_%s.pdf", docId, timeStamp).replace(":", "");
        String coverFilename = String.format("%s_%s_cover.html", docId, timeStamp).replace(":", "");
        String zipFilename = String.format("%s_%s.zip", docId, timeStamp).replace(":", "");
        log(String.format("Converting HTML to PDF for document Id: %s, tag Id: %s, timestamp: %s...", docId, tagId,
            timeStamp));

        if (Utils.isNullOrEmpty(footerContent))
            footerContent =
                "Paper copies of this document may not be current and should not be relied on for official purposes. JPL/Caltech proprietary. Not for public release.";

        try {
            log("Getting 'run as user'...");
            this.user = AuthenticationUtil.getRunAsUser();
            EmsScriptNode userHomeFolder = getUserHomeFolder(user);

            setUserHomeSubDir(userHomeFolder, docId);
            log(String.format("Created \"%s\" subdirectory!", this.userHomeSubDirName));

            createUserFilesystemPath(user);
            String htmlPath =
                saveHtmlToFilesystem(htmlFilename, htmlContent, coverFilename, coverContent, toc, tof, tot, indices,
                    footerContent, headerContent, tagId, timeStamp, docNum, displayTime, customCss, disabledCoverPage);

            handleEmbeddedImage(coverFilename);
            handleEmbeddedImage(htmlFilename);

            String pdfSlidesPath = html2pdfSlides(htmlPath, pdfFilename, userHomeFolder);
            savePdfToRepo(pdfSlidesPath);

            addCssLinks(htmlPath);

            saveHtmlToRepo(htmlFilename, htmlContent);

            String pdfPath = html2pdf(docId, tagId, timeStamp, htmlPath, pdfFilename, userHomeFolder, customCss);

            pdfNode = savePdfToRepo(pdfPath);

            tableToCSV(htmlPath);

            String zipPath = zipWorkingDir(zipFilename);
            saveZipToRepo(zipPath);
        } catch (Throwable ex) {
            response.append(String
                .format("ERROR: Failed to convert HTML to PDF for document Id: %s, tag Id: %s, timestamp: %s! %s",
                    docId, tagId, timeStamp, ex.getMessage()));
            logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
        }
        return pdfNode;
    }

    protected EmsScriptNode getUserHomeFolder(String user) {
        log("Retrieving 'user home'...", true);
        return NodeUtil.getUserHomeFolder(user, true);
    }

    protected void createUserFilesystemPath(String user) throws Throwable {
        log("Creating filesystem directory to store working files...");
        Path userPath = Paths.get(dirRoot, user, this.guid.toString());
        this.fsWorkingDir = userPath.toString();

        try {
            File up = new File(this.fsWorkingDir);
            if (!up.exists()) {
                up.mkdirs();
            }
        } catch (Throwable ex) {
            if (!Files.exists(Paths.get(this.fsWorkingDir))) {
                log(String.format("Unable to create directory %s...%s", this.fsWorkingDir, ex.getMessage()));
                throw new Throwable();
            }
        }
    }

    protected EmsScriptNode saveHtmlToRepo(String htmlFilename, String htmlContent) throws Throwable {
        log(String.format("Saving %s to repository...", htmlFilename));
        Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);

        if (!Files.exists(htmlPath)) {
            try {
                return saveStringToRepo(htmlFilename, htmlContent, "text/html");
            } catch (Throwable ex) {
                response.append("Failed to save HTML content to repository!");
                throw new Throwable();
            }
        } else {
            try {
                return saveFileToRepo(htmlFilename, "text/html", htmlPath.toString());
            } catch (Throwable ex) {
                response.append(ex.getMessage());
                response.append(String.format("Failed to save %s to repository!", htmlFilename));
                throw new Throwable();
            }
        }
    }

    protected EmsScriptNode createScriptNode(String filename) {
        EmsScriptNode node = NodeUtil.getOrCreateContentNode(this.nodeUserHomeSubDir, filename, services);
        if (node == null)
            response.append(String.format("Failed to create nodeRef for %s", filename));
        return node;
    }

    protected EmsScriptNode saveStringToRepo(String filename, String content, String mimeType) throws Throwable {
        EmsScriptNode node = createScriptNode(filename);
        if (node == null)
            throw new Throwable();
        ActionUtil.saveStringToFile(node, mimeType, services, content);
        return node;
    }

    protected void savePdfSlideCss(String htmlPath) throws Throwable {
        StringBuffer css = new StringBuffer();
        css.append(
            "img {max-width: 100%; page-break-inside: avoid; page-break-before: auto; page-break-after: auto; display: block;}");
        css.append("tr, td, th { page-break-inside: avoid; } thead {display: table-header-group;}");
        css.append(".pull-right {float: right;}");
        css.append(".view-title {margin-top: 10pt}");
        css.append(".chapter {page-break-before: always}");
        css.append("table {width: 100%; border-collapse: collapse;}");
        css.append("table, th, td {border: 1px solid black; padding: 4px;}");
        css.append("table, th > p, td > p {margin: 0px; padding: 0px;}");
        css.append("table, th > div > p, td > div > p {margin: 0px; padding: 0px;}");
        css.append("th {background-color: #f2f3f2;}");
        css.append("h1 {font-size: 20px; padding: 0px; margin: 4px;}");
        css.append(".ng-hide {display: none;}");
        css.append("body {font-size: 9pt; font-family: 'Times New Roman', Times, serif; }");
        css.append("caption, figcaption, .mms-equation-caption {text-align: center; font-weight: bold;}");
        css.append(".mms-equation-caption {float: right;}");
        css.append(
            "mms-view-equation, mms-view-figure, mms-view-image {page-break-inside: avoid;}.toc, .tof, .tot {page-break-after:always;}");
        css.append(".toc a, .tof a, .tot a { text-decoration:none; color: #000; font-size:9pt; }");
        css.append(
            ".toc .header, .tof .header, .tot .header { margin-bottom: 4px; font-weight: bold; font-size:24px; }");
        css.append(".toc ul, .tof ul, .tot ul {list-style-type:none; margin: 0; }");
        css.append(".tof ul, .tot ul {padding-left:0;}");
        css.append(".toc ul {padding-left:4em;}");
        css.append(".toc > ul {padding-left:0;}");
        css.append(".toc li > a[href]::after {content: leader('.') target-counter(attr(href), page);}");
        css.append(".tot li > a[href]::after {content: leader('.') target-counter(attr(href), page);}");
        css.append(".tof li > a[href]::after {content: leader('.') target-counter(attr(href), page);}");
        css.append("@page {margin: 1in 0.5in 0.5in 0.5in;}");
        css.append("@page {size: A4 landscape; margin-bottom: 0.3in;}");
        css.append("@page {");
        css.append("	@top-left{");
        css.append("		content: url('http://div27.jpl.nasa.gov/2740/files/logos/nasa_logo(80x66).jpg');");
        css.append("		border-bottom:2px solid green;");
        css.append("	}");
        css.append("	@top-right{");
        css.append("		content:'");
        css.append(extractDocumentTitle(htmlPath));
        css.append("';");
        css.append("		border-bottom:2px solid green;");
        css.append("		font-size:24pt;");
        css.append("		color: #8e2a0b;");
        css.append("		padding-right:10px;");
        css.append("	}");
        css.append("	@bottom-right{");
        css.append("		font-size: 10px; content: counter(page);border-top:1px solid black;");
        css.append("	}");
        css.append("}");
        try {
            saveStringToFileSystem(css.toString(), Paths.get(this.fsWorkingDir, "css", "customPdfSlides.css"));
        } catch (Throwable ex) {
            throw new Throwable("Failed to save custom PDF slides CSS! " + ex.getMessage());
        }
    }

    protected EmsScriptNode savePdfToRepo(String pdfPath) {
        log(String.format("Saving %s to repository...", Paths.get(pdfPath).getFileName()));
        Path path = Paths.get(pdfPath);
        if (!Files.exists(path)) {
            response.append(String.format("PDF generation failed! Unable to locate PDF file at %s!", pdfPath));
            return null;
        }

        EmsScriptNode pdfNode = NodeUtil
            .getOrCreateContentNode(this.nodeUserHomeSubDir, Paths.get(pdfPath).getFileName().toString(), services);
        if (pdfNode == null) {
            logger.error("Failed to create PDF nodeRef.");
            return pdfNode;
        }
        if (!saveFileToRepo(pdfNode, MimetypeMap.MIMETYPE_PDF, pdfPath)) {
            logger.error("Failed to save PDF artifact to repository!");
        }
        return pdfNode;
    }

    protected EmsScriptNode saveZipToRepo(String zipPath) throws Throwable {
        String zipFilename = Paths.get(zipPath).getFileName().toString();
        log(String.format("Saving %s to repository...", zipFilename));

        try {
            return saveFileToRepo(zipFilename, MimetypeMap.MIMETYPE_ZIP, zipPath);
        } catch (Throwable ex) {
            response.append(ex.getMessage());
            throw new Throwable("Failed to save zip artifact to repository!");
        }
    }

    protected EmsScriptNode saveFileToRepo(String filename, String mimeType, String filePath) throws Throwable {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            response.append(String.format("Unable to locate file at %s.", filePath));
            throw new Throwable();
        }

        EmsScriptNode node = NodeUtil.getOrCreateContentNode(this.nodeUserHomeSubDir, filename, services);
        if (node == null) {
            response.append("Failed to create nodeRef!");
            throw new Throwable();
        }
        if (!saveFileToRepo(node, mimeType, filePath)) {
            response.append("Failed to save file to repository!");
            throw new Throwable();
        }
        return node;
    }

    protected boolean saveFileToRepo(EmsScriptNode scriptNode, String mimeType, String filePath) {
        boolean bSuccess = false;
        if (filePath == null || filePath.isEmpty()) {
            log("File path parameter is missing!");
            return false;
        }
        if (!Files.exists(Paths.get(filePath))) {
            log(filePath + " does not exist!");
            return false;
        }

        NodeRef nodeRef = scriptNode.getNodeRef();
        ContentService contentService = scriptNode.getServices().getContentService();

        ContentWriter writer = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
        writer.setLocale(Locale.US);
        File file = new File(filePath);
        writer.setMimetype(mimeType);
        try {
            writer.putContent(file);
            bSuccess = true;
        } catch (Exception ex) {
            log("Failed to save '" + filePath + "' to repository!");
        }
        return bSuccess;
    }

    protected void saveCustomCssToFileSystem(String customCss) throws Throwable {
        log("Saving custom CSS to filesystem...");
        Path customCssPath = Paths.get(this.fsWorkingDir, "css", "customStyles.css");
        saveStringToFileSystem(customCss, customCssPath);
    }

    protected void saveStringToFileSystem(String stringContent, Path filePath) throws Throwable {
        if (Utils.isNullOrEmpty(stringContent))
            return;
        if (filePath == null)
            return;
        Path folder = filePath.getParent();
        try {
            try {
                if (!Files.exists(folder))
                    new File(folder.toString()).mkdirs();
            } catch (Throwable ex) {
                throw new Throwable(
                    String.format("Failed to create %s directory! %s", folder.toString(), ex.getMessage()));
            }

            File file = new File(filePath.toString());
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(stringContent);
            bw.close();
        } catch (Throwable ex) {
            throw new Throwable(
                String.format("Failed to save %s to filesystem! %s", filePath.toString(), ex.getMessage()));
        }
    }

    protected void addCssLinks(String htmlPath) throws Throwable {
        log("Adding CSS links to HTML...");
        if (!Files.exists(Paths.get(htmlPath))) {
            throw new Throwable(String
                .format("Failed to add CSS to HTML file. Expected %s HTML file but it does not exist!", htmlPath));
        }

        try {
            File htmlFile = new File(htmlPath);
            Document html = Jsoup.parse(htmlFile, "UTF-8", "");
            Element head = html.head();
            head.append("<meta charset=\"utf-8\" />");
            // adding custom CSS link
            head.append("<link href=\"css/customStyles.css\" rel=\"stylesheet\" type=\"text/css\" />");
            saveStringToFileSystem(html.toString(), Paths.get(htmlPath));
        } catch (Throwable ex) {
            throw new Throwable("Failed to add CSS to HTML file. " + ex.getMessage());
        }
    }


    /**
     * @param htmlFilename
     * @param htmlContent
     * @param coverFilename
     * @param coverContent
     * @param headerContent     TODO
     * @param tagId             TODO
     * @param displayTime       TODO
     * @param disabledCoverPage TODO
     * @param timestamp         TODO
     * @param jplDocNum         TODO
     * @param toc:              table of contents. passed in from VE
     * @param tof:              table of figures. if passed in from VE, we'd use that. otherwise, generate our default.
     * @param tot:              table of tables. if passed in from VE, we'd use that. otherwise, generate our default.
     * @param indices:          indexes. if passed in from VE, include it to end of document
     * @return path to saved HTML file.
     * @throws Throwable
     */
    protected String saveHtmlToFilesystem(String htmlFilename, String htmlContent, String coverFilename,
        String coverContent, String toc, String tof, String tot, String indices, String footerContent,
        String headerContent, String tagId, String timeStamp, String docNum, String displayTime, String customCss,
        Boolean disabledCoverPage) throws Throwable {
        log(String.format("Saving %s to filesystem...", htmlFilename));
        Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);

        try {
            if (Files.exists(htmlPath)) {
                // TODO file already exists, should we override?
            }

            Document htmlDocument = loadHtmlDocument(htmlContent);
            Element head = htmlDocument.head();
            head.append("<meta charset=\"utf-8\" />");
            //			htmlDocument = addCssLinks(htmlDocument, headerContent,
            //					footerContent, tagId, timeStamp, displayTime);
            htmlDocument = addTot(htmlDocument, tot);
            htmlDocument = addTof(htmlDocument, tof);
            htmlDocument = addToc(htmlDocument, toc);
            if (!disabledCoverPage) {
                coverContent += "<div style=\"page-break-after: always;\"/>";
                htmlDocument = addHtmlToStartOfDocument(htmlDocument, coverContent);
            }
            htmlDocument = addIndices(htmlDocument, indices);

            saveStringToFileSystem(htmlDocument.toString(), htmlPath);
            if (!Utils.isNullOrEmpty(customCss)) {
                saveCustomCssToFileSystem(customCss);
            }
            savePdfSlideCss(htmlPath.toString());


        } catch (Throwable ex) {
            logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
            log(String.format("Failed to save %s to filesystem!", htmlFilename));
            log(ex.getMessage());
            throw new Throwable();
        }
        return htmlPath.toString();
    }


    /**
     * loads HTML string into a JSoup HTML Document object
     *
     * @param htmlString
     * @return JSoup HTML Document object
     */
    protected Document loadHtmlDocument(String htmlString) throws Throwable {
        Document document = Jsoup.parse(htmlString, "UTF-8");
        if (document == null) {
            throw new Throwable("Failed to load HTML content!");
        }
        return document;
    }

    /*
   * adds HTML content to the start/beginning of an HTML document
   *
   * @param document: an JSoup HTML Document object
   * @param htmlString: an HTML string to add
   * @return Document: a JSoup HTML Document object
   *
   */
    protected Document addHtmlToStartOfDocument(Document document, String htmlString) {
        if (Utils.isNullOrEmpty(htmlString))
            return document;
        Element body = document.body();
        if (body != null) {
            if (body.child(0) == null) {
                Element elem = document.createElement("DIV");
                body.appendChild(elem);
            }
            body.child(0).before(htmlString);
        }
        return document;
    }

    protected Document addToc(Document document, String toc) throws Throwable {
        log("Adding table of contents to HTML...");
        if (document == null) {
            throw new Throwable("Null referenced input HTML document object!");
        }
        if (Utils.isNullOrEmpty(toc))
            return document;

        return addHtmlToStartOfDocument(document, toc);
    }

    protected Document addTof(Document document, String tof) throws Throwable {
        log("Adding list of figures to HTML...");
        if (document == null) {
            throw new Throwable("Null referenced input HTML document object!");
        }
        if (Utils.isNullOrEmpty(tof))
            tof = buildTableOfFigures(document);
        return addHtmlToStartOfDocument(document, tof);
    }

    protected Document addTot(Document document, String tot) throws Throwable {
        log("Adding list of tables to HTML...");
        if (Utils.isNullOrEmpty(tot))
            tot = buildTableOfTables(document);
        if (document == null) {
            throw new Throwable("Null referenced HTML document input object!");
        }
        return addHtmlToStartOfDocument(document, tot);
    }

    protected Document addIndices(Document document, String indices) throws Throwable {
        log("Adding indices to HTML...");
        if (Utils.isNullOrEmpty(indices))
            return document;

        if (document == null) {
            throw new Throwable("Null referenced input HTML document object!");
        }
        Element elem = document.createElement("DIV");
        document.body().appendChild(elem);
        elem.attr("class", "indices");
        elem.append(indices);
        return document;
    }

    protected String buildTableOfFigures(Document document) throws Throwable {
        if (document == null) {
            throw new Throwable("Null referenced input HTML document object!");
        }
        StringBuffer tof = new StringBuffer();
        Element body = document.body();
        if (body != null) {
            Elements figures = document.body().select("FIGURE");
            if (figures.size() > 0) {
                tof.append("<div class='tof'>");
                tof.append("   <div class='header'>List of Figures</div>");
                tof.append("   <ul>");
                int index = 0;

                for (Element f : figures) {
                    String id = null;
                    Element gp = null;
                    if (f.parent() != null && f.parent().parent() != null) {
                        gp = f.parent().parent();
                        id = gp.attr("id");
                    }
                    if (Utils.isNullOrEmpty(id)) {
                        id = f.attr("id");
                        if (Utils.isNullOrEmpty(id)) {
                            id = GUID.generate();
                            f.attr("id", id);
                        }
                    }
                    tof.append("  <li><a href='#");
                    tof.append(id);
                    tof.append(" '>");
                    Elements caption = f.select("> figcaption");
                    tof.append("Figure ");
                    tof.append(++index);
                    tof.append(": ");
                    if (caption != null && caption.size() > 0) {
                        tof.append(caption.get(0).text());
                    } else {
                        tof.append("Untitled");
                    }
                    tof.append("</a></li>");
                }

                tof.append("	</ul>");
                tof.append("</div>");
            }
        }
        return tof.toString();
    }

    protected String buildTableOfTables(Document document) throws Throwable {
        log("Buildiing list of tables...");
        if (document == null) {
            throw new Throwable("Null referenced input HTML document object!");
        }
        StringBuffer tot = new StringBuffer();
        Element body = document.body();
        if (body != null) {
            Elements tables = document.body().select("TABLE");
            if (tables.size() > 0) {
                tot.append("<div class='tot'>");
                tot.append("   <div class='header'>List of Tables</div>");
                tot.append("   <ul>");
                int index = 0;

                for (Element t : tables) {
                    String id = null;
                    Element gp = null;
                    if (t.parent() != null && t.parent().parent() != null) {
                        gp = t.parent().parent();
                        id = gp.attr("id");
                    }
                    if (Utils.isNullOrEmpty(id)) {
                        id = t.attr("id");
                        if (Utils.isNullOrEmpty(id)) {
                            id = GUID.generate();
                            t.attr("id", id);
                        }
                    }
                    tot.append("  <li><a href='#");
                    tot.append(id);
                    tot.append(" '>");
                    Elements caption = t.select("> caption");
                    tot.append("Table ");
                    tot.append(++index);
                    tot.append(": ");
                    if (caption != null && caption.size() > 0) {
                        tot.append(caption.get(0).text());
                    } else {
                        tot.append("Untitled");
                    }
                    tot.append("</a></li>");
                }

                tot.append("	</ul>");
                tot.append("</div>");
            }
        }
        return tot.toString();
    }

    protected String buildTableOfTablesAndFigures(String htmlContent) throws Throwable {
        StringBuffer tot = new StringBuffer();
        Document document = Jsoup.parse(htmlContent, "UTF-8");
        if (document == null) {
            throw new Throwable("Failed to parse HTML content!");
        }
        Elements tables = document.body().select("TABLE, FIGURE");
        if (tables.size() > 0) {
            tot.append("<div class='tot'>");
            tot.append("   <div class='header'>List of Tables and Figures</div>");
            tot.append("   <ul>");
            int figIndex = 0;
            int tableIndex = 0;
            String tagName = null;
            for (Element t : tables) {
                tot.append("  <li><a href='#");
                tot.append(t.parent().parent().attr("id"));
                tot.append(" '>");
                Elements caption = null;
                tagName = t.tagName().toUpperCase();
                if (tagName.compareToIgnoreCase("TABLE") == 0) {
                    caption = t.select("> caption");
                    tot.append("Table ");
                    tot.append(++tableIndex);
                } else if (tagName.compareToIgnoreCase("FIGURE") == 0) {
                    caption = t.select("> figcaption");
                    tot.append("Figure ");
                    tot.append(++figIndex);
                }

                tot.append(": ");
                if (caption != null && caption.size() > 0) {
                    tot.append(caption.get(0).text());
                } else {
                    tot.append("Untitled");
                }
                tot.append("</a></li>");
            }

            tot.append("	</ul>");
            tot.append("</div>");
        }
        return tot.toString();
    }

    protected String html2pdf(String docId, String tagId, String timeStamp, String htmlPath, String pdfFilename,
        EmsScriptNode userHomeFolder, String customCss) throws Throwable {
        log("Converting HTML to PDF...");
        if (!Files.exists(Paths.get(htmlPath))) {
            throw new Throwable(String
                .format("Failed to transform HTML to PDF. Expected %s HTML file but it does not exist!", htmlPath));
        }

        String pdfPath = Paths.get(this.fsWorkingDir, pdfFilename).toString();
        //String coverPath = Paths.get(this.fsWorkingDir, coverFilename)
        //		.toString();

        List<String> command = new ArrayList<>();
        command.add("prince");
        command.add("--media");
        command.add("print");
        if (!Utils.isNullOrEmpty(customCss)) {
            command.add("--style");
            command.add(customCss);
        }
        //if(!disabledCoverPage) command.add(coverPath);
        command.add(htmlPath);
        command.add("-o");
        command.add(pdfPath);

        log("htmltopdf command: " + command.toString().replace(",", ""));

        int attempts = 0;
        int ATTEMPTS_MAX = 3;
        boolean success = false;
        RuntimeExec exec = null;
        ExecutionResult execResult = null;
        Process process = null;

        boolean runProcess = true;
        try {
            while (attempts++ < ATTEMPTS_MAX && !success) {
                if (!runProcess) {
                    exec = new RuntimeExec();
                    exec.setCommand(list2Array(command));
                    execResult = exec.execute();
                } else {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    process = pb.start();
                    process.waitFor();
                }
                if (Files.exists(Paths.get(pdfPath))) {
                    success = true;
                    break;
                }
                Thread.sleep(5000);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Throwable(String
                .format("Failed to invoke PrinceXml! Please be sure PrinceXml is installed. %s", ex.getMessage()));
        }

        if (!success && !Files.exists(Paths.get(pdfPath))) {
            String msg = null;
            if (!runProcess) {
                msg = String.format("Failed to transform HTML file '%s' to PDF. Exit value: %d", htmlPath, execResult);
            } else {
                msg = String
                    .format("Failed to transform HTML file '%s' to PDF. Exit value: %d", htmlPath, process.exitValue());
            }
            log(msg);
            throw new Throwable(msg);
        }
        return pdfPath;
    }


    private String extractDocumentTitle(String htmlPath) throws Throwable {
        try {
            File htmlFile = new File(htmlPath);
            Document html = Jsoup.parse(htmlFile, "UTF-8", "");
            Element title = html.select("mms-view > div > h1").first();
            return title.text();
        } catch (Throwable ex) {
            throw new Throwable("Failed to extract document title from HTML file. " + ex.getMessage());
        }
    }

    public String html2pdfSlides(String htmlPath, String pdfFilename, EmsScriptNode userHomeFolder) throws Throwable {
        log("Converting HTML to PDF slides...");
        if (!Files.exists(Paths.get(htmlPath))) {
            throw new Throwable(String
                .format("Failed to transform HTML to PDF slides. Expected %s HTML file but it does not exist!",
                    htmlPath));
        }

        String pdfPath = Paths.get(this.fsWorkingDir, pdfFilename.replace(".pdf", "_slides.pdf")).toString();

        List<String> command = new ArrayList<>();
        command.add("prince");
        command.add("--media");
        command.add("print");
        command.add("--style");
        command.add(Paths.get(this.fsWorkingDir, "css", "customPdfSlides.css").toString());
        command.add(htmlPath);
        command.add("-o");
        command.add(pdfPath);

        log("prince command: " + command.toString().replace(",", ""));

        int attempts = 0;
        int ATTEMPTS_MAX = 3;
        boolean success = false;
        RuntimeExec exec = null;
        ExecutionResult execResult = null;
        Process process = null;

        boolean runProcess = true;
        try {
            while (attempts++ < ATTEMPTS_MAX && !success) {
                if (!runProcess) {
                    exec = new RuntimeExec();
                    exec.setCommand(list2Array(command));
                    execResult = exec.execute();
                } else {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    process = pb.start();
                    process.waitFor();
                }
                if (Files.exists(Paths.get(pdfPath))) {
                    success = true;
                    break;
                }
                Thread.sleep(5000);
            }
        } catch (Exception ex) {
            logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
            throw new Throwable(String
                .format("Failed to invoke PrinceXml! Please be sure PrinceXml is installed. %s", ex.getMessage()));
        }

        if (!success && !Files.exists(Paths.get(pdfPath))) {
            String msg = null;
            if (!runProcess) {
                msg = String.format("Failed to transform HTML file '%s' to PDF slides. Exit value: %d", htmlPath,
                    execResult.getExitValue());
            } else {
                msg = String.format("Failed to transform HTML file '%s' to PDF slides. Exit value: %d", htmlPath,
                    process != null ? process.exitValue() : 1);
            }
            log(msg);
            throw new Throwable(msg);
        }
        return pdfPath;
    }

    /**
     * Helper method to convert a list to an array of specified type
     *
     * @param list
     * @return
     */
    private String[] list2Array(List<String> list) {
        return Arrays.copyOf(list.toArray(), list.toArray().length, String[].class);
    }

    /**
     * Save off the configuration set and kick off snapshot creation in
     * background
     *
     * @param req
     * @param status
     */
    private JsonObject saveAndStartAction(WebScriptRequest req, Status status) {
        JsonObject postJson = null;
        //EmsScriptNode workspace = getWorkspace(req);
        JsonObject reqPostJson = (JsonObject) req.parseContent();
        if (reqPostJson != null) {
            postJson = reqPostJson;
            if (reqPostJson.has("documents")) {
                JsonArray documents = reqPostJson.get("documents").getAsJsonArray();
                if (documents != null) {
                    JsonObject json = documents.get(0).getAsJsonObject();
                    String user = AuthenticationUtil.getRunAsUser();
                    EmsScriptNode userHomeFolder = getUserHomeFolder(user);

                    postJson = handleCreate(json, userHomeFolder, null, status);
                }
            }
        }

        return postJson;
    }

    private JsonObject handleCreate(JsonObject postJson, EmsScriptNode context, EmsScriptNode workspace, Status status) {
        EmsScriptNode jobNode = null;

        if (postJson.has("name")) {
            String name = postJson.get("name").getAsString();
            if (ActionUtil.jobExists(context, name))
                return postJson;

            jobNode = ActionUtil.getOrCreateJob(context, name, "cm:content", status, response, true);

            if (jobNode != null) {
                startAction(jobNode, postJson, workspace);
                return postJson;
            } else {
                log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Couldn't create HTML to PDF job: %s", name);
                return null;
            }
        } else {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Job name not specified");
            return null;
        }
    }

    /**
     * Kick off the actual action in the background
     *
     * @param jobNode
     * @param postJson
     * @param workspace
     */
    public void startAction(EmsScriptNode jobNode, JsonObject postJson, EmsScriptNode workspace) {
        ActionService actionService = services.getActionService();
        Action htmlToPdfAction = actionService.createAction(HtmlToPdfActionExecuter.NAME);
        //htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_WORKSPACE, workspace);
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DOCUMENT_ID, JsonUtil.getOptString(postJson, "docId"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TAG_ID, JsonUtil.getOptString(postJson, "tagId"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TIME_STAMP, JsonUtil.getOptString(postJson, "time"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_COVER, JsonUtil.getOptString(postJson, "cover"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_HTML, JsonUtil.getOptString(postJson, "html"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_HEADER, JsonUtil.getOptString(postJson, "header"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_FOOTER, JsonUtil.getOptString(postJson, "footer"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DOC_NUM, JsonUtil.getOptString(postJson, "docNum"));
        htmlToPdfAction
            .setParameterValue(HtmlToPdfActionExecuter.PARAM_DISPLAY_TIME, JsonUtil.getOptString(postJson, "displayTime"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_CUSTOM_CSS, JsonUtil.getOptString(postJson, "customCss"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOC, JsonUtil.getOptString(postJson, "toc"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOF, JsonUtil.getOptString(postJson, "tof"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_TOT, JsonUtil.getOptString(postJson, "tot"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_INDEX, JsonUtil.getOptString(postJson, "index"));
        htmlToPdfAction.setParameterValue(HtmlToPdfActionExecuter.PARAM_DISABLED_COVER_PAGE,
            JsonUtil.getOptString(postJson, "disabledCoverPage"));
        services.getActionService().executeAction(htmlToPdfAction, jobNode.getNodeRef(), true, true);
    }

    protected String getFormattedDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DATE);

        switch (day % 10) {
            case 1:
                return new SimpleDateFormat("MMMM d'st', yyyy").format(date);
            case 2:
                return new SimpleDateFormat("MMMM d'nd,' yyyy").format(date);
            case 3:
                return new SimpleDateFormat("MMMM d'rd,' yyyy").format(date);
            default:
                return new SimpleDateFormat("MMMM d'th,' yyyy").format(date);
        }
    }

    public void handleEmbeddedImage(String htmlFilename) throws Exception {
        log(String.format("Saving images in %s to filesystem...", htmlFilename));
        Path htmlPath = Paths.get(this.fsWorkingDir, htmlFilename);
        if (!Files.exists(htmlPath))
            return;

        File htmlFile = new File(htmlPath.toString());
        Document document = Jsoup.parse(htmlFile, "UTF-8", "");
        if (document == null)
            return;

        Elements images = document.getElementsByTag("img");

        for (final Element image : images) {
            String src = image.attr("src");
            if (src == null)
                continue;
            try {
                URL url = null;
                if (!src.toLowerCase().startsWith("http")) {
                    // relative URL; needs to prepend URL protocol
                    String protocol = new HostnameGet(this.repository, this.services).getAlfrescoProtocol();
                    // System.out.println(protocol + "://" + src);
                    src = src.replaceAll("\\.\\./", "");
                    // System.out.println("src: " + src);
                    url = new URL(String.format("%s://%s", protocol, src));
                } else {
                    url = new URL(src);
                }

                String hostname = getHostname();
                try {
                    src = src.toLowerCase();
                    String embedHostname = String.format("%s://%s", url.getProtocol(), url.getHost());
                    String alfrescoContext =
                        "workspace/SpacesStore/"; // this.services.getSysAdminParams().getAlfrescoContext();
                    String versionStore = "versionStore/version2Store/";

                    // is image local or remote resource?
                    if (embedHostname.compareToIgnoreCase(hostname) == 0 || src.startsWith("/alfresco/") || src
                        .contains(alfrescoContext.toLowerCase()) || src.contains(versionStore.toLowerCase())) {
                        // local server image > generate image tags
                        String filePath = url.getFile();
                        if (filePath == null || filePath.isEmpty())
                            continue;

                        nodeId = null;
                        storeName = null;
                        if (filePath.contains(alfrescoContext)) {
                            // filePath = "alfresco/d/d/" +
                            // filePath.substring(filePath.indexOf(alfrescoContext));
                            nodeId = filePath.substring(filePath.indexOf(alfrescoContext) + alfrescoContext.length());
                            nodeId = nodeId.substring(0, nodeId.indexOf("/"));
                            storeName = "workspace://SpacesStore/";
                        }
                        if (filePath.contains(versionStore)) {
                            // filePath = "alfresco/d/d/" +
                            // filePath.substring(filePath.indexOf(alfrescoContext));
                            nodeId = filePath.substring(filePath.indexOf(versionStore) + versionStore.length());
                            nodeId = nodeId.substring(0, nodeId.indexOf("/"));
                            storeName = "versionStore://version2Store/";
                        }
                        if (nodeId == null || nodeId.isEmpty())
                            continue;

                        filename = filePath.substring(filePath.lastIndexOf("/") + 1);
                        try {
                            // This is the trouble area, where each image needs
                            // its own transaction:
                            //							new EmsTransaction(services, response, new Status()) {
                            //								@Override
                            //								public void run() throws Exception {
                            DBImage dbImage = retrieveEmbeddedImage(storeName, nodeId, filename, null, null);
                            if (dbImage != null) {
                                image.attr("src", dbImage.getFilePath());
                            }
                            //								}
                            //							};
                        } catch (Exception ex) {
                            // in case it's not a local resource > generate
                            // hyperlink instead
                            // image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ",
                            // src, url.getFile()));
                            // image.remove();
                        }
                    } else { // remote resource > generate a hyperlink
                        // image.before(String.format(" <ulink xl:href=\"%s\"><![CDATA[%s]]></ulink> ",
                        // src, url.getFile()));
                        // image.remove();
                    }
                } catch (Exception ex) {
                    log(Level.WARN, "Failed to retrieve embedded image at %s. %s", src, ex.getMessage());
                    logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
                }
            } catch (Exception ex) {
                log(Level.WARN, "Failed to process embedded image at %s. %s", src, ex.getMessage());
                logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
            }
        }
        try {
            FileUtils.writeStringToFile(htmlFile, document.outerHtml(), "UTF-8");
        } catch (Exception ex) {
            log(Level.ERROR, "Failed to save modified HTML %s. %s", htmlPath, ex.getMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(ex)));
        }
    }

    protected String removeTicketAuthInfo(String filename) {
        if (!filename.contains("?"))
            return filename;
        int idx = filename.indexOf("?");
        return filename.substring(0, idx);
    }

    private DBImage retrieveEmbeddedImage(String storeName, String nodeId, String imgName, EmsScriptNode workspace,
        Object timestamp) throws UnsupportedEncodingException {
        Path imageDirName = Paths.get(this.fsWorkingDir, "images");
        NodeRef imgNodeRef = NodeUtil.getNodeRefFromNodeId(storeName, nodeId);
        if (imgNodeRef == null)
            return null;

        imgName = removeTicketAuthInfo(URLDecoder.decode(imgName, "UTF-8"));
        String imgFilename = imageDirName + File.separator + imgName;

        File imgFile = new File(imgFilename);
        ContentReader imgReader;
        imgReader = this.services.getContentService().getReader(imgNodeRef, ContentModel.PROP_CONTENT);
        if (!Files.exists(imageDirName)) {
            if (!new File(imageDirName.toString()).mkdirs()) {
                log("Failed to create directory for " + imageDirName);
            }
        }
        imgReader.getContent(imgFile);

        DBImage image = new DBImage();
        image.setId(nodeId);
        image.setFilePath("images/" + imgName);
        return image;
    }

    private String getHostname() {
        SysAdminParams sysAdminParams = this.services.getSysAdminParams();
        String hostname = sysAdminParams.getAlfrescoHost();
        if (hostname.startsWith("ip-128-149"))
            hostname = "localhost";
        return String.format("%s://%s", sysAdminParams.getAlfrescoProtocol(), hostname);
    }

    protected String zipWorkingDir(String zipFilename) throws IOException, InterruptedException {
        log("Zipping artifacts within working directory...");
        RuntimeExec exec = new RuntimeExec();
        exec.setProcessDirectory(this.fsWorkingDir);
        List<String> command = new ArrayList<String>();
        command.add("zip");
        command.add("-r");
        command.add(zipFilename);
        command.add(".");
        exec.setCommand(list2Array(command));
        // System.out.println("zip command: " + command);
        ExecutionResult result = exec.execute();

        if (!result.getSuccess()) {
            log("zip failed!");
            log("exit code: " + result.getExitValue());
        }

        return Paths.get(this.fsWorkingDir, zipFilename).toString();
    }

    public void cleanupFiles() {
        if (gov.nasa.jpl.mbee.util.FileUtils.exists(this.fsWorkingDir)) {
            try {
                FileUtils.forceDelete(new File(this.fsWorkingDir));
            } catch (IOException ex) {
                log(String.format("Failed to cleanup temporary files at %s", this.fsWorkingDir));
            }
        }
    }

    private void tableToCSV(String htmlPath) throws Exception {
        File input = new File(htmlPath);
        if (!input.exists())
            return;

        try {
            FileInputStream fileStream = new FileInputStream(input);
            Document document = Jsoup.parse(fileStream, "UTF-8", "");
            if (document == null)
                throw new Exception("Failed to convert tables to CSV! Unabled to load file: " + htmlPath);

            int tableIndex = 1;
            int rowIndex = 1;
            String filename = "";
            int cols = 0;
            for (Element table : document.select("table")) {
                List<List<String>> csv = new ArrayList<>();
                Queue<TableCell> rowQueue = new LinkedList<>();
                Elements elements = table.select("> thead");
                elements.addAll(table.select("> tbody"));
                elements.addAll(table.select("> tfoot"));
                for (Element row : elements.select("> tr")) {
                    List<String> csvRow = new ArrayList<>();
                    cols = row.children().size();
                    for (int i = 0; i < cols; i++) {
                        if (i >= row.children().size()) {
                            for (int k = cols; k > i; k--)
                                csvRow.add("");
                            break;
                        }
                        Element entry = row.child(i);
                        if (entry != null && entry.text() != null && !entry.text().isEmpty()) {
                            csvRow.add(entry.text());

                            //***handling multi-rows***
                            String moreRows = entry.attr("rowspan");
                            if (moreRows != null && !moreRows.isEmpty()) {
                                int additionalRows = Integer.parseInt(moreRows);
                                if (additionalRows > 1) {
                                    for (int ar = 1; ar <= additionalRows; ar++) {
                                        TableCell tableCell = new TableCell(rowIndex + ar, i);
                                        rowQueue.add(tableCell);
                                    }
                                }
                            }
                            //***handling multi-rows***

                            //***handling multi-columns***
                            String rowspan = entry.attr("colspan");
                            if (rowspan == null || rowspan.isEmpty())
                                continue;

                            int irowspan = Integer.parseInt(rowspan);
                            if (irowspan < 2)
                                continue;
                            for (int j = 2; j < irowspan; j++, i++) {
                                csvRow.add("");
                            }
                            //***handling multi-columns***
                        } else
                            csvRow.add("");
                    }
                    csv.add(csvRow);
                    rowIndex++;
                }

                boolean hasTitle = false;
                Elements title = table.select(" > caption");
                if (title != null && title.size() > 0) {
                    String titleText = title.first().text();
                    if (titleText != null && !titleText.isEmpty()) {
                        filename = title.first().text();
                        hasTitle = true;
                    }
                }

                if (!hasTitle)
                    filename = "Untitled";
                filename = "table_" + tableIndex++ + "_" + filename;

                writeCSV(csv, filename, rowQueue, cols);
            }

        } catch (IOException e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            throw new Exception("IOException: unable to read/access file: " + htmlPath);
        } catch (NumberFormatException ne) {
            logger.error(String.format("%s", LogUtil.getStackTrace(ne)));
            throw new Exception("One or more table row/column does not contain a parsable integer.");
        }
    }

    private String getHtmlText(String htmlString) {
        if (htmlString == null || htmlString.isEmpty())
            return "";
        Document document = Jsoup.parseBodyFragment(htmlString);
        if (document == null || document.body() == null)
            return "";
        return document.body().text();
    }


    private void writeCSV(List<List<String>> csv, String filename, Queue<TableCell> rowQueue, int cols)
        throws Exception {
        String QUOTE = "\"";
        String ESCAPED_QUOTE = "\"\"";
        char[] CHARACTERS_THAT_MUST_BE_QUOTED = {',', '"', '\n'};
        filename = getHtmlText(filename);
        if (filename.length() > 100) {
            filename = filename.substring(0, 100);
        }
        File outputFile = new File(Paths.get(this.fsWorkingDir, filename + ".csv").toString());
        FileWriter fw = new FileWriter(outputFile);
        BufferedWriter writer = new BufferedWriter(fw);
        try {
            int rowIndex = 1;
            boolean hasMoreRows;
            for (List<String> row : csv) {
                for (int i = 0; i < row.size() && i < cols; i++) {
                    if (i >= cols)
                        break;
                    hasMoreRows = false;
                    if (!rowQueue.isEmpty()) {
                        TableCell tableCell = rowQueue.peek();
                        if (tableCell.getRow() == rowIndex && tableCell.getColumn() == i) {
                            hasMoreRows = true;
                            rowQueue.remove();
                            if (i < cols - 1)
                                writer.write(",");
                        }
                    }
                    String s = row.get(i);
                    if (s.contains(QUOTE)) {
                        s = s.replace(QUOTE, ESCAPED_QUOTE);
                    }
                    if (StringUtils.indexOfAny(s, CHARACTERS_THAT_MUST_BE_QUOTED) > -1) {
                        s = QUOTE + s + QUOTE;
                    }

                    writer.write(s);
                    if (!hasMoreRows)
                        if (i < cols - 1)
                            writer.write(",");
                }
                writer.write(System.lineSeparator());
                rowIndex++;
            }
        } catch (IOException e) {
            String msg = String
                .format("Failed to save table to CSV to file system for %s. %s", outputFile.getAbsoluteFile(),
                    e.getMessage());
            log(Level.ERROR, msg);
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            throw new Exception(msg);
        } finally {
            writer.close();
            fw.close();
        }
    }

}
