package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.searchbox.core.*;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import gov.nasa.jpl.view_repo.util.EmsConfig;
import gov.nasa.jpl.view_repo.util.JsonUtil;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;
import io.searchbox.action.BulkableAction;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.Refresh;
import io.searchbox.params.Parameters;


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
    private static final String PROFILE = "profile";
    private static final String ARTIFACT = "artifact";

    public void init(String elasticHost) {

        JestClientFactory factory = new JestClientFactory(){
            @Override
            protected HttpClientBuilder configureHttpClient(HttpClientBuilder builder) {
                builder = super.configureHttpClient(builder);
                builder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
                return builder;
            }
        };
        if (elasticHost.contains("https")) {
            factory.setHttpClientConfig(
                new HttpClientConfig.Builder(elasticHost).defaultSchemeForDiscoveredNodes("https").multiThreaded(true)
                    .readTimeout(readTimeout).build());
        } else {
            factory.setHttpClientConfig(
                new HttpClientConfig.Builder(elasticHost).readTimeout(readTimeout).multiThreaded(true).build());
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
        boolean indexExists =
            client.execute(new IndicesExists.Builder(index.toLowerCase().replaceAll("\\s+", "")).build()).isSucceeded();
        if (!indexExists) {
            client.execute(new CreateIndex.Builder(index.toLowerCase().replaceAll("\\s+", "")).build());
        }
    }

    /**
     * Gets the JSON document of element type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JsonObject getElementByElasticId(String id, String index) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+", ""), id).type(ELEMENT).build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JsonObject o = result.getJsonObject().getAsJsonObject("_source");
            o.add(Sjm.ELASTICID, result.getJsonObject().get("_id"));
            return o;
        }

        return null;
    }

    public JsonObject getProfileByElasticId(String id, String index) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+", ""), id).type(PROFILE).build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JsonObject o = result.getJsonObject().get("_source").getAsJsonObject();
            o.add(Sjm.ELASTICID, result.getJsonObject().get("_id"));
            return o;
        }

        return null;
    }

    /**
     * Gets the JSON document of element type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JsonObject getElementByElasticIdArtifact(String id, String index) throws IOException {
        // Cannot use method for commit type
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+", ""), id).type(ARTIFACT).build();

        JestResult result = client.execute(get);

        if (result.isSucceeded()) {
            JsonObject o = result.getJsonObject().get("_source").getAsJsonObject();
            o.add(Sjm.ELASTICID, result.getJsonObject().get("_id"));
            return o;
        }

        return null;
    }

    /**
     * Gets the JSON document of a bool : should commit query
     * result printed as Json looks like:
     * {
     *   "bool":{"should":[{"term":{"added.id":sysmlid}},
     *                     {"term":{"updated.id":sysmlid}},
     *                     {"term":{"deleted.id":sysmlid}}]}
     * }
     * @param sysmlid the sysmlid to add to the term search
     * @return JsonObject o
     */
    public JsonObject getCommitBoolShouldQuery(String sysmlid) {
        JsonObject query = new JsonObject();
        JsonObject bool = new JsonObject();
        query.add("bool", bool);
        JsonArray should = new JsonArray();
        bool.add("should", should);
        JsonObject term1 = new JsonObject();
        term1.addProperty("added.id", sysmlid);
        JsonObject term2 = new JsonObject();
        term2.addProperty("updated.id", sysmlid);
        JsonObject term3 = new JsonObject();
        term3.addProperty("deleted.id", sysmlid);
        JsonObject term = new JsonObject();
        term.add("term", term1);
        should.add(term);
        term = new JsonObject();
        term.add("term", term2);
        should.add(term);
        term = new JsonObject();
        term.add("term", term3);
        should.add(term);
        return query;
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
    public JsonArray getCommitHistory(String sysmlid, String index) throws IOException {

        JsonObject query = new JsonObject();
        query.addProperty("size", resultLimit);
        JsonObject query1 = getCommitBoolShouldQuery(sysmlid);
        query.add("query", query1);
        JsonArray sort = new JsonArray();
        query.add("sort", sort);
        JsonObject sort1 = new JsonObject();
        sort.add(sort1);
        JsonObject sort2 = new JsonObject();
        sort1.add(Sjm.CREATED, sort2);
        sort2.addProperty("order", "desc");

        Search search = new Search.Builder(query.toString())
            .addIndex(index.toLowerCase().replaceAll("\\s+",""))
            .addType(COMMIT)
            .build();
        SearchResult result = client.execute(search);
        
        JsonArray array = new JsonArray();
        
        if (result.isSucceeded() && result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
            	JsonObject o = new JsonObject();
                JsonObject record = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
                o.add(Sjm.SYSMLID, hits.get(i).getAsJsonObject().get("_id"));
                o.add(Sjm.CREATED, record.get(Sjm.CREATED));
                o.add(Sjm.CREATOR, record.get(Sjm.CREATOR));
                array.add(o);
            }
        } else if (!result.isSucceeded()) {
            throw new IOException(String.format("Elasticsearch error[%1$s]:%2$s", 
            		result.getResponseCode(), result.getErrorMessage()));
        }
        return array;
    }


    public Boolean checkForElasticIdInCommit(String sysmlid, String commitId, String index) throws IOException {
        JsonArray must = new JsonArray();
        JsonObject term = new JsonObject();
        JsonObject term1 = new JsonObject();
        term.add("term", term1);
        term1.addProperty("_id", commitId);
        must.add(term);
        must.add(getCommitBoolShouldQuery(sysmlid));
        JsonObject boolQueryMust = new JsonObject();
        boolQueryMust.add("must", must);
        JsonObject query = new JsonObject();
        JsonObject queryClause = new JsonObject();
        queryClause.add("bool", boolQueryMust);
        query.addProperty("size", resultLimit);
        query.add("query", queryClause);
        Search search = new Search.Builder(query.toString())
        		.addIndex(index.toLowerCase()
        		.replaceAll("\\s+",""))
        		.addType(COMMIT)
        		.build();
        SearchResult result = client.execute(search);

        if (!result.isSucceeded())
            throw new IOException(
                    String.format("Elasticsearch error[%1$s]:%2$s",
                            result.getResponseCode(), result.getErrorMessage()));
        if (result.getTotal() > 0)
            return true;
        return false;
    }

    /**
     * Gets the JSON document of commit type using a elastic _id (1)
     *
     * @param id _id elasticsearch property          (2)
     * @return JSONObject o or null
     */
    public JsonObject getCommitByElasticId(String id, String index) throws IOException {
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+", ""), id).type(COMMIT).build();

        JestResult result = client.execute(get);

        if (!result.isSucceeded() && result.getResponseCode() != 404) {
            throw new IOException(
                    String.format("Elasticsearch error[%1$s]:%2$s",
                            result.getResponseCode(), result.getErrorMessage()));
        } else if (result.isSucceeded()) {
            JsonObject o = result.getJsonObject().getAsJsonObject("_source");
            o.add(Sjm.SYSMLID, result.getJsonObject().get("_id"));
            return o;
        }
        return null;
    }

    private static final String elementCommitQuery = "{\"query\":{\"bool\":{\"filter\":[{\"term\":{\"%1$s\":\"%2$s\"}},{\"term\":{\"%3$s\":\"%4$s\"}}]}}}";
    
    public JsonObject getElementByCommitId(String elasticId, String sysmlid, String index) throws IOException {
        String query = String.format(elementCommitQuery, Sjm.COMMITID, elasticId, Sjm.SYSMLID, sysmlid);
        // should passes a json array that is the terms array from above

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", query));
        }

        Search search = new Search.Builder(query).addIndex(index.toLowerCase().replaceAll("\\s+", ""))
            .addType(ELEMENT).build();
        SearchResult result = client.execute(search);

        if (!result.isSucceeded() && result.getResponseCode() != 404) {
            throw new IOException(
                    String.format("Elasticsearch error[%1$s]:%2$s",
                            result.getResponseCode(), result.getErrorMessage()));
        }
        else if (result.isSucceeded()) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            if (hits.size() > 0){
            	JsonObject o = hits.get(0).getAsJsonObject().getAsJsonObject("_source");
                o.add(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id"));
                return o;
            }
        }
        return null;
    }

    private static final String artifactCommitQuery = "{\"query\":{\"bool\":{\"filter\":[{\"term\":{\"%1$s\":\"%2$s\"}},{\"term\":{\"%3$s\":\"%4$s\"}}]}}}";

    public JsonObject getArtifactByCommitId(String elasticId, String sysmlid, String index) throws IOException {
        String query = String.format(artifactCommitQuery, Sjm.COMMITID, elasticId, Sjm.SYSMLID, sysmlid);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", query));
        }

        Search search = new Search.Builder(query).addIndex(index.toLowerCase().replaceAll("\\s+", ""))
            .addType(ARTIFACT).build();
        SearchResult result = client.execute(search);

        if (result.isSucceeded()) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            if (hits.size() > 0) {
                JsonObject o = hits.get(0).getAsJsonObject().getAsJsonObject("_source").getAsJsonObject();
                o.add(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id"));
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
    public JsonArray getElementsFromElasticIds(List<String> ids, String index) throws IOException {
        // :TODO can be cleaned up with the getAPI
        int count = 0;
        JsonArray elements = new JsonArray();

        if (ids.isEmpty()) {
            return elements;
        }

        while (count < ids.size()) {
            // sublist is fromIndex inclusive, toIndex exclusive
            List<String> sub = ids.subList(count, Math.min(ids.size(), count + termLimit));

            JsonObject queryJson = new JsonObject();
            queryJson.addProperty("size", resultLimit);
            JsonObject query = new JsonObject();
            queryJson.add("query", query);
            JsonObject terms = new JsonObject();
            query.add("terms", terms);
            JsonUtil.addStringList(terms, "_id", sub);
            JsonArray sorta = new JsonArray();
            queryJson.add("sort", sorta);
            JsonObject mod = new JsonObject();
            JsonObject order = new JsonObject();
            sorta.add(mod);
            mod.add(Sjm.MODIFIED, order);
            order.addProperty("order", "desc");

            if (logger.isDebugEnabled()) {
                logger.debug(String.format("Search Query %s", queryJson.toString()));
            }

            Search search = new Search.Builder(queryJson.toString())
            		.addIndex(index.toLowerCase().replaceAll("\\s+",""))
            		.build();
            SearchResult result = client.execute(search);

            if (result != null && result.isSucceeded() && result.getTotal() > 0) {
                JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                for (int i = 0; i < hits.size(); i++) {
                    JsonObject o = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
                    o.addProperty(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                    elements.add(o);
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
    public ElasticResult indexElement(JsonObject j, String index) throws IOException {
        // :TODO error handling
        ElasticResult result = new ElasticResult();
        String eType = j.has(COMMIT) ? COMMIT : ELEMENT;

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("indexElement: %s", j));
        }

        JsonObject k;
        if (j.has(eType)) {
            k = removeWrapper(j);
        } else {
            k = j;
        }

        if (k.has(Sjm.SYSMLID)) {
            result.sysmlid = k.get(Sjm.SYSMLID).getAsString();
        }
        if (k.has(Sjm.ELASTICID)) {
            result.elasticId = client.execute(new Index.Builder(k.toString()).id(k.get(Sjm.ELASTICID).getAsString())
                .index(index.toLowerCase().replaceAll("\\s+", "")).type(eType).build()).getId();
        } else {
            result.elasticId = client.execute(
                new Index.Builder(k.toString()).index(index.toLowerCase().replaceAll("\\s+", "")).type(eType).build())
                .getId();
        }
        if (result.elasticId == null)
        	throw new IOException("Unable to index node in elasticsearch");
        
        k.addProperty(Sjm.ELASTICID, result.elasticId);
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

    public boolean updateElement(String id, JsonObject payload, String index) throws IOException {
        JsonObject update = new JsonObject();
        update.add("doc", payload);
        update.addProperty("_source", true);
        JestResult updated = client.execute(
            new Update.Builder(update.toString()).id(id).index(index.toLowerCase().replaceAll("\\s+", "")).type(ELEMENT)
                .build());
        return updated.isSucceeded();
    }

    public JsonObject updateProfile(String id, JsonObject payload, String index) throws IOException {
        JsonObject upsert = new JsonObject();
        upsert.add("doc", payload);
        upsert.addProperty("doc_as_upsert", true);
        upsert.addProperty("_source", true);
        JestResult res = client.execute(
            new Update.Builder(upsert.toString()).id(id).index(index.toLowerCase().replaceAll("\\s+", "")).type(PROFILE)
                .build());
        if (res.isSucceeded())
            return res.getJsonObject().get("_source").getAsJsonObject();
        return new JsonObject();
    }

    /**
     * Index multiple JSON documents by type using the BulkAPI                        (1)
     *
     * @param bulkElements documents to index          (2)
     * @param operation    checks for CRUD operation, does not delete documents
     * @return ElasticResult e
     */
    public boolean bulkIndexElements(JsonArray bulkElements, String operation, boolean refresh, String index, String type)
        throws IOException {
        int limit = Integer.parseInt(EmsConfig.get("elastic.limit.insert"));
        // BulkableAction is generic
        ArrayList<BulkableAction> actions = new ArrayList<>();
        JsonArray currentList = new JsonArray();
        for (int i = 0; i < bulkElements.size(); i++) {
            JsonObject curr = bulkElements.get(i).getAsJsonObject();
            if (operation.equals("delete")) {
                continue;
            } else {
                actions.add(new Index.Builder(curr.toString())
                		.id(curr.get(Sjm.ELASTICID).getAsString())
                		.build());
                currentList.add(curr);
            }
            if ((((i + 1) % limit) == 0 && i != 0) || i == (bulkElements.size() - 1)) {
                BulkResult result = insertBulk(actions, refresh, index.toLowerCase().replaceAll("\\s+", ""), type);
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

    public boolean bulkUpdateElements(Set<String> elements, String payload, String index, String type)
        throws IOException {
        int limit = Integer.parseInt(EmsConfig.get("elastic.limit.insert"));
        ArrayList<BulkableAction> actions = new ArrayList<>();
        JsonArray currentList = new JsonArray();

        int i = 0;
        for (String id : elements) {
            actions.add(new Update.Builder(payload).id(id).build());
            currentList.add(id);

            if ((((i + 1) % limit) == 0 && i != 0) || i == (elements.size() - 1)) {
                BulkResult result = insertBulk(actions, false, index.toLowerCase().replaceAll("\\s+", ""), type);
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
    private BulkResult insertBulk(List<BulkableAction> actions, boolean refresh, String index, String type)
        throws IOException {
        Bulk bulk = new Bulk.Builder().defaultIndex(index).defaultType(type).addAction(actions)
            .setParameter(Parameters.REFRESH, refresh).build();
        return client.execute(bulk);
    }

    // :TODO has to be set to accept multiple indexes as well.  Will need VE changes
    public JsonObject search(JsonObject queryJson) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", queryJson.toString()));
        }

        JsonObject top = new JsonObject();
        JsonArray elements = new JsonArray();

        Search search = new Search.Builder(queryJson.toString()).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {

                JsonObject o = hits.get(i).getAsJsonObject().getAsJsonObject("_source");

                elements.add(o);
            }
        }
        top.add("elements", elements);
        if (result.getJsonObject().has("aggregations")) {
            JsonObject aggs = result.getJsonObject().getAsJsonObject("aggregations");
            top.add("aggregations", aggs);
        }
        return top;
    }

    public JsonObject searchLiteral(JsonObject queryJson) throws IOException {
        Search search = new Search.Builder(queryJson.toString()).build();
        SearchResult result = client.execute(search);
        return result.getJsonObject();
    }

    private static JsonObject removeWrapper(JsonObject jsonObject) {
        String eType = null;
        JsonObject result = new JsonObject();
        if (jsonObject.has(ELEMENT) || jsonObject.has(COMMIT)) {
            eType = jsonObject.has(COMMIT) ? COMMIT : ELEMENT;
        }
        if (eType != null) {
            result = jsonObject.getAsJsonObject(eType);
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
    public JsonObject bulkDeleteByType(String type, ArrayList<String> ids, String index) {
        if (ids.size() == 0)
            return new JsonObject();
        JestResult result = null;
        try {
            ArrayList<Delete> deleteList = new ArrayList<>();

            for (String commitId : ids) {
                deleteList.add(
                    new Delete.Builder(commitId).type(type).index(index.toLowerCase().replaceAll("\\s+", "")).build());
            }
            Bulk bulk = new Bulk.Builder().defaultIndex(index.toLowerCase().replaceAll("\\s+", "")).defaultIndex(type)
                .addAction(deleteList).build();

            result = client.execute(bulk);

            if (!result.isSucceeded()) {
                logger.error("Delete Failed!");
                logger.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));
        }
        JsonObject o = new JsonObject();
        if (result != null)
            o = result.getJsonObject();
        return o;
    }

    /**
     * Performs a delete by query on ElasticSearch using the given field and id.
     *
     * @param field
     * @param projectId
     * @return JSON Response
     */
    // :TODO do we need to search for the project now?
    public JsonObject deleteElasticElements(String field, String projectId){
        JestResult result = null;
        JsonObject query = new JsonObject();
        JsonObject term = new JsonObject();
        query.add("query", term);
        JsonObject termv = new JsonObject();
        termv.addProperty(field, projectId.toLowerCase().replaceAll("\\s+",""));
        term.add("term", termv);

        // Verbose statement to make sure it uses the correct delete by query class from searchbox.
        DeleteByQuery deleteByQuery = new DeleteByQuery.Builder(query.toString())
        		.addIndex(projectId.toLowerCase().replaceAll("\\s+",""))
        		.build();

        try {
            result = client.execute(deleteByQuery);
            if (!result.isSucceeded()) {
                logger.error("Deleting Elastic Elements Failed!");
                logger.error(result.getErrorMessage());
            }
        } catch (Exception e) {
            logger.error(String.format("%s", LogUtil.getStackTrace(e)));

        }
        return result.getJsonObject();
    }

    /**
     * Search elasticsearch for an element based on the sysmlids provided and timestamp. Elasticsearch will find all elements matching
     * the sysmlid then filter and sort by timestamp. If the element doesn't exist at the timestamp it will return null.
     *
     * @param sysmlId
     * @param timestamp
     * @return
     */
    public JsonObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds,
        String index) {
        // Create filter array
        int count = 0;
        while (count < refsCommitIds.size()) {
            List<String> sub = refsCommitIds.subList(count, Math.min(refsCommitIds.size(), count + termLimit));

            JsonArray filter = new JsonArray();
            JsonObject filt1 = new JsonObject();
            JsonObject filtv = new JsonObject();
            JsonObject filtv1 = new JsonObject();
            filter.add(filt1);
            filt1.add("range", filtv);
            filtv.add("_modified", filtv1);
            filtv1.addProperty("lte", timestamp);
            JsonObject filt2 = new JsonObject();
            JsonObject filt2v = new JsonObject();
            filter.add(filt2);
            filt2.add("terms", filt2v);
            JsonUtil.addStringList(filt2v, Sjm.COMMITID, sub);
            JsonObject filt3 = new JsonObject();
            JsonObject filt3v = new JsonObject();
            filter.add(filt3);
            filt3.add("term", filt3v);
            filt3v.addProperty(Sjm.SYSMLID, sysmlId);

            // Create sort
            JsonArray sort = new JsonArray();
            JsonObject modified = new JsonObject();
            JsonObject modifiedSortOpt = new JsonObject();
            sort.add(modified);
            modified.add("_modified", modifiedSortOpt);
            modifiedSortOpt.addProperty("order", "desc");

            // Add filter to bool, then bool to query
            JsonObject query = new JsonObject();
            JsonObject queryv = new JsonObject();
            JsonObject bool = new JsonObject();
            query.add("sort", sort);
            query.add("query", queryv);
            queryv.add("bool", bool);
            bool.add("filter", filter);
            // Add size limit
            query.addProperty("size", "1");

            Search search =
                new Search.Builder(query.toString()).addIndex(index.toLowerCase().replaceAll("\\s+", "")).build();
            SearchResult result;
            try {
                result = client.execute(search);

                if (result.getTotal() > 0) {
                    JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                    if (hits.size() > 0) {
                        return hits.get(0).getAsJsonObject().getAsJsonObject("_source");
                    }
                }
            } catch (IOException e) {
                logger.error(String.format("%s", LogUtil.getStackTrace(e)));
            }
            count += termLimit;
        }
        return new JsonObject();
    }

    public Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String index) {
        int count = 0;
        Map<String, String> deletedElements = new HashMap<>();
        while (count < commitIds.size()) {
            List<String> sub = commitIds.subList(count, Math.min(commitIds.size(), count + termLimit));

            JsonObject queryWrapper = new JsonObject();

            JsonObject query = new JsonObject();
            JsonObject queryv = new JsonObject();
            query.add("bool", queryv);
            JsonObject must = new JsonObject();
            JsonObject exists = new JsonObject();
            queryv.add("must", must);
            must.add("exists", exists);
            exists.addProperty("field", "deleted.id");
            JsonObject filter = new JsonObject();
            JsonObject terms = new JsonObject();
            queryv.add("filter", filter);
            filter.add("terms", terms);
            JsonUtil.addStringList(terms, Sjm.ELASTICID, sub);

            queryWrapper.add("query", query);

            Search search =
                new Search.Builder(queryWrapper.toString()).addIndex(index.toLowerCase().replaceAll("\\s+", ""))
                    .build();

            try {
                SearchResult result = client.execute(search);

                if (result.getTotal() > 0) {
                    JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");

                    int hitSize = hits.size();

                    for (int i = 0; i < hitSize; ++i) {

                        JsonObject hitResult = hits.get(i).getAsJsonObject();
                        JsonArray deletedArray = hitResult.getAsJsonObject("_source").get("deleted").getAsJsonArray();

                        int numDeleted = deletedArray.size();

                        for (int y = 0; y < numDeleted; ++y) {
                            JsonObject deletedObject = deletedArray.get(y).getAsJsonObject();
                            deletedElements.put(deletedObject.get(Sjm.ELASTICID).getAsString(),
                                hitResult.getAsJsonObject("_source").get(Sjm.CREATED).getAsString());
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
