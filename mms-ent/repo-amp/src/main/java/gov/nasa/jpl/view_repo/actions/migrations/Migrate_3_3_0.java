package gov.nasa.jpl.view_repo.actions.migrations;

import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.GraphInterface;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
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

import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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

                    pgh.setWorkspace(ref.first);
                    Pair<String, Long> parentRef = pgh.getParentRef(ref.first);

                    if (!ref.first.equals("master")) {
                        pgh.execUpdate(String.format(
                            "CREATE TABLE artifacts%s (LIKE artifacts INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
                            ref.first));

                        EmsNodeUtil emsNodeUtil = new EmsNodeUtil(projectId, ref.first);
                    }

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
                                String baseId = name.substring(0, name.lastIndexOf('.'));
                                String artifactId = name.startsWith("img_") ? name : baseId + "_" + extension;
                                //String alfrescoId = baseId + System.currentTimeMillis() + "." + extension;

                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(created.getTime());
                                cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                                String timestamp = df.format(cal.getTime());
                                List<String> inRefs = Arrays.asList(ref.first);

                                JSONObject check = eh.getElementsLessThanOrEqualTimestamp(artifactId, timestamp, inRefs, projectId);

                                if (!check.has(Sjm.SYSMLID)) {
                                    JSONObject artifactJson = new JSONObject();
                                    artifactJson.put(Sjm.SYSMLID, artifactId);
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
                                            commitObject.put(Sjm.CREATED, df.format(created));
                                            commitObject.put(Sjm.CREATOR, creator);
                                            commitObject.put(Sjm.SYSMLID, commitId);
                                            commitObject.put(Sjm.PROJECTID, projectId);
                                            commitObject.put(Sjm.TYPE, Sjm.ARTIFACT);
                                            commitObject.put(Sjm.SOURCE, name.startsWith("img_") ? "ve" : "magicdraw");

                                            JSONObject commitArtifact = new JSONObject();
                                            commitArtifact.put(Sjm.SYSMLID, artifactId);
                                            commitArtifact.put(Sjm.ELASTICID, elasticId);
                                            commitArtifact.put(Sjm.TYPE, Sjm.ARTIFACT);
                                            commitArtifact.put(Sjm.CONTENTTYPE, contentType);

                                            commitObject.put("added", new JSONArray().put(0, commitArtifact));

                                            eh.indexElement(commitObject, projectId);

                                            logger.info("JSON: " + commitObject);
                                        } else {
                                            noErrors = false;
                                        }
                                    } catch (Exception e) {
                                        noErrors = false;
                                    }
                                } else {
                                    elasticId = check.getString(Sjm.SYSMLID);
                                }

                                if (pgh.getArtifactFromSysmlId(artifactId, true) == null) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("elasticId", elasticId);
                                    map.put("sysmlId", artifactId);
                                    map.put("initialCommit", commitId);
                                    pgh.insert("artifacts" + ref.first, map);
                                } else {
                                    String query = String.format(
                                        "UPDATE \"artifacts%s\" SET elasticId = ?, lastcommit = ?, deleted = ? WHERE sysmlId = ?",
                                        ref.first);
                                    PreparedStatement statement = pgh.prepareStatement(query);
                                    statement.setString(1, elasticId);
                                    statement.setString(2, commitId);
                                    statement.setBoolean(3, false);
                                    statement.setString(4, artifactId);
                                    statement.execute();
                                }

                                int commitFromDb = pgh.getCommitId(elasticId);
                                if (commitFromDb == 0) {
                                    pgh.insertCommit(elasticId, GraphInterface.DbCommitTypes.COMMIT, creator,
                                        new java.sql.Date(created.getTime()));
                                }
                            }
                        }
                    }
                }
            }
        }

        return noErrors;
    }

    /*
    public JSONObject getModelAtCommit(String commitId, PostgresHelper pgh, ElasticHelper eh, String projectId) {
        JSONObject result = new JSONObject();
        JSONArray artifacts = new JSONArray();
        ArrayList<String> refsCommitsIds = new ArrayList<>();

        Map<String, Object> commit = pgh.getCommit(commitId);
        if (commit != null) {
            String refId = commit.get(Sjm.REFID).toString();

            List<Map<String, Object>> refsCommits = pgh.getRefsCommits(refId, (int) commit.get(Sjm.SYSMLID));
            for (Map<String, Object> ref : refsCommits) {
                refsCommitsIds.add((String) ref.get(Sjm.SYSMLID));
            }

            Map<String, String> deletedElementIds = eh.getDeletedElementsFromCommits(refsCommitsIds, projectId);
            List<String> artifactElasticIds = new ArrayList<>();

            for (Map<String, Object> element : pgh.getAllArtifactsWithLastCommitTimestamp()) {
                processElementForModelAtCommit(a, deletedElementIds, commit, commitId, refsCommitsIds, artifacts,
                    artifactElasticIds);

                if (((Date) element.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
                    if (!deletedElementIds.containsKey((String) element.get(Sjm.ELASTICID))) {
                        elasticIds.add((String) element.get(Sjm.ELASTICID));
                    }
                } else {
                    pastElement = getElementAtCommit((String) element.get(Sjm.SYSMLID), commitId, refsCommitsIds);
                }

                if (pastElement != null && pastElement.has(Sjm.SYSMLID) && !deletedElementIds
                    .containsKey(pastElement.getString(Sjm.ELASTICID))) {
                    elements.put(pastElement);
                }
            }

            try {
                JSONArray artifactElastic = eh.getElementsFromElasticIds(artifactElasticIds, projectId);
                for (int i = 0; i < artifactElastic.length(); i++) {
                    artifacts.put(artifactElastic.getJSONObject(i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.put(Sjm.ARTIFACTS, artifacts);
        }
        return result;
    }
    */
}
