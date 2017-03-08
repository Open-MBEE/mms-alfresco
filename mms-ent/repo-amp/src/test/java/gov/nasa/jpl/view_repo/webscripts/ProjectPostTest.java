///*******************************************************************************
// * Copyright (c) <2013>, California Institute of Technology ("Caltech").  
// * U.S. Government sponsorship acknowledged.
// * 
// * All rights reserved.
// * 
// * Redistribution and use in source and binary forms, with or without modification, are 
// * permitted provided that the following conditions are met:
// * 
// *  - Redistributions of source code must retain the above copyright notice, this list of 
// *    conditions and the following disclaimer.
// *  - Redistributions in binary form must reproduce the above copyright notice, this list 
// *    of conditions and the following disclaimer in the documentation and/or other materials 
// *    provided with the distribution.
// *  - Neither the name of Caltech nor its operating division, the Jet Propulsion Laboratory, 
// *    nor the names of its contributors may be used to endorse or promote products derived 
// *    from this software without specific prior written permission.
// * 
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
// * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
// * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER  
// * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
// * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
// * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
// * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
// * POSSIBILITY OF SUCH DAMAGE.
// ******************************************************************************/
//
//package gov.nasa.jpl.view_repo.webscripts;
//
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import javax.servlet.http.HttpServletResponse;
//
//import org.alfresco.repo.security.authentication.AuthenticationUtil;
//import org.alfresco.service.ServiceRegistry;
//import org.alfresco.service.cmr.site.SiteInfo;
//import org.alfresco.service.cmr.site.SiteService;
//import org.alfresco.util.ApplicationContextHelper;
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.junit.After;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.springframework.context.ApplicationContext;
//import org.springframework.extensions.surf.util.InputStreamContent;
//import org.springframework.extensions.webscripts.Cache;
//import org.springframework.extensions.webscripts.Match;
//import org.springframework.extensions.webscripts.Status;
//import org.springframework.extensions.webscripts.WebScriptRequest;
//import org.springframework.extensions.webscripts.servlet.WebScriptServletRequest;
//
//public class ProjectPostTest {
//    private static final String ADMIN_USER_NAME = "admin";
//
//    protected static ProjectPost projectPostComponent;
//    protected static ApplicationContext applicationContext;
//    
//    private static final String SITE_NAME = "europa";
//    
//    private static SiteService siteService;
//
//    @BeforeClass
//    public static void setUpBeforeClass() throws Exception {
//        initAppContext();
//        initAlfresco();
//    }
//
//    @AfterClass
//    public static void tearDownAfterClass() throws Exception {
//    }
//
//    @Before
//    public void setUp() throws Exception {
//        // remove a site
//        
//    }
//
//    @After
//    public void tearDown() throws Exception {
//    }
//    
//    
//    
//
//    @Test
//    public void nominalTest() throws JSONException {
//        // build up all the request data
//        Map<String, String> templateVars = new HashMap<String, String>();
//        templateVars.put("siteName", SITE_NAME);
//        templateVars.put("projectId", "123456");
//        
//        JSONObject json = new JSONObject();
//        json.append("name", "Clipper");
//        
//        Map<String, String> parameters = new HashMap<String, String>();
//        parameters.put("fix", "true");
//        parameters.put("delete", "false");
//
//        Status status = new Status();
//
//        // create mock and add stubs
//        WebScriptRequest request = mockRequestAndStubs(templateVars, parameters, json);
//
//        // do actual tests
//        Map<String, Object> model;
//
//        // call with no site built
//        model = projectPostComponent.executeImpl(request, status, new Cache());
//        System.out.println(model);
//        assertEquals("Site not found", HttpServletResponse.SC_NOT_FOUND, status.getCode());
//
//        // create the site
//        siteService.createSite(SITE_NAME, SITE_NAME, SITE_NAME, SITE_NAME, true);
//
//        // call the web service, don't force fix
//        parameters.put("fix", "false");
//        model = projectPostComponent.executeImpl(request, status, new Cache());
//        assertEquals("Model folder not found", HttpServletResponse.SC_NOT_FOUND, status.getCode());
//
//        // call the web service, force fix
//        parameters.put("fix", "true");
//        model = projectPostComponent.executeImpl(request, status, new Cache());
//        assertEquals("Project not created", HttpServletResponse.SC_OK, status.getCode());
//        
//        // call web service again with same project
//        model = projectPostComponent.executeImpl(request, status, new Cache());
//        assertEquals("Project already created", HttpServletResponse.SC_FOUND, status.getCode());
//        
//        // call web service with bad JSON
//        json = new JSONObject();
//        model = projectPostComponent.executeImpl(request, status, new Cache());
//        assertEquals("Bad JSON supplied", HttpServletResponse.SC_BAD_REQUEST, status.getCode());
//    }
//
//    protected WebScriptRequest mockRequestAndStubs(Map<String, String> templateVars, Map<String, String> parameters, JSONObject json) {
//        WebScriptRequest request = mock(WebScriptServletRequest.class);
//        
//        Match match = new Match("", templateVars, "");
//        when(request.getServiceMatch()).thenReturn(match);
//        when(request.getContent()).thenReturn(new InputStreamContent(null, null, null));
//        when(request.parseContent()).thenReturn(json);
//        
//        for (String key: parameters.keySet()) {
//            when(request.getParameter(key)).thenReturn(parameters.get(key));
//        }
//        
//        return request;
//    }
//
//    /**
//     * Initialize the application context
//     */
//    private static void initAppContext() {
//        ApplicationContextHelper.setUseLazyLoading(false);
//        ApplicationContextHelper.setNoAutoStart(true);
//        applicationContext = ApplicationContextHelper.getApplicationContext(new String[] { "classpath:alfresco/application-context.xml" });
//        
//        projectPostComponent = (ProjectPost) applicationContext.getBean("webscript.gov.nasa.jpl.javawebscripts.project.post");
//        
//        AuthenticationUtil.setFullyAuthenticatedUser(ADMIN_USER_NAME);
//    }
//    
//    /**
//     * Initialize the Alfresco setup
//     */
//    private static void initAlfresco() {
//        ServiceRegistry services = (ServiceRegistry)applicationContext.getBean("ServiceRegistry");
//        siteService = services.getSiteService();
//        SiteInfo site = siteService.getSite(SITE_NAME);
//        if (site != null) {
//            siteService.deleteSite(SITE_NAME);
//        }
//    }
//}
