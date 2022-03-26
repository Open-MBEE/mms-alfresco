public class VerifyMigration {

    public static boolean checkMigration(ServiceRegistry services, RegistryService registryService) {
        ModuleVersionNumber currentVersion = getCurrentVersion(registryService);
        ModuleVersionNumber installedVersion = getInstalledVersion(services);
        if (isMigrationNeeded(currentVersion, installedVersion)) {
            logger.info("Migration Needed!");
            if (migrationList.contains(installedVersion)) {
                logger.info("Migration path exists.");
                for (ModuleVersionNumber migrationFor : migrationList) {
                    if (currentVersion.compareTo(migrationFor) < 0) {
                        logger.info("Upgrade path found");
                        runMigration(migrationFor, services);
                    }
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static void runMigration(ModuleVersionNumber migrationFor, ServiceRegistry services) {
        try {
            Class classObj = Class.forName("gov.nasa.jpl.view_repo.actions.migrations." + versionToClassname(migrationFor));
            Method method = classObj.getMethod("apply", ServiceRegistry.class);
            logger.info("Invoking migration for: " + migrationFor);
            method.invoke(null, services);
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
