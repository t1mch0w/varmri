len=$(wc -l $1)
sort -nr $1 | awk -v len="${len}" '{print 1-NR/len, $1}'
