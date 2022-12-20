https://www.maine.gov/sos/cec/elec/results/2022/2022GeneralElectionRankedChoiceOffices.html

This is all just ME CD-2

https://www.maine.gov/sos/cec/elec/results/2022/reptocongress2-1.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong2-2.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong2-3.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong2-4.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong2-5.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong6.xlsx
https://www.maine.gov/sos/cec/elec/results/2022/reptocong2-7.xlsx

-rw-rw-r-- 1 bolson bolson 3241757 Dec 19 23:18 reptocong2-2.xlsx
-rw-rw-r-- 1 bolson bolson 3264343 Dec 19 23:18 reptocong2-3.xlsx
-rw-rw-r-- 1 bolson bolson  233613 Dec 19 23:18 reptocong2-4.xlsx
-rw-rw-r-- 1 bolson bolson  478295 Dec 19 23:19 reptocong2-5.xlsx
-rw-rw-r-- 1 bolson bolson   22421 Dec 19 23:19 reptocong2-7.xlsx
-rw-rw-r-- 1 bolson bolson   36516 Dec 19 23:19 reptocong6.xlsx
-rw-rw-r-- 1 bolson bolson 3253889 Dec 19 23:18 reptocongress2-1.xlsx

shasum -a 256 *xlsx
e4b879bcf39e5e532943bd504fb52871ba79780dc3d01979ad8c973fc5836dd5  reptocong2-2.xlsx
077357adb5ee8e566ba6db3dff096b3706f2728c45146913eeba320405f25df7  reptocong2-3.xlsx
1be410667eaecd832ac94b75d8772834d899c9026a73d731366801d6811f5248  reptocong2-4.xlsx
da03dc15182fd4277978226fab6971747b6109fc4bacb64922a30c380224312e  reptocong2-5.xlsx
5e8c86658b4a3df4cd8516d17c5b8fbf62e817aded2119f47c1ae71e9c9a6abf  reptocong2-7.xlsx
3b6ae13cf6d893796c51c4bf5af7de410fd1d3b9bc9be368cfde67c0c70270b7  reptocong6.xlsx
2b3d308e4173a34990a4a266e41caf40702f90f9d629f535cd1120a9d68e96e0  reptocongress2-1.xlsx


python3 ~/src/voteutil/python/voteutil/xlsxtocsv.py *xlsx
python3 ~/src/voteutil/python/maine.py -o 20221108_ME_CD2.nameq *csv
python3 -m voteutil.rcvmatters *.nameq
