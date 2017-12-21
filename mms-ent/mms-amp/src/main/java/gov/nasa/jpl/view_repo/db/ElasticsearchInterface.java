package gov.nasa.jpl.view_repo.db;

//import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.nasa.jpl.view_repo.util.JSONObject;
import gov.nasa.jpl.view_repo.util.JSONArray;

public interface ElasticsearchInterface {
    void init(String elasticHost);

    void close();

    void createIndex(String index) throws IOException;

    JSONObject getElementByElasticId(String id, String index) throws IOException;

    JSONArray getCommitHistory(String sysmlid, String index) throws IOException;

    Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException;

    JSONObject getCommitByElasticId(String id, String index) throws IOException;

    JSONObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException;

    JSONArray getElementsFromElasticIds(List<String> ids, String index) throws IOException;

    ElasticResult indexElement(JSONObject j, String index) throws IOException;

    boolean refreshIndex() throws IOException;

    boolean updateElement(String id, JSONObject payload, String index) throws JSONException, IOException;

    boolean bulkIndexElements(JSONArray bulkElements, String operation, boolean refresh, String index) throws JSONException, IOException;

    boolean bulkUpdateElements(Set<String> elements, String payload, String index) throws JSONException, IOException;

    JSONArray search(JSONObject queryJson) throws IOException;

    JSONObject bulkDeleteByType(String type, ArrayList<String> ids, String index);

    JSONObject deleteElasticElements(String field, String projectId);

    JSONObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds, String index);

    Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index);
}
