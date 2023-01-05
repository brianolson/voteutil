# [download notes lost]

python3 ../../../python/voteutil/xlsxtocsv.py *.xlsx

for cont in cg2 sr41 sr47 sr49 sr90 ss11; do python3 ../../../python/maine.py -o ${cont}.nameq ${cont}*csv; done

python3 -m voteutil.rcvmatters --dir . --html=on
