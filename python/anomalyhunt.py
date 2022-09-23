# https://github.com/brianolson/voteutil
from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import processFile
import glob
import io
import urllib.parse

count = 0
failcount = 0
failhtmls = []

for fname in glob.glob('*.nameq'):
    print(fname)
    names = []
    nameIndexes = {}
    algorithms = [VRR(names), VRR2(names), IRV(names), PickOne(names)]
    with open(fname, 'r') as fin:
        votes, comments = processFile(algorithms, fin, args=None, names=names, nameIndexes=nameIndexes, rankings=True)
    if votes == 0:
        print('{} empty'.format(fname))
        continue
    count += 1
    prevr = None
    preva = None
    html = io.StringIO()
    fail = False
    for alg in algorithms:
        html.write('<h1>{}</h1>\n'.format(alg.name()))
        xr = alg.getResults(html)
        if prevr:
            try:
                if prevr[0][0] != xr[0][0]:
                    if not fail:
                        print(preva, prevr)
                        print(alg.name(), xr)
                        print('{}: {} {} != {} {}'.format(fname, preva, prevr[0][0], alg.name(), xr[0][0]))
                    fail = True
            except:
                print('prevr {!r}, xr {!r}'.format(prevr, xr))
        prevr = xr
        preva = alg.name()
    if fail:
        failcount += 1
    if fail:# or True:
        outname = fname + '.html'
        failhtmls.append(outname)
        print(outname)
        with open(outname, 'wt') as fout:
            fout.write(html.getvalue())
print('Done. {} elections, {} different'.format(count,failcount))
print('\n'.join(failhtmls))
