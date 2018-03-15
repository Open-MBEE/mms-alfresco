*** Settings ***
Documentation	Testing CRUD Operations on Master
Resource		../resources.robot

*** Test Cases ***
GetRef
	[Documentation]		"Get a ref."
	[Tags]				crud		critical		1201
	${result} =			Get		url=${ROOT}/projects/PA/refs/newbranch		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
GetRefDiffParent
	[Documentation]		"Get a ref."
	[Tags]				crud		critical		1202
	${result} =			Get		url=${ROOT}/projects/PA/refs/pa_branch_2 		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


