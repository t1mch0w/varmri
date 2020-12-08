count=0
for j in 0 1 2 3 4 5 6 7
do
#for l in $(find /nfs/*type${j} -name "result_fake" | cut -f-4 -d"/")
for l in $(find /mnt/1207/*type${j} -name "result_fake" | cut -f-5 -d"/")
do
#k=$(echo ${l} | awk -F"/" '{print $3}')
k=$(echo ${l} | awk -F"/" '{print $4}')
echo "start to analyze " ${k}
if [ "$j" -eq "2" ]
then
source latency.sh $l 3.5 0 > ${k}.txt &
elif [ "$j" -eq "3" ]
then
source latency.sh $l 5 2 > ${k}.txt &
elif [ "$j" -eq "4" ]
then
source latency.sh $l 2.5 2 > ${k}.txt &
else
source latency.sh $l 2 > ${k}.txt &
fi 
count=$((${count}+1))
tmpn=$((${count}%10))
if [ "${tmpn}" -eq "0" ]
then
wait
fi
done
done
