https://www.co.pierce.wa.us/2940/Ranked-Choice-Results

  7345621   6012 -rw-r--r--   1 bolson   bolson    6156201 Jan  2  2018 ./county_council_2/ballot.txt
  7345622     36 -rw-r--r--   1 bolson   bolson      32980 Jan  2  2018 ./county_council_2/master.txt
  7345616  43072 -rw-r--r--   1 bolson   bolson   44100711 Jan  2  2018 ./county_executive/ballot.txt
  7345618     36 -rw-r--r--   1 bolson   bolson      33065 Jan  2  2018 ./county_executive/master.txt
  7347202  43068 -rw-r--r--   1 bolson   bolson   44100711 Jan  2  2018 ./acessor_treasurer/ballot.txt
  7347313     36 -rw-r--r--   1 bolson   bolson      33235 Jan  2  2018 ./acessor_treasurer/master.txt

shasum -a 256 */*.txt

de921063a5e7cdc7ae7258f7fce0c2eb4e362b837f0a1bbea48a607df044e246  acessor_treasurer/ballot.txt
113ebb7f2065f1c4b41d8aa31ba1158390d01376844b63465d7fd34fca8fdbc3  acessor_treasurer/master.txt
4c7f9a265f952abf66b03073a8af1cd625bb3fb226dc1b673f1a4b382ea0571d  county_council_2/ballot.txt
ab24c5458d4d0d508be3b0d7650385863209d5e0671a2c89538f6bef2dad6723  county_council_2/master.txt
c9ff03e3e1dbe5544e9a5394d4ffe7f80e7f9629eac71c63cc938ba2d334864d  county_executive/ballot.txt
d8e21c59163f654d4dca95fd22dcbb126891a7e2cb867820079fbb792480e8bc  county_executive/master.txt


python3 ../../../python/rcvToNameEq.py -m acessor_treasurer/master.txt -b acessor_treasurer/ballot.txt -o acessor_treasurer/accesor.nameq --ignore-unknown
python3 ../../../python/rcvToNameEq.py -m county_council_2/master.txt -b county_council_2/ballot.txt -o county_council_2/county_council_2.nameq --ignore-unknown
python3 ../../../python/rcvToNameEq.py -m county_executive/master.txt -b county_executive/ballot.txt -o county_executive/county_executive.nameq --ignore-unknown

python3 -m voteutil.rcvmatters --dir .
