package gov.nasa.jpl.view_repo.actions.migrations;

import com.google.gson.JsonObject;
import gov.nasa.jpl.view_repo.db.ElasticHelper;
import gov.nasa.jpl.view_repo.db.PostgresHelper;
import gov.nasa.jpl.view_repo.util.Sjm;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;

public class Migrate_3_4_0 {

    static Logger logger = Logger.getLogger(Migrate_3_4_0.class);
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final String transientSettings = "{\"transient\": {\"script.max_compilations_per_minute\":120}}";

    private static final String searchQuery =
        "{\"query\":{\"bool\": {\"filter\":[{\"term\":{\"_projectId\":\"%1$s\"}},{\"term\":{\"id\":\"%2$s\"}},{\"term\":{\"_modified\":\"%3$s\"}}]}}, \"from\": 0, \"size\": 1}";


    public static boolean apply(ServiceRegistry services) throws Exception {
        logger.info("Running Migrate_3_4_0");
        PostgresHelper pgh = new PostgresHelper();
        ElasticHelper eh = new ElasticHelper();

        // Temporarily increase max_compilations_per_minute
        eh.updateClusterSettings(transientSettings);

        boolean noErrors = true;

        List<Map<String, String>> orgs = pgh.getOrganizations(null);

        for (Map<String, String> org : orgs) {
            String orgId = org.get("orgId");
            List<Map<String, Object>> projects = pgh.getProjects(orgId);
            for (Map<String, Object> project : projects) {
                String projectId = project.get(Sjm.SYSMLID).toString();
                pgh.setProject(projectId);
                List<Map<String, String>> commits = pgh.getAllCommits();
                for (Map<String, String> commit : commits) {
                    String commitId = commit.get("commitId");
                    if (!commitId.isEmpty()) {
                        JsonObject commitObject = eh.getCommitByElasticId(commitId, projectId);
                        if (commitObject.has(Sjm.CREATED)) {
                            String query = "UPDATE \"commits\" SET timestamp = ? WHERE elasticid = ?";
                            try (PreparedStatement statement = pgh.prepareStatement(query)) {
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
