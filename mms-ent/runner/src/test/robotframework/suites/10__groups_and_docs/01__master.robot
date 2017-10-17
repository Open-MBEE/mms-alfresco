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

GetDoumentByGroupIdSitePackageB
	[Documentation]		"Get documents by groupId from PA"
	[Tags]				groups		critical		1004
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents?groupId=SitePackageB		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdSitePackageD
	[Documentation]		"Get documents by groupId from PA"
	[Tags]				groups		critical		1005
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents?groupId=SitePackageD		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDoumentByGroupIdProjectId
	[Documentation]		"Get documents by groupId from PA"
	[Tags]				groups		critical		1005
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/documents?groupId=PA		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

