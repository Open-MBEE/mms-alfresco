package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

public class MmsConfigurationsGet extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(MmsConfigurationsGet.class);

    public enum Type {
        SINGLE, MULTIPLE
    }

    private Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public MmsConfigurationsGet() {
        super();
    }

    public MmsConfigurationsGet(Repository repository, ServiceRegistry services) {
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
        MmsConfigurationsGet instance = new MmsConfigurationsGet(repository, getServices());
        instance.setType(type);
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();
        WorkspaceNode workspace = getWorkspace(req, user);
        JSONArray configurations = new JSONArray();
        JSONObject result = new JSONObject();

        PostgresHelper pgh = new PostgresHelper(workspace);
        try {
            ElasticHelper eh = new ElasticHelper();

            List<Pair<String, String>> configs = pgh.getTags();

            if (req.getServiceMatch().getTemplateVars().containsKey("configurationId")) {
                String wantedId = req.getServiceMatch().getTemplateVars().get("configurationId");

                for (Pair<String, String> config : configs) {
                    if (config.first.equalsIgnoreCase(wantedId)) {
                        configurations.put(eh.getElementByElasticId(config.first).put("id", config.first));
                        break;
                    }
                }
            } else {
                for (Pair<String, String> config : configs) {
                    configurations.put(eh.getElementByElasticId(config.first).put("id", config.first));
                }
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        result.put("configurations", configurations);

        model.put("res", result);

        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

}
