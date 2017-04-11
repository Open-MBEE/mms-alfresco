*** Settings ***
Documentation    Permissions Testing
Resource        ../resources.robot

#TODO: INCLUDE SETUP FOR CREATING USERS

*** Test Cases ***
NoneRead
	[Documentation]		"Regression Test: 620. Read element with user None"
	[Tags]				620
	${result} =			Get		url=http://None:password@localhost:8080/alfresco/service/workspaces/master/elements/y?extended=true
#	Should Be Equal		${result.status_code}		${404}
#    TODO: It's returning a 401 error instead of 404 which seems okay since it's unauthorized access
	Should Be Equal		${result.status_code}		${401}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

NoneDelete
	[Documentation]		"Regression Test: 621. Delete element with user None"
	[Tags]				621
	${result} =			Delete		url=http://None:password@localhost:8080/alfresco/service/workspaces/master/elements/y
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 sysmlId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

NoneUpdate
	[Documentation]		"Regression Test: 622. Update element with user None"
	[Tags]				622
	${post_json} =		Get File	 NewJsonData/${TEST_NAME}.json
	${result} =			Put    url=http://None:password@localhost:8080/alfresco/service/workspaces/master/elements     data=${post_json}.json       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

NoneCreate
	[Documentation]		"Regression Test: 623. Create element with user None"
	[Tags]				623
	${post_json} =		Get File	 NewJsonData/${TEST_NAME}.json
	${result} =			Post        url=${ROOT}/workspaces/master/elements?extended=true      data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CollaboratorRead
	[Documentation]		"Regression Test: 624. Read element with user Collaborator"
	[Tags]				624
	${result} =			Get		url=${ROOT}/workspaces/master/elements/y?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CollaboratorUpdate
	[Documentation]		"Regression Test: 625. Update element with user Collaborator"
	[Tags]				625
	${post_json} =		Get File	 NewJsonData/${TEST_NAME}.json
	${result} =			Put         url=${ROOT}/workspaces/master/elements     data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CollaboratorCreate
	[Documentation]		"Regression Test: 626. Create element with user Collaborator"
	[Tags]				626
	${post_json} =		Get File	 NewJsonData/${TEST_NAME}.json
	${result} =		    Post        url=${ROOT}/workspaces/master/elements?extended=true     data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CollaboratorDelete
	[Documentation]		"Regression Test: 627. Delete element with user Collaborator"
	[Tags]				627
	${result} =			Delete		url=${ROOT}/workspaces/master/elements/y
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 sysmlId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CollaboratorResurrect
	[Documentation]		"Regression Test: 628. Resurrect element with user Collaborator"
	[Tags]				628
	${post_json} =      Get File        ../JsonData/y.json
	${result} =			Put         url=${ROOT}/workspaces/master/elements      data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConsumerRead
	[Documentation]		"Regression Test: 630. Read element with user Consumer"
	[Tags]				630
	${result} =			Get		url=${ROOT}/workspaces/master/elements/y?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConsumerUpdate
	[Documentation]		"Regression Test: 631. Update element with user Consumer"
	[Tags]				631
	${post_json} =      Get File        ../JsonData/y.json
	${result} =			Put             url=${ROOT}/workspaces/master/elements      data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConsumerCreate
	[Documentation]		"Regression Test: 632. Create element with user Consumer"
	[Tags]				632
	${post_json} =      Get File        NewJsonData/ConsumerCreate.json
	${result} =			Post        url=${ROOT}/workspaces/master/elements?extended=true          data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConsumerDelete
	[Documentation]		"Regression Test: 633. Delete element with user Consumer"
	[Tags]				633
	${result} =			Delete		url=${ROOT}/workspaces/master/elements/y
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 sysmlId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConsumerResurrect
	[Documentation]		"Regression Test: 634. Resurrect element with user Consumer"
	[Tags]				634
	${post_json} =      Get File        ../JsonData/y.json
	${result} =			Put            url=${ROOT}/workspaces/master/elements       data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${403}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


