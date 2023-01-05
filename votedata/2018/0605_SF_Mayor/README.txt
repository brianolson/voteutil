https://sfelections.sfgov.org/june-5-2018-election-results-detailed-reports
http://www.sfelections.org/results/20180605/data/20180607/summary.txt
http://www.sfelections.org/results/20180605/data/20180607/20180607_psov.tsv
http://www.sfelections.org/results/20180605/data/20180607/20180607_ballotimage.txt
http://www.sfelections.org/results/20180605/data/20180607/20180607_masterlookup.txt
http://www.sfelections.org/results/20180605/data/20180607/20180607_turnout.txt
http://www.sfelections.org/results/20180605/data/20180607/20180607_sha512.csv


http://www.sfelections.org/results/20180605/data/20180609/summary.txt
http://www.sfelections.org/results/20180605/data/20180609/20180609_psov.tsv
http://www.sfelections.org/results/20180605/data/20180609/20180609_turnout.txt
http://www.sfelections.org/results/20180605/data/20180609/20180609_sha512.csv
http://www.sfelections.org/results/20180605/data/20180609/J18_UnprocessedBallots_20180609.pdf
http://www.sfelections.org/results/20180605/data/20180609/J18_CanvassUnprocessedBallots_20180609.pdf

curl -O http://www.sfelections.org/results/20180605/data/20180609/20180609_ballotimage.txt
curl -O http://www.sfelections.org/results/20180605/data/20180609/20180609_masterlookup.txt

(mkdir -p ~/psrc && cd ~/psrc && git clone https://github.com/brianolson/voteutil.git && cd ~/psrc/voteutil/java && mvn package)
#python3 ~/psrc/voteutil/python/rcvToNameEq.py -m 20180609_masterlookup.txt -b 20180609_ballotimage.txt -o 20180609_%s.nameq

python3 ../../../python/rcvToNameEq.py --ignore-unknown -m 20180621_masterlookup.txt -b 20180621_ballotimage.txt -o %s.nameq
python3 -m voteutil.rcvmatters --dir .


#java -jar ~/psrc/voteutil/java/target/voteutil-1.0.0.jar --rankings --full-html --explain -i 20180609_Mayor.nameq >/tmp/a.html


http://www.sfelections.org/results/20180605/data/20180612/20180612_ballotimage.txt
http://www.sfelections.org/results/20180605/data/20180612/20180612_masterlookup.txt


http://www.sfelections.org/results/20180605/data/20180615/20180615_ballotimage.txt
http://www.sfelections.org/results/20180605/data/20180615/20180615_masterlookup.txt
http://www.sfelections.org/results/20180605/data/20180615/mayor/20180615_mayor.html


http://www.sfelections.org/results/20180605/data/20180621/20180621_ballotimage.txt
http://www.sfelections.org/results/20180605/data/20180621/20180621_masterlookup.txt
https://sfelections.org/results/20180605/data/20180621/mayor/20180621_mayor.html


(cd ~/src/voteutil/java && mvn package)
java -jar ~/src/voteutil/java/target/voteutil-1.0.0.jar --rankings --full-html --explain -i ~/src/voteutil/data/SF_Mayor_20180605/20180615_Mayor.nameq > /tmp/j.html

~/src/voteutil/python/countvotes.py --rankings --html /tmp/p.html ~/src/voteutil/data/SF_Mayor_20180605/20180615_Mayor.nameq -v

(cd ~/src/voteutil/go/countvotes && go build)
~/src/voteutil/go/countvotes/countvotes --full-html --enable-all --rankings --explain -o /tmp/g.html -i ~/src/voteutil/data/SF_Mayor_20180605/20180615_Mayor.nameq

(cd ~/src/voteutil/go/countvotes && go build) && ~/src/voteutil/go/countvotes/countvotes --full-html --rankings --explain -o /tmp/g.html -i ~/src/voteutil/data/SF_Mayor_20180605/20180615_Mayor.nameq
