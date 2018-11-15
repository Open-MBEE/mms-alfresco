package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public interface IDocStore {
    public static final String ELEMENT = "element";
    public static final String COMMIT = "commit";
    public static final String PROFILE = "profile";
    public static final String ARTIFACT = "artifact";
    public static final String REF = "ref";

    JsonObject getByInternalId(String id, String index, String type) throws IOException;

    JsonObject getByCommitId(String id, String sysmlid, String index, String type) throws IOException;

    JsonObject getCommitBoolShouldQuery(String sysmlid);

    JsonArray getCommitHistory(String sysmlid, String index) throws IOException;

    JsonArray getElementsFromElasticIds(List<String> ids, String index) throws IOException;

    JsonObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds, String index);

    Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index);

    JsonObject search(JsonObject queryJson) throws IOException;

    JsonObject searchLiteral(JsonObject queryJson) throws IOException;

    DocumentResult indexElement(JsonObject j, String index, String eType) throws IOException;

    void createIndex(String index) throws IOException;

    boolean refreshIndex() throws IOException;

    boolean bulkIndexElements(JsonArray bulkElements, String operation, boolean refresh, String index, String type) throws IOException;

    void applyTemplate(String template) throws IOException;

    void updateMapping(String index, String type, String mapping) throws IOException;

    void updateByQuery(String index, String payload, String type) throws IOException;

    void updateClusterSettings(String payload) throws IOException;

    JsonObject updateById(String id, JsonObject payload, String index, String type) throws IOException;

    boolean bulkUpdateElements(Set<String> elements, String payload, String index, String type) throws IOException;

    void deleteIndex(String index) throws IOException;

    void deleteByQuery(String index, String payload, String type) throws IOException;

    JsonObject bulkDeleteByType(Set<String> ids, String index, String type);

    void close();

}
