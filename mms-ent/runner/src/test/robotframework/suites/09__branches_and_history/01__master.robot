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
# TODO write a method to compare commitN to itself on response or its name -- Are these tests irrelevant now?
GetCommitHistoryFromPAOnMasterC0
	[Documentation]		"check get element using commit 0 on master"
	[Tags]				B10
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element?commitId=${commit0}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

GetCommitHistoryFromPAOnMasterC2
	[Documentation]		"get element using commit 2 on master"
	[Tags]				B11
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element?commitId=${commit2}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

GetCommitHistoryFromPAOnNewBranchC0
	[Documentation]		"get element using commit 0 on new branch"
	[Tags]				B12
	${result} =			Get		url=${ROOT}/projects/PA/refs/newbranch/elements/test_history_element?commitId=${commit0}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

GetCommitHistoryFromPAOnNewBranchC3
	[Documentation]		"get element using commit 3 on new branch"
	[Tags]				B13
	${result} =			Get		url=${ROOT}/projects/PA/refs/newbranch/elements/test_history_element?commitId=${commit3}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

GetElementAtCommit
    [Documentation]     "Gets an element at a commit -- Should return element"
    [Tags]              B14
    ${element} =        Get    url=${ROOT}/projects/PA/refs/master/elements/300         headers=&{REQ_HEADER}
    ${commitId} =       Get Commit From Json       ${element.json()}
    ${result} =         Get     url=${ROOT}/projects/PA/refs/master/elements/300?commitId=${commitId}       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
    Should Be Equal     ${result.json()["elements"][0]["id"]}       ${element.json()["elements"][0]["id"]}
    Should Be Equal     ${result.json()["elements"][0]["_commitId"]}       ${element.json()["elements"][0]["_commitId"]}
    Should Be Equal     ${result.json()["elements"][0]["_modified"]}       ${element.json()["elements"][0]["_modified"]}

GetElementBeforeCommit
    [Documentation]     "Gets an element that exists before the commit. Grabs a commit that does not contain the element and requests for the element at that time. It should return the element at a previous commit than the one requested."
    [Tags]              B15
    ${element} =        Get    url=${ROOT}/projects/PA/refs/master/elements/300         headers=&{REQ_HEADER}
    ${commitId} =       Get Commit In Between Latest and Element        ${300}      ${element.json()["elements"][0]["_commitId"]}
    ${result} =         Get     url=${ROOT}/projects/PA/refs/master/elements/300?commitId=${commitId}       headers=&{REQ_HEADER}
    Should Be Equal     ${result.status_code}       ${200}
    # Tests to see that the element is infact the element at a different point, also verifies that the element is not the same element at the commit
    Should Not Be Equal     ${result.json()["elements"][0]["_commitId"]}        ${element.json()["elements"][0]["_commitId"]}
    Should Not Be Equal     ${result.json()["elements"][0]["_commitId"]}        ${commitId}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 id
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementAfterCommit
    [Documentation]     "Get an element that doesn't exist at the current commit. Should return a 404"
    [Tags]              B16
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/CreateElementAfterCommit.json
	${result} =			Post		    url=${ROOT}/projects/PA/refs/master/elements     data=${post_json}		headers=&{REQ_HEADER}
	# Grab an element with an older commitId
    ${element} =        Get    url=${ROOT}/projects/PA/refs/master/elements/300         headers=&{REQ_HEADER}
    ${result} =         Get     url=${ROOT}/projects/PA/refs/master/elements/ElementAfterCommit?commitId=${element.json()["elements"][0]["_commitId"]}       headers=&{REQ_HEADER}
    Should be Equal     ${result.status_code}       ${404}

GetElementAtInvalidCommit
    [Documentation]     "Try to get an element at an invalid commit."
    [Tags]              B17
    ${result} =         Get    url=${ROOT}/projects/PA/refs/master/elements/300?commitId=ThisIdShouldNotExistAtAll         headers=&{REQ_HEADER}
    Should be Equal     ${result.status_code}       ${404}


