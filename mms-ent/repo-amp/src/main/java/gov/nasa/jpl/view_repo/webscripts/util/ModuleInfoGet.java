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
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gov.nasa.jpl.view_repo.util.Sjm;

public class ModuleInfoGet extends DeclarativeWebScript {
    private static Logger logger = Logger.getLogger(ModuleInfoGet.class)
            ;
    private ServiceRegistry services;

    @Override
    protected Map< String, Object > executeImpl( WebScriptRequest req,
                                                 Status status, Cache cache ) {
        Map<String, Object> model = new HashMap<>();

        ModuleService moduleService = (ModuleService)this.services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        JsonObject json = new JsonObject();
        JsonArray modulesJson = new JsonArray();

        List< ModuleDetails > modules = moduleService.getAllModules();
        for (ModuleDetails md: modules) {
            JsonObject jsonModule = new JsonObject();
            jsonModule.addProperty( "title", md.getTitle() );
            jsonModule.addProperty( "version", md.getModuleVersionNumber().toString() );
            modulesJson.add( jsonModule );
        }
        json.add( "modules", modulesJson );
        status.setCode( HttpServletResponse.SC_OK );

        model.put(Sjm.RES, json.toString());
        return model;
    }

    public void setServices(ServiceRegistry registry) {
        services = registry;
    }
}
