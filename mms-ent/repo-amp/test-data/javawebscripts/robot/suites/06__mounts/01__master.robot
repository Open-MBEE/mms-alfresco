*** Settings ***
Documentation    Testing get element/elements/search across mounts
Resource        ../resources.robot

*** Test Cases ***
ProjectCreationForMountsPB
	[Documentation]		"Create a project (ID: 789) under the organization with (ID: initorg).  Symbolically PB."
	[Tags]				11
	${post_json} =		Get File	 JsonData/ProjectCreationForMountsPB.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPC
	[Documentation]		"Create a project (ID: 101112) under the organization with (ID: initorg).  Symbolically PC."
	[Tags]				11
	${post_json} =		Get File	 JsonData/ProjectCreationForMountsPC.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreationForMountsPD
	[Documentation]		"Create a project (ID: 131415) under the organization with (ID: initorg).  Symbolically PD."
	[Tags]				12
	${post_json} =		Get File	 JsonData/ProjectCreationForMountsPD.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPB
	[Documentation]		"Post unique elements to the newly created project (ID: 789). Symbolically PB -> E1 -> E2"
	[Tags]				13
	${post_json} =		Get File	    JsonData/PostElementsToPB.json
	${result} =			Post		url=${ROOT}/projects/789/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPC
	[Documentation]		"Post unique elements to the newly created project (ID: 101112). Symbolically PC -> E3 -> E4"
	[Tags]				14
	${post_json} =		Get File	    JsonData/PostElementsToPC.json
	${result} =			Post		url=${ROOT}/projects/101112/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToPD
	[Documentation]		"Post unique elements to the newly created project (ID: 131415). Symbolically PD -> E5 -> E6"
	[Tags]				15
	${post_json} =		Get File	    JsonData/PostElementsToPD.json
	${result} =			Post		url=${ROOT}/projects/131415/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPA|PB
	[Documentation]		"Assign a mount to project (ID: 123456) which is (ID: 789). Symbolically PA -> PB"
	[Tags]				16
	${post_json} =		Get File	 JsonData/CreateMountPA|PB.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/mounts		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPB|PC
	[Documentation]		"Assign a mount to project (ID: 789) which is (ID: 101112). Symbolically PB -> PC"
	[Tags]				16
	${post_json} =		Get File	 JsonData/CreateMountPB|PC.json
	${result} =			Post		url=${ROOT}/projects/789/refs/master/mounts		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PD
	[Documentation]		"Assign a mount to project (ID: 101112) which is (ID: 121314). Symbolically PC -> PD"
	[Tags]				16
	${post_json} =		Get File	 JsonData/CreateMountPC|PD.json
	${result} =			Post		url=${ROOT}/projects/101112/refs/master/mounts		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPC|PA
	[Documentation]		"Assign a mount to project (ID: 101112) which is (ID: 123456). Symbolically PC -> PA"
	[Tags]				16
	${post_json} =		Get File	 JsonData/CreateMountPC|PA.json
	${result} =			Post		url=${ROOT}/projects/101112/refs/master/mounts		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#get single element in each project, the return object should have the right _projectId and _refId
GetElementFromMountedProject
	[Documentation]		"Gets a element that only exists in project (ID: 123456) mount (ID: 789)."
	[Tags]				17
	${result} =			Get		url=${ROOT}/projects/123456/refs/master/elements/867-5309?depth=-1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
#:TODO 1) recursive mount find, need to write method to check the response properly 2) Change the ref...?
#get single element in each project with depth=-1 and extended=true
#bulk element get (with both e1 and e3 in each project) with depth=-1 and extended=true
