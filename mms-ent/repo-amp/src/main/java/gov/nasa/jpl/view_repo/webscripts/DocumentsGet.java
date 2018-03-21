package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.LogUtil;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class DocumentsGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(DocumentsGet.class);

    public DocumentsGet() {
        super();
    }

    public DocumentsGet(Repository repository, ServiceRegistry services) {
        this.repository = repository;
        this.services = services;
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        DocumentsGet instance = new DocumentsGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        // Checks mms versions
        if (checkMmsVersions) {
            if (compareMmsVersions(req, getResponse(), getResponseStatus())) {
                model.put(Sjm.RES, createResponseJson());
                return model;
            }
        }

        JsonObject jsonObject = new JsonObject();

        try {
            jsonObject.add(Sjm.DOCUMENTS, filterByPermission(handleProducts(req), req));
            model.put(Sjm.RES, jsonObject.toString());
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        status.setCode(responseStatus.getCode());
        if (model.isEmpty()) {
            model.put(Sjm.RES, createResponseJson());
        }

        printFooter(user, logger, timer);

        return model;
    }

    private JsonArray handleProducts(WebScriptRequest req) {

        String refId = getRefId(req);
        String projectId = getProjectId(req);
        String groupId = req.getParameter("groupId");
        String extended = req.getParameter("extended");

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        int trueDepth = 10000;

        return emsNodeUtil.getDocJson((groupId != null && !groupId.equals("")) ? groupId : null, trueDepth, extended != null && extended.equals("true"));
    }

}
