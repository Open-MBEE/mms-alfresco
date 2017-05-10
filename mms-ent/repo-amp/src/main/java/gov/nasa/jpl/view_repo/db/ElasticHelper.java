package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.JsonArray;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Get;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;

/**
 * @author Jason Han jason.han@jpl.nasa.gov, Laura Mann laura.mann@jpl.nasa.gov
 * @version 3.0
 * @since 3.0
 */
public class ElasticHelper {
    private static JestClient client = null;
    private static Logger logger = Logger.getLogger(ElasticHelper.class);
    private static String elementIndex = EmsConfig.get("elastic.index.element");
    private static int resultLimit = Integer.parseInt(EmsConfig.get("elastic.limit.result"));
    private static int termLimit = Integer.parseInt(EmsConfig.get("elastic.limit.term"));

    public void init(String elasticHost) throws UnknownHostException {

        JestClientFactory factory = new JestClientFactory();
        if (elasticHost.contains("https")) {
            factory.setHttpClientConfig(
                new HttpClientConfig.Builder(elasticHost).defaultSchemeForDiscoveredNodes("https").multiThreaded(true)
                    .readTimeout(600000).build());
        } else {
            factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticHost).multiThreaded(true).build());
        }
        client = factory.getObject();
        logger.warn(String
            .format("Initialization complete for ElasticSearch client. Cluster name: mms. %s", client.toString()));
        logger.warn(String.format("ElasticSearch connected to: %s", elasticHost));
    }

    public void close() {

        client.shutdownClient();
        logger.warn("ES JEST client has been closed");
    }

    public ElasticHelper() throws IOException {

        if (client == null) {
            logger.debug("Initializing Elastic client");
            init(EmsConfig.get("elastic.host"));
        }

        createIndex(elementIndex);
    }

    /**
     * Creates elasticsearch index if it doesn't exist        (1)
     *
     * @param index name of the index to create           (2)
     */
    public void createIndex(String index) throws IOException {

        boolean indexExists = client.execute(new IndicesExists.Builder(index).build()).isSucceeded();
        if (!indexExists) {
            client.execute(new CreateIndex.Builder(index).build());
        }
    }

    /**
     * Gets the JSON document of element type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JSONObject getElementByElasticId(String id) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(elementIndex, id).type("element").build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JSONObject o = new JSONObject(result.getJsonObject().get("_source").toString());
            o.put(Sjm.ELASTICID, result.getJsonObject().get("_id").getAsString());
            return o;
        }

        return null;
    }

    /**
     * Gets the JSON document of commit type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JSONObject getCommitByElasticId(String id) throws IOException {
        Get get = new Get.Builder(elementIndex, id).type("commit").build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JSONObject o = new JSONObject(result.getJsonObject().get("_source").toString());
            o.put(Sjm.ELASTICID, result.getJsonObject().get("_id").getAsString());
            return o;
        }

        return null;
    }

    /**
     * Returns the commit history of a element                           (1)
     * <p> Returns a JSONArray of objects that look this:
     * {
     * "id": "commitId",
     * "_timestamp": "timestamp",
     * "_creator": "creator"
     * }                                                                (2)
     * <p>
     *
     * @param sysmlid sysmlId     (3)
     * @return JSONArray array or empty json array
     */
    public JSONArray getCommitHistory(String sysmlid) throws IOException {

        JSONArray should = new JSONArray();
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "added").put("query",
            new JSONObject().put("term", new JSONObject().put("added.id", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "updated").put("query",
            new JSONObject().put("term", new JSONObject().put("updated.id", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "deleted").put("query",
            new JSONObject().put("term", new JSONObject().put("deleted.id", new JSONObject().put("value", sysmlid))))));
        JSONObject query = new JSONObject().put("size", resultLimit)
            .put("query", new JSONObject().put("bool", new JSONObject().put("should", should)))
            .put("sort", new JSONArray().put(new JSONObject().put(Sjm.CREATED, new JSONObject().put("order", "desc"))));

        Search search = new Search.Builder(query.toString()).addIndex(elementIndex).addType("commit").build();
        SearchResult result = client.execute(search);

        JSONArray array = new JSONArray();

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject();
                JSONObject record = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put("id", hits.get(i).getAsJsonObject().get("_id").getAsString());
                o.put("_created", record.get("_created"));
                o.put("_creator", record.get("_creator"));
                array.put(o);
            }
            return array;
        }
        return new JSONArray();
    }

    public Boolean checkForElasticIdInCommit(String sysmlid, String commitId) throws IOException {
        JSONArray should = new JSONArray();
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "added").put("query",
            new JSONObject().put("term", new JSONObject().put("added.id", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "updated").put("query",
            new JSONObject().put("term", new JSONObject().put("updated.id", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "deleted").put("query",
            new JSONObject().put("term", new JSONObject().put("deleted.id", new JSONObject().put("value", sysmlid))))));
        JSONArray must = new JSONArray();
        must.put(new JSONObject().put("term", new JSONObject().put("_id", commitId)));
        JSONObject boolQueryMust = new JSONObject();
        JSONObject boolQueryShould = new JSONObject();
        boolQueryShould.put("bool", new JSONObject().put("should", should));
        boolQueryMust.put("must", must);
        must.put(boolQueryShould);
        JSONObject query =
            new JSONObject().put("size", resultLimit).put("query", new JSONObject().put("bool", boolQueryMust));
        Search search = new Search.Builder(query.toString()).addIndex(elementIndex).addType("commit").build();
        SearchResult result = client.execute(search);

        return result.getTotal() > 0;


    }

    public JSONObject getElementByCommitId(String elasticId, String sysmlid) throws IOException {
        JSONArray filter = new JSONArray();
        filter.put(new JSONObject().put("term", new JSONObject().put("_commitId", elasticId)));
        filter.put(new JSONObject().put("term", new JSONObject().put("id", sysmlid)));

        JSONObject boolQuery = new JSONObject();
        boolQuery.put("filter", filter);

        JSONObject queryJson = new JSONObject().put("query", new JSONObject().put("bool", boolQuery));
        // should passes a json array that is the terms array from above

        logger.debug(String.format("Search Query %s", queryJson.toString()));

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).addType("element").build();
        SearchResult result = client.execute(search);

        if (result.isSucceeded()) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            JSONObject o = new JSONObject(hits.get(0).getAsJsonObject().getAsJsonObject("_source").toString());
            o.put(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id").getAsString());
            return o;
        }
        return null;

    }

    /**
     * A paginated search for a list of elasticsearch _id's, returns empty JSONArray if passed empty list  (1)
     *
     * @param ids list of elasticsearch _id(s) to find          (2)
     * @return JSONArray elements or empty array
     */
    public JSONArray getElementsFromElasticIds(List<String> ids) throws IOException {
        // :TODO can be cleaned up with the getAPI
        int count = 0;
        JSONArray elements = new JSONArray();

        if (ids.size() == 0) {
            return elements;
        }

        while (count < ids.size()) {
            // sublist is fromIndex inclusive, toIndex exclusive
            List<String> sub = ids.subList(count, Math.min(ids.size(), count + termLimit));
            if (count == Math.min(ids.size(), count + termLimit)) {
                sub = new ArrayList<>();
                sub.add(ids.get(count));
            }
            JSONArray elasticids = new JSONArray();
            sub.forEach(elasticids::put);

            JSONObject queryJson = new JSONObject().put("size", resultLimit)
                .put("query", new JSONObject().put("terms", new JSONObject().put("_id", elasticids))).put("sort",
                    new JSONArray().put(new JSONObject().put(Sjm.MODIFIED, new JSONObject().put("order", "desc"))));

            logger.debug(String.format("Search Query %s", queryJson.toString()));

            Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
            SearchResult result = client.execute(search);

            if (result != null && result.getTotal() > 0) {
                JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                for (int i = 0; i < hits.size(); i++) {
                    JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                    o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                    elements.put(o);
                }
            }
            count += termLimit;
        }

        return elements;

    }

    /**
     * Index single JSON document by type                         (1)
     *
     * @param j JSON document to index          (2)
     * @return ElasticResult result
     */
    public ElasticResult indexElement(JSONObject j) throws IOException {
        // :TODO error handling
        ElasticResult result = new ElasticResult();
        String eType = j.has("commit") ? "commit" : "element";


        logger.debug(String.format("indexElement: %s", j));

        JSONObject k = new JSONObject();
        if (j.has(eType)) {
            k = removeWrapper(j);
        } else {
            k = j;
        }

        if (k.has(Sjm.SYSMLID)) {
            result.sysmlid = k.getString(Sjm.SYSMLID);
        }
        if (k.has(Sjm.ELASTICID)) {
            result.elasticId = client.execute(
                new Index.Builder(k.toString()).id(k.getString(Sjm.ELASTICID)).index(elementIndex).type(eType).build())
                .getId();
        } else {
            result.elasticId =
                client.execute(new Index.Builder(k.toString()).index(elementIndex).type(eType).build()).getId();
        }
        k.put(Sjm.ELASTICID, result.elasticId);
        result.current = k;

        return result;
    }

    /**
     * Index multiple JSON documents by type                         (1)
     *
     * @param e array of JSON documents to index          (2)
     * @return map of sysmlids and ElasticResult object
     */
    public Map<String, ElasticResult> indexElements(JSONArray e) throws JSONException, IOException {
        // :TODO error handling
        logger.debug(String.format("Starting to index %d elements.", e.length()));
        Map<String, ElasticResult> sysmlId2ElasticId = new HashMap<>();
        for (int i = 0; i < e.length(); i++) {
            ElasticResult indexResult = indexElement(e.getJSONObject(i));
            logger.debug(String.format("Result: %s", indexResult));

            sysmlId2ElasticId.put(indexResult.current.getString(Sjm.SYSMLID), indexResult);
        }

        return sysmlId2ElasticId;
    }

    /**
     * Index multiple JSON documents by type using the BulkAPI                        (1)
     *
     * @param bulkElements documents to index          (2)
     * @param operation    checks for CRUD operation, does not delete documents
     * @return ElasticResult e
     */
    public boolean bulkIndexElements(JSONArray bulkElements, String operation) throws JSONException, IOException {
        int limit = Integer.parseInt(EmsConfig.get("elastic.limit.insert"));
        // BulkableAction is generic
        ArrayList<BulkableAction> actions = new ArrayList<>();
        JSONArray currentList = new JSONArray();
        for (int i = 0; i < bulkElements.length(); i++) {
            JSONObject curr = bulkElements.getJSONObject(i);
            if (operation.equals("delete")) {
                continue;
            } else {
                actions.add(new Index.Builder(curr.toString()).id(curr.getString(Sjm.ELASTICID)).build());
                currentList.put(curr);
            }
            if ((((i + 1) % limit) == 0 && i != 0) || i == (bulkElements.length() - 1)) {
                BulkResult result = insertBulk(actions);
                if (!result.isSucceeded()) {
                    logger.error(String.format("Elastic Bulk Insert Error: %s", result.getErrorMessage()));
                    logger.error(String.format("Failed items JSON: %s", currentList));
                    result.getFailedItems().forEach((item) -> {
                        logger.error(String.format("Failed item: %s", item.error));
                    });
                    return false;
                }
                actions.clear();
            }
        }
        return true;
    }

    /**
     * Helper method for making bulkAPI requests                       (1)
     *
     * @param actions (2)
     * @return returns result of bulk index
     */
    private BulkResult insertBulk(List<BulkableAction> actions) throws JSONException, IOException {
        Bulk bulk = new Bulk.Builder().defaultIndex(elementIndex).defaultType("element").addAction(actions).build();
        return client.execute(bulk);
    }

    public Map<String, String> search(JSONObject queryJson) throws IOException {
        logger.debug(String.format("Search Query %s", queryJson.toString()));

        //JSONArray elements = new JSONArray();
        Map<String, String> elements = new HashMap<>();

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {

                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());

                elements.put(o.getString(Sjm.SYSMLID), o.toString());
            }
        }

        return elements;
    }

    private static JSONArray removeWrapper(JSONArray jsonArray) {
        JSONArray result = new JSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject json = jsonArray.getJSONObject(i);
            result.put(removeWrapper(json));
        }
        return result;
    }

    private static JSONObject removeWrapper(JSONObject jsonObject) {
        String eType = null;
        if (jsonObject.has("element") || jsonObject.has("commit")) {
            eType = jsonObject.has("commit") ? "commit" : "element";
        }
        if (eType != null) {
            jsonObject = jsonObject.getJSONObject(eType);
        }
        return jsonObject;
    }
}
