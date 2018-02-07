package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public interface ElasticsearchInterface {
    void init(String elasticHost);

    void close();

    void createIndex(String index) throws IOException;

    JsonObject getElementByElasticId(String id, String index) throws IOException;

    JsonArray getCommitHistory(String sysmlid, String index) throws IOException;

    Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException;

    JsonObject getCommitByElasticId(String id, String index) throws IOException;

    JsonObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException;

    JsonArray getElementsFromElasticIds(List<String> ids, String index) throws IOException;

    ElasticResult indexElement(JsonObject j, String index) throws IOException;

    boolean refreshIndex() throws IOException;

    boolean updateElement(String id, JsonObject payload, String index) throws IOException;

    boolean bulkIndexElements(JsonArray bulkElements, String operation, boolean refresh, String index, String type) throws IOException;

    boolean bulkUpdateElements(Set<String> elements, String payload, String index, String type) throws IOException;

    JsonArray search(JsonObject queryJson) throws IOException;

    JsonObject bulkDeleteByType(String type, ArrayList<String> ids, String index);

    JsonObject deleteElasticElements(String field, String projectId);

    JsonObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds, String index);

    Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index);
}
