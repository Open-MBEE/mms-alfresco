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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class WorkspaceGet extends AbstractJavaWebScript{
	static Logger logger = Logger.getLogger(WorkspaceGet.class);

    public WorkspaceGet(){
        super();
    }

    public WorkspaceGet(Repository repositoryHelper, ServiceRegistry service){
        super(repositoryHelper, service);
    }

    @Override
    protected Map<String, Object> executeImpl (WebScriptRequest req, Status status, Cache cache) {
        WorkspaceGet instance = new WorkspaceGet(repository, getServices());
        return instance.executeImplImpl( req, status, cache );
    }

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

        try{
            if(validateRequest(req, status)){
                String wsID = req.getServiceMatch().getTemplateVars().get(REF_ID);
                object = getRef(projectId, wsID);
            }
        } catch (JsonParseException e) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Could not parse JSON request", e);
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error", e);
        }

        if(object == null){
            model.put(Sjm.RES, createResponseJson());
        } else {
            try{
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
                log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not create JSON response", e);
            }
        }

        status.setCode(responseStatus.getCode());
        if (model.isEmpty()) {
            model.put(Sjm.RES, createResponseJson());
        }

        printFooter(user, logger, timer);

        return model;

    }

    protected JsonObject getRef(String projectId, String refId) {
        JsonObject json = new JsonObject();
        JsonArray jsonArray = new JsonArray();
        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, NO_WORKSPACE_ID);
        JsonObject ref = emsNodeUtil.getRefJson(refId);
        jsonArray.add(ref);
        json.add("refs" , jsonArray);
        return json;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String wsId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestContent(req) && checkRequestVariable(wsId, REF_ID);
    }

}
