#!/bin/bash

#Apply cluster
./startExperiment --project osu-nfs-test --name test0 --duration 16 --bindings='{"NumberOfNodes":"2"}' osu-nfs-test,c220g1

#Check status
while true
do
sleep 30
./experimentManifests osu-nfs-test,test0 > xml
node=$(python get_hostname.py xml)
if [ ${node} == "Error" ]
then
continue
else
break
fi
done

#Init
ssh ${node} "cd /proj/osu-nfs-test-PG0/cloudlab_var_script;source init.sh"

#Start test

#
