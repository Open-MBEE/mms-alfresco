package gov.nasa.jpl.view_repo.actions;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.module.ModuleDetails;
import org.alfresco.service.cmr.module.ModuleService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jason Han
 */

public class MigrationRunner {
    static Logger logger = Logger.getLogger(MigrationRunner.class);

    public static ServiceRegistry services = null;
    public static ModuleDetails moduleDetails = null;

    private static final Map<String, Map<String, String>> migrationMap;

    static {
        migrationMap = new HashMap<>();
        Map<String, String> previous = new LinkedHashMap<>();
        previous.put("3.1.0", "migrationThreeTwoOh");
        previous.put("3.2.0", "migrationThreeTwoOh");
        previous.put("3.3.0", "migrationThreeThreeOh");
        migrationMap.put("3.3.0", previous);
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

    @SuppressWarnings("unchecked")
    public static boolean checkMigration(ServiceRegistry services) {
        setServices(services);
        String previousVersion = getPreviousVersion();
        String currentVersion = getCurrentVersion();
        if (isMigrationNeeded(previousVersion, currentVersion)) {
            logger.error("Migration Needed!");
            if (migrationMap.containsKey(currentVersion)) {
                logger.error("Automigration path exists.");
                Iterator it = migrationMap.get(currentVersion).entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = (Map.Entry) it.next();
                    String migrationFor = pair.getKey();
                    if (compareVersions(previousVersion, migrationFor) < 0) {
                        logger.error("Update path found");
                        String methodToRun = pair.getValue();
                        logger.error("methodToRun: " + methodToRun);
                        try {
                            Method method = MigrationRunner.class.getMethod(methodToRun);
                            logger.error("Invoking method: " + methodToRun);
                            method.invoke(null);
                        } catch (NoSuchMethodException nsme) {
                            logger.error("No migration");
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.error("Error invoking migration", e);
                        }
                    }
                    it.remove();
                }
            }
        }
        return false;
    }

    public static String getPreviousVersion() {
        ModuleService moduleService =
            (ModuleService) services.getService(QName.createQName(NamespaceService.ALFRESCO_URI, "ModuleService"));
        List<ModuleDetails> modules = moduleService.getAllModules();
        for (ModuleDetails module : modules) {
            if (module.getId().contains("mms-amp")) {
                moduleDetails = module;
                logger.error("MODULE VERSION IN DB: " + cleanVersion(module.getModuleVersionNumber().toString()));
                return "3.2.4";
                //return cleanVersion(module.getModuleVersionNumber().toString());
            }
        }
        return null;
    }

    public static String getCurrentVersion() {
        try {
            EmsConfig.setAlfrescoProperties(moduleDetails.getProperties());
            logger.error("MODULE VERSION IN PROPERTIES: " + cleanVersion(EmsConfig.get("module.version")));
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
        return migrationMap.containsKey(cleanVersion(currentVersion)) && compareVersions(previousVersion, currentVersion) < 0;
    }

    public static void migrationThreeThreeOh() {
        logger.error("RUNNING migrationThreeThreeOh");
    }

    public static void migrationThreeTwoOh() {
        logger.error("RUNNING migrationThreeTwoOh");
    }
}
