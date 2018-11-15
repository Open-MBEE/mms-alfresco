package gov.nasa.jpl.view_repo.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MarkLogicRestClient
{
	private static Logger logger = Logger.getLogger(MarkLogicRestClient.class);

	String host;
	int mainPort;
	String username;
	String password;

	public MarkLogicRestClient(String host, int mainPort, String username, String password)
	{
		this.host = host;
		this.mainPort = mainPort;
		this.username = username;
		this.password = password;
	}

	public boolean checkIfDBExists(String name)
	{
		URIBuilder uri = null;
		try
		{
			uri = new URIBuilder(host);
		}
		catch (URISyntaxException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}

		uri.setPort(mainPort);
		uri.setPath("/manage/v2/databases/" + name);

		try
		{
			HttpRequestBase request = buildHttpRequest(HttpRequestType.GET, uri);
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(request);

			if (response.getStatusLine().getStatusCode() == 200)
			{
				return true;
			}
		}
		catch (UnsupportedOperationException | IOException | URISyntaxException e)
		{
			e.printStackTrace();
			return false;
		}

		return false;
	}

	public void craeteSuperDatabase()
	{
	}

	// create new database and Rest service at specified port
	public void deleteDatabase(String name, int port)
	{
		URIBuilder uri = null;
		try
		{
			uri = new URIBuilder(host);
		}
		catch (URISyntaxException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		}

		uri.setPort(mainPort);
		uri.setPath("/manage/v2/databases/" + name);

		try
		{
			HttpRequestBase request = buildHttpRequest(HttpRequestType.DELETE, uri);
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(request);

			if (response.getStatusLine().getStatusCode() == 200)
			{
				return;
			}
		}
		catch (UnsupportedOperationException | IOException | URISyntaxException e)
		{
			e.printStackTrace();
			return;
		}

		return;
	}

	// create new database and Rest service at specified port
	public void createDatabase(String name, int port)
	{
		final AtomicReference<Integer> responseCode = new AtomicReference<>();
		final AtomicReference<String> responseBody = new AtomicReference<>();

		URIBuilder uri;
		try
		{
			uri = new URIBuilder(host);
		}
		catch (URISyntaxException e)
		{
			logger.error("[ERROR] Unexpected error in generation of Mark Logic URL. Reason: " + e.getMessage());
			e.printStackTrace();
			return;
		}

		uri.setPort(mainPort);
		uri.setPath("/v1/rest-apis");

		Gson gson = new Gson();
		JsonObject restAPI = new JsonObject();
		restAPI.addProperty("name", name);
		restAPI.addProperty("database", name);
		restAPI.addProperty("port", port);
		JsonObject data = new JsonObject();
		data.add("rest-api", restAPI);

		try
		{
			HttpRequestBase request = buildHttpRequest(HttpRequestType.POST, uri, gson.toJson(data),
					ContentType.APPLICATION_JSON);
			CloseableHttpClient httpclient = HttpClients.createDefault();
			CloseableHttpResponse response = httpclient.execute(request);
			InputStream inputStream = response.getEntity().getContent();

			responseCode.set(response.getStatusLine().getStatusCode());
			if (inputStream != null)
			{
				responseBody.set(IOUtils.toString(inputStream, Charset.defaultCharset()));
			}

			// TODO: deal with response
		}
		catch (UnsupportedOperationException | IOException | URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	private HttpRequestBase buildHttpRequest(HttpRequestType type, URIBuilder requestUri)
			throws IOException, URISyntaxException
	{
		return buildHttpRequest(type, requestUri, null, null);
	}

	private HttpRequestBase buildHttpRequest(HttpRequestType type, URIBuilder requestUri, String sendData,
			ContentType contentType) throws IOException, URISyntaxException
	{
		// build specified request type
		// assume that any request can have a body, and just build the
		// appropriate one
		URI requestDest = requestUri.build();
		final HttpRequestBase request;

		if (type == HttpRequestType.GET && sendData != null)
		{
			throw new IOException("GETs with body are not supported");
		}
		switch (type)
		{
		case DELETE:
			request = new HttpDeleteWithBody(requestDest);
			break;
		case GET:
		default:
			request = new HttpGet(requestDest);
			break;
		case POST:
			request = new HttpPost(requestDest);
			break;
		case PUT:
			request = new HttpPut(requestDest);
			break;
		}

		request.addHeader("charset", (contentType != null ? contentType.getCharset() : Consts.UTF_8).displayName());
		if (sendData != null)
		{
			if (contentType != null)
			{
				request.addHeader("Content-Type", contentType.getMimeType());
			}

			StringEntity strEntity = new StringEntity(sendData);
			// reqEntity.setChunked(true);
			((HttpEntityEnclosingRequest) request).setEntity(strEntity);
		}
		return request;
	}

	public enum HttpRequestType {
		GET, POST, PUT, DELETE
	}

	@NotThreadSafe
	public class HttpGetWithBody extends HttpPost
	{
		public static final String METHOD_NAME = "GET";

		public HttpGetWithBody()
		{
			super();
		}

		public HttpGetWithBody(URI uri)
		{
			super(uri);
		}

		public HttpGetWithBody(String uri)
		{
			super(URI.create(uri));
		}

		@Override
		public String getMethod()
		{
			return "GET";
		}
	}

	@NotThreadSafe
	public class HttpDeleteWithBody extends HttpPost
	{
		public static final String METHOD_NAME = "DELETE";

		public HttpDeleteWithBody()
		{
			super();
		}

		public HttpDeleteWithBody(URI uri)
		{
			super(uri);
		}

		public HttpDeleteWithBody(String uri)
		{
			super(URI.create(uri));
		}

		public String getMethod()
		{
			return "DELETE";
		}
	}

}
