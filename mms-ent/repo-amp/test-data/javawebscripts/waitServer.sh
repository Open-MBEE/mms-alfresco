#!/bin/bash

server=0
serverCount=0
dotCount=0
test=0
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
    exit 0
else
    echo 'NO SERVER'
fi
exit 1

