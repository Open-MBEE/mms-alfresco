/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech").
 * U.S. Government sponsorship acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of
 *    conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory,
 *    nor the names of its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.mbee.util.Utils;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.NodeUtil;

/**
 * Updates the LDAP group that is allowed to do workspace operations- create, merge, diff,
 * and delete.
 *
 * @author gcgandhi
 *
 */
public class UpdateWorkspaceLdapGroup extends AbstractJavaWebScript {
	static Logger logger = Logger.getLogger(UpdateWorkspaceLdapGroup.class);

    public UpdateWorkspaceLdapGroup() {
        super();
    }

    public UpdateWorkspaceLdapGroup(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }


    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if (!checkRequestContent(req)) {
            return false;
        }

        if (!checkRequestVariable(req.getParameter( "ldapGroup" ), "ldapGroup")) {
            return false;
        }

        return true;
    }


    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        UpdateWorkspaceLdapGroup instance = new UpdateWorkspaceLdapGroup(repository, getServices());
        return instance.executeImplImpl(req,  status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> model = new HashMap<>();

        try {
            if (validateRequest(req, status)) {
                String ldapGroup = req.getParameter( "ldapGroup" );
                handleRequest(ldapGroup);
            }

        } catch (Exception e) {
            log(Level.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Internal error stack trace:\n %s \n",e.getLocalizedMessage());
            e.printStackTrace();
        }

        status.setCode(responseStatus.getCode());
        model.put("res", createResponseJson());

        printFooter(user, logger, timer);

        return model;
    }

    private void handleRequest(String ldapGroup) {

        EmsScriptNode branchPermNode = null;
        EmsScriptNode mmsFolder = null;

        if (Utils.isNullOrEmpty( ldapGroup )) {
            log(Level.ERROR, HttpServletResponse.SC_BAD_REQUEST, "Empty or null ldap group");
            return;
        }

        // to make sure no permission issues, run as admin
        String origUser = AuthenticationUtil.getRunAsUser();
        AuthenticationUtil.setRunAsUser( "admin" );

        // Place the node (branch_perms) to hold the LDAP group in CompanyHome/MMS:
        EmsScriptNode context = NodeUtil.getCompanyHome( services );

        if (context != null) {
            mmsFolder = context.childByNamePath( "MMS" );

            // Create MMS folder if needed:
            if (mmsFolder == null) {
                // TODO check write permissions on companyhome?
                mmsFolder = context.createFolder( "MMS" );
            }

            if (mmsFolder != null) {
                branchPermNode = mmsFolder.childByNamePath( "branch_perm" );

                // Create branch_perm node if needed:
                if (branchPermNode == null) {
                    if (mmsFolder.hasPermission( "Write" )) {
                        branchPermNode = mmsFolder.createNode( "branch_perm", "cm:content" );

                        if (branchPermNode == null) {
                            log(Level.ERROR,HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating branch permission node");
                        }
                    }
                    else {
                        log(Level.ERROR,HttpServletResponse.SC_FORBIDDEN, "No permissions to write to MMS folder: "+mmsFolder);
                    }
                }

                // Update branch_perm node:
                if (branchPermNode != null) {
                    branchPermNode.createOrUpdateAspect( "ems:BranchPerm" );
                    branchPermNode.createOrUpdateProperty( "ems:ldapGroup", ldapGroup);
                }

            }
            // mmsFolder is null:
            else {
                log(Level.ERROR,HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating MMS folder");
            }
        }
        // company home was not found:
        else {
            log(Level.ERROR,  HttpServletResponse.SC_INTERNAL_SERVER_ERROR,"Could not find companyhome");
        }

        // Save to cache:
        if (context != null) {
            context.getOrSetCachedVersion();
        }
        if (mmsFolder != null) {
            mmsFolder.getOrSetCachedVersion();
        }
        if (branchPermNode != null) {
            branchPermNode.getOrSetCachedVersion();
        }

        // make sure we're running back as the originalUser
        AuthenticationUtil.setRunAsUser( origUser );

    }

}
