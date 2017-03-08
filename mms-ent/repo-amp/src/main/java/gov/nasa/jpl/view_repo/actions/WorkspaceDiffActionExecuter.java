package gov.nasa.jpl.view_repo.actions;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Status;

/**
 * Handles workspace diffs in the background.
 *
 * @author gcgandhi
 *
 */
public class WorkspaceDiffActionExecuter extends ActionExecuterAbstractBase {

    /**
     * Injected variables from Spring configuration
     */
    private ServiceRegistry services;
    private Repository repository;

    private StringBuffer response;
    private Status responseStatus;
    private String jobStatus;

    static Logger logger = Logger.getLogger(WorkspaceDiffActionExecuter.class);

    public void setRepository(Repository rep) {
        repository = rep;
    }

    public void setServices(ServiceRegistry sr) {
        services = sr;
    }

    @Override
    protected void executeImpl( Action action, NodeRef actionedUponNodeRef ) {
    	// TODO: add implementation

        jobStatus = "Failed";
    }

    @Override
    protected void
            addParameterDefinitions( List< ParameterDefinition > paramList ) {
        // TODO Auto-generated method stub

    }

}