package gov.nasa.jpl.view_repo.db;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.marklogic.client.document.JSONDocumentManager;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.QueryManager;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class MarkLogicImpl implements DocStoreInterface
{
	private static Logger logger = Logger.getLogger(MarkLogicImpl.class);

	Map<String, MarkLogicDBInfo> dbInfo;
	private MarkLogicRestClient mainRestClient = null;

	public MarkLogicImpl() throws IOException
	{
		dbInfo = new HashMap<String, MarkLogicDBInfo>();

		dbInfo.put("MMS-Core", new MarkLogicDBInfo(EmsConfig.get("marklogic.mms-core.name"), Integer.parseInt(EmsConfig.get("marklogic.mms-core.port"))));

		if (mainRestClient == null)
		{
			mainRestClient = new MarkLogicRestClient(EmsConfig.get("marklogic.host"),
					Integer.parseInt(EmsConfig.get("marklogic.confManager.port")),
					EmsConfig.get("marklogic.username"),
					EmsConfig.get("marklogic.password"));
		}
	}

    @Override
    // 1. called to retrieve
    public JsonObject getByInternalId(String id, String name, String type) throws IOException
    {
        MarkLogicDBInfo projectDBInfo = getDBInfo(name);

        JSONDocumentManager docMgr = projectDBInfo.getClient().newJSONDocumentManager();

        String doc = docMgr.read(id, new StringHandle()).get();

        JsonParser parser = new JsonParser();
        JsonObject jsonDoc = parser.parse(doc).getAsJsonObject();

        return jsonDoc;

    }

    @Override
    public JsonObject getByCommitId(String id, String sysmlid, String name, String type) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonArray getCommitHistory(String sysmlid, String name) throws IOException
    {
        QueryManager queryMgr = dbInfo.get(name).getQueryManager();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonArray getElementsFromDocStoreIds(List<String> ids, String name) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject getElementsLessThanOrEqualTimestamp(String sysmlId, String timestamp, List<String> refsCommitIds,
                                                          String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getDeletedElementsFromCommits(List<String> commitIds, String name)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject search(JsonObject queryJson) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DocumentResult indexElement(JsonObject j, String name, String eType) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createIndex(String name) throws IOException
    {
        if (!mainRestClient.checkIfDBExists(convertDBName(name)))
        {
            //TODO: figure out the port assignment
            mainRestClient.createDatabase(convertDBName(name), 8011);
        }
    }

    @Override
    public boolean refreshIndex() throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean bulkIndexElements(JsonArray bulkElements, String operation, boolean refresh, String name,
                                     String type) throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void applyTemplate(String template) throws IOException {

    }

    @Override
    public void updateMapping(String index, String type, String mapping) throws IOException {

    }

    @Override
    public void updateByQuery(String index, String payload, String type) throws IOException {

    }

    @Override
    public void updateClusterSettings(String payload) throws IOException {

    }

    @Override
    public JsonObject updateById(String id, JsonObject payload, String name, String type) throws IOException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean bulkUpdateElements(Set<String> elements, String payload, String name, String type)
        throws IOException
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void deleteIndex(String name) throws IOException
    {
        if (mainRestClient.checkIfDBExists(convertDBName(name)))
        {
            //TODO: figure out the port assignment
            mainRestClient.deleteDatabase(convertDBName(name), 8011);
        }
    }

    @Override
    public void deleteByQuery(String index, String payload, String type) throws IOException {

    }

    @Override
    public JsonObject bulkDeleteByType(Set<String> ids, String name, String type)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close()
    {
        dbInfo.values().forEach(client -> client.getClient().release());
        logger.warn("Mark Logic clients have been closed");
    }

    private MarkLogicDBInfo getDBInfo(String name)
    {
        MarkLogicDBInfo dbInfoForName = dbInfo.get(name);

        if (dbInfoForName == null)
        {
            MarkLogicDBInfo coreDBInfo = dbInfo.get("MMS-Core");
            JSONDocumentManager docMgr = coreDBInfo.getClient().newJSONDocumentManager();

            String doc = docMgr.read(String.format("/dbInfo/%s.json", name), new StringHandle()).get();

            JsonParser parser = new JsonParser();
            JsonObject jsonDoc = parser.parse(doc).getAsJsonObject();

            int port = Integer.parseInt(jsonDoc.get("port").getAsString());

            dbInfoForName = new MarkLogicDBInfo(name, port);
            dbInfo.put(name, dbInfoForName);
        }

        return dbInfoForName;
    }

	private String convertDBName(String name)
	{
	    return name.toLowerCase().replaceAll("\\s+", "");
	}
}
