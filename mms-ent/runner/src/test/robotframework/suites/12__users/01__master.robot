*** Settings ***
Documentation	Testing CRUD Operations on Master
Resource		../resources.robot

*** Test Cases ***
PostManagerProfile
	[Documentation]		"Create a new user profile"
	[Tags]				crud		critical		1201
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/PostManagerProfile.json
	${result} =			Post		url=http://Manager:password@${SERVER}/alfresco/service/projects/users/Manager/profile			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

