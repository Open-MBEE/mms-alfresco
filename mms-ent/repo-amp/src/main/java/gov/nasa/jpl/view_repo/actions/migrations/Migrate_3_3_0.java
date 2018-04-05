package gov.nasa.jpl.view_repo.actions.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.GraphInterface;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

                    if (!ref.first.equals("master")) {
                        pgh.execUpdate(String.format(
                            "CREATE TABLE IF NOT EXISTS artifacts%s (LIKE artifacts INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
                            ref.first));

                        PreparedStatement parentStatement = pgh.prepareStatement(
                            "SELECT commits.elasticid FROM refs JOIN commits ON refs.parentCommit = commits.id WHERE refs.refId = ? LIMIT 1");
                        parentStatement.setString(1, ref.first);
                        ResultSet rs = parentStatement.executeQuery();
                        String parentCommit = null;
                        if (rs.next()) {
                            parentCommit = rs.getString(1);
                        }

                        if (parentCommit != null) {
                            JsonArray parentArtifacts = getArtifactsAtCommit(parentCommit, pgh, eh, projectId);
                            List<Map<String, Object>> artifactInserts = new ArrayList<>();
                            for (int i = 0; i < parentArtifacts.size(); i++) {
                                JsonObject parentArt = parentArtifacts.get(i).getAsJsonObject();

                                if (pgh.getArtifactFromSysmlId(parentArt.get(Sjm.SYSMLID).getAsString(), true) == null) {
                                    Map<String, Object> artifact = new HashMap<>();
                                    artifact.put(Sjm.ELASTICID, parentArt.get(Sjm.ELASTICID).getAsString());
                                    artifact.put(Sjm.SYSMLID, parentArt.get(Sjm.SYSMLID).getAsString());
                                    artifact.put("initialcommit", parentArt.get(Sjm.ELASTICID).getAsString());
                                    artifact.put("lastcommit", parentArt.get(Sjm.COMMITID).getAsString());
                                    artifactInserts.add(artifact);
                                }
                            }
                            if (!artifactInserts.isEmpty()) {
                                pgh.runBatchQueries(artifactInserts, "artifacts");
                            }
                        }
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

                                JsonObject check =
                                    eh.getElementsLessThanOrEqualTimestamp(artifactId, timestamp, inRefs, projectId);

                                if (!check.has(Sjm.SYSMLID)) {
                                    JsonObject artifactJson = new JsonObject();
                                    artifactJson.addProperty(Sjm.SYSMLID, artifactId);
                                    artifactJson.addProperty(Sjm.ELASTICID, elasticId);
                                    artifactJson.addProperty(Sjm.COMMITID, commitId);
                                    artifactJson.addProperty(Sjm.PROJECTID, projectId);
                                    artifactJson.addProperty(Sjm.REFID, ref.first);
                                    JsonArray inRefIds = new JsonArray();
                                    inRefIds.add(ref.first);
                                    artifactJson.add(Sjm.INREFIDS, inRefIds);
                                    artifactJson.addProperty(Sjm.CREATOR, creator);
                                    artifactJson.addProperty(Sjm.MODIFIER, creator);
                                    artifactJson.addProperty(Sjm.CREATED, df.format(created));
                                    artifactJson.addProperty(Sjm.MODIFIED, df.format(created));
                                    artifactJson.addProperty(Sjm.CONTENTTYPE, contentType);
                                    artifactJson.addProperty(Sjm.CHECKSUM, "");

                                    JsonArray artifactJSONForElastic = new JsonArray();
                                    artifactJSONForElastic.add(artifactJson);

                                    try {
                                        boolean bulkEntry =
                                            eh.bulkIndexElements(artifactJSONForElastic, "added", true, projectId,
                                                Sjm.ARTIFACT.toLowerCase());

                                        if (bulkEntry) {

                                            JsonObject commitObject = new JsonObject();
                                            commitObject.addProperty(Sjm.ELASTICID, commitId);
                                            commitObject.addProperty(Sjm.CREATED, df.format(created));
                                            commitObject.addProperty(Sjm.CREATOR, creator);
                                            commitObject.addProperty(Sjm.PROJECTID, projectId);
                                            commitObject.addProperty(Sjm.TYPE, Sjm.ARTIFACT);
                                            commitObject.addProperty(Sjm.SOURCE, name.startsWith("img_") ? "ve" : "magicdraw");

                                            JsonArray added = new JsonArray();
                                            added.set(0, artifactJson);
                                            commitObject.add("added", added);

                                            eh.indexElement(commitObject, projectId, ElasticHelper.COMMIT);

                                            logger.info("JSON: " + commitObject);
                                        } else {
                                            noErrors = false;
                                        }
                                    } catch (Exception e) {
                                        noErrors = false;
                                    }
                                } else {
                                    elasticId = check.get(Sjm.SYSMLID).getAsString();
                                }

                                if (pgh.getArtifactFromSysmlId(artifactId, true) == null) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("elasticId", elasticId);
                                    map.put("sysmlId", artifactId);
                                    map.put("initialCommit", elasticId);
                                    pgh.insert("artifacts" + (ref.first.equals("master") ? "" : ref.first), map);
                                } else {
                                    String query = String.format(
                                        "UPDATE \"artifacts%s\" SET elasticId = ?, lastcommit = ?, deleted = ? WHERE sysmlId = ?",
                                        ref.first.equals("master") ? "" : ref.first);
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

    public static JsonArray getArtifactsAtCommit(String commitId, PostgresHelper pgh, ElasticHelper eh,
        String projectId) {
        JsonArray artifacts = new JsonArray();
        JsonObject pastElement = null;
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
                if (((Date) element.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
                    if (!deletedElementIds.containsKey((String) element.get(Sjm.ELASTICID))) {
                        artifactElasticIds.add((String) element.get(Sjm.ELASTICID));
                    }
                } else {
                    String sysmlId = (String) element.get(Sjm.SYSMLID);

                    try {
                        Map<String, Object> commitObj = pgh.getCommit(commitId);
                        if (commitObj != null) {
                            Date date = (Date) commitObj.get(Sjm.TIMESTAMP);
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(date.getTime());
                            cal.setTimeZone(TimeZone.getTimeZone("GMT"));
                            String timestamp = df.format(cal.getTime());
                            pastElement =
                                eh.getElementsLessThanOrEqualTimestamp(sysmlId, timestamp, refsCommitsIds, projectId);
                        }
                    } catch (Exception e) {
                        logger.error(String.format("%s", LogUtil.getStackTrace(e)));
                    }
                }

                if (pastElement != null && pastElement.has(Sjm.SYSMLID) && !deletedElementIds
                    .containsKey(pastElement.get(Sjm.ELASTICID).getAsString())) {
                    artifacts.add(pastElement);
                }
            }

            try {
                JsonArray artifactElastic = eh.getElementsFromElasticIds(artifactElasticIds, projectId);
                for (int i = 0; i < artifactElastic.size(); i++) {
                    artifacts.add(artifactElastic.get(i).getAsJsonObject());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return artifacts;
    }
}
