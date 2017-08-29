package gov.nasa.jpl.view_repo.db;

import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
import io.searchbox.core.Update;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.params.Parameters;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Delete;


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
    private static int readTimeout = 1000000000;

    public void init(String elasticHost) throws UnknownHostException {

        JestClientFactory factory = new JestClientFactory();
        if (elasticHost.contains("https")) {
            factory.setHttpClientConfig(
                new HttpClientConfig.Builder(elasticHost).defaultSchemeForDiscoveredNodes("https").multiThreaded(true)
                    .readTimeout(readTimeout).build());
        } else {
            factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticHost).readTimeout(readTimeout).multiThreaded(true).build());
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

        //createIndex(elementIndex);
    }

    /**
     * Creates elasticsearch index if it doesn't exist        (1)
     *
     * @param index name of the index to create           (2)
     */
    public void createIndex(String index) throws IOException {
        boolean indexExists = client.execute(new IndicesExists.Builder(index).build()).isSucceeded();
        if (!indexExists) {
            client.execute(new CreateIndex.Builder(index.toLowerCase().replaceAll("\\s+","")).build());
        }
        //initial organization project
    }

    /**
     * Gets the JSON document of element type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JSONObject getElementByElasticId(String id, String index) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+",""), id).type("element").build();

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
    public JSONArray getCommitHistory(String sysmlid, String index) throws IOException {

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

        Search search = new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType("commit").build();
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

    public Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException {
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
        Search search = new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType("commit").build();
        SearchResult result = client.execute(search);

        return result.getTotal() > 0;


    }

    public JSONObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException {
        JSONArray filter = new JSONArray();
        filter.put(new JSONObject().put("term", new JSONObject().put("_commitId", elasticId)));
        filter.put(new JSONObject().put("term", new JSONObject().put("id", sysmlid)));

        JSONObject boolQuery = new JSONObject();
        boolQuery.put("filter", filter);

        JSONObject queryJson = new JSONObject().put("query", new JSONObject().put("bool", boolQuery));
        // should passes a json array that is the terms array from above

        logger.debug(String.format("Search Query %s", queryJson.toString()));

        Search search = new Search.Builder(queryJson.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType("element").build();
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
    public JSONArray getElementsFromElasticIds(List<String> ids, String index) throws IOException {
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

            Search search = new Search.Builder(queryJson.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).build();
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
        return indexElement(j, elementIndex);
    }

    /**
     * Index single JSON document by type                         (1)
     *
     * @param j JSON document to index          (2)
     * @return ElasticResult result
     */
    public ElasticResult indexElement(JSONObject j, String index) throws IOException {
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
                new Index.Builder(k.toString()).id(k.getString(Sjm.ELASTICID)).index(index.toLowerCase().replaceAll("\\s+","")).type(eType).build())
                .getId();
        } else {
            result.elasticId =
                client.execute(new Index.Builder(k.toString()).index(index.toLowerCase().replaceAll("\\s+","")).type(eType).build()).getId();
        }
        k.put(Sjm.ELASTICID, result.elasticId);
        result.current = k;

        return result;
    }
    /**
     * refresh the index                         (1)
     *
     * @return Boolean isRefreshed
     */
    public Boolean refreshIndex() throws IOException {
        Refresh refresh = new Refresh.Builder().addIndex(elementIndex).build();
        JestResult result = client.execute(refresh);
        if(result.isSucceeded()){
            return true;
        }else{
            return false;
        }

    }

    public boolean updateElement(String id, JSONObject payload, String index) throws JSONException, IOException {

        client.execute(new Update.Builder(payload.toString()).id(id).index(index.toLowerCase().replaceAll("\\s+","")).type("element").build());

        return true;
    }

    /**
     * Index multiple JSON documents by type using the BulkAPI                        (1)
     *
     * @param bulkElements documents to index          (2)
     * @param operation    checks for CRUD operation, does not delete documents
     * @return ElasticResult e
     */
    public boolean bulkIndexElements(JSONArray bulkElements, String operation, boolean refresh, String index) throws JSONException, IOException {
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
                BulkResult result = insertBulk(actions, refresh, index.toLowerCase().replaceAll("\\s+",""));
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

    public boolean bulkUpdateElements(Set<String> elements, String payload, String index) throws JSONException, IOException {
        int limit = Integer.parseInt(EmsConfig.get("elastic.limit.insert"));
        ArrayList<BulkableAction> actions = new ArrayList<>();
        JSONArray currentList = new JSONArray();

        int i = 0;
        for (String id: elements) {
            actions.add(new Update.Builder(payload).id(id).build());
            currentList.put(id);

            if ((((i + 1) % limit) == 0 && i != 0) || i == (elements.size() - 1)) {
                BulkResult result = insertBulk(actions, false, index.toLowerCase().replaceAll("\\s+",""));
                if (!result.isSucceeded()) {
                    logger.error(String.format("Elastic Bulk Update Error: %s", result.getErrorMessage()));
                    logger.error(String.format("Failed items JSON: %s", currentList));
                    result.getFailedItems().forEach((item) -> {
                        logger.error(String.format("Failed item: %s", item.error));
                    });
                    //return false;
                }
                actions.clear();
            }
            i++;
        }
        return true;
    }

    /**
     * Helper method for making bulkAPI requests                       (1)
     *
     * @param actions (2)
     * @return returns result of bulk index
     */
    private BulkResult insertBulk(List<BulkableAction> actions, boolean refresh, String index) throws JSONException, IOException {
        Bulk bulk = new Bulk.Builder().defaultIndex(index).defaultType("element").addAction(actions).setParameter(Parameters.REFRESH, refresh).build();
        return client.execute(bulk);
    }
    // :TODO has to be set to accept multiple indexes as well.  Will need VE changes
    public JSONArray search(JSONObject queryJson) throws IOException {
        logger.debug(String.format("Search Query %s", queryJson.toString()));

        //JSONArray elements = new JSONArray();
        JSONArray elements = new JSONArray();

        Search search = new Search.Builder(queryJson.toString()).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {

                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());

                elements.put(o);
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

    /**
     * Takes a type and array list of string ids. Creates a Bulk object and adds a list of actions. Then performs a bulk
     * delete. Returns the JSONObject of the result.
     *
     * @param type
     * @param ids
     * @return JSONObject Result
     */
    public JSONObject bulkDeleteByType(String type, ArrayList<String> ids, String index){
        JestResult result = null;
        try {
            ArrayList<Delete> deleteList = new ArrayList<>();

            for(String commitId : ids){
                deleteList.add(new Delete.Builder(commitId).type(type).index(index.toLowerCase().replaceAll("\\s+","")).build());
            }
            // :TODO why are we using default index?
            Bulk bulk = new Bulk.Builder().defaultIndex(index.toLowerCase().replaceAll("\\s+","")).defaultIndex(type).addAction(deleteList).build();

            result = client.execute(bulk);

            if (!result.isSucceeded()) {
                logger.error("Delete Failed!");
                logger.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return new JSONObject(result.getJsonString());
    }

    /**
     * Performs a delete by query on ElasticSearch using the given field and id.
     * @param field
     * @param projectId
     * @return JSON Response
     */
    // :TODO do we need to search for the project now?
    public JSONObject deleteElasticElements(String field, String projectId){
        JestResult result = null;
        JSONObject query = new JSONObject();
        query.put("query", new JSONObject().put("term", new JSONObject().put(field, projectId.toLowerCase().replaceAll("\\s+",""))));

        // Verbose statement to make sure it uses the correct delete by query class from searchbox.
        DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(query.toString()).addIndex(projectId.toLowerCase().replaceAll("\\s+","")).build();

        try {
            result = client.execute(deleteByQuery);
            if (!result.isSucceeded()) {
                logger.error("Deleting Elastic Elements Failed!");
                logger.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());

        }
        return new JSONObject(result.getJsonString());
    }
}
