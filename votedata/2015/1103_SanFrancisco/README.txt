https://sfelections.org/results/20151103/#english_detail

https://sfelections.org/results/20151103/data/20151119/20151119_ballotimage.txt
https://sfelections.org/results/20151103/data/20151119/20151119_masterlookup.txt
https://sfelections.org/results/20151103/data/20151119/20151119_sha512.csv


python3 ../../../python/rcvToNameEq.py --ignore-unknown -m 20151119_masterlookup.txt -b 20151119_ballotimage.txt -o %s.nameq

python3 -m voteutil.rcvmatters --dir .
