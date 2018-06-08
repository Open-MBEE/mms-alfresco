*** Settings ***
Documentation	Testing CRUD Operations on Master
Resource		../resources.robot

*** Test Cases ***
PostManagerProfile
	[Documentation]		"Create a new user profile"
	[Tags]				crud		critical		1301
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/PostManagerProfile.json
	${result} =			Post		url=http://Manager:password@${SERVER}/alfresco/service/users/Manager/profile			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
GetManagerProfile
	[Documentation]		"Get the Manager Profile."
	[Tags]				crud		critical		1302
	${result} =			Get		url=http://Manager:password@${SERVER}/alfresco/service/users/Manager/profile		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
GetMangerProfileAsCollaborator
	[Documentation]		"Get a profile from another user, should fail."
	[Tags]				crud		critical		1303
	${result} =			Get		url=http://Collaborator:password@${SERVER}/alfresco/service/users/Manager/profile		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${401}

PostManagerProfileAsCollaborator
	[Documentation]		"Create a new user profile"
	[Tags]				crud		critical		1304
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/PostManagerProfile.json
	${result} =			Post		url=http://Collaborator:password@${SERVER}/alfresco/service/users/Manager/profile			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${401}

PostUpdateToManagerProfile
	[Documentation]		"Update and check the response"
	[Tags]				crud		critical		1305
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/PostManagerProfileUpdate.json
	${result} =			Post		url=http://Manager:password@${SERVER}/alfresco/service/users/Manager/profile			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
