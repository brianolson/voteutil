import glob
import json
import io
import re
import os
import urllib.parse
import logging
import zipfile

from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import processFile

logger = logging.getLogger(__name__)

count = 0
failcount = 0

#for fname in glob.glob('*.nameq'):
for fname in ['BOARD OF SUPERVISORS DISTRICT 1.nameq']:
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
        if prevr is not None:
            if prevr[0][0] != xr[0][0]:
                if not fail:
                    print(preva, prevr)
                    print(alg.name(), xr)
                    print('{}: {} {} != {} {}'.format(fname, preva, prevr[0][0], alg.name(), xr[0][0]))
                fail = True
        prevr = xr
        preva = alg.name()
    if fail:
        failcount += 1
    if fail or True:
        outname = fname + '.html'
        print(outname)
        with open(outname, 'wt') as fout:
            fout.write(html.getvalue())
print('Done. {} elections, {} different'.format(count,failcount))
