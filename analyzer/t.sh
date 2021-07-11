for i in 0 1 2 3 4 5 6 7
do
mkdir type$i 
source test.sh $1 0.99 $i 2 0
#source test.sh $1 0.99 $i 5 0
mv *.txt type$i
done
