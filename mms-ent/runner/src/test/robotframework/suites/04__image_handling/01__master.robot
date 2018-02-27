*** Settings ***
Documentation    Testing Workspaces Tags on Master
Resource        ../resources.robot
Library          RequestsLibrary

*** Test Cases ***
PostNewImage
	[Documentation]		"Post a png image to mms"
	[Tags]				images		critical		0401
	Create Session		mmstest  ${ROOT}
    ${image_file} =		Binary Data	${CURDIR}${/}../../assets/gnome.png
    ${files} =			Create Dictionary	file	${image_file}
    ${data} =			Create Dictionary	contentType=image/png	id=mounts
	${result} =			RequestsLibrary.PostRequest		mmstest		/projects/PA/refs/master/artifacts		data=${data}		files=${files}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

PostNewElementForOriginalImage
	[Documentation]		"Post elements to get commitId after new version of image is created."
	[Tags]				images		critical		0406
	${post_json} =		Get File		${CURDIR}/../../JsonData/ImageBeforeCommit.json
	${result} =			requests.Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_commit} =		Get Commit From Json		${result.json()}
	Set Global Variable	  ${image_commit}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetFirstVerisonOfImage
	[Documentation]		"get /projects/PA/refs/master/artifacts/mounts"
	[Tags]				images		critical		0402
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/master/artifacts/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_elastic} =		Get Image Id		${result.json()}
	Set Global Variable	  ${image_elastic}

PostNewVersionOfImage
	[Documentation]		"Post a update to a existing image."
	[Tags]				images		critical		0403
	Create Session		mmstest  ${ROOT}
    ${image_file} =		Binary Data	${CURDIR}${/}../../assets/gnome.png
    ${files} =			Create Dictionary	file	${image_file}
    ${data} =			Create Dictionary	contentType=image/png	id=mounts
	${result} =			RequestsLibrary.PostRequest		mmstest		/projects/PA/refs/master/artifacts		data=${data}		files=${files}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetImage
	[Documentation]		"get /projects/PA/refs/master/artifacts/mounts"
	[Tags]				images		critical		0404
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/master/artifacts/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _timestamp		 _inRefIds		 upload		 location		 _elasticId
    Generate JSON		${TEST_NAME}		${result.json()}		${filter}
    Sleep				${POST_DELAY_INDEXING}
    ${compare_result} =		Compare JSON		${TEST_NAME}
    Should Match Baseline		${compare_result}

GetSecondVerisonOfImage
	[Documentation]		"get /projects/PA/refs/master/artifacts/mounts"
	[Tags]				images		critical		0405
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/master/artifacts/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_elastic_later} =		Get Image Location		${result.json()}
	Set Global Variable	  ${image_elastic_later}

PostNewElementForImageVersion
	[Documentation]		"Post elements to get commitId after new version of image is created."
	[Tags]				images		critical		0406
	${post_json} =		Get File		${CURDIR}/../../JsonData/ImageAfterCommit.json
	${result} =			requests.Post		url=${ROOT}/projects/PA/refs/master/elements		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_commit_master} =		Get Commit From Json		${result.json()}
	Set Global Variable	  ${image_commit_master}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetVersionedImage
	[Documentation]		"get /projects/PA/refs/master/elements/mounts"
	[Tags]				images		critical		0407
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/master/artifacts/mounts?commitId=${image_commit_master}		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified
    Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${image_versioned} =		Get Image Location		${result.json()}
	Set Global Variable	  ${image_versioned}
	Should Not Be Equal   ${image_versioned}    ${image_elastic}

GetOrginalImage
	[Documentation]		"get /projects/PA/refs/master/elements/mounts"
	[Tags]				images		critical		0407
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/master/artifacts/mounts?commitId=${image_commit}		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified
    Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	${image_org} =		Get Image Id		${result.json()}
	Set Global Variable	  ${image_org}
	Should Be Equal   ${image_org}    ${image_elastic}

PostNewBranchForImage
	[Documentation]		"Post new branch to PA"
	[Tags]				images		critical		0408
	${post_json} =		Get File		${CURDIR}/../../JsonData/NewImageBranch.json
	${result} =			requests.Post		url=${ROOT}/projects/PA/refs		data=${post_json}		headers=&{REQ_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetImageFromParent
	[Documentation]		"Get image from parent branch (image get url should use child branch's ref and it should return the image from parent ref)"
	[Tags]				images		critical		0409
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/imagebranch/artifacts/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_child_id} =		Get Image Location		${result.json()}
	Set Global Variable	  ${image_child_id}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Should Be Equal	  ${image_child_id}    ${image_versioned}

PostAnotherVersionOfParentImage
	[Documentation]		"post version of image to parent ref so parent ref has new version of image"
	[Tags]				images		critical		0410
	Create Session		mmstest  ${ROOT}
    ${image_file} =     Binary Data	${CURDIR}${/}../../assets/gnome.png
    ${files} =			Create Dictionary	file	${image_file}
    ${data} =			Create Dictionary	id=mounts	contentType=image/png
    ${result} =			RequestsLibrary.PostRequest		mmstest		/projects/PA/refs/master/artifacts/mounts		data=${data}		files=${files}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetChildImageUrl
	[Documentation]		"get image from child branch again, should be the same as getImageFromParent (should not give back latest version of image in parent ref)"
	[Tags]				images		critical		0411
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/imagebranch/artifacts/mounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${image_child_versioned_url} =		Get Image Location		${result.json()}
	Set Global Variable	  ${image_child_versioned_url}
	Should Be Equal	  ${image_child_versioned_url}    ${image_child_id}

PostImageToMountedProject
	[Documentation]		"post new image to mount to find from PA"
	[Tags]				images		critical		0412
	Create Session		mmstest  ${ROOT}
    ${image_file} =     Binary Data	${CURDIR}${/}../../assets/imagemount.png
    ${files} =			Create Dictionary	file	${image_file}
    ${data} =			Create Dictionary	contentType=image/png	id=imagemount
    ${result} =			RequestsLibrary.PostRequest		mmstest		/projects/PB/refs/master/artifacts/imagemounts		data=${data}		files=${files}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 upload		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}

GetImageInMountedProject
	[Documentation]		"Get Image that is in a Mount related to the current project"
	[Tags]				images		critical		0413
	${result} =			requests.Get		url=${ROOT}/projects/PA/refs/imagebranch/artifacts/imagemounts		headers=&{PNG_GET_HEADER}
	Should Be Equal		${result.status_code}		${200}
	${filter} =			Create List	 _commitId		nodeRefId		 versionedRefId		 _created		 read		 lastModified		 _modified		 siteCharacterizationId		 time_total		 _elasticId		 _timestamp		 _inRefIds		 url		 location
	Generate JSON		${TEST_NAME}		${result.json()}		${filter}
	Sleep				${POST_DELAY_INDEXING}
	${compare_result} =		Compare JSON		${TEST_NAME}
	Should Match Baseline		${compare_result}


