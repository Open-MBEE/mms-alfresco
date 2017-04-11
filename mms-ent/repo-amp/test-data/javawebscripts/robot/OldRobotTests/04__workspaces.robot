*** Settings ***
Documentation    Testing CRUD Operations on Workspaces
Resource        ../resources.robot

*** Test Cases ***
CreateWorkspaceDelete1
	[Documentation]		"Regression Test: 370. Create workspace to be deleted"
	[Tags]				370
	${result} =			Post		url=${ROOT}/workspaces/AA?sourceWorkspace=master        data="{}"       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

CreateWorkspaceDelete2
	[Documentation]		"Regression Test: 380. Create workspace to be deleted"
	[Tags]				380
	${post_json} =		Get File	 NewJsonData/CreateWorkspaceDelete1.json
    ${gv1} =           Get Id From Workspace       CreateWorkspaceDelete1
	${result} =			Post		url=${ROOT}/workspaces/BB?sourceWorkspace=${gv1}        data="{}"       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

#TODO: Deletes aren't supported...maybe
DeleteWorkspace
	[Documentation]		"Regression Test: 390. Delete workspace and its children"
	[Tags]				390
	${gv1} =            Get Id From Workspace       CreateWorkspaceDelete1
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}
	Should Be Equal		${result.status_code}		${200}
	${result} =         Is Empty JSON       ${result.json()}
#	TODO: Update this test when deletes are for sure supported. Right now I can only check if a 200 is returned
#   It returns an empty object when it is successful
#	Should Be True      ${result}

CheckDeleted1
	[Documentation]		"Regression Test: 400. Make sure that AA and its children no longer show up in workspaces"
	[Tags]				400
	${result} =			Get		url=${ROOT}/workspaces?extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CheckDeleted2
	[Documentation]		"Regression Test: 410. Make sure that AA and its children show up in deleted"
	[Tags]				410
	${result} =			Get		url=${ROOT}/workspaces?deleted&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


#TODO: Diffs won't work
DiffWorkspaceCreate1
	[Documentation]		"Regression Test: 530. Diff Workspace Test - Create WS 1" -- READ Doesn't Work
	[Tags]				530
	${gv7} =            Get Copy Time From JSON        PostModelForElementDowngrade         elements
	${result} =			Post		url=${ROOT}/workspaces/ws1?sourceWorkspace=master&copyTime=${gv7}       data='{}'       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        id      modified        created
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

DiffWorkspaceCreate2
	[Documentation]		"Regression Test: 540. Diff Workspace Test - Create WS 2"
	[Tags]				540
	${gv7} =            Get Copy Time From JSON        DiffWorkspaceCreate1        workspaces
	${result} =			Post		url=${ROOT}/workspaces/ws2?sourceWorkspace=master&copyTime=${gv7}             data='{}'       headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        id      modified        created
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

DiffDelete_arg_ev_38307
	[Documentation]		"Regression Test: 550. Diff Workspace Test - Delete element arg_ev_38307"
	[Tags]				550
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1        workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}/elements/arg_ev_38307
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${DELETE_WORKSPACE_DELAY}

DiffPostToWorkspace1
	[Documentation]		"Regression Test: 560. Diff Workspace Test - Post element to workspace"
	[Tags]				560
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
	${post_json} =		Get File	 ../JsonData/newElement.json
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
    #Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}


DiffUpdateElement402
	[Documentation]		"Regression Test: 570. Diff Workspace Test - Update element 402"
	[Tags]				570
	${post_json} =		Get File	 ../JsonData/update402.json
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

DiffCompareWorkspacesForMerge
	[Documentation]		"Regression Test: 580. Diff Workspace Test - Compare workspaces for a merge"
	[Tags]				580
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
    ${gv2} =            Get Id From JSON        DiffWorkspaceCreate2         workspaces
	${result} =			Get		url=${ROOT}/diff/${gv2}/${gv1}/latest/latest?changesForMerge&extended=true
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	Should Be Equal		${result.status_code}		${200}

DiffCompareWorkspaces
	[Documentation]		"Regression Test: 580.5. Diff Workspace Test - Compare workspaces"
	[Tags]				580.5
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
    ${gv2} =            Get Id From JSON        DiffWorkspaceCreate2         workspaces
	${result} =			Get		url=${ROOT}/diff/${gv2}/${gv1}/latest/latest?fullCompare&extended=true
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	Should Be Equal		${result.status_code}		${200}

PostDiff
	[Documentation]		"Regression Test: 581. Post a diff to merge workspaces"
	[Tags]				581
	${result} =			Post		${ROOT}/diff
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}
	Should Be Equal		${result.status_code}		${200}

DiffCompareWorkspacesAgainForMerge
	[Documentation]		"Regression Test: 582. Diff Workspace Test - Compare workspaces again for a merge and make sure the diff is empty now after merging."
	[Tags]				582
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
    ${gv2} =            Get Id From JSON        DiffWorkspaceCreate2         workspaces
	${result} =			Get		url=${ROOT}/diff/${gv2}/${gv1}/latest/latest?changesForMerge&extended=true
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	Should Be Equal		${result.status_code}		${200}

DiffCompareWorkspacesAgain
	[Documentation]		"Regression Test: 582.5. Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging."
	[Tags]				582.5
    ${gv1} =            Get Id From JSON        DiffWorkspaceCreate1         workspaces
    ${gv2} =            Get Id From JSON        DiffWorkspaceCreate2         workspaces
	${result} =			Get		url=${ROOT}/diff/${gv2}/${gv1}/latest/latest?fullCompare&extended=true
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	Should Be Equal		${result.status_code}		${200}

CreateWorkspaceMatrixTest1
	[Documentation]		"Regression Test: 801. Create workspace1 for glom matrix testing"
	[Tags]				810
    ${gv3} =            Get Copy Time From JSON     CreateSubworkspace
	${result} =			Post		${ROOT}/workspaces/wsMatrix2?sourceWorkspace=master&copyTime=${gv3}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        qualifiedName       parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

CreateWorkspaceMatrixTest2
	[Documentation]		"Regression Test: 810. Create workspace2 for glom matrix testing"
	[Tags]				810
    ${gv5} =            Get Copy Time From JSON        DiffCompareWorkspacesAgain         workspaces
	${result} =			Post		${ROOT}/workspaces/wsMatrix2?sourceWorkspace=master&copyTime=${gv5}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        qualifiedName       parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

DeleteAddDeleteWsMatrix2
	[Documentation]		"Regression Test: 811. Delete add_delete_gg"
	[Tags]				811
    ${gv5} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv2}/elements/add_delete_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteUpdateDeleteWsMatrix2
	[Documentation]		"Regression Test: 812. Delete update_delete_gg"
	[Tags]				812
    ${gv5} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv2}/elements/update_delete_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteDeleteDeleteWsMatrix2
	[Documentation]		"Regression Test: 813. Delete delete_delete_gg"
	[Tags]				813
	${gv2} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv2}/elements/delete_delete_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteNoneDeleteWsMatrix2
	[Documentation]		"Regression Test: 814. Delete none_delete_gg"
	[Tags]				814
	${gv2} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv2}/elements/none_delete_gg
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${DELETE_WORKSPACE_DELAY}

PostElementsWsMatrix2
	[Documentation]		"Regression Test: 815. Post elements to the wsMatrix2 branch for glom matrix testing"
	[Tags]				815
	${post_json} =		Get File	 ../JsonData/elementsWsMatrix2.json
	${gv2} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Post		url=${ROOT}/workspaces/${gv2}/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

CompareWorkspacesGlomMatrixForMerge
	[Documentation]		"Regression Test: 816. Compare workspaces at latest times for glom matrix test.  Does merge style diff."
	[Tags]				816
	${gv1} =            Get ID From JSON        DeleteNoneDeleteWsMatrix2       workspaces
	${gv2} =            Get ID From JSON        CreateWorkspaceMatrixTest2      workspaces
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?changesForMerge&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesGlomMatrix
	[Documentation]		"Regression Test: 817. Compare workspaces at latest times for glom matrix test.  Does full compare style diff."
	[Tags]				817
	${gv1} =            Get ID From JSON        DeleteNoneDeleteWsMatrix2       workspaces
	${gv2} =            Get ID From JSON        PostElementsWsMatrix2           elements
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?fullCompare&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


PostElementsMerge1
	[Documentation]		"Regression Test: 900. Post elements to the master branch for merge-style diff testing"
	[Tags]				900
	${post_json} =		Get File	 ../JsonData/elementsMasterMerge1.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

DeleteDeleteDeleteBeforeMasterMerge1
	[Documentation]		"Regression Test: 900.5. Delete delete_delete_before"
	[Tags]				900.5
	${result} =			Delete		url=${ROOT}/workspaces/master/elements/delete_delete_before
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateWorkspaceMerge-style-Test1
	[Documentation]		"Regression Test: 901. Create workspace1 for merge-style diff testing"
	[Tags]				901
	${gv3} =            Get Copy Time From JSON        DeleteDeleteDeleteBeforeMasterMerg1     workspaces
	${result} =			Post		${ROOT}/workspaces/wsMerge1?sourceWorkspace=master&copyTime=${gv3}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId        commitId        created         id      modified        qualifiedName       parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep               ${POST_DELAY_INDEXING}

DeleteDeleteDeleteMasterMerge1
	[Documentation]		"Regression Test: 902. Delete delete_delete_consistent"
	[Tags]				902
	${result} =			Delete		url=${ROOT}/workspaces/master/elements/delete_delete_consistent
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteDeleteUpdateMasterMerge1
	[Documentation]		"Regression Test: 903. Delete delete_update_consistent"
	[Tags]				903
	${result} =			Delete		url=${ROOT}/workspaces/master/elements/delete_update_consistent
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementsMasterMerge1
	[Documentation]		"Regression Test: 904. Post elements to the MasterMerge1 branch for merge-style diff testing"
	[Tags]				904
	${post_json} =		Get File	 ../JsonData/elementsMasterMerge2.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

CompareWorkspacesForMerge-style1
	[Documentation]		"Regression Test: 905. Compare workspaces at latest times for merge-style diff test."
	[Tags]				905
	${gv3} =            Get ID From JSON        PostElementsMasterMerge1           workspaces
	${result} =			Get		url=${ROOT}/diff/master/${gv1}/latest/latest?background&changesForMerge&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier		 diffTime
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMerge-style2
	[Documentation]		"Regression Test: 905.5. Compare workspaces at latest times for merge-style diff test."
	[Tags]				905.5
	${gv1} =            Get ID From JSON        CompareWorkspacesForMerge-style1        workspaces
	${result} =			Get		url=${ROOT}/diff/master/${gv1}/latest/latest?background&fullCompare&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier		 diffTime
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteDeleteDeleteWs1
	[Documentation]		"Regression Test: 906. Delete delete_delete_consistent"
	[Tags]				906
	${gv1} =            Get ID From JSON        CompareWorkspacesForMerge-style2        workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}/elements/delete_delete_consistent
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteUpdateDeleteWs1
	[Documentation]		"Regression Test: 907. Delete update_delete_consistent"
	[Tags]				907
	${gv1} =            Get ID From JSON        DeleteDeleteDeleteWs1        workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}/elements/update_delete_consistent
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

DeleteAddAddBeforeWs1
	[Documentation]		"Regression Test: 907.5. Delete add_add_before"
	[Tags]				907.5
	${gv1} =            Get ID From JSON        DeleteUpdateDeleteWs1        workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}/elements/add_add_before
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostElementsMerge2
	[Documentation]		"Regression Test: 908. Post elements to the master branch for merge-style diff testing"
	[Tags]				908
	${post_json} =		Get File	 ../JsonData/elementsWsMerge-style.json
	${gv1} =            Get ID From JSON        DeleteAddAddBeforeWs1           workspaces
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements?extended=true		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
	#Log To Console      \nDelaying tests -- Waiting for Indexing
	Sleep				${POST_DELAY_INDEXING}

DeleteDeleteDeleteBeforeWs1
	[Documentation]		"Regression Test: 908.5. Delete delete_delete_before"
	[Tags]				908.5
	${gv1} =            Get ID From JSON        PostElementsMerge2           workspaces
	${result} =			Delete		url=${ROOT}/workspaces/${gv1}/elements/delete_delete_before
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 timestamp		 MMS_		 sysmlId		 qualifiedId		 version		 _modified		 sequence
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMerge-style3
	[Documentation]		"Regression Test: 909. Compare workspaces at latest times for merge-style diff test."
	[Tags]				909
	${gv1} =            Get ID From JSON        DeleteDeleteDeleteBeforeWs1           workspaces
	${result} =			Get		url=${ROOT}/diff/master/${gv1}/latest/latest?fullCompare&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMerge-style4
	[Documentation]		"Regression Test: 910. Compare workspaces at latest times for merge-style diff test."
	[Tags]				910
	${gv1} =            Get ID From JSON        CompareWorkspacesForMerge-style3           workspaces
	${result} =			Get		url=${ROOT}/diff/master/${gv1}/latest/latest?changesForMerge&extended=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List        commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
