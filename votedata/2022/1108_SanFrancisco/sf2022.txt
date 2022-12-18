https://sfelections.sfgov.org/november-8-2022-election-results-detailed-reports

https://www.sfelections.org/results/20221108/data/20221201/CVR_Export_20221201120428.zip
-rw-rw-r-- 1 bolson bolson 286316736 Dec 17 11:33 CVR_Export_20221201120428.zip
shasum -a 256 CVR_Export_20221201120428.zip
3966fe3abd980ddb26e245b375cd520c5df5ba21ecfce1830cd74db8123c8b8f  CVR_Export_20221201120428.zip

python3 extractSfCVR.py CVR_Export_20221201120428.zip

python3 -m voteutil.rcvmatters *.nameq
cat *.html > index.html
