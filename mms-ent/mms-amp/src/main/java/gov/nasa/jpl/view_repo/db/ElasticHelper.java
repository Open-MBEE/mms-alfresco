package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
//import org.json.JSONArray;
import org.json.JSONException;
//import org.json.JSONObject;

import com.google.gson.JsonArray;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.JSONObject;
import gov.nasa.jpl.view_repo.util.JSONArray;
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
public class ElasticHelper implements ElasticsearchInterface {
    private static JestClient client = null;
    private static Logger logger = Logger.getLogger(ElasticHelper.class);
    private static String elementIndex = EmsConfig.get("elastic.index.element");
    private static int resultLimit = Integer.parseInt(EmsConfig.get("elastic.limit.result"));
    private static int termLimit = Integer.parseInt(EmsConfig.get("elastic.limit.term"));
    private static int readTimeout = 1000000000;

    private static final String ELEMENT = "element";
    private static final String COMMIT = "commit";

    public void init(String elasticHost) {

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
    }

    /**
     * Creates elasticsearch index if it doesn't exist        (1)
     *
     * @param index name of the index to create           (2)
     */
    public void createIndex(String index) throws IOException {
        boolean indexExists = client.execute(new IndicesExists.Builder(index.toLowerCase().replaceAll("\\s+", "")).build()).isSucceeded();
        if (!indexExists) {
            client.execute(new CreateIndex.Builder(index.toLowerCase().replaceAll("\\s+","")).build());
        }
    }

    /**
     * Gets the JSON document of element type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JSONObject getElementByElasticId(String id, String index) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+",""), id).type(ELEMENT).build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
        	String res = result.getJsonObject().get("_source").toString();
            JSONObject o = new JSONObject(res);
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
        should.put(new JSONObject().put("term", new JSONObject().put("added.id", sysmlid)));
        should.put(new JSONObject().put("term", new JSONObject().put("updated.id", sysmlid)));
        should.put(new JSONObject().put("term", new JSONObject().put("deleted.id", sysmlid)));
        JSONObject query = new JSONObject().put("size", resultLimit)
            .put("query", new JSONObject().put("bool", new JSONObject().put("should", should)))
            .put("sort", new JSONArray().put(new JSONObject().put(Sjm.CREATED, new JSONObject().put("order", "desc"))));

        Search search = new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType(COMMIT).build();
        SearchResult result = client.execute(search);

        JSONArray array = new JSONArray();

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject();
                JSONObject record = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.SYSMLID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                o.put(Sjm.CREATED, record.get(Sjm.CREATED));
                o.put(Sjm.CREATOR, record.get(Sjm.CREATOR));
                array.put(o);
            }
            return array;
        }
        return new JSONArray();
    }


    public Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException {
        JSONArray should = new JSONArray();
        should.put(new JSONObject().put("term", new JSONObject().put("added.id", sysmlid)));
        should.put(new JSONObject().put("term", new JSONObject().put("updated.id", sysmlid)));
        should.put(new JSONObject().put("term", new JSONObject().put("deleted.id", sysmlid)));
        JSONArray must = new JSONArray();
        must.put(new JSONObject().put("term", new JSONObject().put("_id", commitId)));
        JSONObject boolQueryMust = new JSONObject();
        JSONObject boolQueryShould = new JSONObject();
        boolQueryShould.put("bool", new JSONObject().put("should", should));
        boolQueryMust.put("must", must);
        must.put(boolQueryShould);
        JSONObject query =
            new JSONObject().put("size", resultLimit).put("query", new JSONObject().put("bool", boolQueryMust));
        Search search = new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType(COMMIT).build();
        SearchResult result = client.execute(search);

        return result.getTotal() > 0;


    }

    /**
     * Gets the JSON document of commit type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JSONObject getCommitByElasticId(String id, String index) throws IOException {
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+",""), id).type(COMMIT).build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JSONObject o = new JSONObject(result.getJsonObject().get("_source").toString());
            o.put(Sjm.SYSMLID, result.getJsonObject().get("_id").getAsString());
            return o;
        }

        return null;
    }

    public JSONObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException {
        JSONArray filter = new JSONArray();
        filter.put(new JSONObject().put("term", new JSONObject().put(Sjm.COMMITID, elasticId)));
        filter.put(new JSONObject().put("term", new JSONObject().put(Sjm.SYSMLID, sysmlid)));

        JSONObject boolQuery = new JSONObject();
        boolQuery.put("filter", filter);

        JSONObject queryJson = new JSONObject().put("query", new JSONObject().put("bool", boolQuery));
        // should passes a json array that is the terms array from above

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", queryJson.toString()));
        }

        Search search = new Search.Builder(queryJson.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).addType(ELEMENT).build();
        SearchResult result = client.execute(search);

        if (result.isSucceeded()) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            if(hits.size() > 0){
                JSONObject o = new JSONObject(hits.get(0).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id").getAsString());
                return o;
            }
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

        if (ids.isEmpty()) {
            return elements;
        }

        while (count < ids.size()) {
            // sublist is fromIndex inclusive, toIndex exclusive
            List<String> sub = ids.subList(count, Math.min(ids.size(), count + termLimit));

            JSONObject queryJson = new JSONObject().put("size", resultLimit)
                .put("query", new JSONObject().put("terms", new JSONObject().put("_id", sub))).put("sort",
                    new JSONArray().put(new JSONObject().put(Sjm.MODIFIED, new JSONObject().put("order", "desc"))));

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Search Query %s", queryJson.toString()));
            }

            Search search = new Search.Builder(queryJson.toString()).addIndex(index.toLowerCase().replaceAll("\\s+","")).build();
            SearchResult result = client.execute(search);

            if (result != null && result.isSucceeded() && result.getTotal() > 0) {
                JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                for (int i = 0; i < hits.size(); i++) {
                    JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                    o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                    elements.put(o);
                }
            } else if (result != null && !result.isSucceeded()) {
            	throw new IOException(String.format("Search failed:%s", result.getErrorMessage()));
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
    public ElasticResult indexElement(JSONObject j, String index) throws IOException {
        // :TODO error handling
        ElasticResult result = new ElasticResult();
        String eType = j.has(COMMIT) ? COMMIT : ELEMENT;

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("indexElement: %s", j));
        }

        JSONObject k;
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
    //:TODO refactor with project indexes
    public boolean refreshIndex() throws IOException {
        Refresh refresh = new Refresh.Builder().addIndex(elementIndex).build();
        JestResult result = client.execute(refresh);

        return result.isSucceeded();
    }

    public boolean updateElement(String id, JSONObject payload, String index) throws JSONException, IOException {

        client.execute(new Update.Builder(payload.toString()).id(id).index(index.toLowerCase().replaceAll("\\s+","")).type(ELEMENT).build());

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
                    for (BulkResult.BulkResultItem item : result.getFailedItems()) {
                        logger.error(String.format("Failed item: %s", item.error));
                    }
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
                    for (BulkResult.BulkResultItem item : result.getFailedItems()) {
                        logger.error(String.format("Failed item: %s", item.error));
                    }
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
        Bulk bulk = new Bulk.Builder().defaultIndex(index).defaultType(ELEMENT).addAction(actions).setParameter(Parameters.REFRESH, refresh).build();
        return client.execute(bulk);
    }
    // :TODO has to be set to accept multiple indexes as well.  Will need VE changes
    public JSONArray search(JSONObject queryJson) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", queryJson.toString()));
        }

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

    public JSONObject searchLiteral(JSONObject queryJson) throws IOException {
        Search search = new Search.Builder(queryJson.toString()).build();
        SearchResult result = client.execute(search);
        return new JSONObject(result.getJsonObject().toString());
    }

    private static JSONObject removeWrapper(JSONObject jsonObject) {
        String eType = null;
        JSONObject result = new JSONObject();
        if (jsonObject.has(ELEMENT) || jsonObject.has(COMMIT)) {
            eType = jsonObject.has(COMMIT) ? COMMIT : ELEMENT;
        }
        if (eType != null) {
            result = jsonObject.getJSONObject(eType);
        }
        return result;
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
            Bulk bulk = new Bulk.Builder().defaultIndex(index.toLowerCase().replaceAll("\\s+","")).defaultIndex(type).addAction(deleteList).build();

            result = client.execute(bulk);

            if (!result.isSucceeded()) {
                logger.error("Delete Failed!");
                logger.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return new JSONObject(result != null ? result.getJsonString() : "");
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
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));

        }
        return new JSONObject(result.getJsonString());
    }

    /**
     * Search elasticsearch for an element based on the sysmlids provided and timestamp. Elasticsearch will find all elements matching
     * the sysmlid then filter and sort by timestamp. If the element doesn't exist at the timestamp it will return null.
     * @param sysmlId
     * @param timestamp
     * @return
     */
    public JSONObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds, String index) {
        // Create filter array
        int count = 0;
        while (count < refsCommitIds.size()) {
            List<String> sub = refsCommitIds.subList(count, Math.min(refsCommitIds.size(), count + termLimit));

            JSONArray filter = new JSONArray();
            filter.put(new JSONObject().put("range", new JSONObject().put("_modified", new JSONObject().put("lte", timestamp))));
            filter.put(new JSONObject().put("terms", new JSONObject().put(Sjm.COMMITID, sub)));
            filter.put(new JSONObject().put("term", new JSONObject().put(Sjm.SYSMLID, sysmlId)));

            // Create sort
            JSONArray sort = new JSONArray();
            JSONObject modifiedSortOpt = new JSONObject();
            modifiedSortOpt.put("order", "desc");
            sort.put(new JSONObject().put("_modified", modifiedSortOpt));

            // Add filter to bool, then bool to query
            JSONObject bool = new JSONObject().put("bool", new JSONObject().put("filter", filter));
            JSONObject query = new JSONObject().put("query", bool);
            query.put("sort", sort);
            // Add size limit
            query.put("size", "1");

            Search search = new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+", "")).build();
            SearchResult result;
            try {
                result = client.execute(search);

                if (result.getTotal() > 0) {
                    JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                    if (hits.size() > 0) {
                        return new JSONObject(hits.get(0).getAsJsonObject().getAsJsonObject("_source").toString());
                    }
                }
            } catch (IOException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
            count += termLimit;
        }
        return new JSONObject();
    }

    public Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index) {
        int count = 0;
        Map<String, String> deletedElements = new HashMap<>();
        while (count < commitIds.size()) {
            List<String> sub = commitIds.subList(count, Math.min(commitIds.size(), count + termLimit));

            JSONObject queryWrapper = new JSONObject();

            JSONObject query = new JSONObject();
            query.put("must", new JSONObject().put("exists", new JSONObject().put("field", "deleted.id")));
            query.put("filter", new JSONObject().put("terms", new JSONObject().put(Sjm.ELASTICID, sub)));

            queryWrapper.put("query", new JSONObject().put("bool", query));

            Search search = new Search.Builder(queryWrapper.toString()).addIndex(index.toLowerCase().replaceAll("\\s+", "")).build();

            try {
                SearchResult result = client.execute(search);

                if (result.getTotal() > 0) {
                    JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");

                    int hitSize = hits.size();

                    for (int i = 0; i < hitSize; ++i) {

                        JSONObject hitResult = new JSONObject(hits.get(i).getAsJsonObject().toString());
                        JSONArray deletedArray = hitResult.getJSONObject("_source").getJSONArray("deleted");

                        int numDeleted = deletedArray.length();

                        for (int y = 0; y < numDeleted; ++y) {
                            JSONObject deletedObject = deletedArray.getJSONObject(y);
                            deletedElements.put(deletedObject.getString(Sjm.ELASTICID),
                                hitResult.getJSONObject("_source").getString(Sjm.CREATED));
                        }
                    }
                }
            } catch (IOException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
            count += termLimit;
        }
        return deletedElements;
    }
}
