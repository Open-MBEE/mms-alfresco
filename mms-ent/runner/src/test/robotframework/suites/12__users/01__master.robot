*** Settings ***
Documentation	Testing CRUD Operations on Master
Resource		../resources.robot

*** Test Cases ***
PostManagerProfile
	[Documentation]		"Create a new user profile"
	[Tags]				crud		critical		1201
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
	[Tags]				crud		critical		1202
	${result} =			Get		url=http://Manager:password@${SERVER}/alfresco/service/users/Manager/profile		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
GetMangerProfileAsCollaborator
	[Documentation]		"Get a profile from another user, should fail."
	[Tags]				crud		critical		1203
	${result} =			Get		url=http://Collaborator:password@${SERVER}/alfresco/service/users/Manager/profile		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${401}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
PostManagerProfileAsCollaborator
	[Documentation]		"Create a new user profile"
	[Tags]				crud		critical		1201
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/PostManagerProfile.json
	${result} =			Post		url=http://Collaborator:password@${SERVER}/alfresco/service/users/Manager/profile			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${401}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
# Two tests for 401 (from user to another user), that admin users can?
# That after a create and update of the same document there's only one document
# check output for 200 and failure
