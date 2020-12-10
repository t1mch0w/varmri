count=0
#for j in 0 1 2 3 4 5 6 7
for j in 0 1 2 3 4 5
#for j in 7 
do
#for l in $(find /nfs/*type${j} -name "result_fake" | cut -f-4 -d"/")
#for l in $(find /nfs/opt/*type${j} -name "result_fake" | cut -f-4 -d"/")
for l in $(find /mnt/*/*type${j} -name "result_fake" | cut -f-5 -d"/")
#for l in $(find /mnt/1208/*type${j} -name "result_fake" | cut -f-5 -d"/")
do
#k=$(echo ${l} | awk -F"/" '{print $3}')
k=$(echo ${l} | awk -F"/" '{print $4}')
if [ -f "${k}.txt" ]
then
continue
fi
echo "start to analyze " ${k}
if [ "$j" -eq "2" ]
then
#source latency.sh $l 3.5 0 > ${k}.txt &
source latency.sh $l 3.5 0 > ${k}.txt
elif [ "$j" -eq "3" ]
then
#source latency.sh $l 5 2 > ${k}.txt &
source latency.sh $l 5 2 > ${k}.txt
elif [ "$j" -eq "4" ]
then
#source latency.sh $l 7 2 > ${k}.txt &
source latency.sh $l 7 2 > ${k}.txt
else
#source latency.sh $l 2 > ${k}.txt &
source latency.sh $l 2 > ${k}.txt
fi 
count=$((${count}+1))
tmpn=$((${count}%10))
if [ "${tmpn}" -eq "0" ]
then
wait
fi
done
done
