#!/bin/sh
cp ../mms.properties.example ./files/
docker build . -t mms-container
