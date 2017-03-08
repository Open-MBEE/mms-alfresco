
./record_curl.py -n GetAInMaster -g develop --runBranches "develop" -t GET --description "Get element a in the master workspace." --jsonDiff True -d elements/a
./record_curl.py -n GetAInParent -g develop --runBranches "develop" -t GET --description "Get element a in the parent workspace." --jsonDiff True -d elements/a -w theParentWorkspace/
./record_curl.py -n GetAInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element a in the subworkspace." --jsonDiff True -d elements/a -w theSubworkspace/

./record_curl.py -n GetBInMaster -g develop --runBranches "develop" -t GET --description "Get element b in the master workspace." --jsonDiff True -d elements/b
./record_curl.py -n GetBInParent -g develop --runBranches "develop" -t GET --description "Get element b in the parent workspace." --jsonDiff True -d elements/b -w theParentWorkspace/
./record_curl.py -n GetBInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element b in the subworkspace." --jsonDiff True -d elements/b -w theSubworkspace/

./record_curl.py -n GetCInMaster -g develop --runBranches "develop" -t GET --description "Get element c in the master workspace." --jsonDiff True -d elements/c
./record_curl.py -n GetCInParent -g develop --runBranches "develop" -t GET --description "Get element c in the parent workspace." --jsonDiff True -d elements/c -w theParentWorkspace/
./record_curl.py -n GetCInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element c in the subworkspace." --jsonDiff True -d elements/c -w theSubworkspace/

./record_curl.py -n GetDInMaster -g develop --runBranches "develop" -t GET --description "Get element d in the master workspace." --jsonDiff True -d elements/d
./record_curl.py -n GetDInParent -g develop --runBranches "develop" -t GET --description "Get element d in the parent workspace." --jsonDiff True -d elements/d -w theParentWorkspace/
./record_curl.py -n GetDInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element d in the subworkspace." --jsonDiff True -d elements/d -w theSubworkspace/

./record_curl.py -n GetEInMaster -g develop --runBranches "develop" -t GET --description "Get element e in the master workspace." --jsonDiff True -d elements/e
./record_curl.py -n GetEInParent -g develop --runBranches "develop" -t GET --description "Get element e in the parent workspace." --jsonDiff True -d elements/e -w theParentWorkspace/
./record_curl.py -n GetEInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element e in the subworkspace." --jsonDiff True -d elements/e -w theSubworkspace/

./record_curl.py -n GetFInMaster -g develop --runBranches "develop" -t GET --description "Get element f in the master workspace." --jsonDiff True -d elements/f
./record_curl.py -n GetFInParent -g develop --runBranches "develop" -t GET --description "Get element f in the parent workspace." --jsonDiff True -d elements/f -w theParentWorkspace/
./record_curl.py -n GetFInSubworkspace -g develop --runBranches "develop" -t GET --description "Get element f in the subworkspace." --jsonDiff True -d elements/f -w theSubworkspace/

