Data from Paul Riley, Elections Administrator, by email, 2023-01-05

python3 ../../../python/voteutil/xlsxtocsv.py *.xlsx
python3 ../../../python/maine.py --name-junk-trim-cnd1 -o school_board_d3.nameq *csv
python3 -m voteutil.rcvmatters --dir . --html=on
