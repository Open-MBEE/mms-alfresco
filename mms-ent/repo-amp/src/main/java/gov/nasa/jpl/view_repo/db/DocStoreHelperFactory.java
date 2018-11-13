package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class DocStoreHelperFactory
{
	public static DocStoreHelperInterface getDocStore() throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		String docStoreHelperImplementationName = EmsConfig.get("docstore.name");
		Class<?> docStoreHelperImplementation = Class.forName(docStoreHelperImplementationName);
		DocStoreHelperInterface docStoreHelper = (DocStoreHelperInterface) docStoreHelperImplementation.newInstance();
		
		return docStoreHelper;
	}
}
