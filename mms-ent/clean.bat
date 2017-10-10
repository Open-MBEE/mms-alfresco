@echo off
SETLOCAL enabledelayedexpansion
TITLE clean

rem checking for psql, which should be with createdb and dropdb, though not guaranteed
Call psql -V > NUL 2>&1

if %ERRORLEVEL% geq 2 (
   echo clean operation failed.
   echo psql binary not found. Please install it and update your PATH variable.
   rem letting the people figure out why the script stopped. . .
   pause
   EXIT /B 2
)

rem My first though on getting password, though it's in clear text :(
::Set /p "pass= enter Postgres password: "


rem Yeah, I make you type in the same password
rem Implement something like expect or find the windows equivalent
rem for now, I just wanted these commands to be fired off without having to interact too much
rem TODO: implement expect or something similar
Call dropdb -U $dbuser _123456
Call dropdb -U $dbuser $usedb
Call createdb -U $dbuser $usedb
Call psql -U $dbuser -f repo-amp\src\main\java\gov\nasa\jpl\view_repo\db\mms.sql $usedb

echo "Dropping previous databases"

Call repo-amp/src/main/java/gov/nasa/jpl/view_repo/db/mms_mappings.bat
rmdir alf_data_dev /s /q
rem too lazy to cleanly check for the existence of maven in your path :P
rem TODO: fix this
mvnw.cmd clean -Ddependency.surf.version=6.3 -Ppurge
