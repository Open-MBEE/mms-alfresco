*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot

*** Test Cases ***
ProjectCreationForMove
	[Documentation]		"Create a project (ID: MoveModel) under the organization with ID: initorg"
	[Tags]				crud		critical		010401
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMove.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

PostNewElementsToMove
	[Documentation]		"Post elements to the newly created project and organization."
	[Tags]				crud		critical		010402
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostMoveElements.json
	${result} =			Post		url=${ROOT}/projects/CompleteModelGet/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	${commit} =	Get Commit Id		${result.json()}
    Set Global Variable	  ${commit}

GetAllElementsForMove
	[Documentation]		"Get All the elements in the model"
	[Tags]				mounts		critical		010403
	${result} =			Get		url=${ROOT}/projects/MoveModel/refs/master/propertyMove		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ChangeOwner
	[Documentation]		"Change owner of p from b1 to b2. Remove pId from b1's list of ownedAttributeIds.  Insert pId to b2's list of ownedAttributeIds at i"
	[Tags]				crud		critical		010404
	${post_json} =		Get File		${CURDIR}/../../JsonData/MoveOwner.json
	${result} =			Post		url=${ROOT}/projects/CompleteModelGet/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

#GetAllElementsInModelFromCommit
#	[Documentation]		"Get All the elements in the model excluding the element with no owner"
#	[Tags]				mounts		critical		010306
#	${result} =			Get		url=${ROOT}/projects/CompleteModelGet/refs/master/elements?commitId=${commit}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
#
#GetAllElementsByProjectAndRef
#    [Documentation]     "Compare returned models"
#    [Tags]              critical    workspaces      010307
#    ${resultA} =         Get     url=${ROOT}/projects/CompleteModelGet/refs/master/elements		headers=&{REQ_HEADER}
#    ${resultB} =         Get     url=${ROOT}/projects/CompleteModelGet/refs/master/elements/PA?depth=-1		headers=&{REQ_HEADER}
#	Should Not Be Equal     ${resultA.json()}        ${resultB.json()}
