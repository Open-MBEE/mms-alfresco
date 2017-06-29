*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory

*** Test Cases ***
CreateCollaborator
	[Documentation]		"Create Collaborator User"
	[Tags]				1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateCollaborator.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateConsumer
	[Documentation]		"Create Consumer User"
	[Tags]				1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateConsumer.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateManager
	[Documentation]		"Create Manager User"
	[Tags]				1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateManager.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateLoser
	[Documentation]		"Create Loser User"
	[Tags]				1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateLoser.json
	${result} =			Post		url=${ROOT}/api/people		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
