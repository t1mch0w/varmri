#!/bin/bash

local_test_id=$1
local_test_type=$2
num_nodes=$3

#Apply cluster
echo "[test${local_test_id}][$(date)] apply the cluster"
#./startExperiment --project osu-nfs-test --name test${local_test_id} --duration 16 --bindings="{\"NumberOfNodes\":\"$1\"}" osu-nfs-test,c220g1
./startExperiment --project osu-nfs-test --name test${local_test_id} --duration 16 --bindings="{\"NumberOfNodes\":\"${num_nodes}\"}" osu-nfs-test,c220g5
if ! [ $? -eq 0 ]
then
return 1
fi

#Check status
echo "[test${local_test_id}][$(date)] start to check status"
while true
do
sleep 30
#Get node status
status=$(./experimentStatus osu-nfs-test,test${local_test_id} | grep Status | awk '{print $2}')
#Add failed processing
if [ "${status}" = "failed" ]
then
./terminateExperiment osu-nfs-test,test${local_test_id}
return 1
elif [ "${status}" = "ready" ]
then
break
fi
done

#Init each node
./experimentManifests osu-nfs-test,test${local_test_id} > test${local_test_id}.xml
nodes=$(python get_allhosts.py test${local_test_id}.xml)
for node in ${nodes}
do
echo "[test${local_test_id}][$(date)] start to init"
ssh ${node} -o StrictHostKeyChecking=no "cd /proj/osu-nfs-test-PG0/cloudlab_var_script;source init.sh" &
done
wait

#Get node0
./experimentManifests osu-nfs-test,test${local_test_id} > test${local_test_id}.xml
node0=$(python get_hostname.py test${local_test_id}.xml)

#Run the test
echo "[test${local_test_id}][$(date)] start to test"
ssh ${node0} -o StrictHostKeyChecking=no "cd /proj/osu-nfs-test-PG0/cloudlab_var_script;./run_test.sh ${local_test_type} > test_${local_test_id}_type_${local_test_type}.log 2>&1"

#Store results to NFS
target_dir=/nfs/test${local_test_id}_type${local_test_type}
mkdir ${target_dir} 
echo ${nodes} > ${target_dir}/cluster_info
mv /proj/osu-nfs-test-PG0/cloudlab_var_script/test_${local_test_id}_type_${local_test_type}.log ${target_dir}/
mv /mnt/varcloud/bin/test${local_test_id}.xml ${target_dir}/
ssh ${node0} -o StrictHostKeyChecking=no "mv /mnt/asplos2021/varmri/userapp/*-*-*-* ${target_dir}/"

#For ZK-debug
ssh ${node0} -o StrictHostKeyChecking=no "mv /mnt/zk-try/zookeeper.log ${target_dir}/"

#Terminate the clusters
./terminateExperiment osu-nfs-test,test${local_test_id}
#Wait for 10 minutes for node recovery
sleep 600
