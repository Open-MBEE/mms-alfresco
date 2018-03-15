*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot

*** Test Cases ***
ProjectCreationForModel
	[Documentation]		"Create a project (ID: CompleteModelGet) under the organization with ID: initorg"
	[Tags]				crud		critical		010301
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationModel.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

PostNewElementsToModel
	[Documentation]		"Post elements to the newly created project and organization."
	[Tags]				crud		critical		010302
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewElementsForModelGet.json
	${result} =			Post		url=${ROOT}/projects/CompleteModelGet/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${commit} =	Get Commit Id		${result.json()}
    Set Global Variable	  ${commit}

GetAllElementsInModel
	[Documentation]		"Get All the elements in the model excluding the element with no owner"
	[Tags]				mounts		critical		010303
	${result} =			Get		url=${ROOT}/projects/CompleteModelGet/refs/master/elements		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetAllElementsInModelExtended
	[Documentation]		"Get All the elements in the model excluding the element with no owner"
	[Tags]				mounts		critical		010304
	${result} =			Get		url=${ROOT}/projects/CompleteModelGet/refs/master/elements?extended=true		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsToModelForCommit
	[Documentation]		"Post elements to the newly created project and organization."
	[Tags]				crud		critical		010305
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewElementsForModelGetCommit.json
	${result} =			Post		url=${ROOT}/projects/CompleteModelGet/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

GetAllElementsInModelFromCommit
	[Documentation]		"Get All the elements in the model excluding the element with no owner"
	[Tags]				mounts		critical		010306
	${result} =			Get		url=${ROOT}/projects/CompleteModelGet/refs/master/elements?commitId=${commit}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetAllElementsByProjectAndRef
    [Documentation]     "Compare returned models"
    [Tags]              critical    workspaces      010307
    ${resultA} =         Get     url=${ROOT}/projects/CompleteModelGet/refs/master/elements		headers=&{REQ_HEADER}
    ${resultB} =         Get     url=${ROOT}/projects/CompleteModelGet/refs/master/elements/PA?depth=-1		headers=&{REQ_HEADER}
	Should Not Be Equal     ${resultA.json()}        ${resultB.json()}
