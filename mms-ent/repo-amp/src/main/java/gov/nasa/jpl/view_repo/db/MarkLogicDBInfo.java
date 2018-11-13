package gov.nasa.jpl.view_repo.db;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.DatabaseClientFactory.Authentication;
import com.marklogic.client.query.QueryManager;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class MarkLogicDBInfo
{
	private String name;
	private DatabaseClient client = null;
	private MarkLogicRestClient restClient = null;
	private QueryManager queryMgr;

	public MarkLogicDBInfo(String name, int port)
	{
		this.name = name;

		// TODO change to new method based on the SecurityContext
		String host = EmsConfig.get("marklogic.host");
		String username = EmsConfig.get("marklogic.username");
		String password = EmsConfig.get("marklogic.password");
		client = DatabaseClientFactory.newClient(host, port, username, password, Authentication.DIGEST);

		queryMgr = client.newQueryManager();

		restClient = new MarkLogicRestClient(host, port, username, password);
	}

	public DatabaseClient getClient()
	{
		return client;
	}

	public MarkLogicRestClient getRestClient()
	{
		return restClient;
	}
	
	public QueryManager getQueryManager()
	{
		return queryMgr;
	}
}
