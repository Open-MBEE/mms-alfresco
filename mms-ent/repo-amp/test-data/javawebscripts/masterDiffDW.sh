#!/bin/bash

passTest=0
soapServer="128.149.16.xxx:8080"

#start up the server
pkill -fn 'integration-test'
echo 'KILLING SERVER IF ONE IS RUNNING'
sleep 3s

#cd ./../..
./runserver.sh > serverLog.txt &
echo 'STARTING UP SERVER'
sleep 60s

#poll to see if the server is up
cd ./test-data/javawebscripts
server=0
serverCount=0
dotCount=0
echo 'POLLING SERVER'
while [ $server -eq 0 ]; do
        > tempMasterDiff
        netstat -ln | grep '8080' > tempMasterDiff
        count=`sed -n '$=' tempMasterDiff`
        numberRegEx='^[0-9]+$'
        if [[ $count =~ $numberRegEx ]];then
                if [ $count -gt 0 ]; then
                        server=1
                fi
        else
                dotCount=$(($dotCount+1))
                if [ $dotCount -gt 20 ];then
                     echo '...'
                     dotCount=0
                fi
        fi

        #time-out condition
        serverCount=$(($serverCount+1))
        if [ $serverCount -gt 50000 ];then
                server=2
        fi
done

if [ $server -eq 1 ]; then
    echo 'SERVER CONNECTED'
    sleep 60s
    
    diffChoose=2

    if [ $diffChoose -eq 1 ];then
        #run the diff script
        echo 'RUNNING OLD API DIFF SCRIPT'
        echo 'OMITTING WORKSPACES DIFF SCRIPT'
        ./diff2.sh
        passTest=$?

    elif [ $diffChoose -eq 2 ];then
        echo 'RUNNING WORKSPACES DIFF SCRIPT'
        echo 'OMITTING OLD API DIFF SCRIPT'
        #gitBranch=`git branch | grep '*'`
        #echo "$gitBranch"
        echo $GIT_BRANCH
        if [[ "$GIT_BRANCH" == *workspaces ]];then
            echo 'DIFFING  WORKSPACES BRANCH'
            ./diffWorkspaceDW.sh
            passTest=$?
        elif [[ "$GIT_BRANCH" == *develop ]];then
            echo 'DIFFING DEVELOP BRANCH'
            ./diffWorkspaceDWdev.sh
            passTest=$?
        else 
            echo 'RUNNING BOTH OLD API AND WORKSPACES DIFF SCRIPTS'
            #./diff2.sh
            ./diffWorkspaceDW.sh
            passTest=$?
        fi
    fi
    
    #connect to soapUI -- WORK STILL NEEDED
    echo 'RUNNING SOAP UI TESTS'
    #ssh $soapServer 'cd /classPath/; ./soapScript;'
    #classPath=??
    TestSuite="WorkspacesTesting"
    #TestCase="??"
    #./testrunner.sh -f ./soapTestData -s $TestSuite -c $TestCase $classpath
    cd ./soapStuff
    
    for i in $(ls . | grep "soapui-project.xml"); do
         echo RUNNING TEST $i
            ./Resources/app/bin/testrunner.sh -s $TestSuite ./$i
    done

    #shutdown the tomcat server process
    pkill -fn 'integration-test'
    echo 'KILLING SERVER'

    echo 'PASSTEST?'
        echo "$passTest"
    exit $passTest
fi

if [ $server -eq 2 ]; then
    echo 'SERVER TIME-OUT'
    exit 1
fi

