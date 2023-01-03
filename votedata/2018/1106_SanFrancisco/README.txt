https://sfelections.sfgov.org/november-6-2018-election-results-detailed-reports

https://www.sfelections.org/results/20181106/data/20181127/20181127_masterlookup.txt

https://www.sfelections.org/results/20181106/data/20181127/assessor/20181127_assessor_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/defender/20181127_defender_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/d2/20181127_d2_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/d4/20181127_d4_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/d6/20181127_d6_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/d8/20181127_d8_ballotimage.txt
https://www.sfelections.org/results/20181106/data/20181127/d10/20181127_d10_ballotimage.txt

https://www.sfelections.org/results/20181106/data/20181127/20181127_sha512.csv


python3 ../../../python/rcvToNameEq.py -m 20181127_masterlookup.txt --ballotglob \*ballotimage.txt -o %s.nameq

python3 -m voteutil.rcvmatters --dir .
