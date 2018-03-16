package gov.nasa.jpl.view_repo.actions;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import javax.jws.WebParam;
import java.util.List;

/**
 * @author Jason Han
 */

public class MigrationRunner {
    static Logger logger = Logger.getLogger(MigrationRunner.class);

    public static ServiceRegistry services = null;
    public static Repository repository = null;

    public static Repository getRepository() {
        return repository;
    }

    public static void setRepository(Repository repositoryHelper) {
        MigrationRunner.repository = repositoryHelper;
    }

    public static ServiceRegistry getServices() {
        return getServiceRegistry();
    }

    public static void setServices(ServiceRegistry services) {
        MigrationRunner.services = services;
    }

    public static ServiceRegistry getServiceRegistry() {
        return services;
    }

    public static boolean checkMigration() {
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
                return module.getModuleVersionNumber().toString();
            }
        }
        return null;
    }

    public static String getCurrentVersion() {
        return null;
    }
}
