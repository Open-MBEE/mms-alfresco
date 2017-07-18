package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

/**
 * Returns all the projects for a given workspace
 *
 * @author gcgandhi
 *
 */
public class ProjectsGet extends AbstractJavaWebScript{
	static Logger logger = Logger.getLogger(ProjectsGet.class);

    public ProjectsGet() {
        super();
    }

    public ProjectsGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Entry point
     */
    @Override
    protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
        ProjectsGet instance = new ProjectsGet(repository, getServices());
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
        JSONObject json = new JSONObject();
/*
        try {
            if (validateRequest(req, status)) {
                WorkspaceNode workspace = getWorkspace( req );
                json = handleRequest (workspace, status);
            }
        } catch (JSONException e) {
            log(Level.ERROR,HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            e.printStackTrace();
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n%s\n", e.getLocalizedMessage());
            e.printStackTrace();
        } finally {
            try {
                if (!Utils.isNullOrEmpty(response.toString())) {
                    json.put("message", response.toString());
                }
                model.put("res", NodeUtil.jsonToString( json, 4 ));
            } catch ( JSONException e ) {
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON parse exception: %s",e.getMessage());
                if (!model.containsKey( "res" )) {
                    model.put( "res", createResponseJson() );
                }
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
        }

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);
*/
        return model;
    }


    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest (WebScriptRequest req, Status status){
        return !checkRequestContent(req);
    }
}
