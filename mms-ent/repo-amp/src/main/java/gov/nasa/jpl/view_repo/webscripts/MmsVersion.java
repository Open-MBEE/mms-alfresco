package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.LogUtil;
import gov.nasa.jpl.view_repo.util.Sjm;

public class MmsVersion extends AbstractJavaWebScript {
    private static Logger logger = Logger.getLogger(MmsVersion.class);

    protected boolean prettyPrint = true;
    public MmsVersion() {
        super();
    }

    public MmsVersion(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    /**
     * Webscript entry point
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        MmsVersion instance = new MmsVersion(repository, getServices());
        return instance.executeImplImpl(req,  status, cache);
    }
    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        JsonObject mmsVersion;

        if (logger.isDebugEnabled()) {
        	logger.debug("Checking MMS Versions");
        }

        mmsVersion = getMMSversion(getServices());
        if (prettyPrint) {
        	Gson gson = new GsonBuilder().setPrettyPrinting().create();
        	model.put(Sjm.RES, gson.toJson(mmsVersion));
        } else {
        	model.put(Sjm.RES, mmsVersion);
        }
        status.setCode(responseStatus.getCode());

        printFooter(user, logger, timer);

        return model;
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        // TODO Auto-generated method stub
        return false;
    }

}
