/*******************************************************************************
 * Copyright (c) <2013>, California Institute of Technology ("Caltech"). U.S. Government sponsorship
 * acknowledged.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. - Neither the name of Caltech nor its operating
 * division, the Jet Propulsion Laboratory, nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package gov.nasa.jpl.view_repo.webscripts;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

import gov.nasa.jpl.mbee.util.Timer;
import gov.nasa.jpl.view_repo.util.Sjm;
import gov.nasa.jpl.view_repo.util.WorkspaceNode;

public class ProductGet extends AbstractJavaWebScript {
	Logger logger = Logger.getLogger(ProductGet.class);

    public ProductGet() {
        super();
    }

    public ProductGet(Repository repositoryHelper, ServiceRegistry registry) {
        super(repositoryHelper, registry);
    }

    @Override
    protected boolean validateRequest(WebScriptRequest req, Status status) {
        String productId = AbstractJavaWebScript.getIdFromRequest(req);

        return checkRequestVariable(productId, "id");
    }

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        ProductGet instance = new ProductGet(repository, getServices());
        return instance.executeImplImpl(req, status, cache);
    }

    @Override
    protected Map<String, Object> executeImplImpl(WebScriptRequest req, Status status, Cache cache) {
        String user = AuthenticationUtil.getFullyAuthenticatedUser();
        printHeader(user, logger, req);
        Timer timer = new Timer();

        Map<String, Object> res = new HashMap<>();
        JSONObject model = new JSONObject();

        if (validateRequest(req, status)) {
            String projectId = getProjectId(req);
            WorkspaceNode workspace = getWorkspace(req);
            boolean recurse = getBooleanArg(req, "recurse", true);
            String productId = getIdFromRequest(req);
            boolean gettingDisplayedElements = isDisplayedElementRequest(req);

            boolean gettingContainedViews = false;
            if (!gettingDisplayedElements) {
                gettingContainedViews = isContainedViewRequest(req);
            }

            JSONArray elements = ViewGet.handleGetElement(productId, projectId, workspace, gettingDisplayedElements,
                            gettingContainedViews, recurse);
            model.put(gettingDisplayedElements ? Sjm.ELEMENTS : (gettingContainedViews ? "views" : "products"), elements);

            res.put("res", model);

            status.setCode(responseStatus.getCode());
        } else {
            model.put("products", new JSONArray());
        }

        printFooter(user, logger, timer);

        return res;
    }

}
