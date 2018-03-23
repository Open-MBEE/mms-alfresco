package gov.nasa.jpl.view_repo.actions.migrations;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.Sjm;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

public class Migrate_3_3_0 {

    static Logger logger = Logger.getLogger(Migrate_3_3_0.class);

    public static boolean apply(ServiceRegistry services, NodeRef root) {
        logger.error("RUNNING migrationThreeThreeOh");
        PostgresHelper pgh = new PostgresHelper();

        String adminUserName = AuthenticationUtil.getAdminUserName();
        AuthenticationUtil.setFullyAuthenticatedUser(adminUserName);

        FileFolderService fileFolderService = (FileFolderService) services
            .getService(QName.createQName(NamespaceService.ALFRESCO_URI, "FileFolderService"));

        logger.error("FileFolderService loaded");

        List<Map<String, String>> orgs = pgh.getOrganizations(null);

        for (Map<String, String> org : orgs) {
            String orgId = org.get("orgId");
            SiteService siteService = services.getSiteService();

            logger.error("SiteService loaded");

            SiteInfo siteInfo = siteService.getSite(orgId);
            List<Map<String, Object>> projects = pgh.getProjects(orgId);
            EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services);
            logger.error("SiteNode loaded");
            for (Map<String, Object> project : projects) {
                logger.error("Updating: " + project.get(Sjm.SYSMLID).toString());
                pgh.setProject(project.get(Sjm.SYSMLID).toString());
                pgh.execUpdate(
                    "CREATE TABLE IF NOT EXISTS artifacts(id bigserial primary key, elasticId text not null unique, sysmlId text not null unique, lastCommit text, initialCommit text, deleted boolean default false);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS artifactIndex on artifacts(id);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS sysmlArtifactIndex on artifacts(sysmlId);");

                List<Pair<String, String>> refs = pgh.getRefsElastic();

                logger.error("Getting files");
                for (Pair<String, String> ref : refs) {
                    EmsScriptNode refNode = siteNode.childByNamePath(ref.first);
                    List<FileInfo> files = fileFolderService.list(refNode.getNodeRef());
                    for (FileInfo file : files) {
                        logger.error(file.getName());
                    }
                }
            }
        }

        return false;
    }
}
