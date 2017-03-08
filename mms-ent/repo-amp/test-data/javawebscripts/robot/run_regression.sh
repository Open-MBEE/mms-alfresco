#!/bin/bash

curpath=$(echo $PWD | sed 's/\//\\\//g')
sedcmd="s/\/Users\/dank\/git\/mms-all-in-one\/mms-ent\/repo-amp\/test-data\/javawebscripts\/robot/$curpath/g"

#sed -i '' $sedcmd regression_test_suite.robot
sed -i '' $sedcmd suite/resources.robot

#python -m robot.tidy -f  html regression_test_suite.robot regression_test_suite.html

#robot -L trace -d output regression_test_suite.html
#robot -L trace -d output regression_test_suite.robot

robot -L trace -d output suite

open output/log.html
