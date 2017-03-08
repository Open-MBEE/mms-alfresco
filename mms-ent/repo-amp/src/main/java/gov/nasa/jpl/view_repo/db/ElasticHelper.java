package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Delete;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.DeleteIndex;
import io.searchbox.indices.IndicesExists;

public class ElasticHelper {
    private static JestClient client = null;
    private static Logger logger = Logger.getLogger(ElasticHelper.class);
    private static String elementIndex = "mms";
    // TODO: pull this value from config file
    private static int resultLimit = 10000;
    private static int termLimit = 10000;

    public void init(String elasticHost) throws UnknownHostException {

        JestClientFactory factory = new JestClientFactory();
        if (elasticHost.contains("https")) {
            factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticHost)
                            .defaultSchemeForDiscoveredNodes("https").multiThreaded(true).build());
        } else {
            factory.setHttpClientConfig(new HttpClientConfig.Builder(elasticHost).multiThreaded(true).build());
        }
        client = factory.getObject();
        logger.warn(String.format("Initialization complete for ElasticSearch client. Cluster name: mms. %s",
                        client.toString()));
        logger.warn(String.format("ElasticSearch connected to: %s", elasticHost));
    }

    public void close() {

        client.shutdownClient();
    }

    public ElasticHelper() throws IOException {

        if (client == null) {
            logger.debug("Initializing Elastic client");
            init(EmsConfig.get("elastic.host"));
        }

        createIndex(elementIndex);
    }

    // Create index if not exists
    public void createIndex(String index) throws IOException {

        boolean indexExists = client.execute(new IndicesExists.Builder(index).build()).isSucceeded();
        if (!indexExists) {
            client.execute(new CreateIndex.Builder(index).build());
        }
    }

    // delete element given sysmlid from given index
    // TODO: can be generalized to do from multiple indexes also
    public void deleteElement(String sysmlid) throws IOException {

        JSONObject queryJson = new JSONObject().put("query",
                        new JSONObject().put("term", new JSONObject().put(Sjm.SYSMLID, sysmlid)));

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);
        List<SearchResult.Hit<JSONObject, Void>> hits = result.getHits(JSONObject.class);

        for (SearchResult.Hit<JSONObject, Void> h : hits) {
            client.execute(new Delete.Builder(h.source.getString("_id")).index(elementIndex).type("element").build());
        }
    }

    // Delete index if exists
    public void deleteIndex(String name) throws IOException {

        boolean indexExists = client.execute(new IndicesExists.Builder(name).build()).isSucceeded();
        if (indexExists) {
            client.execute(new DeleteIndex.Builder(name).build());
        }
    }

    // get all versions of element by sysmlid. sorted by modified in descending
    // order
    // which means that most recent one is first
    public JSONObject getElement(String sysmlid) throws IOException {

        JSONObject queryJson = new JSONObject()
                        .put("query", new JSONObject().put("filtered",
                                        new JSONObject().put("filter",
                                                        new JSONObject().put("term",
                                                                        new JSONObject().put(Sjm.SYSMLID, sysmlid)))))
                        .put("sort", new JSONArray().put(
                                        new JSONObject().put(Sjm.MODIFIED, new JSONObject().put("order", "desc"))));

        try {
            Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
            SearchResult result = client.execute(search);
            if (result.getTotal() > 0) {
                JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
                JSONObject o = new JSONObject(hits.get(0).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id").getAsString());
                return o;
            }
        } catch (Exception e) {
            logger.warn(String.format("%s", LogUtil.getStackTrace(e)));
        }

        return null;
    }

    public JSONObject getElementByElasticId(String id) throws IOException {

        JSONObject queryJson = new JSONObject().put("query",
                        new JSONObject().put("terms", new JSONObject().put("_id", new JSONArray().put(id))));

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);
        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            JSONObject o = new JSONObject(hits.get(0).getAsJsonObject().getAsJsonObject("_source").toString());
            o.put(Sjm.ELASTICID, hits.get(0).getAsJsonObject().get("_id").getAsString());
            return o;
        }

        return null;
    }

    // get entire history for element by sysmlid
    // return a JSON array with the most recent version first
    public JSONArray getElementHistory(String sysmlid) throws IOException {

        JSONObject queryJson = new JSONObject()
                        .put("size", resultLimit).put("query",
                                        new JSONObject().put("filtered",
                                                        new JSONObject().put("filter",
                                                                        new JSONObject().put("term", new JSONObject()
                                                                                        .put(Sjm.SYSMLID, sysmlid)))))
                        .put("sort", new JSONArray().put(
                                        new JSONObject().put(Sjm.MODIFIED, new JSONObject().put("order", "desc"))));

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            JSONArray array = new JSONArray();
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                array.put(o);
            }
            removeWrapper(array); // don't need this
            return array;
        }

        return new JSONArray();
    }

    // get entire history for element by sysmlid
    // return a JSON array with the most recent version first
    public JSONArray getElementCommitHistory(String sysmlid) throws IOException {

        JSONArray should = new JSONArray();
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "added").put("query", new JSONObject()
                        .put("term", new JSONObject().put("added.sysmlId", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested",
                        new JSONObject().put("path", "updated").put("query",
                                        new JSONObject().put("term", new JSONObject().put("updated.sysmlId",
                                                        new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested", new JSONObject().put("path", "moved").put("query", new JSONObject()
                        .put("term", new JSONObject().put("moved.sysmlId", new JSONObject().put("value", sysmlid))))));
        should.put(new JSONObject().put("nested",
                        new JSONObject().put("path", "deleted").put("query",
                                        new JSONObject().put("term", new JSONObject().put("deleted.sysmlId",
                                                        new JSONObject().put("value", sysmlid))))));
        JSONObject query = new JSONObject().put("size", resultLimit)
                        .put("query", new JSONObject().put("bool", new JSONObject().put("should", should)))
                        .put("sort", new JSONArray()
                                        .put(new JSONObject().put(Sjm.CREATED, new JSONObject().put("order", "desc"))));

        Search search = new Search.Builder(query.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            JSONArray array = new JSONArray();
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                array.put(o);
            }
            return array;
        }

        return new JSONArray();
    }

    public JSONArray getElementsFromElasticIds(List<String> ids) throws IOException {

        int count = 0;
        JSONArray elements = new JSONArray();

        if (ids.size() == 0) {
            return elements;
        }

        while (count < ids.size()) {
            // sublist is fromIndex inclusive, toIndex exclusive
            List<String> sub = ids.subList(count, Math.min(ids.size(), count + termLimit));
            if (count == Math.min(ids.size(), count + termLimit)) {
                sub = new ArrayList<String>();
                sub.add(ids.get(count));
            }
            JSONArray elasticids = new JSONArray();
            sub.forEach(elasticids::put);

            JSONObject queryJson = new JSONObject()
                            .put("size", resultLimit).put("query",
                                            new JSONObject().put("filtered",
                                                            new JSONObject().put("filter",
                                                                            new JSONObject().put("terms",
                                                                                            new JSONObject().put("_id",
                                                                                                            elasticids)))))
                            .put("sort", new JSONArray().put(
                                            new JSONObject().put(Sjm.MODIFIED, new JSONObject().put("order", "desc"))));

            logger.debug(String.format("Search Query %s", queryJson.toString()));

            Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
            SearchResult result = client.execute(search);

            if (result.getTotal() > 0) {
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

    // index JSON Object (element) into an index
    public ElasticResult indexElement(JSONObject j) throws IOException {

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
        result.elasticId =
            client.execute(new Index.Builder(k.toString()).index(elementIndex).type(eType).build()).getId();
        k.put(Sjm.ELASTICID, result.elasticId);
        result.current = k;

        return result;
    }

    public ElasticResult indexCommit(JSONObject j) throws IOException {

        ElasticResult result = new ElasticResult();
        String eType = "commit";


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
        result.elasticId = client.execute(
            new Index.Builder(k.toString()).index(elementIndex).type(eType).id(k.getString(Sjm.ELASTICID)).build())
            .getId();

        k.put(Sjm.ELASTICID, result.elasticId);
        result.current = k;


        return result;
    }

    public Map<String, ElasticResult> indexElements(JSONArray e) throws JSONException, IOException {

        logger.debug(String.format("Starting to index %d elements.", e.length()));
        Map<String, ElasticResult> sysmlId2ElasticId = new HashMap<>();
        for (int i = 0; i < e.length(); i++) {
            ElasticResult indexResult = indexElement(e.getJSONObject(i));
            logger.debug(String.format("Result: %s", indexResult));

            sysmlId2ElasticId.put(indexResult.current.getString(Sjm.SYSMLID), indexResult);
        }

        return sysmlId2ElasticId;
    }

    public String bulkIndexElements(JSONArray bulkElements, String operation) throws JSONException, IOException {
        int limit = Integer.parseInt(EmsConfig.get("elastic.limit.insert"));
        // BulkableAction is generic
        ArrayList<BulkableAction> actions = new ArrayList<BulkableAction>();
        for (int i = 0; i < bulkElements.length(); i++) {
            JSONObject curr = bulkElements.getJSONObject(i);
            if (operation.equals("delete")) {
                actions.add(new Delete.Builder(curr.getString(Sjm.ELASTICID)).index(elementIndex).type("element")
                                .build());
            } else {
                actions.add(new Index.Builder(curr.toString()).id(curr.getString(Sjm.ELASTICID)).build());
            }
            if ((((i + 1) % limit) == 0 && i != 0) || i == (bulkElements.length() - 1)) {
                BulkResult result = insertBulk(actions);
                if (!result.isSucceeded()) {
                    return result.getErrorMessage();
                }
                actions.clear();
            }
        }
        return "1";
    }

    private BulkResult insertBulk(List<BulkableAction> actions) throws JSONException, IOException {
        Bulk bulk = new Bulk.Builder().defaultIndex(elementIndex).defaultType("element").addAction(actions).build();
        BulkResult result = client.execute(bulk);
        return result;
    }

    public JSONArray search(Map<String, String> params) throws IOException {

        JSONArray terms = new JSONArray();
        for (Map.Entry<String, String> pair : params.entrySet()) {
            JSONObject term = new JSONObject();
            JSONObject termContainer = new JSONObject();
            term.put(pair.getKey(), pair.getValue().toLowerCase());
            termContainer.put("term", term);
            terms.put(termContainer);
        }

        JSONObject queryJson = new JSONObject().put("size", resultLimit).put("query", new JSONObject().put("filtered",
                        new JSONObject().put("filter", new JSONObject().put("or", terms))));

        logger.debug(String.format("Search Query %s", queryJson.toString()));

        JSONArray elements = new JSONArray();

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                elements.put(o);
            }
        }

        return elements;
    }

    public JSONArray search(String keyword) throws IOException {

        JSONObject queryJson = new JSONObject().put("size", resultLimit).put("query",
                        new JSONObject().put("query_string", new JSONObject().put("query", keyword)));

        JSONArray elements = new JSONArray();

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                removeWrapper(o);
                o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                elements.put(o);
            }
        }

        return elements;
    }

    public JSONArray searchCommits(Map<String, String> params) throws IOException {

        JSONArray terms = new JSONArray();
        for (Map.Entry<String, String> pair : params.entrySet()) {
            JSONObject term = new JSONObject();
            JSONObject termContainer = new JSONObject();
            term.put(pair.getKey(), pair.getValue().toLowerCase());
            termContainer.put("term", term);
            terms.put(termContainer);
        }

        JSONObject queryJson = new JSONObject().put("size", resultLimit).put("query", new JSONObject().put("filtered",
                        new JSONObject().put("filter", new JSONObject().put("or", terms))));

        logger.debug(String.format("Search Query %s", queryJson.toString()));

        JSONArray elements = new JSONArray();

        Search search = new Search.Builder(queryJson.toString()).addIndex(elementIndex).build();
        SearchResult result = client.execute(search);

        if (result.getTotal() > 0) {
            JsonArray hits = result.getJsonObject().getAsJsonObject("hits").getAsJsonArray("hits");
            for (int i = 0; i < hits.size(); i++) {
                JSONObject o = new JSONObject(hits.get(i).getAsJsonObject().getAsJsonObject("_source").toString());
                o.put(Sjm.ELASTICID, hits.get(i).getAsJsonObject().get("_id").getAsString());
                removeWrapper(o);
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
}
