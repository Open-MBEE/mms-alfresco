package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Jason Han
 */

public class MigrationRunner {
    static Logger logger = Logger.getLogger(MigrationRunner.class);

    public static ServiceRegistry services = null;
    public static ModuleDetails moduleDetails = null;

    public static ServiceRegistry getServices() {
        return getServiceRegistry();
    }

    public static void setServices(ServiceRegistry services) {
        MigrationRunner.services = services;
    }

    public static ServiceRegistry getServiceRegistry() {
        return services;
    }

    public static boolean checkMigration(ServiceRegistry services) {
        setServices(services);
        String previousVersion = getPreviousVersion();
        String currentVersion = getCurrentVersion();
        return false;
    }

    public static String getPreviousVersion() {
        ModuleService moduleService =
            (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        List<ModuleDetails> modules = moduleService.getAllModules();
        for (ModuleDetails module : modules) {
            if (module.getId().contains("mms-amp")) {
                moduleDetails = module;
                logger.error("MODULE VERSION IN DB: " + module.getModuleVersionNumber().toString());
                List<String> editions = module.getEditions();
                if (editions != null && editions.size() > 0) {
                    for (String edition : editions) {
                        logger.error("Edition: " + edition);
                    }
                } else {
                    logger.error("No editions found.");
                }
                return module.getModuleVersionNumber().toString();
            }
        }
        return null;
    }

    public static String getCurrentVersion() {
        try {
            EmsConfig.setAlfrescoProperties(moduleDetails.getProperties());
            logger.error("MODULE VERSION IN PROPERTIES: " + EmsConfig.get("module.version"));
            return EmsConfig.get("module.version");
        } catch (Exception e) {
            logger.error(e);
        }

        return null;
    }
}
