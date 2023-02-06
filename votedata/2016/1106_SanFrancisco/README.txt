https://sfelections.sfgov.org/november-8-2016-election-results-summary

http://www.sfelections.org/results/20161108/data/20161206/20161206_masterlookup.txt
http://www.sfelections.org/results/20161108/data/20161206/20161206_masterlookup.txt
http://www.sfelections.org/results/20161108/data/20161206/20161206_sha512.csv
http://www.sfelections.org/results/20161108/data/20161206/20161206_ballotimage.txt

python3 ../../../python/rcvToNameEq.py --ignore-unknown -m 20161206_masterlookup.txt -b 20161206_ballotimage.txt -o %s.nameq

python3 -m voteutil.matters --dir .
