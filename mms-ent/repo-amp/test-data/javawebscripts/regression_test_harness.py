#
# The regression harness.  Define the tests to run in this file.
#
# TODO:
#    -Have script check test list to ensure unique numbers/names
#    -Fix run server if we use it
#    -Get SOAP UI tests working
#    -Add ability to run test functions instead of just curl string commands

from regression_lib import *


##########################################################################################
#
# TABLE OF ALL POSSIBLE TESTS TO RUN
#     MUST HAVE A UNIQUE TEST NUMBER AND UNIQUE TEST NAME
#
##########################################################################################
tests = [\

# [
# Test Number,
# Test Name,
# Test Description,
# Curl Cmd,
# Use JsonDiff,
# Output Filters (ie lines in the .json output with these strings will be filtered out)
# Branch Names that will run this test by default
# Set up function (Optional)
# Post process function (Optional)
# Tear down function (Optional)
# Delay in seconds before running the test (Optional)
# ]

# POSTS: ==========================
[
10,
"PostSite",
"Create a project and site",
create_curl_cmd(type="POST", data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/sites/europa/projects?createSite=true", project_post=True),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

[
20,
"PostElementsNew",
"Post elements to the master branch",
create_curl_cmd(type="POST", data="elementsNew.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
21,
"PostElementsBadOwners",
"Post elements to the master branch that have owners that cant be found",
create_curl_cmd(type="POST", data="badOwners.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
30,
"PostViews",
"Post views",
create_curl_cmd(type="POST", data="views.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
False,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
40,
"PostProducts",
"Post products",
create_curl_cmd(type="POST", data="products.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
False,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# GETS: ==========================

[
45,
"GetSites",
"Get sites",
create_curl_cmd(type="GET", data="sites", base_url=BASE_URL_WS, branch="master/"),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

[
50,
"GetProject",
"Get project",
create_curl_cmd(type="GET", data="sites/europa/projects/123456", base_url=BASE_URL_WS,
                branch="master/"),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

[
51,
"GetProjects",
"Get all projects for master",
create_curl_cmd(type="GET", data="projects", base_url=BASE_URL_WS,
                branch="master/"),
True,
None,
["test", "workspaces", "develop", "develop2"]
],

[
60,
"GetElementsRecursively",
"Get all elements recursively",
create_curl_cmd(type="GET", data="elements/123456?recurse=true", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
61,
"GetElementsDepth0",
"Get elements recursively depth 0",
create_curl_cmd(type="GET", data="elements/123456?depth=0", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
62,
"GetElementsDepth1",
"Get elements recursively depth 1",
create_curl_cmd(type="GET", data="elements/123456?depth=1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
63,
"GetElementsDepth2",
"Get elements recursively depth 2",
create_curl_cmd(type="GET", data="elements/123456?depth=2", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
64,
"GetElementsDepthAll",
"Get elements recursively depth -1",
create_curl_cmd(type="GET", data="elements/123456?depth=-1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
65,
"GetElementsDepthInvalid",
"Get elements recursively depth invalid",
create_curl_cmd(type="GET", data="elements/123456?depth=invalid", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
66,
"GetElementsConnected",
"Get elements that are connected",
create_curl_cmd(type="GET", data="elements/300?recurse=true&connected=true", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
67,
"GetElementsRelationship",
"Get elements that have relationship DirectedRelationship, starting with 302",
create_curl_cmd(type="GET", data="elements/303?recurse=true&connected=true&relationship=DirectedRelationship", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

[
70,
"GetViews",
"Get views",
create_curl_cmd(type="GET", data="views/301", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
80,
"GetViewElements",
"Get view elements",
create_curl_cmd(type="GET", data="views/301/elements", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
90,
"GetProducts",
"Get product",
create_curl_cmd(type="GET", data="products/301", base_url=BASE_URL_WS,
                branch="master/sites/europa/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# Note: Need a delay before doing this search, b/c it will find "some*" within the
#       documentation, which does not get indexed by alfresco right away
[
110,
"GetSearch",
"Get search",
create_curl_cmd(type="GET", data="search?keyword=some*", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces"],
None,
None,
None,
2
],

# [
# 99,
# "GetSearch",
# "Get search",
# create_curl_cmd(type="GET",data="element/search?keyword=some*",base_url=BASE_URL_WS,
#                branch="master/"),
# True,
# common_filters,
# ["foo"],
# None,
# Node,
# None,
# 0
# ],

# DELETES: ==========================

[
120,
"Delete6666",
"Delete element 6666",
create_curl_cmd(type="DELETE", data="elements/6666", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop", "develop2"]
],

# POST CHANGES: ==========================

# Note: currently not an equivalent in workspaces for this URL, but we may add it
#[
#130,
#"PostChange",
#"Post changes to directed relationships only (without owners)",
#create_curl_cmd(type="POST", data="directedrelationships.json", base_url=BASE_URL_WS,
#                branch="master/", post_type="elements"),
#True,
#common_filters,
#["test", "workspaces", "develop", "develop2"],
#None,
#None,
#set_read_to_gv6_delta_gv7
         #],

# CONFIGURATIONS: ==========================

[
140,
"PostConfig",
"Post configuration",
create_curl_cmd(type="POST", data="configuration.json", base_url=BASE_URL_WS,
                branch="master/sites/europa/", post_type="configurations"),
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop", "develop2"]
],

[
150,
"GetConfig",
"Get configurations",
create_curl_cmd(type="GET", data="sites/europa/configurations", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop", "develop2"]
],

[
154,
"PostConfigAgain",
"Post same configuration again",
create_curl_cmd(type="POST", data="configuration.json", base_url=BASE_URL_WS,
                branch="master/", post_type="configurations"),
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop"]
],

[
155,
"GetConfigAgain",
"Get configurations",
create_curl_cmd(type="GET", data="sites/europa/configurations", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop"],
],

# WORKSPACES: ==========================

# This test relies on test 130
[
160,
"CreateWorkspace1",
"Create workspace test 1",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsA?sourceWorkspace=master&copyTime=$gv6"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv1
],

# mimic md where a branch requires a project post to sync the project version
[
161,
"PostProjectWorkspace1",
"Post project to sync branch version for workspace 1",
create_curl_cmd(type="POST", base_url=BASE_URL_WS, post_type="",
                data='\'{"elements":[{"sysmlid":"123456","specialization":{"type":"Project", "projectVersion":"0"}}]}\'',
                branch="/$gv1/sites/europa/projects?createSite=true",
                project_post=True),
True,
common_filters,
["test", "develop"]
],


# This test case depends on CreateWorkspace1 and uses gv1 set by the previous test
[
162,
"CreateWorkspace2",
"Create workspace test 2",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsB?sourceWorkspace=$gv1&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv2,
# 60
],

# mimic md where a branch requires a project post to sync the project version
[
163,
"PostProjectWorkspace2",
"Post project to sync branch version for workspace 2 - sub workspace",
create_curl_cmd(type="POST", base_url=BASE_URL_WS, post_type="",
                data='\'{"elements":[{"sysmlid":"123456","specialization":{"type":"Project", "projectVersion":"0"}}]}\'',
                branch="/$gv2/sites/europa/projects?createSite=true",
                project_post=True),
True,
common_filters,
["test", "develop"]
],


[
164,
"CreateWorkspaceWithJson",
"Create a workspace using a json",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="", data="NewWorkspacePost.json"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
do176
],

# This test case depends on the previous one and uses gv3 set by the previous test
[
165,
"ModifyWorkspaceWithJson",
"Modifies a workspace name/description",
'''curl %s %s '$gv3' "%s"''' % (CURL_FLAGS, CURL_POST_FLAGS, BASE_URL_WS),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
],

[
166,
"GetWorkspaces",
"Get workspaces",
create_curl_cmd(type="GET", base_url=BASE_URL_WS_NOBS, branch=""),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"'],
["test", "workspaces", "develop"]
],

# This test case depends on test 160/170 thats sets gv1,gv2
[
167,
"PostToWorkspace",
"Post element to workspace",
create_curl_cmd(type="POST", data="x.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv2/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_read_to_gv4
],

# This test case depends on test 170 thats sets gv2
[
168,
"CompareWorkspacesForMerge",
"Compare workspaces for a merge of the second into the first",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
# do20
None
],

[
168.5,
"CompareWorkspaces",
"Compare workspaces",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
# do20
None
],

[
169,
"CompareWorkspacesForMergeBackground1",
"Compare workspaces for a merge in the background, this will return that it is in process",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
169.5,
"CompareWorkspacesBackground1",
"Compare workspaces in the background, this will return that it is in process",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
170,
"CompareWorkspacesBackground2",
"Compare workspaces in the background again, this will return the results of the background diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
20
],

[
170.5,
"CompareWorkspacesBackground2",
"Compare workspaces in the background again, this will return the results of the background diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
20
],

[
171,
"CompareWorkspacesGlomForMerge1",
"Compare workspaces for a merge where there is a initial background diff stored",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
],

[
171.5,
"CompareWorkspacesGlom1",
"Compare workspaces where there is a initial background diff stored",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
],

[
172,
"PostToWorkspaceForGlom",
"Post element to workspace",
create_curl_cmd(type="POST",data="glomPost.json",base_url=BASE_URL_WS,
                post_type="elements", branch="$gv2/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
None,
],

[
173,
"CompareWorkspacesGlomForMerge2",
"Compare workspaces for a merge where there is a initial background diff stored and a change has been made since then.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
],

[
173.5,
"CompareWorkspacesGlom2",
"Compare workspaces where there is a initial background diff stored and a change has been made since then.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
None,
],

# This test case depends on the previous one and uses gv4 set by the previous test
[
174,
"CreateWorkspaceWithBranchTime",
"Create workspace with a branch time",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsT?sourceWorkspace=$gv1&copyTime=$gv4"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv5
],

# This test case depends on the previous one
[
175,
"PostToWorkspaceWithBranchTime",
"Post element to workspace with a branch time",
create_curl_cmd(type="POST", data="y.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv5/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# This test case depends on test 160 thats sets gv1
[
176,
"PostToWorkspaceForConflict1",
"Post element to workspace1 so that we get a conflict",
create_curl_cmd(type="POST", data="conflict1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# This test case depends on test 220 thats sets gv5
[
177,
"PostToWorkspaceForConflict2",
"Post element to workspace with a branch time so that we get a conflict",
create_curl_cmd(type="POST", data="conflict2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv5/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# This test case depends on test 220 thats sets gv5
[
178,
"PostToWorkspaceForMoved",
"Post element to workspace with a branch time so that we get a moved element",
create_curl_cmd(type="POST", data="moved.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv5/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# This test case depends on test 220 thats sets gv5
[
179,
"PostToWorkspaceForTypeChange",
"Post element to workspace with a branch time so that we get a type change",
create_curl_cmd(type="POST", data="typeChange.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv5/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# This test case depends on test 160 thats sets gv1
[
180,
"PostToWorkspaceForWs1Change",
"Post element to workspace1 so that we dont detect it in the branch workspace.  Changes 303",
create_curl_cmd(type="POST", data="modified303.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

[
181,
"GetElement303",
"Get element 303",
create_curl_cmd(type="GET", data="elements/303", base_url=BASE_URL_WS,
                branch="$gv5/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

# This test case depends on the previous two
[
182,
"CompareWorkspacesWithBranchTimeForMerge",
"Compare workspaces with branch times for a merge",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv5/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
182.5,
"CompareWorkspacesWithBranchTime",
"Compare workspaces",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv5/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

# This test case depends on previous ones
[
183,
"PostToWorkspace3",
"Post element z to workspace",
create_curl_cmd(type="POST", data="z.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_read_to_gv7
],

# This test case depends on previous ones
[
184,
"CreateWorkspaceWithBranchTime2",
"Create workspace with a branch time using the current time for the branch time",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsT2?sourceWorkspace=$gv1&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv6
],

# This test case depends on the previous ones
[
185,
"CompareWorkspacesWithBranchTimesForMerge",
"Compare workspaces each of which with a branch time and with a modified element on the common parent for a merge",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv5/$gv6/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
185.5,
"CompareWorkspacesWithBranchTimes",
"Compare workspaces both which have a branch time and with a modified element on the common parent",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv5/$gv6/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
186,
"CompareWorkspacesForMergeBackgroundOutdated",
"Compare workspaces for a merge in the background, this will return that it is outdated",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
186.5,
"CompareWorkspacesBackgroundOutdated",
"Compare workspaces in the background, this will return that it is outdated",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
187,
"CompareWorkspacesForMergeBackgroundRecalculate",
"Compare workspaces for a merge in the background, and forces a recalculate on a outdated diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background=true&recalculate=true&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
187.5,
"CompareWorkspacesBackgroundRecalculate",
"Compare workspaces in the background, and forces a recalculate on a outdated diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?background=true&recalculate=true&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
188,
"CreateWorkspaceAgain1",
"Create workspace for another diff test",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsG1?sourceWorkspace=master&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv1
],

[
189,
"CreateWorkspaceAgain2",
"Create workspace for another diff test",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsG2?sourceWorkspace=master&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv2
],

# This is to test CMED-533.  Where we post the same elements to two different workspaces and diff.
[
190,
"PostToWorkspaceG1ForCMED533",
"Post elements to workspace wsG1 for testing CMED-533",
create_curl_cmd(type="POST", data="elementsForBothWorkspaces.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"],
],

# This test case depends on test 234
[
191,
"PostToWorkspaceG1",
"Post element to workspace wsG1",
create_curl_cmd(type="POST", data="x.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_read_to_gv3
],

[
192,
"PostToMaster",
"Post element to master for a later diff",
create_curl_cmd(type="POST", data="y.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# This is to test CMED-533.  Where we post the same elements to two different workspaces and diff.
[
193,
"PostToWorkspaceG2ForCMED533",
"Post elements to workspace wsG2 for testing CMED-533",
create_curl_cmd(type="POST", data="elementsForBothWorkspaces.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv2/"),
True,
common_filters,
["test", "workspaces", "develop"],
],

# This test case depends on test 235
[
194,
"PostToWorkspaceG2",
"Post element to workspace wsG2",
create_curl_cmd(type="POST", data="z.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv2/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_read_to_gv4
],

# This test case depends on test 234 and 235
[
195,
"CompareWorkspacesG1G2ForMerge",
"Compare workspaces wsG1 and wsG2 with timestamps for a merge",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
195.5,
"CompareWorkspacesG1G2",
"Compare workspaces wsG1 and wsG2 with timestamps",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
196,
"CompareWorkspacesG1G2ForMergeBackground",
"Compare workspaces wsG1 and wsG2 with timestamps for a merge in the background to set up a initial diff for the next test",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?background=true&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"', '"diffTime"'],
["test", "workspaces", "develop"]
],

[
196.5,
"CompareWorkspacesG1G2Background",
"Compare workspaces wsG1 and wsG2 with timestamps in background to set up a initial diff for the next test",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?background=true&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"', '"diffTime"'],
["test", "workspaces", "develop"]
],

[
197,
"CompareWorkspacesG1G2ForMergeGlom",
"Compare workspaces wsG1 and wsG2 with timestamps for a merge with an initial diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
197.5,
"CompareWorkspacesG1G2Glom",
"Compare workspaces wsG1 and wsG2 with timestamps with a initial diff",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/$gv3/$gv4?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

# This test case depends on test 220 this makes sure that recursive get on ws gets
# everything as expected (we should also do something like 225 to make sure a modified element
# shows up in the recurse properly as well)
[
198,
"RecursiveGetOnWorkspaces",
"Makes sure that a recursive get on a modified workspace returns the modified elements",
create_curl_cmd(type="GET", base_url=BASE_URL_WS,
                branch="$gv1/elements/302?recurse=true"),
True,
common_filters,
["test", "develop"]
],

[
199,
"PostSiteInWorkspace",
"Create a project and site in a workspace",
create_curl_cmd(type="POST", data='\'{"elements":[{"sysmlid":"proj_id_001","name":"PROJ_1","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="$gv1/sites/site_in_ws/projects?createSite=true", project_post=True),
False,
None,
["test", "workspaces", "develop"]
],

[
200,
"GetSiteInWorkspace",
"Get site in workspace",
create_curl_cmd(type="GET", data="sites", base_url=BASE_URL_WS, branch="$gv1/"),
False,
None,
["test", "workspaces", "develop"]
],


[
201,
"GetProductsInSiteInWorkspace",
"Get products for a site in a workspace",
create_curl_cmd(type="GET", data="products", base_url=BASE_URL_WS,
                branch="$gv1/sites/europa/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

[
202,
"PostNotInPastToWorkspace",
"Post element to master workspace for a diff test",
create_curl_cmd(type="POST", data="notInThePast.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
None,
set_read_delta_to_gv1,
10
],

# This test depends on the previous one:
[
203,
"CompareWorkspacesForMergeNotInPast",
"Compare workspace master with itself for a merge at the current time and a time in the past",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/master/latest/$gv1?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop", "develop2"]
],

[
203.5,
"CompareWorkspacesNotInPast",
"Compare workspace master with itself at the current time and a time in the past",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/master/latest/$gv1?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"creator"', '"modifier"'],
["test", "workspaces", "develop", "develop2"]
],

[
204,
"CompareWorkspacesForMergeNotInPastBackground",
"Compare workspace master with itself for a merge at the current time and a time in the past in the background",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/master/latest/$gv1?background&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop", "develop2"]
],

[
204.5,
"CompareWorkspacesNotInPastBackground",
"Compare workspace master with itself at the current time and a time in the past in the background",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/master/latest/$gv1?background&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"', '"diffTime"', '"creator"', '"modifier"'],
["test", "workspaces", "develop", "develop2"]
],

# A series of test cases for workspaces in workspaces

[
205,
"CreateParentWorkspace",
"Create a workspace to be a parent of another",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="parentWorkspace1?sourceWorkspace=master&copyTime=$gv1"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop", "develop2"],
None,
None,
set_wsid_to_gv1
],

[
206,
"PostToMasterAgain",
"Post new element to master",
create_curl_cmd(type="POST", data="a.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
None,
set_read_delta_to_gv2
],

[
207,
"CreateSubworkspace",
"Create workspace inside a workspace",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="subworkspace1?sourceWorkspace=$gv1&copyTime=$gv2"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"'],
["test", "workspaces", "develop", "develop2"],
None,
None,
set_wsid_to_gv3,
30
],

[
208,
"GetElementInMasterFromSubworkspace",
"Get an element that only exists in the master from a subworkspace after its parent branch was created but before the it was created, it wont find the element",
create_curl_cmd(type="GET", data="elements/a", base_url=BASE_URL_WS,
                branch="$gv3/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
209,
"PostAToMaster",
"Post element a to master.",
create_curl_cmd(type="POST", data="a.json", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
210,
"CreateAParentWorkspace",
"Create a \"parent\" workspace off of master..",
create_curl_cmd(type="POST", data="", base_url=BASE_URL_WS, post_type="", branch="theParentWorkspace?sourceWorkspace=master", project_post=False),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "develop"]
],

[
211,
"PostBToMaster",
"Post element b to master.",
create_curl_cmd(type="POST", data="b.json", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
212,
"PostCToParent",
"Post element c to the parent workspace.",
create_curl_cmd(type="POST", data="c.json", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
213,
"CreateASubWorkspace",
"Create a \"subworkspace\" workspace off of the parent.",
create_curl_cmd(type="POST", data="", base_url=BASE_URL_WS, post_type="", branch="theSubworkspace?sourceWorkspace=theParentWorkspace", project_post=False),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"', '"parent"'],
["test", "develop"]
],

[
214,
"PostDToMaster",
"Post element d to master.",
create_curl_cmd(type="POST", data="d.json", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
215,
"PostEToParent",
"Post element e to the parent workspace.",
create_curl_cmd(type="POST", data="e.json", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
216,
"PostFToSubworkspace",
"Post element f to the subworkspace.",
create_curl_cmd(type="POST", data="f.json", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
217,
"GetAInMaster",
"Get element a in the master workspace.",
create_curl_cmd(type="GET", data="elements/a", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
218,
"GetAInParent",
"Get element a in the parent workspace.",
create_curl_cmd(type="GET", data="elements/a", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
219,
"GetAInSubworkspace",
"Get element a in the subworkspace.",
create_curl_cmd(type="GET", data="elements/a", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
220,
"GetBInMaster",
"Get element b in the master workspace.",
create_curl_cmd(type="GET", data="elements/b", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
221,
"GetBInParent",
"Get element b in the parent workspace.",
create_curl_cmd(type="GET", data="elements/b", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
222,
"GetBInSubworkspace",
"Get element b in the subworkspace.",
create_curl_cmd(type="GET", data="elements/b", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
223,
"GetCInMaster",
"Get element c in the master workspace.",
create_curl_cmd(type="GET", data="elements/c", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
224,
"GetCInParent",
"Get element c in the parent workspace.",
create_curl_cmd(type="GET", data="elements/c", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
225,
"GetCInSubworkspace",
"Get element c in the subworkspace.",
create_curl_cmd(type="GET", data="elements/c", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
226,
"GetDInMaster",
"Get element d in the master workspace.",
create_curl_cmd(type="GET", data="elements/d", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
227,
"GetDInParent",
"Get element d in the parent workspace.",
create_curl_cmd(type="GET", data="elements/d", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
228,
"GetDInSubworkspace",
"Get element d in the subworkspace.",
create_curl_cmd(type="GET", data="elements/d", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
229,
"GetEInMaster",
"Get element e in the master workspace.",
create_curl_cmd(type="GET", data="elements/e", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
230,
"GetEInParent",
"Get element e in the parent workspace.",
create_curl_cmd(type="GET", data="elements/e", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
231,
"GetEInSubworkspace",
"Get element e in the subworkspace.",
create_curl_cmd(type="GET", data="elements/e", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
232,
"GetFInMaster",
"Get element f in the master workspace.",
create_curl_cmd(type="GET", data="elements/f", base_url=BASE_URL_WS, post_type="elements", branch="master/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
233,
"GetFInParent",
"Get element f in the parent workspace.",
create_curl_cmd(type="GET", data="elements/f", base_url=BASE_URL_WS, post_type="elements", branch="theParentWorkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
234,
"GetFInSubworkspace",
"Get element f in the subworkspace.",
create_curl_cmd(type="GET", data="elements/f", base_url=BASE_URL_WS, post_type="elements", branch="theSubworkspace/", project_post=False),
True,
common_filters,
["test", "develop"]
],

[
235,
"CompareMasterAToLatestForMerge",
"Compare master to itself for a merge between post time of a and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/master/2015-08-24T08:46:58.502-0700/latest?changesForMerge", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
235.5,
"CompareMasterAToLatest",
"Compare master to itself between post time of a and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/master/2015-08-24T08:46:58.502-0700/latest?fullCompare", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
236,
"CompareMasterBToLatestForMerge",
"Compare master to itself for a merge between the post times of b and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/master/2015-08-27T15:40:26.891-0700/latest?changesForMerge", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
236.5,
"CompareMasterBToLatest",
"Compare master to itself between the post times of b and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/master/2015-08-27T15:40:26.891-0700/latest?fullCompare", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
237,
"CompareMasterParentLatestToLatestForMerge",
"Compare master to theParentWorkspace for a merge with timepoints latest and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theParentWorkspace/latest/latest?changesForMerge", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
237.5,
"CompareMasterParentLatestToLatest",
"Compare master to theParentWorkspace with timepoints latest and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theParentWorkspace/latest/latest?fullCompare", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
238,
"CompareMasterParentBranchTimeToLatest",
"Compare master to theParentWorkspace with timepoints at creation of parent and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest?changesForMerge", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
238.5,
"CompareMasterParentBranchTimeToLatestForMerge",
"Compare master to theParentWorkspace with timepoints for a merge at creation of parent and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theParentWorkspace/2015-08-24T08:47:10.054-0700/latest&fullCompare", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
239,
"CompareMasterSubworkspaceLatestToLatestForMerge",
"Compare master to theSubworkspace for a merge with timepoints at latest and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theSubworkspace/latest/latest?changesForMerge", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],

[
239.5,
"CompareMasterSubworkspaceLatestToLatest",
"Compare master to theSubworkspace with timepoints at latest and latest",
create_curl_cmd(type="GET", data="", base_url="http://localhost:8080/alfresco/service/", post_type="elements", branch="diff/master/theSubworkspace/latest/latest?fullCompare", project_post=False),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
[]
],



# SNAPSHOTS: ==========================

# This functionality is deprecated:
# [
# 240,
# "PostSnapshot",
# "Post snapshot test",
# create_curl_cmd(type="POST",base_url=BASE_URL_WS,
#                 branch="master/sites/europa/products/301/",
#                 post_type="snapshots"),
# True,
# common_filters+['"created"','"id"','"url"'],
# ["test","workspaces","develop", "develop2"],
# None,
# None,
# None,
# 3
# ],

# EXPRESSIONS: ==========================

# Note: currently not an equivalent in workspaces for this URL, but we may add it
[
255,
"SolveConstraint",
"Post expressions with a constraint and solve for the constraint.",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="expressionElementsNew.json",
                branch="master/",
                post_type="elements?fix=true"),
True,
common_filters,
["test", "workspaces"]
],

[
260,
"PostDemo1",
"Post data for demo 1 of server side docgen",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="BluCamNameListExpr.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters + ['"sysmlid"', '"qualifiedId"', '"message"'],
["test", "workspaces", "develop", "develop2"]
],

    # Temporarily removed this from develop -- fix and add back in! FIXME
[
270,
"Demo1",
"Server side docgen demo 1",
create_curl_cmd(type="GET", data="views/_17_0_2_3_e610336_1394148311476_17302_29388", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop2"]
],

[
280,
"PostDemo2",
"Post data for demo 2 of server side docgen",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="BLUCamTest.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop", "develop2"]
],

[
290,
"Demo2",
"Server side docgen demo 2",
create_curl_cmd(type="GET", data="views/_17_0_2_3_e610336_1394148233838_91795_29332", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
292,
"PostSiteAndProject3",
"Create a site and a project for demo 3 of server side docgen",
create_curl_cmd(type="POST", data='\'{"elements":[{"sysmlid":"PROJECT-71724d08-6d79-42b2-b9ec-dc39f20a3660","name":"BikeProject","specialization":{"type":"Project"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/sites/demo3site/projects?createSite=true", project_post=True),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

[
293,
"PostDemo3",
"Post data for demo 3 of server side docgen",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="bike.json",
                branch="master/sites/demo3site/",
                post_type="elements"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop", "develop2"]
],

[
294,
"PostViewDemo3",
"Post view data for demo 3 of server side docgen",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="connectorView.json",
                branch="master/sites/demo3site/",
                post_type="elements"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop", "develop2"]
],

[
295,
"Demo3",
"Server side docgen demo 3",
create_curl_cmd(type="GET", data="views/_17_0_2_3_e610336_1394148311476_17302_29388_X", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop2", ]
],




# NEW URLS: ==========================

[
300,
"GetSites2",
"Get all the sites for a workspace",
create_curl_cmd(type="GET", data="sites", base_url=BASE_URL_WS,
                branch="master/"),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

[
310,
"GetProductViews",
"Get all views for a product",
create_curl_cmd(type="GET", data="products/301/views", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# Getting the view elements tested in GetViewElements

[
320,
"PostElementX",
"Post element to the master branch/site",
create_curl_cmd(type="POST", data="x.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/sites/europa/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
None,
set_read_to_gv7
],

# Posting a new project/site tested in PostSite

[
330,
"UpdateProject",
"Update a project",
create_curl_cmd(type="POST", data='\'{"elements":[{"sysmlid":"123456","name":"JW_TEST2","specialization":{"type":"Project","projectVersion":"1"}}]}\'',
                base_url=BASE_URL_WS,
                branch="master/projects", project_post=True),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

# Get project w/ site included tested in GetProject

[
340,
"GetProjectOnly",
"Get project w/o specifying the site",
create_curl_cmd(type="GET", data="projects/123456", base_url=BASE_URL_WS,
                branch="master/"),
False,
None,
["test", "workspaces", "develop", "develop2"]
],

# ARTIFACTS: ==========================

[
350,
"PostArtifact",
"Post artifact to the master branch",
'curl %s %s -H "Content-Type: multipart/form-data;" --form "file=@JsonData/x.json" --form "title=JsonData/x.json" --form "desc=stuffs" --form "content=@JsonData/x.json" %smaster/sites/europa/artifacts/folder1/folder2/xartifact' % (CURL_FLAGS, CURL_POST_FLAGS_NO_DATA, BASE_URL_WS),
True,
None,
["test", "workspaces", "develop", "develop2"]
],

[
360,
"GetArtifact",
"Get artifact from the master branch",
create_curl_cmd(type="GET", data="artifacts/xartifact?extension=svg&cs=3463563326", base_url=BASE_URL_WS,
                branch="master/"),
False,
['"url"'],
["test", "workspaces", "develop", "develop2"]
],

[
370,
"CreateWorkspaceDelete1",
"Create workspace to be deleted",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="AA?sourceWorkspace=master&copyTime=$gv7"),
True,
common_filters + ['"parent"', '"id"', '"qualifiedId"', '"branched"'],
["test", "develop"],
None,
None,
set_wsid_to_gv1
],

# This test depends on the previous one for gv1
[
380,
"CreateWorkspaceDelete2",
"Create workspace to be deleted",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="BB?sourceWorkspace=$gv1&copyTime=$gv7"),
True,
common_filters + ['"parent"', '"id"', '"qualifiedId"', '"branched"'],
["test", "develop"]
],

# This test depends on 370 for gv1
[
390,
"DeleteWorkspace",
"Delete workspace and its children",
create_curl_cmd(type="DELETE", base_url=BASE_URL_WS,
                post_type="", branch="$gv1"),
True,
common_filters + ['"parent"', '"id"', '"qualifiedId"'],
["test", "develop"]
],

[
400,
"CheckDeleted1",
"Make sure that AA and its children no longer show up in workspaces",
create_curl_cmd(type="GET", base_url=BASE_URL_WS_NOBS,
                post_type="", branch=""),
True,
common_filters + ['"parent"', '"id"', '"qualifiedId"', '"branched"'],
["test"]
],

[
410,
"CheckDeleted2",
"Make sure that AA and its children show up in deleted",
create_curl_cmd(type="GET", base_url=BASE_URL_WS_NOBS,
                post_type="", branch="?deleted"),
True,
common_filters + ['"parent"', '"id"', '"qualifiedId"', '"branched"'],
["test", "develop"]
],

# # TODO: placeholder to put in post to get back workspace A (need the ID from 380)
[
420,
"UnDeleteWorkspace",
"Undelete workspace",
'echo "temporary placeholder"',
False,
None,
["test", "develop"]
],

# SITE PACKAGES: ==========================
# Cant run these tests in regression b/c you need
# to bring up share also.
[
430,
"PostSitePackage",
"Create a site package",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="SitePackage.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters,
[]
],

[
440,
"PostElementSitePackage",
"Post a product to a site package",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="ElementSitePackage.json",
                branch="master/sites/site_package/",
                post_type="elements"),
True,
common_filters + ['"message"'],
[]
],

[
450,
"GetSitePackageProducts",
"Get site package products",
create_curl_cmd(type="GET", data="products", base_url=BASE_URL_WS,
                branch="master/sites/site_package/"),
True,
common_filters,
[]
],

[
451,
"SitePackageBugTest1",
"Create packages A2, B2, and C2, where A2/B2 are site packages for CMED-871 testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="SitePkgBugTest1.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters,
[]
],

[
452,
"SitePackageBugTest2",
"Moves package B2 under package C2 for CMED-871 testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="SitePkgBugTest2.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters,
[]
],

[
453,
"SitePackageBugTest3",
"Makes package C2 a site package for CMED-871 testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                data="SitePkgBugTest3.json",
                branch="master/sites/europa/",
                post_type="elements"),
True,
common_filters,
[]
],

# CONTENT MODEL UPDATES: ==========================

[
460,
"PostContentModelUpdates",
"Post content model udpates for sysml 2.0",
create_curl_cmd(type="POST", data="contentModelUpdates.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# CMED-416: ==========================

[
470,
"PostDuplicateSysmlNames1",
"Post a element that will be used in the next test to generate a error",
create_curl_cmd(type="POST", data="cmed416_1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

[
480,
"PostDuplicateSysmlNames2",
"Post a element with the same type, sysml name, and parent as the previous test to generate at error",
create_curl_cmd(type="POST", data="cmed416_2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
False,
None,
[]
],

# DOWNGRADING: ==================

[
500,
"PostModelForDowngrade",
"Post model for downgrade test",
create_curl_cmd(type="POST", data="productsDowngrade.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
510,
"PostModelForViewDowngrade",
"Post model for view downgrade",
create_curl_cmd(type="POST", data="viewDowngrade.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
520,
"PostModelForElementDowngrade",
"Post model for element downgrade",
create_curl_cmd(type="POST", data="elementDowngrade.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
None,
set_read_to_gv7
],

# DiffPost (Merge) (CMED-471) Tests: ==================

[
530,
"DiffWorkspaceCreate1",
"Diff Workspace Test - Create WS 1",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="ws1?sourceWorkspace=master&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv1
],

[
540,
"DiffWorkspaceCreate2",
"Diff Workspace Test - Create WS 2",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="ws2?sourceWorkspace=master&copyTime=$gv7"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv2
],

# This test is dependent on 255
# FIXME -- temporarily removed from "develop"
[
550,
"DiffDelete_arg_ev_38307",  # deletes element arg_ev_38307 from ws1
"Diff Workspace Test - Delete element arg_ev_38307",
create_curl_cmd(type="DELETE", data="elements/arg_ev_38307", base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"'],
["test", "workspaces"]
],

[
560,
"DiffPostToWorkspace1",  # posts newElement to ws1
"Diff Workspace Test - Post element to workspace",
create_curl_cmd(type="POST", data="newElement.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
570,
"DiffUpdateElement402",  # changes element 402 documentation to "x is x" in branch ws1
"Diff Workspace Test - Update element 402",
create_curl_cmd(type="POST", data="update402.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# FIXME -- temporarily removed from "develop"
[
580,
"DiffCompareWorkspacesForMerge",
"Diff Workspace Test - Compare workspaces for a merge",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv2/$gv1/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
set_json_output_to_gv3
],

[
580.5,
"DiffCompareWorkspaces",
"Diff Workspace Test - Compare workspaces",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv2/$gv1/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
None,
None,
set_json_output_to_gv3
],


[
581,
"PostDiff",
"Post a diff to merge workspaces",
'curl %s %s \'$gv3\' "%sdiff"' % (CURL_FLAGS, CURL_POST_FLAGS, SERVICE_URL),
True,
common_filters + ['"id"', '"qualifiedId"', '"timestamp"'],
["test", "workspaces", "develop"],
],

# Diff again should be empty.  This test depends on the previous one.
[
582,
"DiffCompareWorkspacesAgainForMerge",
"Diff Workspace Test - Compare workspaces again for a merge and make sure the diff is empty now after merging.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv2/$gv1/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],

[
582.5,
"DiffCompareWorkspacesAgain",
"Diff Workspace Test - Compare workspaces again and make sure the diff is empty now after merging.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv2/$gv1/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"],
],


# EXPRESSION PARSING =====================================================

[
600,
"ParseSimpleExpression",
"Parse \"1 + 1\" from URL and create expression elements",
create_curl_cmd(type="POST", data="operation.json", base_url=BASE_URL_WS,
                post_type="elements?expression=1%2B1", branch="master/"),
True,
common_filters + ['MMS_'],
["test", "workspaces", "develop", "develop2"]
],

[
601,
"ParseAndEvaluateTextExpressionInFile",
"Parse text expression in file, create expression elements for it, and then evaluate the expression elements",
create_curl_cmd(type="POST", data="onePlusOne.k", base_url=BASE_URL_WS,
                post_type="elements?evaluate", branch="master/"),
True,
common_filters + ['MMS_'],
["test", "workspaces", "develop", "develop2"]
],
# PERMISSION TESTING =====================================================

# Creating users for user testing
[
610,
"CreateCollaborator",
"Create Collaborator user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Collaborator", "firstName": "Collaborator", "lastName": "user", "email": "Collaborator@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteCollaborator"]}\'',
                base_url=SERVICE_URL,
                post_type="", branch="api/people", project_post=True),
False,
common_filters + ['MMS_', '"url"'],
["test", "workspaces", "develop", "develop2"]
],

[
611,
"CreateContributor",
"Create Contributor user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Contributor", "firstName": "Contributor", "lastName": "user", "email": "Contributor@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteContributor"]}\'',
                base_url=SERVICE_URL,
                post_type="", branch="api/people", project_post=True),
False,
common_filters + ['MMS_', '"url"'],
["test", "workspaces", "develop", "develop2"]
],

[
612,
"CreateConsumer",
"Create Consumer user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Consumer", "firstName": "Consumer", "lastName": "user", "email": "Consumer@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteConsumer"]}\'',
                base_url=SERVICE_URL,
                post_type="", branch="api/people", project_post=True),
False,
common_filters + ['MMS_', '"url"'],
["test", "workspaces", "develop", "develop2"]
],

[
613,
"CreateManager",
"Create Manager user for europa",
create_curl_cmd(type="POST",
                data='\'{"userName": "Manager", "firstName": "Manager", "lastName": "user", "email": "Manager@jpl.nasa.gov", "groups": ["GROUP_site_europa_SiteManager"]}\'',
                base_url=SERVICE_URL,
                post_type="", branch="api/people", project_post=True),
False,
common_filters + ['MMS_', '"url"'],
["test", "workspaces", "develop", "develop2"]
],

[
614,
"CreateNone",
"Create user with no europa priveleges",
create_curl_cmd(type="POST",
                data='\'{"userName": "None", "firstName": "None", "lastName": "user", "email": "None@jpl.nasa.gov"}\'',
                base_url=SERVICE_URL,
                post_type="", branch="api/people", project_post=True),
False,
common_filters + ['MMS_', '"url"'],
["test", "workspaces", "develop", "develop2"]
],

# lets do the None permissions
[
620,
"NoneRead",
"Read element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
621,
"NoneDelete",
"Delete element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -X DELETE " + BASE_URL_WS + "master/elements/y",
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop", "develop2"]
],

# FIXME -- Many of the followig test cases fail and are being removed from the
# "develop" regression.
[
622,
"NoneUpdate",
"Update element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by None\"}]}'",
True,
common_filters,
["test", "workspaces"]
],

[
623,
"NoneCreate",
"Create element with user None",
"curl -w '\\n%{http_code}\\n' -u None:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychild\",\"documentation\":\"y child\",\"owner\":\"y\"}]}'",
True,
common_filters,
["test", "workspaces"]
],

[
624,
"CollaboratorRead",
"Read element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test", "workspaces"]
],

[
625,
"CollaboratorUpdate",
"Update element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by Collaborator\"}]}'",
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
626,
"CollaboratorCreate",
"Create element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychild\",\"documentation\":\"y child\",\"owner\":\"y\"}]}'",
True,
common_filters,
["test", "workspaces"]
],

[
627,
"CollaboratorDelete",
"Delete element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -X DELETE " + BASE_URL_WS + "master/elements/y",
True,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces"]
],

[
628,
"CollaboratorResurrect",
"Resurrect element with user Collaborator",
"curl -w '\\n%{http_code}\\n' -u Collaborator:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements --data @JsonData/y.json",
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# Consumer permissions

[
630,
"ConsumerRead",
"Read element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -X GET " + BASE_URL_WS + "master/elements/y",
True,
common_filters,
["test", "workspaces"]
],

[
631,
"ConsumerUpdate",
"Update element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"y\",\"documentation\":\"y is modified by Consumer\"}]}'",
False,
common_filters,
["test", "workspaces"],
None,
removeCmNames,
None
],

[
632,
"ConsumerCreate",
"Create element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements -d '{\"elements\":[{\"sysmlid\":\"ychildOfConsumer\",\"documentation\":\"y child of Consumer\",\"owner\":\"y\"}]}'",
False,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
removeCmNames,
None
],

[
633,
"ConsumerDelete",
"Delete element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -X DELETE " + BASE_URL_WS + "master/elements/y",
False,
common_filters + ['"timestamp"', '"id"'],
["test", "workspaces", "develop", "develop2"],
None,
removeCmNames,
None
],

[
634,
"ConsumerResurrect",
"Resurrect element with user Consumer",
"curl -w '\\n%{http_code}\\n' -u Consumer:password -H Content-Type:application/json " + BASE_URL_WS + "master/elements --data @JsonData/y.json",
False,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
removeCmNames,
None
],

# NULL PROPERTIES =====================================================

[
640,
"PostNullElements",
"Post elements to the master branch with null properties",
create_curl_cmd(type="POST", data="nullElements.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# JSON CACHE TESTING =====================================================

# These test post the same element with updates to the embedded value spec.
# This tests that the json cache doesnt return a outdated embedded value spec
[
650,
"TestJsonCache1",
"Post elements for json cache testing.",
create_curl_cmd(type="POST", data="jsonCache1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
651,
"TestJsonCache2",
"Post elements for json cache testing.",
create_curl_cmd(type="POST", data="jsonCache2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
652,
"TestJsonCache3",
"Post elements for json cache testing.",
create_curl_cmd(type="POST", data="jsonCache3.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
653,
"TestJsonCache4",
"Post elements for json cache testing.",
create_curl_cmd(type="POST", data="jsonCache4.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# RESURRECTION TESTING (CMED-430): ==========================

[
660,
"TestResurrection1",
"Post elements for resurrection of parents testing.  Has two parents that will be resurrected.",
create_curl_cmd(type="POST", data="resurrectParents.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

# This test depends on the previous one:
[
661,
"DeleteParents",
"Delete parents",
create_curl_cmd(type="DELETE", data="elements/parentToDelete1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop", "develop2"]
],

[
662,
"TestResurrection2",
"Post elements for resurrection of parents testing.  Has two parents that will be resurrected.",
create_curl_cmd(type="POST", data="resurrectParentsChild.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
663,
"TestGetAfterResurrection",
"Performs a recursive get to make sure the ownedChildren were property set after resurrection.",
create_curl_cmd(type="GET", data="elements/123456?recurse=true", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"MMS_', 'MMS_'],
["test", "workspaces", "develop"]
],

# ELEMENTS PROPERTY SERVICE (CMED-835): ==========================

[
670,
"PostElementsWithProperites",
"Post elements for the next several tests",
create_curl_cmd(type="POST", data="elementsWithProperties.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
671,
"GetSearchSlotProperty",
'Searching for the property "real" having value 5.39 (slot property)',
create_curl_cmd(type="GET", data="search?keyword=5.39&filters=value&propertyName=real", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"],
None,
None,
None,
70
],

[
672,
"GetSearchSlotPropertyOffNom",
'Searching for the property "foo" having value 5.39 (slot property).  This should fail',
create_curl_cmd(type="GET", data="search?keyword=5.39&filters=value&propertyName=foo", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
673,
"GetSearchNonSlotProperty",
'Searching for the property "real55" having value 34.5 (non-slot property)',
create_curl_cmd(type="GET", data="search?keyword=34.5&filters=value&propertyName=real55", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
674,
"GetSearchNonSlotPropertyOffNom",
'Searching for the property "real55" having value 34.5 (non-slot property).  This should fail.',
create_curl_cmd(type="GET",data="search?keyword=34.5&filters=value&propertyName=gg",base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
675,
"GetSearchElementWithProperty",
'Searching for element that owns a Property',
create_curl_cmd(type="GET", data="search?keyword=steroetyped&filters=name", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],


# Make sure aspect version history is accurate (CMED-939)

[
700,
"PostElementsForAspectHistoryCheck",
'Post elements to check for aspect changes in version history',
create_curl_cmd(type="POST", data="elementsForAspectHistoryCheck.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
701,
"CheckIfPostedAspectsInHistory",
"Get the previously posted elements at timestamp=now to see if their type aspects were recorded properly.",
create_curl_cmd(type="GET", data="elements/aspect_history_zzz?recurse=true&timestamp=$gv1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
set_gv1_to_current_time
],

[
702,
"DeleteElementForAspectHistoryCheck",
"Delete a property to see if the Delete aspect is recorded in the version history",
create_curl_cmd(type="DELETE", data="elements/property_zzz", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop", "develop2"]
],

[
703,
"UpdateElementsForAspectHistoryCheck",
'Post updates to element types to check for aspect changes in version history',
create_curl_cmd(type="POST", data="aspectChanges.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop", "develop2"]
],

[
704,
"CheckIfAspectUpdatesInHistory",
"Get the previously updated elements at timestamp=now to see if changes to their type aspects were recorded properly.",
create_curl_cmd(type="GET", data="elements/aspect_history_zzz?recurse=true&timestamp=$gv1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
set_gv1_to_current_time
],

[
705,
"CheckIfAspectDeleteInHistory",
"Get the previously deleted element at timestamp=now to see if the Deleted aspect was recorded properly.",
create_curl_cmd(type="GET", data="elements/property_zzz?timestamp=$gv1", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"]
],

# Testing the diff glom matrix ==========================

[
800,
"PostElementsMatrix1",
"Post elements to the master branch for glom matrix testing",
create_curl_cmd(type="POST", data="elementsMatrix1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv3
],

[
801,
"CreateWorkspaceMatrixTest1",
"Create workspace1 for glom matrix testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsMatrix1?sourceWorkspace=master&copyTime=$gv3"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv1
],

[
802,
"DeleteDeleteAddWsMatrix1",
"Delete delete_add_gg",
create_curl_cmd(type="DELETE",data="elements/delete_add_gg",base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
803,
"DeleteDeleteUpdateWsMatrix1",
"Delete delete_update_gg",
create_curl_cmd(type="DELETE",data="elements/delete_update_gg",base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
804,
"DeleteDeleteDeleteWsMatrix1",
"Delete delete_delete_gg",
create_curl_cmd(type="DELETE",data="elements/delete_delete_gg",base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
805,
"DeleteDeleteNoneWsMatrix1",
"Delete delete_none_gg",
create_curl_cmd(type="DELETE",data="elements/delete_none_gg",base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
806,
"PostElementsWsMatrix1",
"Post elements to the wsMatrix1 branch for glom matrix testing",
create_curl_cmd(type="POST", data="elementsWsMatrix1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv4
],

[
807,
"DeleteUpdateAddMaster",
"Delete update_add_gg",
create_curl_cmd(type="DELETE",data="elements/update_add_gg",base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
808,
"DeleteDeleteAddMaster",
"Delete delete_add_gg",
create_curl_cmd(type="DELETE",data="elements/delete_add_gg",base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
809,
"PostElementsMatrix2",
"Post elements to the master branch for glom matrix testing",
create_curl_cmd(type="POST", data="elementsMatrix2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv5
],

[
810,
"CreateWorkspaceMatrixTest2",
"Create workspace2 for glom matrix testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsMatrix2?sourceWorkspace=master&copyTime=$gv5"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv2
],

[
811,
"DeleteAddDeleteWsMatrix2",
"Delete add_delete_gg",
create_curl_cmd(type="DELETE",data="elements/add_delete_gg",base_url=BASE_URL_WS,
                branch="$gv2/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
812,
"DeleteUpdateDeleteWsMatrix2",
"Delete update_delete_gg",
create_curl_cmd(type="DELETE",data="elements/update_delete_gg",base_url=BASE_URL_WS,
                branch="$gv2/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
813,
"DeleteDeleteDeleteWsMatrix2",
"Delete delete_delete_gg",
create_curl_cmd(type="DELETE",data="elements/delete_delete_gg",base_url=BASE_URL_WS,
                branch="$gv2/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
814,
"DeleteNoneDeleteWsMatrix2",
"Delete none_delete_gg",
create_curl_cmd(type="DELETE",data="elements/none_delete_gg",base_url=BASE_URL_WS,
                branch="$gv2/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
815,
"PostElementsWsMatrix2",
"Post elements to the wsMatrix2 branch for glom matrix testing",
create_curl_cmd(type="POST", data="elementsWsMatrix2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv2/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv6
],

[
816,
"CompareWorkspacesGlomMatrixForMerge",
"Compare workspaces at latest times for glom matrix test.  Does merge style diff.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],

[
817,
"CompareWorkspacesGlomMatrix",
"Compare workspaces at latest times for glom matrix test.  Does full compare style diff.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/$gv1/$gv2/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],


# Testing  the merge-style diff
# tests 900-909 create a branch off of master to test conflicts AA, DD, DU, UD, UU
[
900,
"PostElementsMerge1",
"Post elements to the master branch for merge-style diff testing",
create_curl_cmd(type="POST", data="elementsMasterMerge1.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv3
],

[
900.5,
"DeleteDeleteDeleteBeforeMasterMerge1",
"Delete delete_delete_before",
create_curl_cmd(type="DELETE", data="elements/delete_delete_before", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
901,
"CreateWorkspaceMerge-style-Test1",
"Create workspace1 for merge-style diff testing",
create_curl_cmd(type="POST", base_url=BASE_URL_WS,
                post_type="", branch="wsMerge1?sourceWorkspace=master&copyTime=$gv3"),
True,
common_filters + ['"branched"', '"created"', '"id"', '"qualifiedId"'],
["test", "workspaces", "develop"],
None,
None,
set_wsid_to_gv1
],
[
902,
"DeleteDeleteDeleteMasterMerge1",
"Delete delete_delete_consistent",
create_curl_cmd(type="DELETE", data="elements/delete_delete_consistent", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
903,
"DeleteDeleteUpdateMasterMerge1",
"Delete delete_update_consistent",
create_curl_cmd(type="DELETE", data="elements/delete_update_consistent", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"],
],
[
904,
"PostElementsMasterMerge1",
"Post elements to the MasterMerge1 branch for merge-style diff testing",
create_curl_cmd(type="POST", data="elementsMasterMerge2.json", base_url=BASE_URL_WS,
                post_type="elements", branch="master/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv4
],
[
905,
"CompareWorkspacesForMerge-style1",
"Compare workspaces at latest times for merge-style diff test.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/$gv1/latest/latest?background&changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"', '"diffTime"'],
["test", "workspaces", "develop"]
],
[
905.5,
"CompareWorkspacesForMerge-style2",
"Compare workspaces at latest times for merge-style diff test.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/$gv1/latest/latest?background&fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"', '"diffTime"'],
["test", "workspaces", "develop"]
],

[
906,
"DeleteDeleteDeleteWs1",
"Delete delete_delete_consistent",
create_curl_cmd(type="DELETE", data="elements/delete_delete_consistent", base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
907,
"DeleteUpdateDeleteWs1",
"Delete update_delete_consistent",
create_curl_cmd(type="DELETE", data="elements/update_delete_consistent", base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
907.5,
"DeleteAddAddBeforeWs1",
"Delete add_add_before",
create_curl_cmd(type="DELETE", data="elements/add_add_before", base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
908,
"PostElementsMerge2",
"Post elements to the master branch for merge-style diff testing",
create_curl_cmd(type="POST", data="elementsWsMerge-style.json", base_url=BASE_URL_WS,
                post_type="elements", branch="$gv1/"),
True,
common_filters,
["test", "workspaces", "develop"],
None,
None,
set_last_read_to_gv5,
],

[
908.5,
"DeleteDeleteDeleteBeforeWs1",
"Delete delete_delete_before",
create_curl_cmd(type="DELETE", data="elements/delete_delete_before", base_url=BASE_URL_WS,
                branch="$gv1/"),
True,
common_filters + ['"timestamp"', '"MMS_', '"id"', '"qualifiedId"', '"version"', '"modified"', '"sequence"'],
["test", "workspaces", "develop"]
],

[
909,
"CompareWorkspacesForMerge-style3",
"Compare workspaces at latest times for merge-style diff test.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/$gv1/latest/latest?fullCompare"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],
[
910,
"CompareWorkspacesForMerge-style4",
"Compare workspaces at latest times for merge-style diff test.",
create_curl_cmd(type="GET", base_url=SERVICE_URL,
                branch="diff/master/$gv1/latest/latest?changesForMerge"),
True,
common_filters + ['"id"', '"qualifiedId"', '"creator"', '"modifier"'],
["test", "workspaces", "develop"]
],


# Additional searches after everything is completed ==========================
[
10000,
"GetSearchDocumentation",
"Get search documentation",
create_curl_cmd(type="GET", data="search?keyword=some*&filters=documentation", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["develop", "workspaces"]
],

[
10001,
"GetSearchAspects",
"Get search aspects",
create_curl_cmd(type="GET", data="search?keyword=View&filters=aspect", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["develop", "workspaces"]
],

[
10002,
"GetSearchId",
"Get search id",
create_curl_cmd(type="GET", data="search?keyword=300&filters=id", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters,
["develop", "workspaces"]
],

# # temporarily remove this, the filters don't appear to be working quite right
[
10003,
"GetSearchValue",
"Get search value",
create_curl_cmd(type="GET", data="search?keyword=dlam_string&filters=value", base_url=BASE_URL_WS,
                branch="master/"),
True,
common_filters + ['"qualifiedId"', '"sysmlid"'],
["workspaces"]
],

]

##########################################################################################
#
# MAIN METHOD
#
##########################################################################################
if __name__ == '__main__':

    run(tests)
