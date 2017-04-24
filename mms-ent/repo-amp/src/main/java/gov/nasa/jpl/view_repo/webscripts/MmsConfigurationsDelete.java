package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.chemistry.opencmis.commons.impl.json.JSONArray;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

public class MmsConfigurationsDelete extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsConfigurationsDelete.class);

    public MmsConfigurationsDelete() {
        super();
    }

    public MmsConfigurationsDelete(Repository repository, ServiceRegistry services) {
        this.repository = repository;
        this.services = services;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsConfigurationsDelete instance = new MmsConfigurationsDelete(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        WorkspaceNode workspace = getWorkspace(req, user);
        JSONObject result = new JSONObject();

        PostgresHelper pgh = new PostgresHelper(workspace);
        try {
            if (req.getServiceMatch().getTemplateVars().containsKey("configurationId")) {
                String id = req.getServiceMatch().getTemplateVars().get("configurationId");
                pgh.deleteRef(id);
                JSONArray deleted = new JSONArray();
                result.put("deleted", deleted.add(id));
            }

        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        status.setCode(responseStatus.getCode());

        model.put("res", result);

        printFooter(user, logger, timer);

        return model;
    }

}
