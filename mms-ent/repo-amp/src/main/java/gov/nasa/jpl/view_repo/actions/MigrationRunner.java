package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.alfresco.repo.admin.patch.AbstractPatch;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

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
    public static ModuleDetails moduleDetails;

    private static boolean isRunning = false;

    private static final List<String> migrationList;

    static {
        migrationList = new LinkedList<>();
        migrationList.add("3.1.0");
        migrationList.add("3.2.0");
        migrationList.add("3.3.0");
    }

    public void setServices(ServiceRegistry services) {
        this.services = services;
    }

    @Override
    protected String applyInternal() throws Exception {
        logger.info("Starting execution of patch");
        StoreRef store = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
        NodeRef rootRef = services.getNodeService().getRootNode(store);
        if (checkMigration(services, rootRef)) {
            return "Migration executed successfully";
        } else {
            throw new Exception("Migration failed");
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean checkMigration(ServiceRegistry services, NodeRef root) {
        String previousVersion = getPreviousVersion(services);
        String currentVersion = getCurrentVersion();
        if (isMigrationNeeded(previousVersion, currentVersion)) {
            logger.info("Migration Needed!");
            if (migrationList.contains(currentVersion)) {
                logger.info("Automigration path exists.");
                for (String migrationFor : migrationList) {
                    if (compareVersions(previousVersion, migrationFor) < 0) {
                        logger.info("Update path found");
                        logger.info("Migration For: " + migrationFor);
                        try {
                            Class clazz = Class.forName("gov.nasa.jpl.view_repo.actions.migrations." + versionToClassname(migrationFor));
                            Method method = clazz.getMethod("apply", ServiceRegistry.class);
                            logger.info("Invoking migration for: " + migrationFor);
                            return (boolean) method.invoke(null, services);
                        } catch (ClassNotFoundException cnfe) {
                            logger.info("No migration found: ", cnfe);
                        } catch (NoSuchMethodException nsme) {
                            logger.info("Error executing migration: ", nsme);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.info("Error invoking migration", e);
                        } catch (Exception e) {
                            logger.info("General Error: ", e);
                        }
                    }
                }
            }
        }
        return false;
    }

    public static String getPreviousVersion(ServiceRegistry services) {
        ModuleService moduleService =
            (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        List<ModuleDetails> modules = moduleService.getAllModules();
        for (ModuleDetails module : modules) {
            if (module.getId().contains("mms-amp")) {
                moduleDetails = module;
                logger.info("MODULE VERSION IN DB: " + cleanVersion(module.getModuleVersionNumber().toString()));
                return cleanVersion(module.getModuleVersionNumber().toString());
            }
        }
        return null;
    }

    public static String getCurrentVersion() {
        try {
            EmsConfig.setAlfrescoProperties(moduleDetails.getProperties());
            logger.info("MODULE VERSION IN PROPERTIES: " + cleanVersion(EmsConfig.get("module.version")));
            return cleanVersion(EmsConfig.get("module.version"));
        } catch (Exception e) {
            logger.error(e);
        }

        return null;
    }

    public static String cleanVersion(String version) {
        if (version == null) {
            return null;
        }
        return version.indexOf('-') > -1 ? version.substring(0, version.indexOf('-')) : version;
    }

    public static String versionToClassname(String version) {
        return "Migrate_" + version.replace('.', '_');
    }

    public static int compareVersions(String previous, String current) {
        String[] previousVersionString = cleanVersion(previous).split("\\.");
        int[] previousVersionArr = new int[previousVersionString.length];
        String[] currentVersionString = cleanVersion(current).split("\\.");
        int[] currentVersionArr = new int[currentVersionString.length];

        for (int i = 0; i < previousVersionString.length; i++) {
            previousVersionArr[i] = Integer.valueOf(previousVersionString[i]);
        }

        for (int i = 0; i < currentVersionString.length; i++) {
            currentVersionArr[i] = Integer.valueOf(currentVersionString[i]);
        }

        int max =
            previousVersionArr.length > currentVersionArr.length ? previousVersionArr.length : currentVersionArr.length;

        for (int i = 0; i < max; i++) {
            int prev = i < previousVersionArr.length ? previousVersionArr[i] : 0;
            int curr = i < currentVersionArr.length ? currentVersionArr[i] : 0;
            if (prev != curr) {
                return prev < curr ? -1 : 1;
            }
        }
        return 0;
    }

    public static boolean isMigrationNeeded(String previousVersion, String currentVersion) {
        return migrationList.contains(cleanVersion(currentVersion)) && compareVersions(previousVersion, currentVersion) < 0;
    }
}
