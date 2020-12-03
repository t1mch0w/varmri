time java -cp commons-math3-3.6.1.jar:. -Xms180g Analyzer $1/result_fake $1/userapp.log $1/kmesg $2
#time java -cp commons-math3-3.6.1.jar:. -Xms180g -agentlib:hprof=heap=sites Analyzer $1/result_fake $1/userapp.log $1/kmesg $2
