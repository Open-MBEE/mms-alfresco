package gov.nasa.jpl.view_repo.db;

import gov.nasa.jpl.view_repo.util.EmsConfig;

public class DocStoreFactory
{
	public static DocStoreInterface getDocStore() throws ClassNotFoundException, InstantiationException, IllegalAccessException
	{
		String docStoreHelperImplementationName = EmsConfig.get("docstore.impl");
		Class<?> docStoreHelperImplementation = Class.forName(docStoreHelperImplementationName);
		DocStoreInterface docStoreHelper = (DocStoreInterface) docStoreHelperImplementation.newInstance();

		return docStoreHelper;
	}
}
