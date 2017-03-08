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
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

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
        JSONObject object = null;

        try{
            if(validateRequest(req, status)){
                String userName = AuthenticationUtil.getRunAsUser();
                String wsID = req.getServiceMatch().getTemplateVars().get(REF_ID);
                WorkspaceNode workspace =
                        WorkspaceNode.getWorkspaceFromId( wsID,
                                                                  getServices(),
                                                                  getResponse(),
                                                                  status,
                                                                  //false,
                                                                  userName );
                object = getWorkspace(workspace, wsID);
            }
        } catch (JSONException e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "JSON object could not be created \n");
            e.printStackTrace();
        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal error stack trace \n");
            e.printStackTrace();
        }

        if(object == null){
            model.put("res", createResponseJson());
        } else {
            try{
                if (!Utils.isNullOrEmpty(response.toString())) object.put("message", response.toString());
                model.put("res", NodeUtil.jsonToString( object, 4 ));
            } catch (JSONException e){
                e.printStackTrace();
                model.put("res", createResponseJson());
            }
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;

    }
    protected Object getStringIfNull(Object object){
        if(object == null)
            return "null";
        return object;
    }

    protected JSONObject getWorkspace(WorkspaceNode ws, String wsID) throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject interiorJson = new JSONObject();
        if(ws == null){
            if (wsID.equals("master")) {
                WorkspaceNode.addWorkspaceNamesAndIds(interiorJson, ws, true );
                jsonArray.put(interiorJson);
            } else {
                log(Level.WARN, HttpServletResponse.SC_NOT_FOUND, "Workspace not found: %s", (ws == null ? null : ws.getSysmlId()));
            }
        } else {
            if(checkPermissions(ws, PermissionService.READ))  {
                jsonArray.put(ws.toJSONObject(ws, null));
            } else {
                log(Level.WARN, HttpServletResponse.SC_FORBIDDEN, "No read permissions for workspace: %s", (ws == null ? null : ws.getSysmlId()));
            }
        }
        json.put("refs" , jsonArray);
        return json;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        if(checkRequestContent(req) == false)
            return false;

        String wsId = req.getServiceMatch().getTemplateVars().get(REF_ID);

        if(checkRequestVariable(wsId, REF_ID) == false)
            return false;
        return true;
    }

}
