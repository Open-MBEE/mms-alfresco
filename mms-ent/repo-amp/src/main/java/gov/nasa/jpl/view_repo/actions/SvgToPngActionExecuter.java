package gov.nasa.jpl.view_repo.actions;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.SvgToPngPost;

public class SvgToPngActionExecuter extends ActionExecuterAbstractBase{
	static Logger logger = Logger.getLogger(SvgToPngActionExecuter.class);

	/**
	 * Injected variables from Spring configuration
	 */
	private ServiceRegistry services;
	private Repository repository;

	private StringBuffer response = new StringBuffer();
	private Status responseStatus;

	public static final String NAME = "svgToPng";

	public void setRepository(Repository rep) {
		repository = rep;
	}

	public void setServices(ServiceRegistry sr) {
		services = sr;
	}

	public SvgToPngActionExecuter(){
		super();
	}

	public SvgToPngActionExecuter(Repository repositoryHelper, ServiceRegistry registry){
		super();
		setRepository(repositoryHelper);
		setServices(registry);
	}

	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		SvgToPngActionExecuter instance = new SvgToPngActionExecuter(
				repository, services);
		instance.clearCache();
		instance.executeImplImpl(action, nodeRef);
	}

	private void executeImplImpl(final Action action, final NodeRef nodeRef) {
		EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
		SvgToPngPost svgToPng = new SvgToPngPost(repository, services);
		try{
			svgToPng.process();
		}
		catch(Throwable ex){
			String err = "Unexpected error occurred while converting SVGs to PNGs. " + ex.getMessage();
			response.append(err);
			logger.error(err);
		}
		response.append(svgToPng.getResponse());
		sendEmail(jobNode, svgToPng, response);
	}

	protected void sendEmail(EmsScriptNode jobNode, SvgToPngPost svgToPng, StringBuffer response){
		String subject = "SVGs To PNGs Completed";
		EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode,
				MimetypeMap.MIMETYPE_TEXT_PLAIN, services,
				subject + System.lineSeparator() + System.lineSeparator()
						+ response.toString());
		String msg = buildEmailMessage(response, logNode);
		ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);
		if (logger.isInfoEnabled()) {
			logger.info("Completed SVGs to PNGs conversion.");
		}
	}

	protected String buildEmailMessage(StringBuffer response, EmsScriptNode logNode){
		StringBuffer buf = new StringBuffer();
		HostnameGet hostnameGet = new HostnameGet(this.repository,
				this.services);
		String contextUrl = hostnameGet.getAlfrescoUrl() + "/alfresco";
		buf.append("Log: ");
		buf.append(contextUrl);
		buf.append(logNode.getUrl());

		return buf.toString();
	}

	protected void clearCache() {
		response = new StringBuffer();
		responseStatus = new Status();
		NodeUtil.setBeenInsideTransaction(false);
		NodeUtil.setBeenOutsideTransaction(false);
		NodeUtil.setInsideTransactionNow(false);
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// TODO Auto-generated method stub
	}

}
