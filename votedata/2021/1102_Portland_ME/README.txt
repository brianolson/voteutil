Data from Paul Riley, Elections Administrator, by email, 2023-01-05

python3 ../../../python/voteutil/xlsxtocsv.py *.xlsx
python3 ../../../python/maine.py *csv -o city_council_at_large.nameq
python3 -m voteutil.rcvmatters --dir . --html=on
