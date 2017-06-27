package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.LogUtil;

/**
 * Created by dank on 6/26/17.
 */
public class ProjectDelete extends AbstractJavaWebScript {
    static Logger logger = Logger.getLogger(ProjectGet.class);

    public ProjectDelete() {
        super();
    }

    public ProjectDelete(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProjectDelete instance = new ProjectDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        JSONObject json = null;

        try {
            if (validateRequest(req, status)) {

                String projectId = getProjectId(req);
                logger.info("PROJECT ID \n" + projectId);

            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON could not be created\n");
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace:\n %s \n",
                e.getLocalizedMessage());
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        if (json == null) {
            model.put("res", createResponseJson());
        } else {
            model.put("res", json);
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    /**
     * Validate the request and check some permissions
     */
    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        return checkRequestContent(req);
    }
}
