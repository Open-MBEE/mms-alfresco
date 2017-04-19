package gov.nasa.jpl.view_repo.webscripts;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import gov.nasa.jpl.view_repo.util.*;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PermissionService;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

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
        JSONObject json = null;
        String projectId = getProjectId(req);

        try {
            if (validateRequest(req, status)) {
                json = handleWorkspace (projectId, req.getParameter( "deleted" ) != null);
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n", e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) json.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( json, 4 ));
            } catch ( JSONException e ) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                model.put("res", createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    protected JSONObject handleWorkspace (String projectId, boolean findDeleted) throws JSONException{
        JSONObject json = new JSONObject();
        JSONArray jArray = null;
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, "master");
        jArray = emsNodeUtil.getRefsJson();
        json.put("refs", jArray);
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
