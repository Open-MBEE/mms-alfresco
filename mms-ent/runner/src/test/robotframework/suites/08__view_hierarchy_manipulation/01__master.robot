*** Settings ***
Documentation	Testing get element/elements/search across mounts
Resource		../resources.robot

*** Test Cases ***

PostNewElementsToPAHierarchy
	[Documentation]		"Post elements to PA"
	[Tags]				views		critical		0801
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPAHierarchy.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPBHierarchy
	[Documentation]		"Post elements to PB"
	[Tags]				views		critical		0802
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPBHierarchy.json
	${result} =			Post		url=${ROOT}/projects/PB/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementV1FromPA
	[Documentation]		"Get element v1 in PA, return should contain _childViews "
	[Tags]				views		critical		0803
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/v1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPAUpdateModel
	[Documentation]		"post these elements to PA (update model directly to add child view)"
	[Tags]				views		critical		0804
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPAUpdateModel.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementV1FromPAAddChildView
	[Documentation]		"get element v1 in PA again, return adds childview"
	[Tags]				views		critical		0805
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/v1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPAChildView
	[Documentation]		"post this to PA (reorder single view) with ?childviews=true."
	[Tags]				views		critical		0806
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPAAddChildView.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements?childviews=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		id		ownerId		_appliedStereotypeIds		associationId		memberEndIds		ownedEndIds		ownedAttributeIds		appliedStereotypeInstanceId		stereotypedElementId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPAChildViewAcrossMounts
	[Documentation]		"post this to PA (add view across mount) with childviews=true"
	[Tags]				views		critical		0807
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPAAddChildViewsAcrossMounts.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements?childviews=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		id		ownerId		_appliedStereotypeIds		associationId		memberEndIds		ownedEndIds		ownedAttributeIds		appliedStereotypeInstanceId		stereotypedElementId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ReorderChildViewAcrossMountsFromPA
	[Documentation]		"post this to PA (reorder with view across mount) with childviews=true"
	[Tags]				views		critical		0808
	${post_json} =		Get File		${CURDIR}/../../JsonData/ReorderChildViewAcrossMountsFromPA.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements?childviews=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		id		ownerId		_appliedStereotypeIds		associationId		memberEndIds		ownedEndIds		ownedAttributeIds		appliedStereotypeInstanceId		stereotypedElementId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
# TODO V9 get element va via PA (check view get across mounts), should return
