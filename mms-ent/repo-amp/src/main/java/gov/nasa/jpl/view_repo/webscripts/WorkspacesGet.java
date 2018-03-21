package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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

import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;


public class WorkspacesGet extends AbstractJavaWebScript{
	static Logger logger = Logger.getLogger(WorkspacesGet.class);

    protected boolean gettingContainedWorkspaces = false;

    public WorkspacesGet() {
        super();
    }

    public WorkspacesGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
        WorkspacesGet instance = new WorkspacesGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache );
    }

    /**
     * Need wrapper for actual execution to be run in different instance since
     * @param req
     * @param status
     * @param cache
     * @return
     */
    @Override
    protected Map<String, Object> executeImplImpl (WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JsonObject object = null;
        String projectId = getProjectId(req);
        String[] accepts = req.getHeaderValues("Accept");
        String accept = (accepts != null && accepts.length != 0) ? accepts[0] : "";

        try {
            if (validateRequest(req, status)) {
                object = handleWorkspace (projectId, req.getParameter( "deleted" ) != null);
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
            } catch ( JsonParseException e ) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put(Sjm.RES, createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    protected JsonObject handleWorkspace (String projectId, boolean findDeleted) {
        JsonObject json = new JsonObject();
        JsonArray jArray = null;
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
        jArray = emsNodeUtil.getRefsJson();
        json.add("refs", jArray);
        return json;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest (WebScriptRequest req, Status status){
        return checkRequestContent(req);
    }
}
