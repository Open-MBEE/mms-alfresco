package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.admin.SysAdminParams;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.JsonObject;

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
		JsonObject jsonObj = new JsonObject();
		SysAdminParams sysAdminParams = this.services.getSysAdminParams();

		JsonObject alfrescoJson = new JsonObject();
		alfrescoJson.addProperty("protocol", sysAdminParams.getAlfrescoProtocol());
		alfrescoJson.addProperty("host", sysAdminParams.getAlfrescoHost());
		alfrescoJson.addProperty("port", sysAdminParams.getAlfrescoPort());

		JsonObject shareJson = new JsonObject();
		shareJson.addProperty("protocol", sysAdminParams.getShareProtocol());
		shareJson.addProperty("host", sysAdminParams.getShareHost());
		shareJson.addProperty("port", sysAdminParams.getSharePort());

		jsonObj.add("alfresco", alfrescoJson);
		jsonObj.add("share", shareJson);

		model.put(Sjm.RES, jsonObj.toString());
		
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
		if(alfrescoUrl==null || alfrescoUrl.isEmpty() || alfrescoPort > 0) {
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
