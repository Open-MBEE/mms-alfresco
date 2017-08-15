*** Settings ***
Documentation    Testing Workspaces Tags on Master
Resource        ../resources.robot

*** Test Cases ***
#PostNewImage
#	[Documentation]		"Post elements to the newly created project and organization."
#	[Tags]				crud		critical		0401
#	${post_json} =		Get File		${CURDIR}/assets/.json
#	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}

#PostNewVersionOfImage
#	[Documentation]		"Post elements to the newly created project and organization."
#	[Tags]				crud		critical		0401
#	${post_json} =		Get File		${CURDIR}/assets/.json
#	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
#GetImage
#	[Documentation]		"get /projects/PA/refs/master/elements/test_history_element/history"
#	[Tags]				branches		critical		0402
#	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element/history		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${commit0} =		Commit Naught		${result.json()}
#	Set Global Variable	  ${commit0}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}

#PostNewBranch
#	[Documentation]		"Post new branch to PA"
#	[Tags]				branches		critical		0905
#	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewBranchToPA.json
#	${result} =			Post		url=${ROOT}/projects/PA/refs		data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
#
#PostNewMount
#	[Documentation]		"Create a project (ID: PB) under the organization with (ID: initorg).  Symbolically PB."
#	[Tags]				mounts		critical		0201
#	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPB.json
#	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
