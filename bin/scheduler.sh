#!/bin/bash
export PYTHONPATH="/mnt/varcloud/emulab-xmlrpc"

#export avai_nodes=30
#test_id=0
export avai_nodes=117
test_id=34

#thread_id to #nodes
declare -A node_info
#thread_id to test_id
declare -A thread_test_info
#test_id to thread_id
declare -A test_thread_info

get_num_nodes () {
local_test_type=$1
if [ "${local_test_type}" = "0" ] || [ "${local_test_type}" = "1" ] || [ "${local_test_type}" = "3" ] || [ "${local_test_type}" = "4" ]
then
num_nodes=4
elif [ "${local_test_type}" = "5" ]
then
num_nodes=5
elif [ "${local_test_type}" = "2" ]
then
num_nodes=2
else
num_nodes=1
fi
echo ${num_nodes}
}

run_test () {
local_test_id=$1
local_test_type=$2
local_num_nodes=$3
avai_nodes=$(echo "${avai_nodes}-${local_num_nodes}" | bc)
echo "[scheduler][$(date)] test${local_test_id} starts. threadId= $! test_type= ${local_test_type} num_nodes= ${local_num_nodes} avai_nodes= ${avai_nodes}"
source ./run_test.sh ${local_test_id} ${local_test_type} ${local_num_nodes} &
node_info["$!"]=${local_num_nodes}
thread_test_info["$!"]=${local_test_id}
test_thread_info["${local_test_id}"]=$!
}

update_test () {
#Check whether the log has anything wrong.
for flog in $(ls /proj/osu-nfs-test-PG0/cloudlab_var_script/*.log)
do
#ssh connection problem
grep "port 22: Connection timed out" ${flog}
if [ "$?" = "0" ]
then
local_test_id=$(echo ${flog} | awk -F"_" '{print $2}')
tid=${test_thread_info[${local_test_id}]}
echo "[scheduler][$(date)] detect problems in test${local_test_id} num_nodes= ${node_info[${tid}]}"
kill -9 ${tid}
./terminateExperiment osu-nfs-test,test${local_test_id}
sleep 600
fi
done
#Check whether the test has finished.
for tid in "${!node_info[@]}"
do
if ! [ -d /proc/${tid} ]
then
local_test_id=${thread_test_info[${tid}]}
avai_nodes=$(echo "${avai_nodes}+${node_info[${tid}]}" | bc)
echo "[scheduler][$(date)] test${local_test_id} finishes. threadId= $tid num_nodes= ${node_info[${tid}]} avai_nodes= ${avai_nodes}"
unset node_info[${tid}]
unset thread_test_info[${tid}]
unset test_thread_info[${local_test_id}]
fi
done
}

while true
do
#get test type and required number of nodes
test_type=$(echo $RANDOM % 8 | bc)
num_nodes=$(get_num_nodes ${test_type})

#wait until we have enough nodes
while [ "${num_nodes}" -gt "${avai_nodes}" ]
do
echo "[scheduler][$(date)] test${test_id} test_type= ${test_type} num_nodes= ${num_nodes} avai_nodes= ${avai_nodes}"
sleep 600
update_test
done
run_test ${test_id} ${test_type} ${num_nodes}
test_id=$(echo "${test_id}+1" | bc)

#sleep for 60 sec, in case two requests of applying clusters are too close.
sleep 300
done
