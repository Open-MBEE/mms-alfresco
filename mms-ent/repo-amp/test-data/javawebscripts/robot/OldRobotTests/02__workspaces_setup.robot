*** Settings ***
Documentation    Testing CRUD Operations on Workspaces
Resource        ../resources.robot

*** Test Cases ***
CreateWorkspace1
	[Documentation]		"Regression Test: 160. Create workspace test 1"
	[Tags]				160
#	${result} =			Post		${ROOT}/workspaces/wsA?sourceWorkspace=master&copyTime=${gv6}
    ${post_json} =		Get File	 newJsonData/${TEST_NAME}.json
	${result} =			Post		url=${ROOT}/workspaces/wsA?sourceWorkspace=master       data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        qualifiedName
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

#TODO: Might be deprecated
CreateWorkspace2
	[Documentation]		"Regression Test: 162. Create workspace test 2"
	[Tags]				162
    ${gv1} =            Get Id From Workspace       CreateWorkspace1
    # TODO: FIX THIS! -- Post the workspace twice... For Some reason the first post doesn't work properly.
	${getResult} =      Get                 ${ROOT}/workspaces/Workspace1
	${result} =			Post		url=${ROOT}/workspaces/wsB?sourceWorkspace=${gv1}      data="{}"               headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        qualifiedName       parent
	Generate JSON	    ${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

CreateWorkspaceWithJson
	[Documentation]		"Regression Test: 164. Create a workspace using a json"
	[Tags]				164
	${post_json} =		Get File	 ../JsonData/NewWorkspacePost.json
	${result} =			Post		url=${ROOT}/workspaces/		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

ModifyWorkspaceWithJson
	[Documentation]		"Regression Test: 165. Modifies a workspace name/description"
	[Tags]				165
	${result} =			Post		${ROOT}/workspaces/
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

GetWorkspaces
	[Documentation]		"Regression Test: 166. Get workspaces"
	[Tags]				166
	Sleep				.5s
	${result} =			Get		url=${ROOT}/workspaces?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspace
	[Documentation]		"Regression Test: 167. Post element to workspace"
	[Tags]				167
	${post_json} =		Get File	 ../JsonData/x.json
    ${gv2} =            Get Id From Json     CreateWorkspace2      workspaces
	${result} =			Post		url=${ROOT}/workspaces/${gv2}/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

