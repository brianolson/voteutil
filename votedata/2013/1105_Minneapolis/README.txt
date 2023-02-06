https://vote.minneapolismn.gov/results-data/election-results/2013/

https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Mayor-CVR.xlsx
https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Board-of-Estimate-CVR.xlsx
https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Park-At-Large-CVR.xlsx

https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Council-Ward-1-CVR.xlsx
curl -L -O https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Council-Ward-'[2-13]'-CVR.xlsx
curl -L -O https://vote.minneapolismn.gov/media/-www-content-assets/documents/2013-Park-District-'[1-6]'-CVR.xlsx

python3 -m voteutil.xlsxtocsv *.xlsx

python3 ~/src/voteutil/python/maine.py --verbose --each *.csv

python3 -m voteutil.rcvmatters *.nameq
