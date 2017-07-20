*** Settings ***
Documentation    Testing CRUD Operations on Master
Resource        ../resources.robot
Suite Setup     Purge Results Directory

*** Test Cases ***
InitializeOrganization
	[Documentation]		"Initialize MMS with an organization. ID: initorg, name: initorg"
	[Tags]				crud critical 0101
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/InitializeOrganization.json
	${result} =			Post		url=${ROOT}/orgs		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetOrgs
	[Documentation]		"Get all orgs."
	[Tags]				crud critical 0102
	${result} =			Get		url=${ROOT}/orgs		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetOrg
	[Documentation]		"Get first org."
	[Tags]				crud critical 0103
	${result} =			Get		url=${ROOT}/orgs/initorg		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreation
	[Documentation]		"Create a project (ID: PA) under the organization with ID: initorg"
	[Tags]				crud critical 0104
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

BadIdProjectCreation
	[Documentation]		"Try to Create a project with an invalid ID under the organization with ID: initorg"
	[Tags]				crud critical 0105
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/BadIdProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}

BadOrgProjectCreation
	[Documentation]		"Try to Create a project under the organization with ID: invalid"
	[Tags]				crud critical 0106
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/BadOrgProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/invalid/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${500}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProjects
	[Documentation]		"Get all projects in org."
	[Tags]				crud critical 0107
	${result} =			Get		url=${ROOT}/orgs/initorg/projects		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProject
	[Documentation]		"Get posted project."
	[Tags]				crud critical 0108
	${result} =			Get		url=${ROOT}/orgs/initorg/projects/PA		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElements
	[Documentation]		"Post elements to the newly created project and organization."
	[Tags]				crud critical 0109
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/PostNewElements.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateElements
    [Documentation]     "Update an existing element.  Creates versions of a element."
    [Tags]              crud critical 0110
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateProject
    [Documentation]     "Update an existing project."
    [Tags]              crud critical 0111
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/ProjectUpdate.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects/PA		data=${post_json}		headers=&{REQ_HEADER}
    Should Be Equal		${result.status_code}		${200}

DeleteProject
    [Documentation]  "Delete an existing project"
    [Tags]            crud critical 0112
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/ProjectForDeleteProject.json
	${result} =			Post		    url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
    ${result} =         Delete      url=${ROOT}/projects/${TEST_NAME}
    Should Be Equal     ${result.status_code}       ${200}
	${result} =			Get		url=${ROOT}/orgs/initorg/projects/${TEST_NAME}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

RecreateDeletedProject
    [Documentation]     "Test recreating the DeleteProject again to make sure that it can be recreated without issues."
    [Tags]              crud critical 0113
	${post_json} =		Get File	    ${CURDIR}/../../JsonData/ProjectForDeleteProject.json
	${result} =			Post		    url=${ROOT}/orgs/initorg/projects		    data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${result} =			Get		url=${ROOT}/projects/DeleteProject		headers=&{REQ_HEADER}
	Log To Console      ${result.json()}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List     _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
