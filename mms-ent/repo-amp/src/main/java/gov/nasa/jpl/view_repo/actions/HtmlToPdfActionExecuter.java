package gov.nasa.jpl.view_repo.actions;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;
import gov.nasa.jpl.view_repo.webscripts.HostnameGet;
import gov.nasa.jpl.view_repo.webscripts.HtmlToPdfPost;

/**
 * Action for converting HTML to PDF in the background asynchronously
 *
 * @author lho
 *
 */
public class HtmlToPdfActionExecuter extends ActionExecuterAbstractBase {
	static Logger logger = Logger.getLogger(HtmlToPdfActionExecuter.class);
	/**
	 * Injected variables from Spring configuration
	 */
	private ServiceRegistry services;
	private Repository repository;

	private StringBuffer response = new StringBuffer();
	private Status responseStatus;

	// Parameter values to be passed in when the action is created
	public static final String NAME = "htmlToPdf";
	public static final String PARAM_SITE_NAME = "siteName";
	public static final String PARAM_DOCUMENT_ID = "documentId";
	public static final String PARAM_COVER = "cover";
	public static final String PARAM_HTML = "html";
	public static final String PARAM_HEADER = "header";
	public static final String PARAM_FOOTER = "footer";
	public static final String PARAM_TAG_ID = "tagId";
	public static final String PARAM_DOC_NUM = "docnum";
	public static final String PARAM_VERSION = "version";
	public static final String PARAM_TIME_STAMP = "timeStamp";
	public static final String PARAM_DISPLAY_TIME = "displayTime";
	public static final String PARAM_CUSTOM_CSS = "customCss";
	public static final String PARAM_WORKSPACE = "workspace";
	public static final String PARAM_POST_JSON = "postJson";
	public static final String PARAM_TOC = "toc";
	public static final String PARAM_TOF = "tof";
	public static final String PARAM_TOT = "tot";
	public static final String PARAM_INDEX = "index";
	public static final String PARAM_DISABLED_COVER_PAGE = "disabledCoverPage";

	public void setRepository(Repository rep) {
		repository = rep;
	}

	public void setServices(ServiceRegistry sr) {
		services = sr;
	}

	public HtmlToPdfActionExecuter() {
		super();
	}

	public HtmlToPdfActionExecuter(Repository repositoryHelper,
			ServiceRegistry registry) {
		super();
		setRepository(repositoryHelper);
		setServices(registry);
	}

	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		HtmlToPdfActionExecuter instance = new HtmlToPdfActionExecuter(
				repository, services);
		instance.clearCache();
		instance.executeImplImpl(action, nodeRef);
	}

	private void executeImplImpl(final Action action, final NodeRef nodeRef) {

		EmsScriptNode jobNode = new EmsScriptNode(nodeRef, services, response);
		String documentId = (String) action
				.getParameterValue(PARAM_DOCUMENT_ID);
		String tagId = (String) action.getParameterValue(PARAM_TAG_ID);
		String timeStamp = (String) action.getParameterValue(PARAM_TIME_STAMP);
		String htmlContent = (String) action.getParameterValue(PARAM_HTML);
		String coverContent = (String) action.getParameterValue(PARAM_COVER);
		String toc = (String) action.getParameterValue(PARAM_TOC);
		String tof = (String) action.getParameterValue(PARAM_TOF);
		String tot = (String) action.getParameterValue(PARAM_TOT);
		String indices = (String) action.getParameterValue(PARAM_INDEX);
		String headerContent = (String) action.getParameterValue(PARAM_HEADER);
		String footerContent = (String) action.getParameterValue(PARAM_FOOTER);
		String docNum = (String) action.getParameterValue(PARAM_DOC_NUM);
		String displayTime = (String) action.getParameterValue(PARAM_DISPLAY_TIME);
		String customCss = (String) action.getParameterValue(PARAM_CUSTOM_CSS);
		String disabledCvrPg = (String)action.getParameterValue(PARAM_DISABLED_COVER_PAGE);

		timeStamp = timeStamp.toLowerCase().replace("/", "-").replaceAll("\\s+", "").replaceAll("[^A-Za-z0-9]", "");

		Boolean disabledCoverPage = false;
		if(!StringUtils.isEmpty(disabledCvrPg)) disabledCoverPage = Boolean.parseBoolean(disabledCvrPg);

		HtmlToPdfPost htmlToPdf = new HtmlToPdfPost(repository, services);
		EmsScriptNode pdfNode = null;
		try{
			pdfNode = htmlToPdf.convert(documentId, tagId, timeStamp,
				htmlContent, coverContent, toc, tof, tot, indices, headerContent, footerContent, docNum, displayTime, customCss, disabledCoverPage);
			response.append(htmlToPdf.getResponse().toString());
			response.append("Sending email to user...");
		}
		catch(Throwable ex){
			ex.printStackTrace();
		}
		finally{
			//htmlToPdf.cleanupFiles();
			sendEmail(jobNode, pdfNode, response);
		}
	}

	protected void sendEmail(EmsScriptNode jobNode, EmsScriptNode pdfNode,
			StringBuffer response) {
		String status = (pdfNode != null) ? "completed"
				: "completed with errors";
		String subject = String.format("HTML to PDF generation %s.", status);
		EmsScriptNode logNode = ActionUtil.saveLogToFile(jobNode,
				MimetypeMap.MIMETYPE_TEXT_PLAIN, services,
				subject + System.lineSeparator() + System.lineSeparator()
						+ response.toString());
		String msg = buildEmailMessage(pdfNode, response, logNode);
		ActionUtil.sendEmailToModifier(jobNode, msg, subject, services);
		if (logger.isDebugEnabled())
			logger.debug("Completed HTML to PDF generation.");

	}

	protected String buildEmailMessage(EmsScriptNode pdfNode,
			StringBuffer response, EmsScriptNode logNode) {
		StringBuffer buf = new StringBuffer();
		HostnameGet hostnameGet = new HostnameGet(this.repository,
				this.services);
		String contextUrl = hostnameGet.getAlfrescoUrl() + "/share/proxy/alfresco/slingshot/node/content";

		if (pdfNode == null) {
			buf.append("HTML to PDF generation completed with errors. Please review the below link for detailed information.");
		} else {
			buf.append("HTML to PDF generation succeeded.");
			buf.append(System.lineSeparator());
			buf.append(System.lineSeparator());
			buf.append("You can access the PDF file at ");
			buf.append(contextUrl + pdfNode.getUrl().replace("/d/d",""));
		}

		buf.append(System.lineSeparator());
		buf.append(System.lineSeparator());

		EmsScriptNode parentNode = null;
		if(pdfNode != null) parentNode = pdfNode.getParent();
		if(parentNode != null){
			String shareUrl = hostnameGet.getShareUrl();
			buf.append("Directory link: ");
			buf.append(shareUrl);
			buf.append("/share/page/repository#filter=path%7C%2F");
			buf.append(parentNode.getUrl().replace("/d/d",""));
			buf.append(System.lineSeparator());
			buf.append(System.lineSeparator());
		}

		//buf.append("Log: ");
		//buf.append(contextUrl);
		//buf.append(logNode.getUrl().replace("/d/d",""));

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
