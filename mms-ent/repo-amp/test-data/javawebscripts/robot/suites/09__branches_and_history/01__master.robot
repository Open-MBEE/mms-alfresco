*** Settings ***
Documentation    Testing branches and history
Resource        ../resources.robot

*** Test Cases ***

PostNewElementsToPAHistory
	[Documentation]		"Post elements to PA"
	[Tags]				B1
	${post_json} =		Get File	    JsonData/PostElementsToPAHistory.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


GetElementHistoryFromPA
	[Documentation]		"get /projects/PA/refs/master/elements/test_history_element/history"
	[Tags]				B2
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/test_history_element/history		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
