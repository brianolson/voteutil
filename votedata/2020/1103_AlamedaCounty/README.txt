2023-02-05 05:57:05 EST (Sunday, February 05 05:57:05 AM)
received in reply to email to digital.services@acgov.org
$ ls -l November\ 3\,\ 2020\ General\ Election\ -\ CVR\ Exports\ copy.zip
-rw-r--r--@ 1 bolson  staff  175519813 Feb  5 06:26 November 3, 2020 General Election - CVR Exports copy.zip
$ shasum -a 256 November\ 3\,\ 2020\ General\ Election\ -\ CVR\ Exports\ copy.zip
d9f44a60cabb4bea7903160bfef56f15286956b1b8e703752f4d28cabcda15bf  November 3, 2020 General Election - CVR Exports copy.zip

# unzip

python3 2020_AlamedaCounty.py *.csv

python3 -m voteutil.rcvmatters --html=on *.nameq
