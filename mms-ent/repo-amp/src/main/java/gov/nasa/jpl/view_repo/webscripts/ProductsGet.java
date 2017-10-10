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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class ProductsGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProductsGet.class);

    public ProductsGet() {
        super();
    }

    public ProductsGet(Repository repository, ServiceRegistry services) {
        this.repository = repository;
        this.services = services;
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProductsGet instance = new ProductsGet(repository, getServices());
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

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put(Sjm.DOCUMENTS, filterByPermission(handleProducts(req), req));
            model.put(Sjm.RES, jsonObject.toString());
        } catch (Exception e) {
            model.put(Sjm.RES, createResponseJson());
            if (e instanceof JSONException) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON creation error");
            } else {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    private JSONArray handleProducts(WebScriptRequest req) throws JSONException {

        String commitId = req.getParameter("commitId");
        String refId = getRefId(req);
        String projectId = getProjectId(req);
        String extended = req.getParameter("extended");
        String groupId = req.getServiceMatch().getTemplateVars().get("groupId");

        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, refId);

        // :TODO write a recursive function to find children in the  given current groupId

        int trueDepth = groupId != null ? 1 : 10000;

        return emsNodeUtil.getDocJson((groupId != null && !groupId.equals("")) ? groupId : null, commitId,
            extended != null && extended.equals("true"), trueDepth);
    }

}
