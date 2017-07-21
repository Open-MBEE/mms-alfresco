*** Settings ***
Documentation    Testing branches and history
Resource        ../resources.robot

*** Test Cases ***

PostNewElementsToPAHistory
	[Documentation]		"Post elements to PA"
	[Tags]				B1
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostElementsToPAHistory.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementHistoryFromPA
	[Documentation]		"get /projects/PA/refs/master/elements/test_history_element/history"
	[Tags]				B2
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element/history		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${commit0} =		Commit Naught		${result.json()}
	Set Global Variable      ${commit0}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostUpdateToElementHistoryInPA
	[Documentation]		"Post elements to PA"
	[Tags]				B3
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostUpdateToElementHistoryInPA.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementHistoryFromPAWithUpdate
	[Documentation]		"get /projects/PA/refs/master/elements/test_history_element/history"
	[Tags]				B4
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element/history		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${commit1} =		Commit Naught		${result.json()}
	Set Global Variable      ${commit1}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewBranchToPA
	[Documentation]		"Post new branch to PA"
	[Tags]				B5
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostNewBranchToPA.json
	${result} =			Post		url=${ROOT}/projects/PA/refs		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostUpdateToElementMasterInPA
	[Documentation]		"Post update to element in master"
	[Tags]				B6
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostUpdateToElementMasterInPA.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostUpdateToElementBranchInPA
	[Documentation]		"Post update to element in newbranch"
	[Tags]				B7
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostUpdateToElementBranchInPA.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/newbranch/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementHistoryFromPAOnMaster
	[Documentation]		"get history on master"
	[Tags]				B8
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element/history		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${commit2} =		Commit Naught		${result.json()}
	Set Global Variable      ${commit2}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementHistoryFromPAOnNewBranch
	[Documentation]		"get history on branch"
	[Tags]				B9
	${result} =			Get		url=${ROOT}/projects/PA/refs/newbranch/elements/test_history_element/history		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${commit3} =		Commit Naught		${result.json()}
	Set Global Variable      ${commit3}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
