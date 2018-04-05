/**
 *
 */
package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import gov.nasa.jpl.view_repo.util.Sjm;
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
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;

/**
 * @author dank
 *
 */
public class HistoryGet extends ModelGet {

    static Logger logger = Logger.getLogger(HistoryGet.class);

    /**
     *
     */
    public HistoryGet() {
        super();
    }

    /**
     *
     * @param repositoryHelper
     * @param registry
     */
    public HistoryGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        HistoryGet instance = new HistoryGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }


    @Override
	protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Accept: %s", accept));
        }

        if (logger.isDebugEnabled()) {
            logger.debug(user + " " + req.getURL());
        }

        JsonObject top = new JsonObject();
        JsonArray elementsJson = handleRequest(req);

        try {
            if (elementsJson.size() > 0) {
                top.add(Sjm.COMMITS, elementsJson);
            } else {
                responseStatus.setCode(HttpServletResponse.SC_NOT_FOUND);
            }

            if (!Utils.isNullOrEmpty(response.toString()))
                top.addProperty("message", response.toString());

        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        status.setCode(responseStatus.getCode());
        if (prettyPrint || accept.contains("webp")) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            model.put(Sjm.RES, gson.toJson(top));
        } else {
            model.put(Sjm.RES, top);
        }
        printFooter(user, logger, timer);
        return model;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return true;
    }

    /**
     * Wrapper for handling a request and getting the appropriate JSONArray of
     * elements
     *
     * @param req
     * @return
     */
    private JsonArray handleRequest(WebScriptRequest req) {
        JsonArray jsonHist = new JsonArray();
        try {
            String[] idKeys = { "elementId", "artifactId" };
            String modelId = null;
            for (String idKey : idKeys) {
                modelId = req.getServiceMatch().getTemplateVars().get(idKey);
                if (modelId != null) {
                    break;
                }
            }

            if (modelId == null) {
                logger.error("Model ID Null...");
                log(Level.ERROR, HttpServletResponse.SC_NOT_FOUND, "Could not find element");
                return jsonHist;
            }

            EmsNodeUtil emsNodeUtil = new EmsNodeUtil(getProjectId(req), getRefId(req));
            jsonHist = emsNodeUtil.getNodeHistory(modelId);
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return jsonHist;
    }
}
