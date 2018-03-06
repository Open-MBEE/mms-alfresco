*** Settings ***
Documentation	Testing get element/elements/search across mounts
Resource		../resources.robot

*** Test Cases ***
ProjectCreationForMountsCommit
	[Documentation]		"Project for mount history test. Create a project (ID: PB) under the organization with (ID: initorg).  Symbolically MountCommit."
	[Tags]				mounts		critical		020201
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountCommit.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		 _created		 _modified		 _elasticId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToMountCommit
	[Documentation]		"Post unique elements to the newly created project (ID: MountCommit). Symbolically MountCommit -> mc1 -> mc2"
	[Tags]				mounts		critical		020202
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostElementsToMountCommit.json
	${result} =			Post		url=${ROOT}/projects/MountCommit/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreationPAtoMountCommit
	[Documentation]		"Assign a mount to project (ID: PA) which is (ID: MountCommit). Symbolically PA -> MountCommit"
	[Tags]				mounts		critical		020203
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/CreateMountPAtoMountCommit.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementFromMountedProjectMountCommit
	[Documentation]		"Gets a element that only exists in project (ID: MountCommit) from (ID: PA)."
	[Tags]				mounts		critical		020204
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/mc1		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


SearchAcrossMountsByCommit
    [Documentation]     "Search for an element on PA mount that exists by commit over mounts."
	[Tags]				mounts		critical		020205
	${refHistory} =			Get		url=${ROOT}/projects/PA/refs/master/history		headers=&{REQ_HEADER}
    ${latestCommit} =   Set Variable        ${refHistory.json()["commits"][0]["id"]}
	${element} =			Get		url=${ROOT}/projects/MountCommit/refs/master/elements/mc1/history		headers=&{REQ_HEADER}
	${commitId} =       Set Variable        ${element.json()["commits"][0]["id"]}
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/UpdateMountCommitElement.json
	${result} =			Post		url=${ROOT}/projects/MountCommit/refs/master/elements			data=${post_json}		headers=&{REQ_HEADER}
	Sleep				${POST_DELAY_INDEXING}
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/GetElementAcrossMountCommit.json
	${new_element} =        Put     url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}         headers=&{REQ_HEADER}
    Should Not Be Equal  ${commitId}        ${new_element.json()["elements"][0]["_commitId"]}
    ${post_json} =		Get File	 ${CURDIR}/../../JsonData/GetElementAcrossMountCommit.json
    ${old_element} =        Put     url=${ROOT}/projects/PA/refs/master/elements?commitId=${latestCommit}		data=${post_json}         headers=&{REQ_HEADER}
    Should Be Equal     ${commitId}        ${old_element.json()["elements"][0]["_commitId"]}




