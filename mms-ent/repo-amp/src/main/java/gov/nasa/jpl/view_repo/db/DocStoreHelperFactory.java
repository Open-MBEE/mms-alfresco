package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class DocStoreHelperFactory
{
	public static IDocStore getDocStore() throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		String docStoreHelperImplementationName = EmsConfig.get("docstore.name");
		Class<?> docStoreHelperImplementation = Class.forName(docStoreHelperImplementationName);
		IDocStore docStoreHelper = (IDocStore) docStoreHelperImplementation.newInstance();

		return docStoreHelper;
	}
}
