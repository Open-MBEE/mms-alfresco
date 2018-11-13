package gov.nasa.jpl.view_repo.actions.migrations;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gov.nasa.jpl.mbee.util.Pair;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;

public class Migrate_3_4_0 {

    static Logger logger = Logger.getLogger(Migrate_3_4_0.class);
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static JsonParser parser = new JsonParser();

    private static final String transientSettings = "{\"transient\": {\"script.max_compilations_per_minute\":120}}";

    private static final String updateMasterRef =
        "{\"query\": {\"terms\": {\"id\":[\"master\"]}}, \"script\": {\"inline\": \"ctx._source.type = \\\"Branch\\\"\"}}";

    private static final String deleteQuery =
        "{\"query\": {\"constant_score\": {\"filter\": {\"terms\": {}}}}}";

    private static final String updateQuery = "UPDATE \"commits\" SET timestamp = ? WHERE elasticid = ?";

    public static boolean apply(ServiceRegistry services) throws Exception {
        logger.info("Running Migrate_3_4_0");
        PostgresHelper pgh = new PostgresHelper();
        ElasticHelper eh = new ElasticHelper();

        JsonObject mappingTemplate = new JsonObject();

        // Temporarily increase max_compilations_per_minute
        eh.updateClusterSettings(transientSettings);

        boolean noErrors = true;

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceAsStream = classLoader.getResourceAsStream("mapping_template.json");
        Scanner s = new Scanner(resourceAsStream).useDelimiter("\\A");
        if (s.hasNext()) {
            mappingTemplate = JsonUtil.buildFromString(s.next());
            eh.applyTemplate(mappingTemplate.toString());
        }
        eh.updateMapping(EmsConfig.get("elastic.index.element"), ElasticHelper.PROFILE,
            mappingTemplate.get("mappings").getAsJsonObject().get(ElasticHelper.PROFILE).getAsJsonObject().toString());
        List<Map<String, String>> orgs = pgh.getOrganizations(null);

        for (Map<String, String> org : orgs) {
            String orgId = org.get("orgId");
            List<Map<String, Object>> projects = pgh.getProjects(orgId);
            for (Map<String, Object> project : projects) {
                String projectId = project.get(Sjm.SYSMLID).toString();
                pgh.setProject(projectId);

                if (mappingTemplate.isJsonObject()) {
                    eh.updateMapping(projectId, ElasticHelper.REF,
                        mappingTemplate.get("mappings").getAsJsonObject().get(ElasticHelper.REF).getAsJsonObject().toString());
                }

                List<Pair<String, String>> allRefs = pgh.getRefsElastic(true);

                if (!allRefs.isEmpty()) {
                    Set<String> refsToMove = new HashSet<>();
                    JsonArray insertPayload = new JsonArray();
                    JsonArray deleteSet = new JsonArray();

                    for (Pair<String, String> singleRef : allRefs) {
                        refsToMove.add(singleRef.second);
                    }

                    List<String> refIds = new ArrayList<>();
                    refIds.addAll(refsToMove);

                    JsonArray refs = eh.getElementsFromElasticIds(refIds, projectId);

                    if (refs.size() > 0) {
                        for (int i = 0; i < refs.size(); i++) {
                            JsonObject ref = refs.get(i).getAsJsonObject();
                            if (ref.get(Sjm.ELASTICID) != null) {
                                insertPayload.add(ref);
                            }
                            if (ref.get(Sjm.SYSMLID) != null) {
                                deleteSet.add(ref.get(Sjm.SYSMLID).getAsString());
                            }
                        }

                        if (deleteSet.size() > 0) {
                            JsonObject delQuery = parser.parse(deleteQuery).getAsJsonObject();
                            delQuery.get("query").getAsJsonObject().get("constant_score").getAsJsonObject()
                                .get("filter").getAsJsonObject().get("terms").getAsJsonObject()
                                .add(Sjm.SYSMLID, deleteSet);
                            eh.deleteByQuery(projectId, delQuery.toString(), ElasticHelper.ELEMENT);
                        }

                        if (!refsToMove.isEmpty()) {
                            eh.bulkIndexElements(insertPayload, "added", true, projectId, ElasticHelper.REF);
                            eh.updateByQuery(projectId, updateMasterRef, ElasticHelper.REF);
                        }
                    }
                }

                List<Map<String, String>> commits = pgh.getAllCommits();
                for (Map<String, String> commit : commits) {
                    String commitId = commit.get("commitId");
                    if (!commitId.isEmpty()) {
                        JsonObject commitObject = eh.getByInternalId(commitId, projectId, ElasticHelper.COMMIT);
                        if (commitObject != null && commitObject.has(Sjm.CREATED)) {
                            try (PreparedStatement statement = pgh.prepareStatement(updateQuery)) {
                                Date created = df.parse(commitObject.get(Sjm.CREATED).getAsString());
                                Timestamp ts = new Timestamp(created.getTime());
                                statement.setTimestamp(1, ts);
                                statement.setString(2, commitId);
                                statement.execute();
                            } catch (ParseException pe) {
                                logger.info("Unable to parse date: ", pe);
                            }
                        } else {
                            logger.error("Commit object has no created date.");
                            noErrors = false;
                        }
                    }
                }
            }
        }

        return noErrors;
    }
}
