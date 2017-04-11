*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory


*** Test Cases ***
InitializeOrganization
	[Documentation]		"Regression Test: 10. Create a project and site"
	[Tags]				1
	${post_json} =		Get File	 NewJsonData/InitializeOrganization.json
	${result} =			Post		url=${ROOT}/orgs		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostSite
	[Documentation]		"Regression Test: 10. Create a project and site"
	[Tags]				10
	${post_json} =		Get File	 ../JsonData/postSite.json
	${result} =			Post		url=${ROOT}/orgs/vetest/projects    data=${post_json}		headers=&{REQ_HEADER}
	Post		        url=${ROOT}/orgs/vetest/projects                data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementsNew
	[Documentation]		"Regression Test: 20. Post elements to the master branch"
	[Tags]				20
	${post_json} =		Get File	 ../JsonData/elementsNew.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp     
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementsBadOwners
	[Documentation]		"Regression Test: 21. Post elements to the master branch that have owners that cant be found"
	[Tags]				21
	${post_json} =		Get File	 ../JsonData/badOwners.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostViews
	[Documentation]		"Regression Test: 30. Post views"
	[Tags]				30
	${post_json} =		Get File	 ../JsonData/newViews.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostProducts
	[Documentation]		"Regression Test: 40. Post products"
	[Tags]				40
	${post_json} =		Get File	 ../JsonData/products.json
	Sleep				2s
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSites
	[Documentation]		"Regression Test: 45. Get sites"
	[Tags]				45
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/sites
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProject
	[Documentation]		"Regression Test: 50. Get project"
	[Tags]				50
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/sites/europa/projects/123456
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId    _elasticId      _created        _modified
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


GetProjects
	[Documentation]		"Regression Test: 51. Get all projects for master"
	[Tags]				51
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/projects
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId    _elasticId      _created        _modified
	${is_empty} =       Check If Elements Is Empty      ${result.json()}
    Run Keyword If      ${is_empty} == 'TRUE'       ${result} =			Get		url=${ROOT}/projects/123456/refs/master/projects
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsRecursively
	[Documentation]		"Regression Test: 60. Get all elements recursively"
	[Tags]				60
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?recurse=true&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsDepth0
	[Documentation]		"Regression Test: 61. Get elements recursively depth 0"
	[Tags]				61
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?depth=0&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsDepth1
	[Documentation]		"Regression Test: 62. Get elements recursively depth 1"
	[Tags]				62
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?depth=1&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsDepth2
	[Documentation]		"Regression Test: 63. Get elements recursively depth 2"
	[Tags]				63
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?depth=2&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsDepthAll
	[Documentation]		"Regression Test: 64. Get elements recursively depth -1"
	[Tags]				64
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?depth=-1&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementsDepthInvalid
	[Documentation]		"Regression Test: 65. Get elements recursively depth invalid"
	[Tags]				65
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?depth=invalid
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#DOESN'T WORK
GetElementsConnected
	[Documentation]		"Regression Test: 66. Get elements that are connected"
	[Tags]				66
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/300?recurse=true&connected=true?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#DOESN'T WORK
GetElementsRelationship
	[Documentation]		"Regression Test: 67. Get elements that have relationship DirectedRelationship, starting with 302"
	[Tags]				67
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/303?recurse=true&connected=true&relationship=DirectedRelationship
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#TODO: DITTO
GetViewElements
	[Documentation]		"Regression Test: 80. Get view elements"
	[Tags]				80
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/views/_18_0_2_b4c02e1_1435176831233_484911_180279_vc_expression/elements
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProduct
	[Documentation]		"Regression Test: 90. Get product"
	[Tags]				90
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/sites/europa/products/301?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSearch
	[Documentation]		"Regression Test: 110. Get search"
	[Tags]				110
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/search?keyword=some*
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

Delete6666
	[Documentation]		"Regression Test: 120. Delete element 6666"
	[Tags]				120
	${result} =			Delete		url=${ROOT}/projects/123456/refs/master/elements/6666
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				${DELETE_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostContentModelUpdates
	[Documentation]		"Regression Test: 460. Post content model udpates for sysml 2.0"
	[Tags]				460
	${post_json} =		Get File	 ../JsonData/contentModelUpdates.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#TODO: Doesn't have to pass
PostDuplicateSysmlNames1
	[Documentation]		"Regression Test: 470. Post a element that will be used in the next test to generate a error"
	[Tags]				470
	${post_json} =		Get File	 ../JsonData/cmed416_1.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

#TODO: Doesn't have to pass
PostDuplicateSysmlNames2
	[Documentation]		"Regression Test: 480. Post a element with the same type, sysml name, and parent as the previous test to generate at error"
	[Tags]				480
	${post_json} =		Get File	 ../JsonData/cmed416_2.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List     commitId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostModelForDowngrade
	[Documentation]		"Regression Test: 500. Post model for downgrade test"
	[Tags]				500
	${post_json} =		Get File	 ../JsonData/productsDowngrade.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostModelForViewDowngrade
	[Documentation]		"Regression Test: 510. Post model for view downgrade"
	[Tags]				510
	${post_json} =		Get File	 ../JsonData/viewDowngrade.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostModelForElementDowngrade
	[Documentation]		"Regression Test: 520. Post model for element downgrade"
	[Tags]				520
	${post_json} =		Get File	 ../JsonData/elementDowngrade.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNullElements
	[Documentation]		"Regression Test: 640. Post elements to the master branch with null properties"
	[Tags]				640
	${post_json} =		Get File	 ../JsonData/nullElements.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

TestResurrection1
	[Documentation]		"Regression Test: 660. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	[Tags]				660
	${post_json} =		Get File	 ../JsonData/resurrectParents.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteParents
	[Documentation]		"Regression Test: 661. Delete parents"
	[Tags]				661
	${result} =			Delete		url=${ROOT}/projects/123456/refs/master/elements/parentToDelete1
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				${DELETE_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

TestResurrection2
	[Documentation]		"Regression Test: 662. Post elements for resurrection of parents testing.  Has two parents that will be resurrected."
	[Tags]				662
	${post_json} =		Get File	 ../JsonData/resurrectParentsChild.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

TestGetAfterResurrection
	[Documentation]		"Regression Test: 663. Performs a recursive get to make sure the ownedChildren were property set after resurrection."
	[Tags]				663
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/123456?recurse=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteUpdateAddMaster
	[Documentation]		"Regression Test: 807. Delete update_add_gg"
	[Tags]				807
	${result} =			Delete		url=${ROOT}/projects/123456/refs/master/elements/update_add_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				${DELETE_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteDeleteAddMaster
	[Documentation]		"Regression Test: 808. Delete delete_add_gg"
	[Tags]				808
	${result} =			Delete		url=${ROOT}/projects/123456/refs/master/elements/delete_add_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				${DELETE_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementsMatrix2
	[Documentation]		"Regression Test: 809. Post elements to the master branch for glom matrix testing"
	[Tags]				809
	${post_json} =		Get File	 ../JsonData/elementsMatrix2.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestPostSite
	[Documentation]		"Regression Test: 10200. Create a VE test project and site"
	[Tags]				10200
	${post_json} =		Get File	 NewJsonData/${TEST_NAME}.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/sites/vetest/projects?createSite=true     data=${post_json}       headers=&{REQ_HEADER}
	Post		        url=${ROOT}/projects/123456/refs/master/sites/vetest/projects?createSite=true     data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestPost
	[Documentation]		"Regression Test: 10201. Post Hierarchy test project elements to the master branch"
	[Tags]				10201
	${post_json} =		Get File	 ../JsonData/viewHierarchy.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestPostConstraints
	[Documentation]		"Regression Test: 10202. Post Hierarchy test view constraints"
	[Tags]				10202
	${post_json} =		Get File	 ../JsonData/viewHierarchyConstraints.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestAddToC
	[Documentation]		"Regression Test: 10203. Hierarchy test adding to view c"
	[Tags]				10203
	${post_json} =		Get File	 ../JsonData/viewHierarchyAddToC.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestAddToA
	[Documentation]		"Regression Test: 10204. Hierarchy test adding to view A"
	[Tags]				10204
	${post_json} =		Get File	 ../JsonData/viewHierarchyAddToA.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestReorderB
	[Documentation]		"Regression Test: 10205. Hierarchy test reordering B"
	[Tags]				10205
	${post_json} =		Get File	 ../JsonData/viewHierarchyReorderB.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestMoveF
	[Documentation]		"Regression Test: 10206. Hierarchy test moving view F from view B to view D"
	[Tags]				10206
	${post_json} =		Get File	 ../JsonData/viewHierarchyMoveF.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

HierarchyTestDeleted
	[Documentation]		"Regression Test: 10207. Hierarchy test checking that the move deleted association and properties"
	[Tags]				10207
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 YY_		 MM_		 DD_		 HH_		 _owner		 qualifiedId		 qualifiedName		 MMS_		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
