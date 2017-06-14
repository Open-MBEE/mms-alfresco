*** Settings ***
Documentation    Testing get element/elements/search across mounts
Resource        ../resources.robot

*** Test Cases ***
ProjectCreationForMountsPB
	[Documentation]		"Create a project (ID: PB) under the organization with (ID: initorg).  Symbolically PB."
	[Tags]				M1
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPB.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPC
	[Documentation]		"Create a project (ID: PC) under the organization with (ID: initorg).  Symbolically PC."
	[Tags]				M2
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPC.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPD
	[Documentation]		"Create a project (ID: PD) under the organization with (ID: initorg).  Symbolically PD."
	[Tags]				M3
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPD.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPB
	[Documentation]		"Post unique elements to the newly created project (ID: PB). Symbolically PB -> E1 -> E2"
	[Tags]				M4
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostElementsToPB.json
	${result} =			Post		url=${ROOT}/projects/PB/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPC
	[Documentation]		"Post unique elements to the newly created project (ID: PC). Symbolically PC -> E3 -> E4"
	[Tags]				M5
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostElementsToPC.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPD
	[Documentation]		"Post unique elements to the newly created project (ID: PD). Symbolically PD -> E5 -> E6"
	[Tags]				M6
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostElementsToPD.json
	${result} =			Post		url=${ROOT}/projects/PD/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPA|PB
	[Documentation]		"Assign a mount to project (ID: PA) which is (ID: PB). Symbolically PA -> PB"
	[Tags]				M7
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPA|PB.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPB|PC
	[Documentation]		"Assign a mount to project (ID: PB) which is (ID: PC). Symbolically PB -> PC"
	[Tags]				M8
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPB|PC.json
	${result} =			Post		url=${ROOT}/projects/PB/refs/master/elements		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PD
	[Documentation]		"Assign a mount to project (ID: PC) which is (ID: 121314). Symbolically PC -> PD"
	[Tags]				M9
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPC|PD.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PA
	[Documentation]		"Assign a mount to project (ID: PC) which is (ID: PA). Symbolically PC -> PA"
	[Tags]				M10
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPC|PA.json
	${result} =			Post		url=${ROOT}/projects/PC/refs/master/elements	    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##get single element in each project, the return object should have the right _projectId matches the project it's contained in
# # So in the next example _projectID: PB and _refId is master.  Returns both e1 and e2 b/c of depth
GetElementFromMountedProjectPB
	[Documentation]		"Gets a element that only exists in project (ID: PB) from (ID: PA)."
	[Tags]				M11
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPC
	[Documentation]		"Gets a element that only exists in project (ID: PC) from (ID: PA)."
	[Tags]				M12
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e3		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPD
	[Documentation]		"Gets a element that only exists in project (ID: PD) from (ID: PA)."
	[Tags]				M13
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e5		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectPA|PC
	[Documentation]		"Gets a element that only exists in project (ID: PA) from (ID: PC)."
	[Tags]				M14
	${result} =			Get		url=${ROOT}/projects/PC/refs/master/elements/300		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##get single element in each project with depth=-1 and extended=true
GetExtendedElementFromMountedProjectPB
	[Documentation]		"Gets a element that only exists in project (ID: PB) from (ID: PA)."
	[Tags]				M15
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e1?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPC
	[Documentation]		"Gets a element that only exists in project (ID: PC) from (ID: PA)."
	[Tags]				M16
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e3?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPD
	[Documentation]		"Gets a element that only exists in project (ID: PD) from (ID: PA)."
	[Tags]				M17
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/e5?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementFromMountedProjectPA|PC
	[Documentation]		"Gets a element that only exists in project (ID: PA) from (ID: PC)."
	[Tags]				M18
	${result} =			Get		url=${ROOT}/projects/PC/refs/master/elements/300?depth=-1&extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##bulk element get for root only
GetElementsFromAllMountedProjectsViaPA
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				M19
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/GetAllElementsAcrossMounts.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
## bulk get with depth = -1 get all elements and make sure there are no duplicates in the return
GetExtendedElementsFromAllMountedProjectsViaPA
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				M20
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/GetAllElementsAcrossMounts.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements?depth=-1&extended=true		data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
##Request all the elements and their children and make sure there's no duplicates
GetElementsFromAllMountedProjectsViaPADuplicates
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				M21
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/GellAllElementsAcrossMountsDuplicates.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetExtendedElementsFromAllMountedProjectsViaPADuplicates
	[Documentation]		"Gets all elements that are in other projects mounted to PA."
	[Tags]				M22
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/GellAllElementsAcrossMountsDuplicates.json
	${result} =			Put		url=${ROOT}/projects/PA/refs/master/elements?depth=-1&extended=true		data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


