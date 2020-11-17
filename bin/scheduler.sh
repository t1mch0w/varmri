#!/bin/bash
export PYTHONPATH="/mnt/varcloud/emulab-xmlrpc"

export avai_nodes=29
test_id=0
declare -A cluster_info

get_num_nodes () {
test_type=$1
if [ "${test_type}" = "0" ] || [ "${test_type}" = "1" ] || [ "${test_type}" = "3" ] || [ "${test_type}" = "4" ]
then
num_nodes=4
elif [ "${test_type}" = "5" ]
then
num_nodes=5
elif [ "${test_type}" = "2" ]
then
num_nodes=2
else
num_nodes=1
fi
echo ${num_nodes}
}

run_test () {
test_id=$1
test_type=$2
num_nodes=$3
avai_nodes=$(echo "${avai_nodes}-${num_nodes}" | bc)
source ./run_test.sh ${test_id} ${test_type} ${num_nodes} &
echo "[scheduler] test${test_id} starts. threadId= $! test_type= ${test_type} num_nodes= ${num_nodes} avai_nodes= ${avai_nodes}"
cluster_info["$!"]=${num_nodes}
}

update_test () {
for tid in "${!cluster_info[@]}"
do
if ! [ -d /proc/${tid} ]
then
avai_nodes=$(echo "${avai_nodes}+${cluster_info[${tid}]}" | bc)
echo "[scheduler] one test finishes. threadId= $tid num_nodes= ${cluster_info[${tid}]} avai_nodes= ${avai_nodes}"
unset cluster_info[${tid}]
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
echo "[scheduler] test${test_id} test_type= ${test_type} num_nodes= ${num_nodes} avai_nodes= ${avai_nodes}"
sleep 30
update_test
done
run_test ${test_id} ${test_type} ${num_nodes}
test_id=$(echo "${test_id}+1" | bc)

#sleep for 5 sec, in case two requests of applying clusters are too close.
sleep 5
done
