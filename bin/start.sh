#!/bin/bash

name=$1
testid=$2

#Apply cluster
echo "Apply the cluster"
#./startExperiment --project osu-nfs-test --name test$name --duration 16 --bindings="{\"NumberOfNodes\":\"$1\"}" osu-nfs-test,c220g1
./startExperiment --project osu-nfs-test --name test$name --duration 16 --bindings="{\"NumberOfNodes\":\"5\"}" osu-nfs-test,c220g5

#Check status
echo "start to check status"
while true
do
sleep 30
./experimentManifests osu-nfs-test,test$name > test$name.xml
#Get node
node=$(python get_hostname.py test$name.xml)
if [ ${node} = "Error" ]
then
continue
else
break
fi
done

#Init
echo "start to init"
ssh ${node} -o StrictHostKeyChecking=no "cd /proj/osu-nfs-test-PG0/cloudlab_var_script;source init.sh"

#Start test
echo "start to test"
ssh ${node} -o StrictHostKeyChecking=no "cd /proj/osu-nfs-test-PG0/cloudlab_var_script;nohup bash ./start_test.sh ${testid} > test$name.log 2>&1 &"
