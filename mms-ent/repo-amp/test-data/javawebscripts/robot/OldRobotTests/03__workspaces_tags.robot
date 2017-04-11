*** Settings ***
Documentation    Testing CRUD Operations
Resource        ../resources.robot

*** Test Cases ***
CompareWorkspacesForMerge
	[Documentation]		"Regression Test: 168. Compare workspaces for a merge of the second into the first"
	[Tags]				168
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspaces
	[Documentation]		"Regression Test: 168.5. Compare workspaces"
	[Tags]				168.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMergeBackground1
	[Documentation]		"Regression Test: 169. Compare workspaces for a merge in the background, this will return that it is in process"
	[Tags]				169
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesBackground1
	[Documentation]		"Regression Test: 169.5. Compare workspaces in the background, this will return that it is in process"
	[Tags]				169.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170. Compare workspaces in the background again, this will return the results of the background diff"
	[Tags]				170
    ${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesBackground2
	[Documentation]		"Regression Test: 170.5. Compare workspaces in the background again, this will return the results of the background diff"
	[Tags]				170.5
    ${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesGlomForMerge1
	[Documentation]		"Regression Test: 171. Compare workspaces for a merge where there is a initial background diff stored"
	[Tags]				171
    ${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesGlom1
	[Documentation]		"Regression Test: 171.5. Compare workspaces where there is a initial background diff stored"
	[Tags]				171.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForGlom
	[Documentation]		"Regression Test: 172. Post element to workspace"
	[Tags]				172
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${post_json} =		Get File	 ../JsonData/glomPost.json
	${result} =			Post		url=${ROOT}/workspaces/${gv2}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesGlomForMerge2
	[Documentation]		"Regression Test: 173. Compare workspaces for a merge where there is a initial background diff stored and a change has been made since then."
	[Tags]				173
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesGlom2
	[Documentation]		"Regression Test: 173.5. Compare workspaces where there is a initial background diff stored and a change has been made since then."
	[Tags]				173.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 174. Create workspace with a branch time"
	[Tags]				174
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv4} =            Get Copy Time From JSON     PostToWorkspace     elements
	${result} =			Post		${ROOT}/workspaces/wsT?sourceWorkspace=${gv1}&copyTime=${gv4}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId		 parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceWithBranchTime
	[Documentation]		"Regression Test: 175. Post element to workspace with a branch time"
	[Tags]				175
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${post_json} =		Get File	 ../JsonData/y.json
	${result} =			Post		url=${ROOT}/workspaces/${gv5}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForConflict1
	[Documentation]		"Regression Test: 176. Post element to workspace1 so that we get a conflict"
	[Tags]				176
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${post_json} =		Get File	 ../JsonData/conflict1.json
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForConflict2
	[Documentation]		"Regression Test: 177. Post element to workspace with a branch time so that we get a conflict"
	[Tags]				177
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${post_json} =		Get File	 ../JsonData/conflict2.json
	${result} =			Post		url=${ROOT}/workspaces/${gv5}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForMoved
	[Documentation]		"Regression Test: 178. Post element to workspace with a branch time so that we get a moved element"
	[Tags]				178
	${post_json} =		Get File	 ../JsonData/moved.json
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${result} =			Post		url=${ROOT}/workspaces/${gv5}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForTypeChange
	[Documentation]		"Regression Test: 179. Post element to workspace with a branch time so that we get a type change"
	[Tags]				179
	${post_json} =		Get File	 ../JsonData/typeChange.json
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${result} =			Post		url=${ROOT}/workspaces/${gv5}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceForWs1Change
	[Documentation]		"Regression Test: 180. Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303"
	[Tags]				180
	${post_json} =		Get File	 ../JsonData/modified303.json
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElement303
	[Documentation]		"Regression Test: 181. Get element 303"
	[Tags]				181
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${result} =			Get		url=${ROOT}/workspaces/${gv5}/elements/303
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 MMS_		 MMS_
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesWithBranchTimeForMerge
	[Documentation]		"Regression Test: 182. Compare workspaces with branch times for a merge"
	[Tags]				182
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv5}/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesWithBranchTime
	[Documentation]		"Regression Test: 182.5. Compare workspaces"
	[Tags]				182.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv5}/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspace3
	[Documentation]		"Regression Test: 183. Post element z to workspace"
	[Tags]				183
	${post_json} =		Get File	 ../JsonData/z.json
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateWorkspaceWithBranchTime2
	[Documentation]		"Regression Test: 184. Create workspace with a branch time using the current time for the branch time"
	[Tags]				184
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv7} =            Get Copy Time From JSON     PostToWorkspace3        elements
	${result} =			Post		${ROOT}/workspaces/wsT2?sourceWorkspace=${gv1}&copyTime=${gv7}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId		 parent		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesWithBranchTimesForMerge
	[Documentation]		"Regression Test: 185. Compare workspaces each of which with a branch time and with a modified element on the common parent for a merge"
	[Tags]				185
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${gv6} =            Get Id From Workspace       CreateWorkspaceWithBranchTime2
	${result} =			Get		url=${ROOT}/diff/${gv5}/${gv6}/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesWithBranchTimes
	[Documentation]		"Regression Test: 185.5. Compare workspaces both which have a branch time and with a modified element on the common parent"
	[Tags]				185.5
	${gv5} =            Get Id From Workspace       CreateWorkspaceWithBranchTime
	${gv6} =            Get Id From Workspace       CreateWorkspaceWithBranchTime2
	${result} =			Get		url=${ROOT}/diff/${gv5}/${gv6}/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMergeBackgroundOutdated
	[Documentation]		"Regression Test: 186. Compare workspaces for a merge in the background, this will return that it is outdated"
	[Tags]				186
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesBackgroundOutdated
	[Documentation]		"Regression Test: 186.5. Compare workspaces in the background, this will return that it is outdated"
	[Tags]				186.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMergeBackgroundRecalculate
	[Documentation]		"Regression Test: 187. Compare workspaces for a merge in the background, and forces a recalculate on a outdated diff"
	[Tags]				187
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background=true&recalculate=true&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesBackgroundRecalculate
	[Documentation]		"Regression Test: 187.5. Compare workspaces in the background, and forces a recalculate on a outdated diff"
	[Tags]				187.5
	${gv1} =            Get Id From Workspace       CreateWorkspace1
	${gv2} =            Get Id From Workspace       CreateWorkspace2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/latest/latest?background=true&recalculate=true&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateWorkspaceAgain1
	[Documentation]		"Regression Test: 188. Create workspace for another diff test"
	[Tags]				188
	${gv7} =            Get Copy Time From JSON     PostToWorkspace3        elements
	${result} =			Post		${ROOT}/workspaces/wsG1?sourceWorkspace=master&copyTime=${gv7}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateWorkspaceAgain2
	[Documentation]		"Regression Test: 189. Create workspace for another diff test"
	[Tags]				189
	${gv7} =            Get Copy Time From JSON     PostToWorkspace3        elements
	${result} =			Post		${ROOT}/workspaces/wsG2?sourceWorkspace=master&copyTime=${gv7}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceG1ForCMED533
	[Documentation]		"Regression Test: 190. Post elements to workspace wsG1 for testing CMED-533"
	[Tags]				190
	${post_json} =		Get File	 ../JsonData/elementsForBothWorkspaces.json
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceG1
	[Documentation]		"Regression Test: 191. Post element to workspace wsG1"
	[Tags]				191
	${post_json} =		Get File	 ../JsonData/x.json
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Post		url=${ROOT}/workspaces/${gv1}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToMaster
	[Documentation]		"Regression Test: 192. Post element to master for a later diff"
	[Tags]				192
	${post_json} =		Get File	 ../JsonData/y.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceG2ForCMED533
	[Documentation]		"Regression Test: 193. Post elements to workspace wsG2 for testing CMED-533"
	[Tags]				193
	${post_json} =		Get File	 ../JsonData/elementsForBothWorkspaces.json
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${result} =			Post		url=${ROOT}/workspaces/${gv2}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToWorkspaceG2
	[Documentation]		"Regression Test: 194. Post element to workspace wsG2"
	[Tags]				194
	${post_json} =		Get File	 ../JsonData/z.json
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${result} =			Post		url=${ROOT}/workspaces/${gv2}/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2ForMerge
	[Documentation]		"Regression Test: 195. Compare workspaces wsG1 and wsG2 with timestamps for a merge"
	[Tags]				195
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2
	[Documentation]		"Regression Test: 195.5. Compare workspaces wsG1 and wsG2 with timestamps"
	[Tags]				195.5
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2ForMergeBackground
	[Documentation]		"Regression Test: 196. Compare workspaces wsG1 and wsG2 with timestamps for a merge in the background to set up a initial diff for the next test"
	[Tags]				196
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?background=true&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier		 diffTime
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2Background
	[Documentation]		"Regression Test: 196.5. Compare workspaces wsG1 and wsG2 with timestamps in background to set up a initial diff for the next test"
	[Tags]				196.5
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?background=true&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier		 diffTime
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2ForMergeGlom
	[Documentation]		"Regression Test: 197. Compare workspaces wsG1 and wsG2 with timestamps for a merge with an initial diff"
	[Tags]				197
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesG1G2Glom
	[Documentation]		"Regression Test: 197.5. Compare workspaces wsG1 and wsG2 with timestamps with a initial diff"
	[Tags]				197.5
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${gv2} =            Get Id From Workspace       CreateWorkspaceAgain2
	${gv3} =            Get Id From Workspace       PostToWorkspaceG1
	${gv4} =            Get Id From Workspace       PostToWorkspaceG2
	${result} =			Get		url=${ROOT}/diff/${gv1}/${gv2}/${gv3}/${gv4}?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

RecursiveGetOnWorkspaces
	[Documentation]		"Regression Test: 198. Makes sure that a recursive get on a modified workspace returns the modified elements"
	[Tags]				198
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/workspaces/${gv1}/elements/302?recurse=true
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostSiteInWorkspace
	[Documentation]		"Regression Test: 199. Create a project and site in a workspace"
	[Tags]				199
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Post		${ROOT}/workspaces/${gv1}/sites/site_in_ws/projects?createSite=true
	Should Be Equal		${result.status_code}		${400}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetSiteInWorkspace
	[Documentation]		"Regression Test: 200. Get site in workspace"
	[Tags]				200
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/workspaces/${gv1}/sites
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetProductsInSiteInWorkspace
	[Documentation]		"Regression Test: 201. Get products for a site in a workspace"
	[Tags]				201
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/workspaces/${gv1}/sites/europa/products
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNotInPastToWorkspace
	[Documentation]		"Regression Test: 202. Post element to master workspace for a diff test"
	[Tags]				202
	${post_json} =		Get File	 ../JsonData/notInThePast.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMergeNotInPast
	[Documentation]		"Regression Test: 203. Compare workspace master with itself for a merge at the current time and a time in the past"
	[Tags]				203
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/diff/master/master/latest/${gv1}?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesNotInPast
	[Documentation]		"Regression Test: 203.5. Compare workspace master with itself at the current time and a time in the past"
	[Tags]				203.5
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/diff/master/master/latest/${gv1}?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesForMergeNotInPastBackground
	[Documentation]		"Regression Test: 204. Compare workspace master with itself for a merge at the current time and a time in the past in the background"
	[Tags]				204
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/diff/master/master/latest/${gv1}?background&changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareWorkspacesNotInPastBackground
	[Documentation]		"Regression Test: 204.5. Compare workspace master with itself at the current time and a time in the past in the background"
	[Tags]				204.5
	${gv1} =            Get Id From Workspace       CreateWorkspaceAgain1
	${result} =			Get		url=${ROOT}/diff/master/master/latest/${gv1}?background&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 timestamp		 diffTime		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateParentWorkspace
	[Documentation]		"Regression Test: 205. Create a workspace to be a parent of another"
	[Tags]				205
	${gv1} =            Get Copy Time From JSON     CreateWorkspaceAgain1       elements
	${result} =			Post		${ROOT}/workspaces/parentWorkspace1?sourceWorkspace=master&copyTime=${gv1}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostToMasterAgain
	[Documentation]		"Regression Test: 206. Post new element to master"
	[Tags]				206
	${post_json} =		Get File	 ../JsonData/a.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateSubworkspace
	[Documentation]		"Regression Test: 207. Create workspace inside a workspace"
	[Tags]				207
	${gv1} =            Get Id From Workspace       CreateParentWorkspace
	${gv2} =            Get Copy Time From JSON     CreateWorkspaceAgain2       workspaces
	${result} =			Post		${ROOT}/workspaces/subworkspace1?sourceWorkspace=${gv1}&copyTime=${gv2}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId		 parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetElementInMasterFromSubworkspace
	[Documentation]		"Regression Test: 208. Get an element that only exists in the master from a subworkspace after its parent branch was created but before the it was created, it wont find the element"
	[Tags]				208
	${gv3} =            Get Id From Workspace       CreateSubworkspace
	${result} =			Get		url=${ROOT}/workspaces/${gv3}/elements/a
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostAToMaster
	[Documentation]		"Regression Test: 209. Post element a to master."
	[Tags]				209
	${post_json} =		Get File	 ../JsonData/a.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateAParentWorkspace
	[Documentation]		"Regression Test: 210. Create a "parent" workspace off of master.."
	[Tags]				210
	${result} =			Post		${ROOT}/workspaces/theParentWorkspace?sourceWorkspace=master
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostBToMaster
	[Documentation]		"Regression Test: 211. Post element b to master."
	[Tags]				211
	${post_json} =		Get File	 ../JsonData/b.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostCToParent
	[Documentation]		"Regression Test: 212. Post element c to the parent workspace."
	[Tags]				212
	${post_json} =		Get File	 ../JsonData/c.json
	${result} =			Post		url=${ROOT}/workspaces/theParentWorkspace/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CreateASubWorkspace
	[Documentation]		"Regression Test: 213. Create a "subworkspace" workspace off of the parent."
	[Tags]				213
	${result} =			Post		${ROOT}/workspaces/theSubworkspace?sourceWorkspace=theParentWorkspace
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 branched		 _created		 sysmlId		 qualifiedId		 parent
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostDToMaster
	[Documentation]		"Regression Test: 214. Post element d to master."
	[Tags]				214
	${post_json} =		Get File	 ../JsonData/d.json
	${result} =			Post		url=${ROOT}/workspaces/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostEToParent
	[Documentation]		"Regression Test: 215. Post element e to the parent workspace."
	[Tags]				215
	${post_json} =		Get File	 ../JsonData/e.json
	${result} =			Post		url=${ROOT}/workspaces/theParentWorkspace/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostFToSubworkspace
	[Documentation]		"Regression Test: 216. Post element f to the subworkspace."
	[Tags]				216
	${post_json} =		Get File	 ../JsonData/f.json
	${result} =			Post		url=${ROOT}/workspaces/theSubworkspace/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetAInMaster
	[Documentation]		"Regression Test: 217. Get element a in the master workspace."
	[Tags]				217
	${result} =			Get		url=${ROOT}/workspaces/master/elements/a
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetAInParent
	[Documentation]		"Regression Test: 218. Get element a in the parent workspace."
	[Tags]				218
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/a
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetAInSubworkspace
	[Documentation]		"Regression Test: 219. Get element a in the subworkspace."
	[Tags]				219
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/a
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetBInMaster
	[Documentation]		"Regression Test: 220. Get element b in the master workspace."
	[Tags]				220
	${result} =			Get		url=${ROOT}/workspaces/master/elements/b
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetBInParent
	[Documentation]		"Regression Test: 221. Get element b in the parent workspace."
	[Tags]				221
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/b
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetBInSubworkspace
	[Documentation]		"Regression Test: 222. Get element b in the subworkspace."
	[Tags]				222
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/b
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetCInMaster
	[Documentation]		"Regression Test: 223. Get element c in the master workspace."
	[Tags]				223
	${result} =			Get		url=${ROOT}/workspaces/master/elements/c
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetCInParent
	[Documentation]		"Regression Test: 224. Get element c in the parent workspace."
	[Tags]				224
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/c
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetCInSubworkspace
	[Documentation]		"Regression Test: 225. Get element c in the subworkspace."
	[Tags]				225
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/c
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDInMaster
	[Documentation]		"Regression Test: 226. Get element d in the master workspace."
	[Tags]				226
	${result} =			Get		url=${ROOT}/workspaces/master/elements/d
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDInParent
	[Documentation]		"Regression Test: 227. Get element d in the parent workspace."
	[Tags]				227
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/d
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetDInSubworkspace
	[Documentation]		"Regression Test: 228. Get element d in the subworkspace."
	[Tags]				228
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/d
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetEInMaster
	[Documentation]		"Regression Test: 229. Get element e in the master workspace."
	[Tags]				229
	${result} =			Get		url=${ROOT}/workspaces/master/elements/e
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetEInParent
	[Documentation]		"Regression Test: 230. Get element e in the parent workspace."
	[Tags]				230
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/e
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetEInSubworkspace
	[Documentation]		"Regression Test: 231. Get element e in the subworkspace."
	[Tags]				231
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/e
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetFInMaster
	[Documentation]		"Regression Test: 232. Get element f in the master workspace."
	[Tags]				232
	${result} =			Get		url=${ROOT}/workspaces/master/elements/f
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetFInParent
	[Documentation]		"Regression Test: 233. Get element f in the parent workspace."
	[Tags]				233
	${result} =			Get		url=${ROOT}/workspaces/theParentWorkspace/elements/f
	Should Be Equal		${result.status_code}		${404}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetFInSubworkspace
	[Documentation]		"Regression Test: 234. Get element f in the subworkspace."
	[Tags]				234
	${result} =			Get		url=${ROOT}/workspaces/theSubworkspace/elements/f
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterAToLatestForMerge
	[Documentation]		"Regression Test: 235. Compare master to itself for a merge between post time of a and latest"
	[Tags]				235
	${result} =			Get		url=${ROOT}/diff/master/master/2015-08-24T08:46:58.502-0700/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterAToLatest
	[Documentation]		"Regression Test: 235.5. Compare master to itself between post time of a and latest"
	[Tags]				235.5
	${result} =			Get		url=${ROOT}/diff/master/master/2015-08-24T08:46:58.502-0700/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterBToLatestForMerge
	[Documentation]		"Regression Test: 236. Compare master to itself for a merge between the post times of b and latest"
	[Tags]				236
	${result} =			Get		url=${ROOT}/diff/master/master/2015-08-27T15:40:26.891-0700/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterBToLatest
	[Documentation]		"Regression Test: 236.5. Compare master to itself between the post times of b and latest"
	[Tags]				236.5
	${result} =			Get		url=${ROOT}/diff/master/master/2015-08-27T15:40:26.891-0700/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterParentLatestToLatestForMerge
	[Documentation]		"Regression Test: 237. Compare master to theParentWorkspace for a merge with timepoints latest and latest"
	[Tags]				237
	${result} =			Get		url=${ROOT}/diff/master/theParentWorkspace/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterParentLatestToLatest
	[Documentation]		"Regression Test: 237.5. Compare master to theParentWorkspace with timepoints latest and latest"
	[Tags]				237.5
	${result} =			Get		url=${ROOT}/diff/master/theParentWorkspace/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterParentBranchTimeToLatest
	[Documentation]		"Regression Test: 238. Compare master to theParentWorkspace with timepoints at creation of parent and latest"
	[Tags]				238
	${result} =			Get		url=${ROOT}/diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterParentBranchTimeToLatestForMerge
	[Documentation]		"Regression Test: 238.5. Compare master to theParentWorkspace with timepoints for a merge at creation of parent and latest"
	[Tags]				238.5
	${result} =			Get		url=${ROOT}/diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest&fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterSubworkspaceLatestToLatestForMerge
	[Documentation]		"Regression Test: 239. Compare master to theSubworkspace for a merge with timepoints at latest and latest"
	[Tags]				239
	${result} =			Get		url=${ROOT}/diff/master/theSubworkspace/latest/latest?changesForMerge
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

CompareMasterSubworkspaceLatestToLatest
	[Documentation]		"Regression Test: 239.5. Compare master to theSubworkspace with timepoints at latest and latest"
	[Tags]				239.5
	${result} =			Get		url=${ROOT}/diff/master/theSubworkspace/latest/latest?fullCompare
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 sysmlId		 qualifiedId		 _creator		 _modifier
	Generate JSON			${TEST_NAME}		${result.json()}		${filter}
	Sleep				.5s
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}