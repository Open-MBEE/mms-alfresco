*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory

*** Test Cases ***
ProjectCreationForMounts
	[Documentation]		"Create a project (ID: 789) under the organization with ID: initorg"
	[Tags]				1
	${post_json} =		Get File	 JsonData/ProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementsTo789
	[Documentation]		"Post elements to the newly created project for mounts and organization."
	[Tags]				2
	${post_json} =		Get File	    JsonData/PostNewElements.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

MountCreation
	[Documentation]		"Assign a mount to project 123456 which is 789"
	[Tags]				3
	${post_json} =		Get File	 JsonData/CreateMount.json
	${result} =			Post		url=${ROOT}/projects/123456/refs/master/mounts		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		_mounts		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
