#!/usr/bin/env bash

find ../../repo-amp/target -name 'mms-*.amp' -exec cp {} ./files/mms-repo.amp \;
find ../../share-amp/target -name 'mms-*.amp' -exec cp {} ./files/mms-share.amp \;

docker build -t mms-container:0.1 .
