*** Settings ***
Documentation	Test cf relationships
Resource		../resources.robot

*** Test Cases ***

PostViewWithCfs
	[Documentation]		"Post entire view"
	[Tags]				cfs		critical		0501
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostViewWithCfs.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetCfIdsFromView
	[Documentation]		"Get all cfed ids from view posted, should include all ids from post "
	[Tags]				cfs		critical		0502
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/cfview/cfids		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ConvertHtmlToDocx
    [Documentation]     "Convert an HTML string to a docx"
    [Tags]              docx    critical        0503
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ConverHtmlToDocx.json
	${result} =         Post         url=${ROOT}/projects/PA/refs/master/documents/DocA/htmlToWord/ConverHtmlToDocx         data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		 _created		 _modified		 _elasticId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


