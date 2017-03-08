package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.json.JSONArray;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.util.NodeUtil;
//import gov.nasa.jpl.view_repo.webscripts.WebScriptUtil;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

public class ModuleInfoGet extends DeclarativeWebScript {
    private static Logger logger = Logger.getLogger(ModuleInfoGet.class)
            ;
    private ServiceRegistry services;

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<>();
        
        ModuleService moduleService = (ModuleService)this.services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        JSONObject json = new JSONObject();
        JSONArray modulesJson = new JSONArray();
        
        List< ModuleDetails > modules = moduleService.getAllModules();
        for (ModuleDetails md: modules) {
            JSONObject jsonModule = new JSONObject();
            jsonModule.put( "title", md.getTitle() );
            jsonModule.put( "version", md.getVersion() );
            modulesJson.put( jsonModule );
        }
        json.put( "modules", modulesJson );
        status.setCode( HttpServletResponse.SC_OK );

        model.put("res", json.toString(2));
        return model;
    }
    
    
    //Placed within for testing purposes!
    // TODO: Remove once finished with MmsVersion service
    public static JSONObject checkMMSversion(WebScriptRequest req) {
        boolean matchVersions = AbstractJavaWebScript.getBooleanArg(req, "mmsVersion", false);
        JSONObject jsonVersion = null;
        String mmsVersion = NodeUtil.getMMSversion();
        if (logger.isDebugEnabled()) logger.debug("Check versions?" + matchVersions);
        if (matchVersions) {
            jsonVersion = new JSONObject();
            jsonVersion.put("version", mmsVersion);
        }
        return jsonVersion;
    }

    public void setServices(ServiceRegistry registry) {
        services = registry;
    }
}
