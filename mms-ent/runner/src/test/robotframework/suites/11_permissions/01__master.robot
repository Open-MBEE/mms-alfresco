*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory

*** Test Cases ***
CreateManager
	[Documentation]		"Create a Manager User"
	[Tags]				create 3
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateManager.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateCollaborator
	[Documentation]		"Create a Collaborator User"
	[Tags]				create 1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateCollaborator.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateConsumer
	[Documentation]		"Create a Consumer User"
	[Tags]				create 2
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateConsumer.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateLoser
	[Documentation]		"Create a Loser User"
	[Tags]				create 4
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateLoser.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ReadAsManager
    [Documentation]     "Read an existing element as Manager."
    [Tags]              read 3
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Get		url=http://Manager:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements/300		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ReadAsCollaborator
    [Documentation]     "Read an existing element as Collaborator."
    [Tags]              read 1
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Get		url=http://Collaborator:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements/300		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ReadAsConsumer
    [Documentation]     "Read an existing element as Consumer."
    [Tags]              read 2
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Get		url=http://Consumer:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements/300		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ReadAsLoser
    [Documentation]     "Read an existing element as Loser."
    [Tags]              read 4
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Get		url=http://None:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements/300		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${403}

UpdateAsManager
    [Documentation]     "Update an existing element as Manager."
    [Tags]              update 3
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=http://Manager:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateAsCollaborator
    [Documentation]     "Update an existing element as Collaborator."
    [Tags]              update 1
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=http://Collaborator:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateAsConsumer
    [Documentation]     "Update an existing element as Consumer."
    [Tags]              update 2
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=http://Consumer:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${403}
	Sleep				${POST_DELAY_INDEXING}

UpdateAsLoser
    [Documentation]     "Update an existing element as Loser."
    [Tags]              update 4
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=http://None:password@${SERVER}/alfresco/service/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${403}
	Sleep				${POST_DELAY_INDEXING}
