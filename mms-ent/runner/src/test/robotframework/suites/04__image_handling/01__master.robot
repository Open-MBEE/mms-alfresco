*** Settings ***
Documentation    Testing Workspaces Tags on Master
Resource        ../resources.robot

*** Test Cases ***
PostNewImage
	[Documentation]		"Post a png image to mms"
	[Tags]				images		critical		0401
    ${image_file} =     Binary Data    ${CURDIR}${/}../../assets/mounts.png
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements/mounts		data=${image_file}		headers=&{PNG_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}
# The image versioning works, but nothing is returned from the server that you can verify this with
PostNewVersionOfImage
	[Documentation]		"Post a update to a existing image."
	[Tags]				images		critical		0402
    ${image_file} =     Binary Data    ${CURDIR}${/}../../assets/mounts.png
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements/mounts		data=${image_file}		headers=&{PNG_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetImage
	[Documentation]		"get /projects/PA/refs/master/elements/mounts"
	[Tags]				images		critical		0403
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_url} =		Get Image Url		${result.json()}
	Set Global Variable	  ${image_url}

PostNewElementForImageVersion
	[Documentation]		"Post elements to get commitId after new version of image is created."
	[Tags]				images		critical		0404
	${post_json} =		Get File		${CURDIR}/../../JsonData/ImageAfterCommit.json
	${result} =			Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_commit_master} =		Get Commit From Json		${result.json()}
	Set Global Variable	  ${image_commit_master}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetVersionedImage
	[Documentation]		"get /projects/PA/refs/master/elements/mounts"
	[Tags]				images		critical		0405
	${result} =			Get		url=${ROOT}/projects/PA/refs/master/elements/mounts?commitId=${image_commit_master}		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_versioned_url} =		Get Image Url		${result.json()}
	Set Global Variable	  ${image_versioned_url}
	Should Not Be Equal   ${image_versioned_url}    ${image_url}

PostNewBranchForImage
	[Documentation]		"Post new branch to PA"
	[Tags]				images		critical		0406
	${post_json} =		Get File		${CURDIR}/../../JsonData/NewImageBranch.json
	${result} =			Post		url=${ROOT}/projects/PA/refs		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetImageFromParent
	[Documentation]		"Get image from parent branch (image get url should use child branch's ref and it should return the image from parent ref)"
	[Tags]				images		critical		0407
	${result} =			Get		url=${ROOT}/projects/imageBranch/refs/master/elements/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_child_url} =		Get Image Url		${result.json()}
	Set Global Variable	  ${image_child_url}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Should Be Equal	  ${image_child_url}    ${image_versioned_url}

#Get image from parent branch (image get url should use child branch's ref and it should return the image from parent ref)
#PostNewMount
#	[Documentation]		"Create a project (ID: PB) under the organization with (ID: initorg).  Symbolically PB."
#	[Tags]				mounts		critical		0201
#	${post_json} =		Get File	 ${CURDIR}/../../JsonData/ProjectCreationForMountsPB.json
#	${result} =			Post		url=${ROOT}/orgs/initorg/projects			data=${post_json}		headers=&{REQ_HEADER}
#	Should Be Equal		${result.status_code}		${200}
#	${filter} =			Create List	 _commitId
#	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
#	Sleep				${POST_DELAY_INDEXING}
#	${compare_result} =		Compare JSON		${TEST_NAME}
#	Should Match Baseline		${compare_result}
