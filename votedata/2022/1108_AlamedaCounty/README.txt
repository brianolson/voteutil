2023-02-05 05:57:05 EST (Sunday, February 05 05:57:05 AM)
received in reply to email to digital.services@acgov.org

$ ls -l *.zip
-rw-r--r-- 1 bolson bolson  258524 Feb  5 06:23 CVR_Export_AlbanyRCV_20221108.zip
-rw-r--r-- 1 bolson bolson 1141715 Feb  5 06:23 CVR_Export_BerkeleyRCV_20221108.zip
-rw-r--r-- 1 bolson bolson 5328877 Feb  5 06:23 CVR_Export_OaklandRCV_20221108.zip
-rw-r--r-- 1 bolson bolson  736729 Feb  5 06:23 CVR_Export_SanLeandroRCV_20221108.zip
$ shasum -a 256 *.zip
870655b130cdda19bc108cdccf815cef8074c337262618b0e722b089c3a5f195  CVR_Export_AlbanyRCV_20221108.zip
387ee152bca43da3fbe291fd9e0861fad0f344c9919da3fcc97deee0b27366b1  CVR_Export_BerkeleyRCV_20221108.zip
94425c76b7cce15587da87aeb297760e5ed1d4846b78bd0bdd01f8efba6bc7fb  CVR_Export_OaklandRCV_20221108.zip
6a2fdd43925dba851cfc1cb478cb06bcb7e9816e1a7d7a6c4daaccd61179bafa  CVR_Export_SanLeandroRCV_20221108.zip

python3 ../../2020/1103_AlamedaCounty/2020_AlamedaCounty.py *.csv
python3 -m voteutil.rcvmatters --html=on --dir=.
