report.html:	nameq ~/src/voteutil/c/countvotes
	~/src/voteutil/c/countvotes --full-html --rankings -o report.html nameq --explain

nameq:	~/src/voteutil/python/rcvToNameEq.py master_lookup.txt ballot_image.txt
	~/src/voteutil/python/rcvToNameEq.py --master=master_lookup.txt --ballots=ballot_image.txt --out=nameq
