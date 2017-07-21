*** Settings ***
Documentation	Testing get element/elements/search across mounts
Resource		../resources.robot

*** Test Cases ***
ProjectCreationForMountsPB
	[Documentation]		"Create a project (ID: PB) under the organization with (ID: initorg).  Symbolically PB."
	[Tags]				mounts		critical		0201
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPB.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPC
	[Documentation]		"Create a project (ID: PC) under the organization with (ID: initorg).  Symbolically PC."
	[Tags]				mounts		critical		0202
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPC.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPD
	[Documentation]		"Create a project (ID: PD) under the organization with (ID: initorg).  Symbolically PD."
	[Tags]				mounts		critical		0203
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPD.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPB
	[Documentation]		"Post unique elements to the newly created project (ID: PB). Symbolically PB -> E1 -> E2"
	[Tags]				mounts		critical		0204
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPB.json
	${result} =			Post		url=${ROOT}/projects/PB/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPC
	[Documentation]		"Post unique elements to the newly created project (ID: PC). Symbolically PC -> E3 -> E4"
	[Tags]				mounts		critical		0205
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPC.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPD
	[Documentation]		"Post unique elements to the newly created project (ID: PD). Symbolically PD -> E5 -> E6"
	[Tags]				mounts		critical		0206
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToPD.json
	${result} =			Post		url=${ROOT}/projects/PD/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPA|PB
	[Documentation]		"Assign a mount to project (ID: PA) which is (ID: PB). Symbolically PA -> PB"
	[Tags]				mounts		critical		0207
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPA|PB.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPB|PC
	[Documentation]		"Assign a mount to project (ID: PB) which is (ID: PC). Symbolically PB -> PC"
	[Tags]				mounts		critical		0208
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPB|PC.json
	${result} =			Post		url=${ROOT}/projects/PB/refs/master/elements			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PD
	[Documentation]		"Assign a mount to project (ID: PC) which is (ID: 121314). Symbolically PC -> PD"
	[Tags]				mounts		critical		0209
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPC|PD.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PA
	[Documentation]		"Assign a mount to project (ID: PC) which is (ID: PA). Symbolically PC -> PA"
	[Tags]				mounts		critical		0210
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPC|PA.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##get single element in each project, the return object should have the right _projectId matches the project it's contained in
# # So in the next example _projectID: PB and _refId is master.  Returns both e1 and e2 b/c of depth
GetElementFromMountedProjectPB
	[Documentation]		"Gets a element that only exists in project (ID: PB) from (ID: PA)."
	[Tags]				mounts		critical		0211
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPC
	[Documentation]		"Gets a element that only exists in project (ID: PC) from (ID: PA)."
	[Tags]				mounts		critical		0212
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e3		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPD
	[Documentation]		"Gets a element that only exists in project (ID: PD) from (ID: PA)."
	[Tags]				mounts		critical		0213
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e5		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPA|PC
	[Documentation]		"Gets a element that only exists in project (ID: PA) from (ID: PC)."
	[Tags]				mounts		critical		0214
	${result} =			Get		url=${ROOT}/projects/PC/refs/master/elements/300		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##get single element in each project with depth=-1 and extended=true
GetExtendedElementFromMountedProjectPB
	[Documentation]		"Gets a element that only exists in project (ID: PB) from (ID: PA)."
	[Tags]				mounts		critical		0215
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e1?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPC
	[Documentation]		"Gets a element that only exists in project (ID: PC) from (ID: PA)."
	[Tags]				mounts		critical		0216
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e3?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPD
	[Documentation]		"Gets a element that only exists in project (ID: PD) from (ID: PA)."
	[Tags]				mounts		critical		0217
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e5?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPA|PC
	[Documentation]		"Gets a element that only exists in project (ID: PA) from (ID: PC)."
	[Tags]				mounts		critical		0218
	${result} =			Get		url=${ROOT}/projects/PC/refs/master/elements/300?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##bulk element get for root only
GetElementsFromAllMountedProjectsViaPA
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				mounts		critical		0219
	${post_json} =		Get File		${CURDIR}/../../JsonData/GetAllElementsAcrossMounts.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}	   headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
## bulk get with depth = -1 get all elements and make sure there are no duplicates in the return
GetExtendedElementsFromAllMountedProjectsViaPA
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				mounts		critical		0220
	${post_json} =		Get File		${CURDIR}/../../JsonData/GetAllElementsAcrossMounts.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements?depth=-1&extended=true		data=${post_json}	   headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##Request all the elements and their children and make sure there's no duplicates
GetElementsFromAllMountedProjectsViaPADuplicates
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				mounts		critical		0221
	${post_json} =		Get File		${CURDIR}/../../JsonData/GellAllElementsAcrossMountsDuplicates.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}	   headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementsFromAllMountedProjectsViaPADuplicates
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				mounts		critical		0222
	${post_json} =		Get File		${CURDIR}/../../JsonData/GellAllElementsAcrossMountsDuplicates.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements?depth=-1&extended=true		data=${post_json}	   headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


