https://www.portlandmaine.gov/172/Elections-Voting

https://content.civicplus.com/api/assets/c2ddd728-3ebb-45fc-bfbc-6fb28f908f62
https://content.civicplus.com/api/assets/ae48b2ea-2118-49d7-8df8-9d550d77ccd4

Data from Paul Riley, Elections Administrator, by email, 2023-01-05

python3 ../../../python/voteutil/xlsxtocsv.py *.xlsx
python3 ../../../python/maine.py *AL*csv -o school_board_at_large.nameq
python3 ../../../python/maine.py *D5*csv -o school_board_d5_at_large.nameq
python3 -m voteutil.rcvmatters --dir . --html=on
