package gov.nasa.jpl.view_repo.actions.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.*;
import gov.nasa.jpl.view_repo.db.ElasticImpl;
import gov.nasa.jpl.view_repo.util.CommitUtil;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.EmsNodeUtil;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.extensions.surf.util.URLEncoder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.UUID;

public class Migrate_3_3_0 {

    static Logger logger = Logger.getLogger(Migrate_3_3_0.class);
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final String transientSettings = "{\"transient\": {\"script.max_compilations_per_minute\":120}}";

    private static final String refScript =
        "{\"script\": {\"inline\": \"if(ctx._source.containsKey(\\\"%1$s\\\")){ctx._source.%1$s.add(params.refId)} else {ctx._source.%1$s = [params.refId]}\", \"params\":{\"refId\":\"%2$s\"}}}";

    private static final String artifactToElementScript =
        "{\"query\": {\"terms\":{\"id\":[\"%s\"]} }, \"script\": {\"inline\": \"ctx._source._artifactIds = [ctx._source.id + \\\"_svg\\\", ctx._source.id + \\\"_png\\\"]\"}}";

    private static final String searchQuery =
        "{\"query\":{\"bool\": {\"filter\":[{\"term\":{\"_projectId\":\"%1$s\"}},{\"term\":{\"id\":\"%2$s\"}},{\"term\":{\"_modified\":\"%3$s\"}}]}}, \"from\": 0, \"size\": 1}";

    private static final String renameScript =
        "{\"query\": {\"exists\":{\"field\":\"_isSite\"} }, \"script\": {\"inline\": \"ctx._source._isGroup = ctx._source.remove(\\\"_isSite\\\")\"}}";

    private static final String deleteCommitFix =
        "{\"script\": {\"inline\": \"if(!ctx._source.containsKey(\\\"_projectId\\\")){ctx._source._projectId = params.projectId}\", \"params\": {\"projectId\": \"%s\"}}}";

    private static final String ivanFix =
        "{\"query\": { \"match_all\": {} }, \"script\": {\"inline\": \"for (int i = 0; i < ctx._source.added.size(); i++) {if(!ctx._source.added[i].containsKey(\\\"type\\\")){ctx._source.added[i].type = \\\"element\\\"}} for (int i = 0; i < ctx._source.updated.size(); i++) {if(!ctx._source.updated[i].containsKey(\\\"type\\\")){ctx._source.updated[i].type = \\\"element\\\"}} for (int i = 0; i < ctx._source.deleted.size(); i++) {if(!ctx._source.deleted[i].containsKey(\\\"type\\\")){ctx._source.deleted[i].type = \\\"element\\\"}}\"}}";

    public static boolean apply(ServiceRegistry services) throws Exception {
        logger.info("Running Migrate_3_3_0");
        PostgresHelper pgh = new PostgresHelper();
        ElasticImpl eh = new ElasticImpl();

        // Temporarily increase max_compilations_per_minute
        eh.updateClusterSettings(transientSettings);

        boolean noErrors = true;

        final String adminUserName = AuthenticationUtil.getAdminUserName();
        AuthenticationUtil.setFullyAuthenticatedUser(adminUserName);

        FileFolderService fileFolderService = (FileFolderService) services
            .getService(QName.createQName(NamespaceService.ALFRESCO_URI, "FileFolderService"));

        logger.info("FileFolderService loaded");

        List<Map<String, String>> orgs = pgh.getOrganizations(null);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream("mapping_template.json");
        Scanner s = new Scanner(resourceAsStream).useDelimiter("\\A");
        JsonObject mappingTemplate = new JsonObject();
        if (s.hasNext()) {
            mappingTemplate = JsonUtil.buildFromString(s.next());
            eh.applyTemplate(mappingTemplate.toString());
        }

        for (Map<String, String> org : orgs) {
            String orgId = org.get("orgId");
            SiteService siteService = services.getSiteService();
            ContentService contentService = services.getContentService();
            VersionService versionService = services.getVersionService();

            logger.info("SiteService loaded");

            SiteInfo siteInfo = siteService.getSite(orgId);

            try (PreparedStatement statement = pgh.getConn("config").prepareStatement("DROP TABLE IF EXISTS projectMounts")) {
                statement.execute();
            }

            List<Map<String, Object>> projects = pgh.getProjects(orgId);
            if (projects.isEmpty()) {
                continue;
            }

            EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services);
            logger.info("SiteNode loaded: " + siteNode.getName());
            logger.info("Iterating through projects for " + siteNode.getName());
            for (Map<String, Object> project : projects) {

                String projectId = project.get(Sjm.SYSMLID).toString();

                logger.info("ProjectID: " + projectId);
                EmsScriptNode projectNode = siteNode.childByNamePath(projectId);

                if (projectNode == null) {
                    logger.info("ProjectNode not found for: " + projectId);
                    continue;
                }

                logger.info("ProjectNode loaded: " + projectNode.getName());

                //Reindex to rename fields
                eh.updateByQuery(projectId, renameScript, "element");

                if (mappingTemplate.isJsonObject()) {
                    eh.updateMapping(projectId, "element",
                        mappingTemplate.get("mappings").getAsJsonObject().get("element").getAsJsonObject().toString());
                    eh.updateMapping(projectId, "commit",
                        mappingTemplate.get("mappings").getAsJsonObject().get("commit").getAsJsonObject().toString());
                    eh.updateMapping(projectId, "artifact",
                        mappingTemplate.get("mappings").getAsJsonObject().get("artifact").getAsJsonObject().toString());
                }

                eh.updateByQuery(projectId, ivanFix, "commit");

                logger.info("Updating: " + projectId);

                pgh.setProject(projectId);
                pgh.execUpdate(
                    "CREATE TABLE IF NOT EXISTS artifacts(id bigserial primary key, elasticId text not null unique, sysmlId text not null unique, lastCommit text, initialCommit text, deleted boolean default false);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS artifactIndex on artifacts(id);");
                pgh.execUpdate("CREATE INDEX IF NOT EXISTS sysmlArtifactIndex on artifacts(sysmlId);");

                pgh.execUpdate("DROP TABLE IF EXISTS commitParent");

                String initialCommit = pgh.getProjectInitialCommit();

                List<Pair<String, String>> refs = pgh.getRefsElastic(true);

                logger.info("Getting files");

                Set<String> mdArtifacts = new HashSet<>();
                for (Pair<String, String> ref : refs) {
                    Set<String> artifactsToUpdate = new HashSet<>();

                    String refId = ref.first.replace("_", "-");
                    boolean isTag = pgh.isTag(refId);

                    logger.info("RefId: " + refId);
                    logger.info("isTag" + isTag);

                    pgh.setWorkspace(refId);

                    if (!ref.first.equals("master")) {
                        pgh.execUpdate(String.format(
                            "CREATE TABLE IF NOT EXISTS artifacts%s (LIKE artifacts INCLUDING DEFAULTS INCLUDING CONSTRAINTS INCLUDING INDEXES)",
                            ref.first));

                        PreparedStatement parentStatement = pgh.prepareStatement(
                            "SELECT commits.elasticid, refs.parent FROM refs JOIN commits ON refs.parentCommit = commits.id WHERE refs.refId = ? LIMIT 1");
                        parentStatement.setString(1, ref.first);
                        try (ResultSet rs = parentStatement.executeQuery()) {
                            String parentCommit = null;
                            String parentRefId = null;
                            if (rs.next()) {
                                parentCommit = rs.getString(1);
                                parentRefId = rs.getString(2).equals("") ? "master" : rs.getString(2);
                            }

                            if (parentCommit != null) {
                                pgh.setWorkspace(parentRefId);
                                JsonArray parentArtifacts = getArtifactsAtCommit(parentCommit, pgh, eh, projectId);
                                List<Map<String, Object>> artifactInserts = new ArrayList<>();
                                for (int i = 0; i < parentArtifacts.size(); i++) {
                                    JsonObject parentArt = parentArtifacts.get(i).getAsJsonObject();

                                    Artifact parentArtNode =
                                        pgh.getArtifactFromSysmlId(parentArt.get(Sjm.SYSMLID).getAsString(), true);

                                    if (parentArtNode != null) {
                                        Map<String, Object> artifact = new HashMap<>();
                                        artifact.put(Sjm.ELASTICID, parentArt.get(Sjm.ELASTICID).getAsString());
                                        artifact.put(Sjm.SYSMLID, parentArt.get(Sjm.SYSMLID).getAsString());
                                        artifact.put("initialcommit", parentArtNode.getInitialCommit());
                                        artifact.put("lastcommit", parentArt.get(Sjm.COMMITID).getAsString());
                                        artifactInserts.add(artifact);
                                    }
                                }
                                pgh.setWorkspace(refId);
                                if (!artifactInserts.isEmpty()) {
                                    pgh.runBatchQueries(artifactInserts, "artifacts");
                                }
                            }
                        }
                    }

                    if (isTag) {
                        pgh.execQuery(String
                            .format("GRANT INSERT, UPDATE, DELETE ON nodes%1$s, edges%1$s, artifacts%1$s FROM %2$s",
                                PostgresHelper.sanitizeRefId(refId), EmsConfig.get("pg.user")));
                    }

                    // Insert part property type childview edges
                    List<Node> latestPropertyNodes = pgh.getNodesByType(GraphInterface.DbNodeTypes.PROPERTY);

                    if (latestPropertyNodes.size() > 0) {
                        List<String> propertyElasticIds = new ArrayList<>();
                        for (int i = 0; i < latestPropertyNodes.size(); i++) {
                            Node latestPropertyNode = latestPropertyNodes.get(i);
                            if (!latestPropertyNode.getElasticId().isEmpty()) {
                                propertyElasticIds.add(latestPropertyNode.getElasticId());
                            }
                        }

                        JsonArray propertyElasticElements = eh.getElementsFromDocStoreIds(propertyElasticIds, projectId);
                        for (int i = 0; i < propertyElasticElements.size(); i++) {
                            JsonObject propertyNodeElastic = propertyElasticElements.get(i).getAsJsonObject();
                            if (CommitUtil.isPartProperty(propertyNodeElastic) && propertyNodeElastic.has(Sjm.TYPEID)
                                && !propertyNodeElastic.get(Sjm.TYPEID).isJsonNull() && !propertyNodeElastic.get(Sjm.TYPEID)
                                .getAsString().isEmpty()) {
                                pgh.insertEdge(propertyNodeElastic.get(Sjm.SYSMLID).getAsString(),
                                    propertyNodeElastic.get(Sjm.TYPEID).getAsString(), GraphInterface.DbEdgeTypes.CHILDVIEW);
                            }
                        }
                    }

                    EmsScriptNode refNode = projectNode.childByNamePath("/refs/" + refId);

                    if (refNode == null) {
                        logger.info("Ref not found: " + refId);
                        continue;
                    }

                    Set<String> refCommitElastics = new HashSet<>();
                    List<Map<String, Object>> refCommits = pgh.getRefsCommits(ref.first);
                    for (Map<String, Object> refCommit : refCommits) {
                        // Elastic ID for commit map is actually id not elasticid
                        if (refCommit.containsKey(Sjm.SYSMLID) && initialCommit != null && !initialCommit
                            .equals(refCommit.get(Sjm.SYSMLID).toString())) {
                            refCommitElastics.add(refCommit.get(Sjm.SYSMLID).toString());
                        }
                    }
                    String deleteFixToRun = String.format(deleteCommitFix, projectId);
                    eh.bulkUpdateElements(refCommitElastics, deleteFixToRun, projectId, "commit");

                    List<FileInfo> files = fileFolderService.list(refNode.getNodeRef());

                    for (FileInfo file : files) {
                        if (file != null && !file.isFolder()) {
                            VersionHistory versionHistory = versionService.getVersionHistory(file.getNodeRef());
                            if (versionHistory == null) {
                                continue;
                            }
                            List<Version> versions = new ArrayList<>(versionHistory.getAllVersions());
                            ListIterator it = versions.listIterator(versions.size());

                            String artifactId = null;
                            String realCreator = null;
                            Date realCreated = null;

                            while (it.hasPrevious()) {
                                Version version = (Version) it.previous();
                                FileInfo versionedFile = fileFolderService.getFileInfo(version.getVersionedNodeRef());
                                String name = versionedFile.getName();
                                String url = String.format("/service/api/node/content/versionStore/version2Store/%s/%s",
                                    version.getVersionProperty("node-uuid"),
                                    URLEncoder.encode(versionedFile.getName()));

                                String modifier = version.getFrozenModifier();
                                Date modified = version.getFrozenModifiedDate();
                                if (realCreator == null) {
                                    realCreator = version.getFrozenModifier();
                                }
                                if (realCreated == null) {
                                    realCreated = version.getFrozenModifiedDate();
                                }
                                String creator = realCreator;
                                Date created = realCreated;
                                String contentType = file.getContentData().getMimetype();
                                String commitId = UUID.randomUUID().toString();
                                String extension = FilenameUtils.getExtension(name);
                                String elasticId = UUID.randomUUID().toString();
                                String baseId = name.substring(0, name.lastIndexOf('.'));

                                if (!baseId.startsWith("img_")) {
                                    mdArtifacts.add(baseId);
                                    artifactId = baseId + "_" + extension;
                                } else {
                                    artifactId = baseId;
                                }

                                String checkQuery =
                                    String.format(searchQuery, projectId, artifactId, df.format(modified));
                                JsonObject checkQueryObj = JsonUtil.buildFromString(checkQuery);
                                JsonObject check = eh.search(checkQueryObj);

                                if (!check.has(Sjm.ELEMENTS) || check.getAsJsonArray(Sjm.ELEMENTS).size() < 1) {
                                    JsonObject artifactJson = new JsonObject();
                                    artifactJson.addProperty(Sjm.SYSMLID, artifactId);
                                    artifactJson.addProperty(Sjm.ELASTICID, elasticId);
                                    artifactJson.addProperty(Sjm.COMMITID, commitId);
                                    artifactJson.addProperty(Sjm.PROJECTID, projectId);
                                    artifactJson.addProperty(Sjm.REFID, refId);
                                    artifactJson.add(Sjm.INREFIDS, new JsonArray());
                                    artifactJson.addProperty(Sjm.CREATOR, creator);
                                    artifactJson.addProperty(Sjm.MODIFIER, modifier);
                                    artifactJson.addProperty(Sjm.CREATED, df.format(created));
                                    artifactJson.addProperty(Sjm.MODIFIED, df.format(modified));
                                    artifactJson.addProperty(Sjm.CONTENTTYPE, contentType);
                                    artifactJson.addProperty(Sjm.ARTIFACTLOCATION, url);
                                    InputStream is =
                                        contentService.getReader(versionedFile.getNodeRef(), ContentModel.PROP_CONTENT)
                                            .getContentInputStream();
                                    Scanner s2 = new Scanner(is).useDelimiter("\\A");

                                    if (s2.hasNext()) {
                                        artifactJson.addProperty(Sjm.CHECKSUM, EmsNodeUtil.md5Hash(s2.next()));
                                    }

                                    JsonArray artifactJSONForElastic = new JsonArray();
                                    artifactJSONForElastic.add(artifactJson);

                                    try {
                                        boolean bulkEntry =
                                            eh.bulkIndexElements(artifactJSONForElastic, "added", true, projectId,
                                                Sjm.ARTIFACT.toLowerCase());

                                        if (bulkEntry) {

                                            JsonObject commitObject = new JsonObject();
                                            commitObject.addProperty(Sjm.ELASTICID, commitId);
                                            commitObject.addProperty(Sjm.CREATED, df.format(modified));
                                            commitObject.addProperty(Sjm.CREATOR, modifier);
                                            commitObject.addProperty(Sjm.PROJECTID, projectId);
                                            commitObject
                                                .addProperty(Sjm.SOURCE, name.startsWith("img_") ? "ve" : "magicdraw");

                                            JsonArray added = new JsonArray();

                                            JsonObject simpleArtifactJson = new JsonObject();
                                            simpleArtifactJson.addProperty(Sjm.SYSMLID, artifactId);
                                            simpleArtifactJson.addProperty(Sjm.ELASTICID, elasticId);
                                            simpleArtifactJson.addProperty(Sjm.TYPE, Sjm.ARTIFACT.toLowerCase());
                                            simpleArtifactJson.addProperty(Sjm.CONTENTTYPE, contentType);

                                            added.add(simpleArtifactJson);
                                            commitObject.add("added", added);
                                            commitObject.add("updated", new JsonArray());
                                            commitObject.add("deleted", new JsonArray());
                                            commitObject.addProperty(Sjm.PROJECTID, projectId);
                                            commitObject.addProperty(Sjm.REFID, refId);

                                            eh.indexElement(commitObject, projectId, DocStoreInterface.COMMIT);

                                            logger.debug("Indexed JSON: " + commitObject);
                                        } else {
                                            logger.info("Bulk insert failed for: " + projectId);
                                            noErrors = false;
                                        }
                                    } catch (Exception e) {
                                        logger.info("Exception when indexing elements for: " + projectId, e);
                                        noErrors = false;
                                    }
                                } else {
                                    elasticId =
                                        check.getAsJsonArray(Sjm.ELEMENTS).get(0).getAsJsonObject().get(Sjm.ELASTICID)
                                            .getAsString();
                                    commitId =
                                        check.getAsJsonArray(Sjm.ELEMENTS).get(0).getAsJsonObject().get(Sjm.COMMITID)
                                            .getAsString();
                                }

                                if (pgh.getArtifactFromSysmlId(artifactId, true) == null) {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("elasticId", elasticId);
                                    map.put("sysmlId", artifactId);
                                    map.put("lastCommit", commitId);
                                    map.put("initialCommit", elasticId);
                                    pgh.insert("artifacts" + (ref.first.equals("master") ? "" : ref.first), map);
                                } else {
                                    String query = String.format(
                                        "UPDATE \"artifacts%s\" SET elasticId = ?, lastcommit = ?, deleted = ? WHERE sysmlId = ?",
                                        ref.first.equals("master") ? "" : ref.first);
                                    try (PreparedStatement statement = pgh.prepareStatement(query)) {
                                        statement.setString(1, elasticId);
                                        statement.setString(2, commitId);
                                        statement.setBoolean(3, false);
                                        statement.setString(4, artifactId);
                                        statement.execute();
                                    }
                                }

                                Map<String, String> commitFromDb =
                                    pgh.getCommitAndTimestamp("timestamp", new Timestamp(modified.getTime()));
                                if (commitFromDb == null || !commitFromDb.get(Sjm.COMMITID).equals(commitId)) {
                                    pgh.insertCommit(commitId, GraphInterface.DbCommitTypes.COMMIT, creator,
                                        new Timestamp(modified.getTime()));
                                }
                            } // End of While Loop

                            Artifact latestArtifact = pgh.getArtifactFromSysmlId(artifactId, true);
                            if (latestArtifact != null) {
                                artifactsToUpdate.add(latestArtifact.getElasticId());
                            }
                        }
                    }

                    if (isTag) {
                        pgh.setAsTag(refId);
                    }

                    String refScriptToRun = String.format(refScript, Sjm.INREFIDS, refId);
                    eh.bulkUpdateElements(artifactsToUpdate, refScriptToRun, projectId, "artifact");
                }
                if (!mdArtifacts.isEmpty()) {
                    String artifactToElementScriptToRun =
                        String.format(artifactToElementScript, String.join("\",\"", mdArtifacts));
                    eh.updateByQuery(projectId, artifactToElementScriptToRun, "element");
                }
            }
        }

        return noErrors;
    }

    public static JsonArray getArtifactsAtCommit(String commitId, PostgresHelper pgh, ElasticImpl eh,
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

            List<String> artifactElasticIds = new ArrayList<>();

            for (Map<String, Object> element : pgh.getAllArtifactsWithLastCommitTimestamp()) {
                if (((Date) element.get(Sjm.TIMESTAMP)).getTime() <= ((Date) commit.get(Sjm.TIMESTAMP)).getTime()) {
                    artifactElasticIds.add((String) element.get(Sjm.ELASTICID));
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

                if (pastElement != null && pastElement.has(Sjm.SYSMLID)) {
                    artifacts.add(pastElement);
                }
            }

            try {
                JsonArray artifactElastic = eh.getElementsFromDocStoreIds(artifactElasticIds, projectId);
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
