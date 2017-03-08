#!/bin/bash

set -v # echoes commands before they are executed

#viewRepoDir=`pwd`
#if [ $viewRepoDir != *view-repo ]; then

ws=ws04
JsonDataDir=JsonData

sed 's/x/a/g' ${JsonDataDir}/x.json > ${JsonDataDir}/a.json
sed 's/x/b/g' ${JsonDataDir}/x.json > ${JsonDataDir}/b.json
sed 's/x/c/g' ${JsonDataDir}/x.json > ${JsonDataDir}/c.json
sed 's/x/d/g' ${JsonDataDir}/x.json > ${JsonDataDir}/d.json

wspost $ws

timeBefore=`date '+%Y-%m-%dT%H:%M:%S.000%z'`

modelpost $ws ${JsonDataDir}/b.json

timeBetween=`date '+%Y-%m-%dT%H:%M:%S.000%z'`

modelpost master ${JsonDataDir}/a.json

timeAfter=`date '+%Y-%m-%dT%H:%M:%S.000%z'`

wsdiff master $ws

wsdiff master master $timeBefore $timeAfter

wsdiff master $ws $timeBetween $timeBetween

