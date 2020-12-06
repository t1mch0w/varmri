for j in 0 1 2 3 4 5 6 7
do
#for l in $(find /nfs/*type${j} -name "result_fake" | cut -f-4 -d"/"); do 
for l in $(find /mnt/1204/*type${j} -name "result_fake" | cut -f-5 -d"/"); do 
if [ "$j" -eq "2" ]
then
echo $l $(source latency.sh $l 3.5 0)
elif [ "$j" -eq "3" ]
then
echo $l $(source latency.sh $l 5 2)
elif [ "$j" -eq "4" ]
then
echo $l $(source latency.sh $l 2.5 2)
else
echo $l $(source latency.sh $l 2)
fi
done > t${j} &
done
