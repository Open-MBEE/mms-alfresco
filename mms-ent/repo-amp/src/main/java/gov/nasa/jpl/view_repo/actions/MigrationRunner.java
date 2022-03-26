package gov.nasa.jpl.view_repo.actions;

import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.repo.admin.registry.RegistryKey;
import org.alfresco.repo.admin.registry.RegistryService;
import org.alfresco.repo.module.ModuleComponentHelper;
import org.alfresco.repo.module.ModuleVersionNumber;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.VersionNumber;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Jason Han
 */

public class MigrationRunner extends AbstractPatch {
    static Logger logger = Logger.getLogger(MigrationRunner.class);

    private static final String PATCH_ID = "gov.nasa.jpl.view_repo.actions.MigrationRunner";

    public ServiceRegistry services;
    public RegistryService registryService;

    private static final String PATH_MODULES = "modules";
    private static final String REGISTRY_PROPERTY_CURRENT_VERSION = "currentVersion";
    private static final String MODULE_ID = "mms-amp";

    private static final List<ModuleVersionNumber> migrationList;

    static {
        migrationList = new LinkedList<>();
        migrationList.add(new ModuleVersionNumber("3.1.0"));
        migrationList.add(new ModuleVersionNumber("3.2.0"));
        migrationList.add(new ModuleVersionNumber("3.3.0"));
        migrationList.add(new ModuleVersionNumber("3.4.0"));
    }

    public void setServices(ServiceRegistry services) {
        this.services = services;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    @Override
    protected String applyInternal() throws Exception {
        logger.info("Starting execution of patch");
        if (checkMigration(services, registryService)) {
            return "Migration executed successfully";
        } else {
            throw new Exception("Migration failed");
        }
    }



    public static ModuleVersionNumber getCurrentVersion(RegistryService registryService) {
        ModuleVersionNumber response = null;

        RegistryKey moduleKeyCurrentVersion = new RegistryKey(
                ModuleComponentHelper.URI_MODULES_1_0,
            PATH_MODULES, MODULE_ID, REGISTRY_PROPERTY_CURRENT_VERSION);
        Serializable versionCurrent = registryService.getProperty(moduleKeyCurrentVersion);

        if (versionCurrent != null) {
            ModuleVersionNumber current = MigrationRunner.getModuleVersionNumber(versionCurrent);
            if (current.toString().indexOf('-') > -1) {
                response = MigrationRunner.getModuleVersionNumber(cleanVersion(current.toString()));
            } else {
                response = current;
            }
            logger.info("Current Version: " + response.toString());
        }

        return response;
    }

    public static ModuleVersionNumber getInstalledVersion(ServiceRegistry services) {
        ModuleVersionNumber response = null;

        ModuleService moduleService =
            (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        ModuleDetails md = moduleService.getModule(MODULE_ID);

        if (md != null) {
            ModuleVersionNumber installed = md.getModuleVersionNumber();
            if (installed.toString().indexOf('-') > -1) {
                response = MigrationRunner.getModuleVersionNumber(cleanVersion(installed.toString()));
            } else {
                response = installed;
            }
            logger.info("Installed Version: " + response.toString());
        }

        return response;
    }

    public static String cleanVersion(String version) {
        if (version == null) {
            return null;
        }
        return version.indexOf('-') > -1 ? version.substring(0, version.indexOf('-')) : version;
    }

    public static String versionToClassname(ModuleVersionNumber version) {
        String versionString = cleanVersion(version.toString());
        return "Migrate_" + versionString.replace('.', '_');
    }

    public static boolean isMigrationNeeded(ModuleVersionNumber previousVersion, ModuleVersionNumber currentVersion) {
        if (currentVersion == null || previousVersion == null) {
            return false;
        }
        return migrationList.contains(currentVersion) && previousVersion.compareTo(currentVersion) < 0;
    }

    protected static ModuleVersionNumber getModuleVersionNumber(Serializable moduleVersion) {
        if (moduleVersion instanceof ModuleVersionNumber) return (ModuleVersionNumber) moduleVersion;
        if (moduleVersion instanceof VersionNumber) return new ModuleVersionNumber((VersionNumber)moduleVersion);
        if (moduleVersion instanceof String) return new ModuleVersionNumber((String)moduleVersion);
        return null;
    }
}
