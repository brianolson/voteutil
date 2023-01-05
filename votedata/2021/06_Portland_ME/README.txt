Data from Paul Riley, Elections Administrator, by email, 2023-01-05

python3 ../../../python/voteutil/xlsxtocsv.py 06-2021\ CVR\ Export.xlsx
python3 ../../../python/maine.py 06-2021\ CVR\ Export.csv -o chater_commissioner.nameq
python3 -m voteutil.rcvmatters --dir . --html=on
