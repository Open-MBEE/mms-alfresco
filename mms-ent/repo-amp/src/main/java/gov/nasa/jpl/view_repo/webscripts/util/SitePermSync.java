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

package gov.nasa.jpl.view_repo.webscripts.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteMemberInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.view_repo.util.Acm;
import gov.nasa.jpl.view_repo.util.EmsScriptNode;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;
import gov.nasa.jpl.view_repo.webscripts.AbstractJavaWebScript;

/**
 *
 */

public class SitePermSync extends AbstractJavaWebScript{

    public SitePermSync() {
        super();
    }

    public SitePermSync(Repository repositoryHelper, ServiceRegistry registry){
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        if(!checkRequestContent ( req )) {
            return false;
        }

        String workspaceId = req.getServiceMatch().getTemplateVars().get(REF_ID);
        return checkRequestVariable(workspaceId, REF_ID);
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache){
        SitePermSync instance = new SitePermSync(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    /**
     * Wrapped executeImpl so this can run in its own scopes.
     * @param req
     * @param status
     * @param cache
     * @return
     */
    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<>();
        /*
        WorkspaceNode ws = this.getWorkspace( req );
        List<SiteInfo> sites = services.getSiteService().listSites(null);

        JSONArray msgs = new JSONArray();

        for (SiteInfo siteInfo : sites ) {
            EmsScriptNode siteNode = new EmsScriptNode(siteInfo.getNodeRef(), services, response);
            NodeRef sitePkgNR = (NodeRef) siteNode.getNodeRefProperty(Acm.ACM_SITE_PACKAGE, null, ws);
            if (sitePkgNR == null) {
                msgs.put( "Could not find site package for site: " + siteNode.getName() );
            } else {
                EmsScriptNode sitePkg = new EmsScriptNode(sitePkgNR, services, response);
                updatePermissions(siteInfo, sitePkg, ws);
            }
        }

        JSONObject json = new JSONObject();
        try {
            json.put( "msgs", msgs );
            model.put( "res", json.toString() );
            status.setCode( HttpServletResponse.SC_ACCEPTED );
        } catch ( JSONException e ) {
            model.put("res", "Error creating JSON output");
            status.setCode( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        }
*/
        return model;
    }

}


