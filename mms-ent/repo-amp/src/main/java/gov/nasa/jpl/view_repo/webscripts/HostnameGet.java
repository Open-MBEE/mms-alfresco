package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.Sjm;

public class HostnameGet extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(HostnameGet.class);

	private final String LOCAL_HOST = "localhost";
	private final String LOCAL_HOST_IP = "127.0.0.1";

	@Override
    protected Map< String, Object > executeImpl( WebScriptRequest req, Status status, Cache cache ) {
	    HostnameGet instance = new HostnameGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache );
	}

    @Override
    protected Map< String, Object > executeImplImpl( WebScriptRequest req,
                                                     Status status, Cache cache ) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

		Map< String, Object > model = new HashMap<>();
		JSONObject jsonObj = new JSONObject();
		SysAdminParams sysAdminParams = this.services.getSysAdminParams();

		JSONObject alfrescoJson = new JSONObject();
		try{
			alfrescoJson.put("protocol", sysAdminParams.getAlfrescoProtocol());
			alfrescoJson.put("host", sysAdminParams.getAlfrescoHost());
			alfrescoJson.put("port", sysAdminParams.getAlfrescoPort());

			JSONObject shareJson = new JSONObject();
			shareJson.put("protocol", sysAdminParams.getShareProtocol());
			shareJson.put("host", sysAdminParams.getShareHost());
			shareJson.put("port", sysAdminParams.getSharePort());

			jsonObj.put("alfresco", alfrescoJson);
			jsonObj.put("share", shareJson);

            model.put(Sjm.RES, jsonObj.toString(4));
        } catch(JSONException js) {
			status.setCode(Status.STATUS_NOT_FOUND);
			status.setMessage("Cannot get host name information.");
			status.setException(js);
			status.setRedirect(true);
		}

		printFooter(user, logger, timer);

		return model;
	}

	@Override
	protected boolean validateRequest(WebScriptRequest req, Status status) {
		// TODO Auto-generated method stub
		return false;
	}

	private String alfrescoContext;
	public String getAlfrescoContext(){
		return alfrescoContext;
	}

	private String alfrescoHost;
	public String getAlfrescoHost(){
		return alfrescoHost;
	}

	private int alfrescoPort;
	public int getAlfrescoPort(){
		return alfrescoPort;
	}

	private String alfrescoProtocol;
	public String getAlfrescoProtocol(){
		return alfrescoProtocol;
	}

	private String alfrescoUrl;
	public String getAlfrescoUrl(){
		if(alfrescoUrl==null || alfrescoUrl.isEmpty()){
			if(this.alfrescoHost.compareToIgnoreCase(LOCAL_HOST)==0 || this.alfrescoHost.compareToIgnoreCase(LOCAL_HOST_IP)==0){
				alfrescoUrl = this.alfrescoProtocol + "://" + this.alfrescoHost + ":" + alfrescoPort;
			}
			else{
				alfrescoUrl = this.alfrescoProtocol + "://" + this.alfrescoHost;
			}
		}
		return alfrescoUrl;
	}

	private String shareHost;
	public String getShareHost(){ return shareHost;}

	private int sharePort;
	public int getSharePoint(){return sharePort;}

	private String shareProtocol;
	public String getShareProtocol(){return shareProtocol;}

	private String shareUrl;
	public String getShareUrl(){
		if(Utils.isNullOrEmpty(shareUrl)){
			if(this.shareHost.compareToIgnoreCase(LOCAL_HOST)==0 || this.shareHost.compareToIgnoreCase(LOCAL_HOST_IP)==0){
				shareUrl = this.shareProtocol + "://" + this.shareHost + ":" + sharePort;
			}
			else{
				shareUrl = this.shareProtocol + "://" + this.shareHost;
			}
		}
		return shareUrl;
	}

	public HostnameGet(){
		super();

	}

	public HostnameGet(Repository repositoryHelper, ServiceRegistry registry) {
		super(repositoryHelper, registry);
		SysAdminParams sysAdminParams = this.services.getSysAdminParams();
		this.alfrescoContext = sysAdminParams.getAlfrescoContext();
		this.alfrescoHost = sysAdminParams.getAlfrescoHost();
		this.alfrescoPort = sysAdminParams.getAlfrescoPort();
		this.alfrescoProtocol = sysAdminParams.getAlfrescoProtocol();

		this.shareHost = sysAdminParams.getShareHost();
		this.sharePort = sysAdminParams.getSharePort();
		this.shareProtocol = sysAdminParams.getShareProtocol();
	}

}
