package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.view_repo.util.SerialJSONArray;
import org.json.JSONException;
import gov.nasa.jpl.view_repo.util.SerialJSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ElasticsearchInterface {
    void init(String elasticHost);

    void close();

    void createIndex(String index) throws IOException;

    SerialJSONObject getElementByElasticId(String id, String index) throws IOException;

    SerialJSONArray getCommitHistory(String sysmlid, String index) throws IOException;

    Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException;

    SerialJSONObject getCommitByElasticId(String id, String index) throws IOException;

    SerialJSONObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException;

    SerialJSONArray getElementsFromElasticIds(List<String> ids, String index) throws IOException;

    ElasticResult indexElement(SerialJSONObject j, String index) throws IOException;

    boolean refreshIndex() throws IOException;

    boolean updateElement(String id, SerialJSONObject payload, String index) throws JSONException, IOException;

    boolean bulkIndexElements(SerialJSONArray bulkElements, String operation, boolean refresh, String index) throws JSONException, IOException;

    boolean bulkUpdateElements(Set<String> elements, String payload, String index) throws JSONException, IOException;

    SerialJSONArray search(SerialJSONObject queryJson) throws IOException;

    SerialJSONObject bulkDeleteByType(String type, ArrayList<String> ids, String index);

    SerialJSONObject deleteElasticElements(String field, String projectId);

    SerialJSONObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds, String index);

    Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index);
}
