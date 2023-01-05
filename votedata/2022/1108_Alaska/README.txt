https://www.elections.alaska.gov/election-results/e/?id=22genr

https://elections.alaska.gov/results/22GENR/rcv/CVR_Export.zip

$ shasum -a 256 CVR_Export.zip
405dce1e924c0a6557178b820d2f6e873e2375dba058c74fbc5a61e1d1004e65  CVR_Export.zip

$ ls -l CVR_Export.zip
-rw-rw-r-- 1 bolson bolson 44019091 Dec 22 15:57 CVR_Export.zip

python3 ../../2022/1108_SanFrancisco/extractSfCVR.py CVR_Export.zip
