@echo off
rem This script expects PATH variable to contain the curl bin directory

SETLOCAL enabledelayedexpansion
TITLE mms_mappings

SET HOSTNAME=%COMPUTERNAME%


SET MY_DIR=%~dp0

CALL curl > NUL 2>&1

if %ERRORLEVEL% geq 2 (
   echo curl binary not found. Please install it and update your PATH variable.
   echo MMS Update failed.
   EXIT /B 2
)

rem delete if previously exists and then create
printf "\ndeleting db: "
curl -XDELETE "http://localhost:9200/_all/"

printf "\ncreating template: "
curl -XPUT "http://localhost:9200/_template/template" -d @!MY_DIR!/mms_mappings.json


ENDLOCAL
