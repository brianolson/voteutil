all:	d2.html d6.html d8.html d10.html

d2.html:	d2_nameq ~/src/voteutil/c/countvotes
	~/src/voteutil/c/countvotes --full-html --rankings -o d2.html d2_nameq --explain

d6.html:	d6_nameq ~/src/voteutil/c/countvotes
	~/src/voteutil/c/countvotes --full-html --rankings -o d6.html d6_nameq --explain

d8.html:	d8_nameq ~/src/voteutil/c/countvotes
	~/src/voteutil/c/countvotes --full-html --rankings -o d8.html d8_nameq --explain

d10.html:	dA_nameq ~/src/voteutil/c/countvotes
	~/src/voteutil/c/countvotes --full-html --rankings -o d10.html dA_nameq --explain

all_nameq:	d2_nameq d6_nameq d8_nameq dA_nameq

d2_nameq:	~/src/voteutil/python/rcvToNameEq.py MasterLookup-D2.txt BallotImage-D2.txt
	~/src/voteutil/python/rcvToNameEq.py --master=MasterLookup-D2.txt --ballots=BallotImage-D2.txt --out=d2_nameq

d6_nameq:	~/src/voteutil/python/rcvToNameEq.py MasterLookup-D6.txt BallotImage-D6.txt
	~/src/voteutil/python/rcvToNameEq.py --master=MasterLookup-D6.txt --ballots=BallotImage-D6.txt --out=d6_nameq

d8_nameq:	~/src/voteutil/python/rcvToNameEq.py MasterLookup-D8.txt BallotImage-D8.txt
	~/src/voteutil/python/rcvToNameEq.py --master=MasterLookup-D8.txt --ballots=BallotImage-D8.txt --out=d8_nameq

dA_nameq:	~/src/voteutil/python/rcvToNameEq.py MasterLookup-D10.txt BallotImage-D10.txt
	~/src/voteutil/python/rcvToNameEq.py --master=MasterLookup-D10.txt --ballots=BallotImage-D10.txt --out=dA_nameq

