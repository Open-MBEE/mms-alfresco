*** Settings ***
Documentation	Testing groups and docs
Resource		../resources.robot

*** Test Cases ***

PostNewElementsToPAGroupsAndDocs
	[Documentation]		"Post elements to PA"
	[Tags]				groups		critical		1001
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewElementsToPAGroupsAndDocs.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


GetGroupsFromPA
	[Documentation]		"get groups"
	[Tags]				groups		critical		1002
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/groups		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDocumentsFromPA
	[Documentation]		"get documents"
	[Tags]				groups		critical		1003
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNestedDocument
	[Documentation]		"Post nested document to PA"
	[Tags]				groups		critical		1004
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNestedDocument.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDocumentsAndNestedDocsFromPA
	[Documentation]		"Get all documents from PA"
	[Tags]				groups		critical		1005
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupId
	[Documentation]		"Get documents by groupId from PA"
	[Tags]				groups		critical		1006
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents/group1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdWithDepth1
	[Documentation]		"Get documents by groupId and depth at 1 from PA"
	[Tags]				groups		critical		1007
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents/group1?depth=1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdWithDepth2
	[Documentation]		"Get documents by groupId and depth at 2 from PA"
	[Tags]				groups		critical		1008
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents/group1?depth=2		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdWithDepth3
	[Documentation]		"Get documents by groupId and depth at 3 from PA"
	[Tags]				groups		critical		1009
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents/group1?depth=3		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdWithInvalidDepth
	[Documentation]		"Get documents by groupId and an invalid depth."
	[Tags]				groups		critical		1010
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents/group1?depth=invalidDepth		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${500}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
