*** Settings ***
Documentation    Testing Projects
Resource        ../resources.robot

*** Test Cases ***
#TODO: Won't work
SolveConstraint
	[Documentation]		"Regression Test: 255. Post expressions with a constraint and solve for the constraint."
	[Tags]				255
	${post_json} =		Get File	 ../JsonData/expressionElementsNew.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements?fix=true&extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#TODO: Won't work \/
PostDemo1
	[Documentation]		"Regression Test: 260. Post data for demo 1 of server side docgen"
	[Tags]				260
	${post_json} =		Get File	 ../JsonData/BluCamNameListExpr.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlid		 qualifiedId		 message
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

Demo1
	[Documentation]		"Regression Test: 270. Server side docgen demo 1"
	[Tags]				270
	${result} =			Get		url=${ROOT}/workspaces/master/views/_17_0_2_3_e610336_1394148311476_17302_29388
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostDemo2
	[Documentation]		"Regression Test: 280. Post data for demo 2 of server side docgen"
	[Tags]				280
	${post_json} =		Get File	 ../JsonData/BLUCamTest.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 MMS_		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#TODO: Won't work
Demo2
	[Documentation]		"Regression Test: 290. Server side docgen demo 2"
	[Tags]				290
	${result} =			Get		url=${ROOT}/workspaces/master/views/_17_0_2_3_e610336_1394148233838_91795_29332
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostSiteAndProject3
	[Documentation]		"Regression Test: 292. Create a site and a project for demo 3 of server side docgen"
	[Tags]				292
	${result} =			Post		url=${ROOT}/workspaces/master/sites/demo3site/projects?createSite=true      data='{}'       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#TODO: Won't work
PostDemo3
	[Documentation]		"Regression Test: 293. Post data for demo 3 of server side docgen"
	[Tags]				293
	${post_json} =		Get File	 ../JsonData/bike.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/demo3site/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 MMS_		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostViewDemo3
	[Documentation]		"Regression Test: 294. Post view data for demo 3 of server side docgen"
	[Tags]				294
	${post_json} =		Get File	 ../JsonData/connectorView.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/demo3site/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 MMS_		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#TODO: Won't work
Demo3
	[Documentation]		"Regression Test: 295. Server side docgen demo 3"
	[Tags]				295
	${result} =			Get		url=${ROOT}/workspaces/master/views/_17_0_2_3_e610336_1394148311476_17302_29388_X
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#TODO: MOVE TO MASTER (UNTIL STOP)
GetSites2
	[Documentation]		"Regression Test: 300. Get all the sites for a workspace"
	[Tags]				300
	${result} =			Get		url=${ROOT}/workspaces/master/sites
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProductViews
	[Documentation]		"Regression Test: 310. Get all views for a product"
	[Tags]				310
	${result} =			Get		url=${ROOT}/workspaces/master/products/301/views
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementX
	[Documentation]		"Regression Test: 320. Post element to the master branch/site"
	[Tags]				320
	${post_json} =		Get File	 ../JsonData/x.json
	${result} =			Post		url=${ROOT}/workspaces/master/sites/europa/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


UpdateProject
	[Documentation]		"Regression Test: 330. Update a project"
	[Tags]				330
	${result} =			Post		${ROOT}/workspaces/master/projects
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#TODO: STOP



GetProjectOnly
	[Documentation]		"Regression Test: 340. Get project w/o specifying the site"
	[Tags]				340
	${result} =			Get		url=${ROOT}/workspaces/master/projects/123456
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


PostElementsWithProperites
	[Documentation]		"Regression Test: 670. Post elements for the next several tests"
	[Tags]				670
	${post_json} =		Get File	 ../JsonData/elementsWithProperties.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

