# San Francisco 2020-11 RCV Vote Processing

This is what I'm doing* to try and reproduce the results of the 2020-November San Francisco general election.

```sh
git clone https://github.com/brianolson/voteutil.git
python3 -m venv vu
. vu/bin/activate
pip install jupyter
(cd voteutil/python && pip install -e .)
cd voteutil/votedata/2020/SanFrancisco
# download data from
# https://sfelections.sfgov.org/november-3-2020-election-results-detailed-reports
jupyter notebook SanFrancisco2020.ipynb &
```

(* I already had my environment set up with slightly different directories than this, but I think this should work)