package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.searchbox.cluster.UpdateSettings;
import io.searchbox.core.*;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.indices.template.PutTemplate;
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

    public static final String ELEMENT = "element";
    public static final String COMMIT = "commit";
    public static final String PROFILE = "profile";
    public static final String ARTIFACT = "artifact";
    public static final String REF = "ref";

    private static final String COMMIT_QUERY = "{\"query\":{\"bool\":{\"filter\":[{\"term\":{\"%1$s\":\"%2$s\"}},{\"term\":{\"%3$s\":\"%4$s\"}}]}}}";

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

    public void deleteIndex(String index) throws IOException {
        DeleteIndex indexExists = new DeleteIndex.Builder(index.toLowerCase().replaceAll("\\s+", "")).build();
        client.execute(indexExists);
    }

    public void applyTemplate(String template) throws IOException {
        PutTemplate.Builder putTemplateBuilder =
            new PutTemplate.Builder("template", template);
        client.execute(putTemplateBuilder.build());
    }

    public void updateMapping(String index, String type, String mapping) throws IOException {
        PutMapping.Builder putMappingBuilder = new PutMapping.Builder(index.toLowerCase().replaceAll("\\s+", ""), type, mapping);
        client.execute(putMappingBuilder.build());
    }

    public void updateByQuery(String index, String payload, String type) throws IOException {
        UpdateByQuery updateByQuery =
            new UpdateByQuery.Builder(payload).addIndex(index.toLowerCase().replaceAll("\\s+", "")).addType(type)
                .build();
        client.execute(updateByQuery);
    }

    public void updateClusterSettings(String payload) throws IOException {
        UpdateSettings updateSettings = new UpdateSettings.Builder(payload).build();
        client.execute(updateSettings);
    }

    public JsonObject getByElasticId(String id, String index, String type) throws IOException {
        Get get = new Get.Builder(index.toLowerCase().replaceAll("\\s+", ""), id).type(type).build();

        JestResult result = client.execute(get);
        if (result.isSucceeded()) {
            JsonObject o = result.getJsonObject().get("_source").getAsJsonObject();
            o.add(Sjm.ELASTICID, result.getJsonObject().get("_id"));
            if (type.equals(COMMIT)) {
                o.add(Sjm.SYSMLID, result.getJsonObject().get("_id"));
            }
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
                if (record.has(Sjm.COMMENT)) {
                    o.add(Sjm.COMMENT, record.get(Sjm.COMMENT));
                }
                array.add(o);
            }
        } else if (!result.isSucceeded()) {
            throw new IOException(String.format("Elasticsearch error[%1$s]:%2$s",
            		result.getResponseCode(), result.getErrorMessage()));
        }
        return array;
    }

    public JsonObject getByCommitId(String elasticId, String sysmlid, String index, String type) throws IOException {
        String query = String.format(COMMIT_QUERY, Sjm.COMMITID, elasticId, Sjm.SYSMLID, sysmlid);

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Search Query %s", query));
        }

        Search search = new Search.Builder(query).addIndex(index.toLowerCase().replaceAll("\\s+", ""))
            .addType(type).build();
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
    public ElasticResult indexElement(JsonObject j, String index, String eType) throws IOException {
        // :TODO error handling
        ElasticResult result = new ElasticResult();

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("indexElement: %s", j));
        }

        if (j.has(Sjm.SYSMLID)) {
            result.sysmlid = j.get(Sjm.SYSMLID).getAsString();
        }
        if (j.has(Sjm.ELASTICID)) {
            result.elasticId = client.execute(new Index.Builder(j.toString()).id(j.get(Sjm.ELASTICID).getAsString())
                .index(index.toLowerCase().replaceAll("\\s+", "")).type(eType).build()).getId();
        } else {
            result.elasticId = client.execute(
                new Index.Builder(j.toString()).index(index.toLowerCase().replaceAll("\\s+", "")).type(eType).build())
                .getId();
        }
        if (result.elasticId == null) {
            throw new IOException("Unable to index node in elasticsearch");
        }

        j.addProperty(Sjm.ELASTICID, result.elasticId);
        result.current = j;

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

    public JsonObject updateById(String id, JsonObject payload, String index, String type) throws IOException {
        JsonObject upsert = new JsonObject();
        upsert.add("doc", payload);
        upsert.addProperty("doc_as_upsert", true);
        upsert.addProperty("_source", true);
        JestResult res = client.execute(
            new Update.Builder(upsert.toString()).id(id).index(index.toLowerCase().replaceAll("\\s+", "")).type(type)
                .build());
        if (res.isSucceeded()) {
            return res.getJsonObject().get("get").getAsJsonObject().get("_source").getAsJsonObject();
        }
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

        if (result == null) {
            return top;
        }

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

    /**
     * Takes a type and array list of string ids. Creates a Bulk object and adds a list of actions. Then performs a bulk
     * delete. Returns the JSONObject of the result.
     *
     * @param type
     * @param ids
     * @return JSONObject Result
     */
    public JsonObject bulkDeleteByType(String type, ArrayList<String> ids, String index) {
        if (ids.isEmpty()) {
            return new JsonObject();
        }

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
        if (result != null) {
            o = result.getJsonObject();
        }
        return o;
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
        return null;
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
