#!/bin/bash

java -cp .:${HOME}/.m2/repository/gov/nasa/jpl/mbee/util/mbee_util/2.2.0-SNAPSHOT/mbee_util-2.2.0-SNAPSHOT.jar:../../src/main/amp/web/WEB-INF/lib/mbee_util.jar:../../target/mms-repo-ent-war/WEB-INF/lib/json-20140107.jar:../../target/mms-repo-war/WEB-INF/lib/json-20090211.jar:../../target/mms-repo-war/WEB-INF/lib/json-20140107.jar:../../target/classes gov.nasa.jpl.view_repo.util.JsonDiff $1 $2

