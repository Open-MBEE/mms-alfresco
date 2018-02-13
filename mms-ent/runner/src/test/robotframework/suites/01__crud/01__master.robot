*** Settings ***
Documentation	Testing CRUD Operations on Master
Resource		../resources.robot
Suite Setup	 Purge Results Directory

*** Test Cases ***
InitializeOrganization
	[Documentation]		"Initialize MMS with an organization. ID: initorg, name: initorg"
	[Tags]				crud		critical		0101
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/InitializeOrganization.json
	${result} =			Post		url=${ROOT}/orgs			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateOrganization
	[Documentation]		"Update an organization. ID: initorg, name: initorg"
	[Tags]				crud		critical		0118
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/UpdateOrganization.json
	${result} =			Post		url=${ROOT}/orgs			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

BadOrganization
	[Documentation]		"Try to create an organization with bad request"
	[Tags]				crud		critical		0101a
	${result} =			Post		url=${ROOT}/orgs			data=""		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}

GetOrgs
	[Documentation]		"Get all orgs."
	[Tags]				crud		critical		0102
	${result} =			Get		url=${ROOT}/orgs		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetOrg
	[Documentation]		"Get first org."
	[Tags]				crud		critical		0103
	${result} =			Get		url=${ROOT}/orgs/initorg		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ProjectCreation
	[Documentation]		"Create a project (ID: PA) under the organization with ID: initorg"
	[Tags]				crud		critical		0104
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		 _created		 _modified		 _elasticId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

BadIdProjectCreation
	[Documentation]		"Try to Create a project with an invalid ID under the organization with ID: initorg"
	[Tags]				crud		critical		0105
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/BadIdProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}

BadOrgProjectCreation
	[Documentation]		"Try to Create a project under the organization with ID: invalid"
	[Tags]				crud		critical		0106
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/BadOrgProjectCreation.json
	${result} =			Post		url=${ROOT}/orgs/invalid/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${500}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProjects
	[Documentation]		"Get all projects in org."
	[Tags]				crud		critical		0107
	${result} =			Get		url=${ROOT}/orgs/initorg/projects		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProject
	[Documentation]		"Get posted project."
	[Tags]				crud		critical		0108
	${result} =			Get		url=${ROOT}/projects/PA		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElements
	[Documentation]		"Post elements to the newly created project and organization."
	[Tags]				crud		critical		0109
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewElements.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

UpdateElements
	[Documentation]	 "Update an existing element.  Creates versions of a element."
	[Tags]			  crud		critical		0110
	${post_json} =		Get File		${CURDIR}/../../JsonData/UpdateElements.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
    Sleep               ${POST_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

InvalidUpdateElement
	[Documentation]	 "Update an existing element with no changes. Should ignore commit."
	[Tags]			  crud		critical		0111
	${post_json} =		Get File		${CURDIR}/../../JsonData/UpdateElements.json
	${elementa} =		Get	url=${ROOT}/projects/PA/refs/master/elements/300		headers=&{REQ_HEADER}
    ${commitIda} =		Get Commit From Json		${elementa.json()}
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Should Be Equal		${result.status_code}		${200}
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	Sleep				${POST_DELAY_INDEXING}
	${elementb} =		Get	url=${ROOT}/projects/PA/refs/master/elements/300		headers=&{REQ_HEADER}
    ${commitIdb} =		Get Commit From Json		${elementb.json()}
	Should Be Equal		${commitIda}		${commitIdb}

UpdateProject
	[Documentation]	 "Update an existing project."
	[Tags]			  crud		critical		0112
	${post_json} =		Get File		${CURDIR}/../../JsonData/ProjectUpdate.json
	${result} =			Post		url=${ROOT}/orgs/initorg/projects/PA		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}

DeleteProject
	[Documentation]  "Delete an existing project"
	[Tags]			crud		critical		0113
	${post_json} =		Get File		${CURDIR}/../../JsonData/ProjectForDeleteProject.json
	${result} =			Post			url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	${post_json} =		Get File		${CURDIR}/../../JsonData/PostNewElementsForDeleteProject.json
	${result} =			Post			url=${ROOT}/projects/DeleteProject/refs/master/elements         data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	${result} =		 Delete	  url=${ROOT}/projects/${TEST_NAME}
	Should Be Equal	 ${result.status_code}	   ${200}
	${check} =			Get		url=${ROOT}/orgs/initorg/projects/${TEST_NAME}		headers=&{REQ_HEADER}
	Should Be Equal		${check.status_code}		${404}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

RecreateDeletedProject
	[Documentation]	 "Test recreating the DeleteProject again to make sure that it can be recreated without issues."
	[Tags]			  crud		critical		0114
	${post_json} =		Get File		${CURDIR}/../../JsonData/ProjectForDeleteProject.json
	${result} =			Post			url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Sleep				${POST_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}
	${result} =			Get		url=${ROOT}/projects/DeleteProject		headers=&{REQ_HEADER}
	Log To Console	  ${result.json()}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	${result} =		 Delete	  url=${ROOT}/projects/DeleteProject
	Should Be Equal	 ${result.status_code}	   ${200}

OverwriteElement
    [Documentation]     "Test creating an element with certain properties, then overwriting the element with an update."
    [Tags]              crud        critical        0115
    ${post_json} =      Get File        ${CURDIR}/../../JsonData/BaseOverwriteElement.json
    ${base_element} =         Post            url=${ROOT}/projects/PA/refs/master/elements        data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${base_element.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
    ${post_json} =      Get File        ${CURDIR}/../../JsonData/OverwrittenElement.json
    ${overwritten_element} =         Post            url=${ROOT}/projects/PA/refs/master/elements?overwrite=true        data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${overwritten_element.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	${result} =			Compare Json To Json		${base_element.json()}		${overwritten_element.json()}
	Should not be true      ${result}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${overwritten_element.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteElement
    [Documentation]  "Test deleting an element"
    [Tags]           crud       critical        0116
    # Create element first
    ${post_json} =      Get File        ${CURDIR}/../../JsonData/DeleteElement.json
    ${result} =         Post            url=${ROOT}/projects/PA/refs/master/elements        data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	# Delete element
	${result} =         Delete          url=${ROOT}/projects/PA/refs/master/elements/DeleteElement          headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	# Try to get element
	${result} =         Get         url=${ROOT}/projects/PA/refs/master/elements/DeleteElement          headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ResurrectElement
    [Documentation]     "Test resurrecting deleted element"
    [Tags]              crud        critical        0117
    # Resurrect Element by posting update to it
    ${post_json} =      Get File        ${CURDIR}/../../JsonData/ResurrectElement.json
    ${result} =         Post            url=${ROOT}/projects/PA/refs/master/elements        data=${post_json}       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	Sleep				${POST_DELAY_INDEXING}
	${result} =         Get             url=${ROOT}/projects/PA/refs/master/elements/DeleteElement        headers=&{REQ_HEADER}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteOrgWithProject
	[Documentation]		"Create an organization, add project, try to delete the org."
	[Tags]				crud		critical		0118
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/DeleteOrganization.json
	${result} =			Post		url=${ROOT}/orgs			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	# Add a project to the organization
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectForDeleteOrganization.json
	${result} =			Post		url=${ROOT}/orgs/deleteorg/projects			data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${result} =         Delete          url=${ROOT}/orgs/deleteorg          headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteOrgWithNoProject
	[Documentation]		"Delete project then organization"
	[Tags]				crud		critical		0119
	# Delete project
	${result} =		    Delete	  url=${ROOT}/projects/deleteorgproject
	Should Be Equal	    ${result.status_code}	   ${200}
	Sleep				${POST_DELAY_INDEXING}
	${result} =			Get		url=${ROOT}/orgs/initorg/projects/deleteorgproject		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${404}
	${result} =         Delete          url=${ROOT}/orgs/deleteorg          headers=&{REQ_HEADER}
	Sleep				${DELETE_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${BRANCH_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

ResurrectDeleteOrg
	[Documentation]		"Resurrect the deleted organization."
	[Tags]				crud		critical		0120
	${post_json} =		Get File	 ${CURDIR}/../../JsonData/DeleteOrganization.json
	${result} =			Post		url=${ROOT}/orgs			data=${post_json}		headers=&{REQ_HEADER}
	Sleep				${POST_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		 _created		 _modified		 _elasticId
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
