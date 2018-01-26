package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.Sjm;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;

public class WorkspaceHistoryGet extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(WorkspaceHistoryGet.class);

    public WorkspaceHistoryGet() {
        super();
    }

    public WorkspaceHistoryGet(Repository repositoryHelper, ServiceRegistry service) {
        super(repositoryHelper, service);
    }

    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        WorkspaceHistoryGet instance = new WorkspaceHistoryGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JsonObject object = null;
        String projectId = getProjectId(req);
        String limit = req.getParameter("limit");
        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        try {
            if (validateRequest(req, status)) {
                String wsID = req.getServiceMatch().getTemplateVars().get(REF_ID);
                object = getRefHistory(req, projectId, wsID);
            }
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        if (object == null) {
            model.put(Sjm.RES, createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    object.addProperty("message", response.toString());
                }
                if (prettyPrint || accept.contains("webp")) {
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    model.put(Sjm.RES, gson.toJson(object));
                } else {
                    model.put(Sjm.RES, object);
                }
            } catch (JsonParseException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put(Sjm.RES, createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;

    }

    protected JsonObject getRefHistory(WebScriptRequest req, String projectId, String refId) {
        JsonObject json = new JsonObject();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
        JsonArray commits = emsNodeUtil.getRefHistory(refId);
        String timestamp = req.getParameter("maxTimestamp");

        if (timestamp != null && !timestamp.isEmpty()) {
            commits = emsNodeUtil.getNearestCommitFromTimestamp(timestamp, commits);
        }
        json.add("commits", commits);
        return json;
    }

    @Override protected boolean validateRequest(WebScriptRequest req, Status status) {
        String wsId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestContent(req) && checkRequestVariable(wsId, REF_ID);
    }

}
