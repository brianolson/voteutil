https://sfelections.org/results/20141104/data/D10.zip

$ shasum -a 256 D10.zip
7113a69f2cc7917f83e58147254fa7968a0c8ff283ce342dcddd05a572d34bcc  D10.zip

python3 ../../../python/rcvToNameEq.py --ignore-unknown -m D10_MasterLookup.txt -b D10_BallotImage.txt -o %s.nameq

python3 -m voteutil.rcvmatters --dir .
