downloaded a long time ago (~2011)

$ ls -l *.zip
-rw-r--r-- 1 bolson bolson 2254961 Nov 12  2011 DA.zip
-rw-r--r-- 1 bolson bolson 2445321 Nov 12  2011 Mayor.zip
-rw-r--r-- 1 bolson bolson 2215761 Nov 12  2011 Sheriff.zip
$ shasum -a 256 *.zip
467945355257177fdb8f15f0eb35a2dbe0bd939a41f17eedb346a2a07e5968b9  DA.zip
d49f5b0f9c208cc115af2ed522cabe63b078ed7f7c36efacbec0ce610d9b97ca  Mayor.zip
0137043637aab742b99df09524a79f9b538e97dddf189627158eb340fc3adcff  Sheriff.zip

# unzip

$ ls -l *txt
-rw-r--r-- 1 bolson bolson 26424951 Nov 12  2011 DA-BallotImage.txt
-rw-r--r-- 1 bolson bolson    37656 Nov 12  2011 DA-MasterLookUp.txt
-rw-r--r-- 1 bolson bolson 26424951 Nov 12  2011 Mayor-BallotImage.txt
-rw-r--r-- 1 bolson bolson    39270 Nov 12  2011 Mayor-MasterLookUp.txt
-rw-r--r-- 1 bolson bolson 26424951 Nov 12  2011 Sheriff-BallotImage.txt
-rw-r--r-- 1 bolson bolson    37570 Nov 12  2011 Sheriff-MasterLookUp.txt
$ shasum -a 256 *txt
6e2739baf010361c5440903d1c1b29395812f45625dc01a1931c5236ac828a46  DA-BallotImage.txt
050a77abab17a27faf0d73e804d94425be930f0a66e99aea4ee7c839a7754451  DA-MasterLookUp.txt
b3f6338cf62e5bbad525fe7b3764450cddcd71f9996f731541a54d0f83c01047  Mayor-BallotImage.txt
924742c5f9b66813740cac57e83e859c47dd6e90826858e9de6f07ac78312094  Mayor-MasterLookUp.txt
b7e031d495805a2b462d2ebfaf5018ff9aa1dc01fca521e5197cea1ac851e6ce  Sheriff-BallotImage.txt
82acaea9ca9aa5ee0e9f23b8de9b5658173d0e2f1bc9d264b2f401040fe58b4f  Sheriff-MasterLookUp.txt

python3 ../../../python/rcvToNameEq.py --master=DA-MasterLookUp.txt --ballots=DA-BallotImage.txt --out=DA-nameq
python3 ../../../python/rcvToNameEq.py --master=Mayor-MasterLookUp.txt --ballots=Mayor-BallotImage.txt --out=Mayor-nameq
python3 ../../../python/rcvToNameEq.py --master=Sheriff-MasterLookUp.txt --ballots=Sheriff-BallotImage.txt --out=Sheriff-nameq
python3 -m voteutil.rcvmatters --dir . --html=on
