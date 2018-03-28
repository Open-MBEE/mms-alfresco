package gov.nasa.jpl.view_repo.actions.migrations;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
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
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Migrate_3_3_0 {

    static Logger logger = Logger.getLogger(Migrate_3_3_0.class);
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    public static boolean apply(ServiceRegistry services) throws Exception {
        logger.info("RUNNING migrationThreeThreeOh");
        PostgresHelper pgh = new PostgresHelper();
        boolean noErrors = true;

        final String adminUserName = AuthenticationUtil.getAdminUserName();
        AuthenticationUtil.setFullyAuthenticatedUser(adminUserName);

        FileFolderService fileFolderService = (FileFolderService) services
            .getService(QName.createQName(NamespaceService.ALFRESCO_URI, "FileFolderService"));

        logger.info("FileFolderService loaded");

        List<Map<String, String>> orgs = pgh.getOrganizations(null);

        for (Map<String, String> org : orgs) {
            String orgId = org.get("orgId");
            SiteService siteService = services.getSiteService();
            VersionService versionService = services.getVersionService();

            logger.info("SiteService loaded");

            SiteInfo siteInfo = siteService.getSite(orgId);
            List<Map<String, Object>> projects = pgh.getProjects(orgId);

            if (projects.size() < 1) {
                continue;
            }

            EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services);
            logger.info("SiteNode loaded: " + siteNode.getName());
            logger.info("Iterating through projects for " + siteNode.getName());
            for (Map<String, Object> project : projects) {
                String projectId = project.get(Sjm.SYSMLID).toString();

                logger.info("ProjectID: " + projectId);
                EmsScriptNode projectNode = siteNode.childByNamePath(projectId);
                logger.info("ProjectNode loaded: " + projectNode.getName());

                ElasticHelper eh = new ElasticHelper();

                logger.info("Updating: " + projectId);
                pgh.setProject(projectId);
                pgh.execUpdate(
                    "CREATE TABLE IF NOT EXISTS artifacts(id bigserial primary key, elasticId text not null unique, sysmlId text not null unique, lastCommit text, initialCommit text, deleted boolean default false);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS artifactIndex on artifacts(id);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS sysmlArtifactIndex on artifacts(sysmlId);");

                List<Pair<String, String>> refs = pgh.getRefsElastic();

                logger.info("Getting files");
                for (Pair<String, String> ref : refs) {
                    EmsScriptNode refNode = projectNode.childByNamePath("/refs/" + ref.first);

                    List<FileInfo> files = fileFolderService.list(refNode.getNodeRef());
                    for (FileInfo file : files) {
                        if (!file.isFolder()) {
                            VersionHistory versionHistory = versionService.getVersionHistory(file.getNodeRef());
                            Collection<Version> versions = versionHistory.getAllVersions();
                            Iterator it = versions.iterator();
                            while (it.hasNext()) {
                                Version version = (Version) it.next();
                                FileInfo frozenFile = fileFolderService.getFileInfo(version.getFrozenStateNodeRef());
                                String name = frozenFile.getName();
                                String creator = version.getFrozenModifier();
                                Date created = version.getFrozenModifiedDate();
                                String contentType = file.getContentData().getMimetype();
                                String commitId = UUID.randomUUID().toString();
                                String extension = FilenameUtils.getExtension(name);
                                String elasticId = UUID.randomUUID().toString();
                                String artifactId = name.substring(0, name.lastIndexOf('.'));
                                String alfrescoId = artifactId + System.currentTimeMillis() + "." + extension;

                                JSONObject artifactJson = new JSONObject();
                                artifactJson.put(Sjm.SYSMLID, alfrescoId);
                                artifactJson.put(Sjm.ELASTICID, elasticId);
                                artifactJson.put(Sjm.COMMITID, commitId);
                                artifactJson.put(Sjm.PROJECTID, projectId);
                                artifactJson.put(Sjm.REFID, ref.first);
                                artifactJson.put(Sjm.INREFIDS, new JSONArray().put(ref.first));
                                artifactJson.put(Sjm.CREATOR, creator);
                                artifactJson.put(Sjm.MODIFIER, creator);
                                artifactJson.put(Sjm.CREATED, df.format(created));
                                artifactJson.put(Sjm.MODIFIED, df.format(created));

                                JSONArray artifactJSONForElastic = new JSONArray();
                                artifactJSONForElastic.put(artifactJson);

                                try {
                                    boolean bulkEntry =
                                        eh.bulkIndexElements(artifactJSONForElastic, "added", true, projectId,
                                            Sjm.ARTIFACT.toLowerCase());

                                    if (bulkEntry) {
                                        JSONObject commitObject = new JSONObject();
                                        commitObject.put(Sjm.CREATED, created.toString());
                                        commitObject.put(Sjm.CREATOR, creator);
                                        commitObject.put(Sjm.SYSMLID, commitId);
                                        commitObject.put(Sjm.PROJECTID, projectId);
                                        commitObject.put(Sjm.TYPE, Sjm.ARTIFACT);
                                        commitObject.put(Sjm.SOURCE, name.startsWith("img_") ? "ve" : "magicdraw");

                                        JSONObject commitArtifact = new JSONObject();
                                        commitArtifact.put(Sjm.SYSMLID, alfrescoId);
                                        commitArtifact.put(Sjm.ELASTICID, elasticId);
                                        commitArtifact.put(Sjm.TYPE, Sjm.ARTIFACT);
                                        commitArtifact.put(Sjm.CONTENTTYPE, contentType);

                                        commitObject.put("added", new JSONArray().put(0, commitArtifact));

                                        eh.indexElement(commitObject, projectId);
                                        logger.info("JSON: " + commitObject);
                                    }
                                } catch (Exception e) {
                                    noErrors = false;
                                }
                            }
                        }
                    }
                }
            }
        }

        return noErrors;
    }
}
