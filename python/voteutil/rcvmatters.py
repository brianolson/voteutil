#!/usr/bin/env python3
#
# rcvmatters
#
# test .nameq election data
# generate html report
# replicate public count
# check IRV vs Condorcet vs Pick-Favorite outcomes

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

def outname(fname):
    if fname.endswith('.nameq'):
        return fname[:-6] + '.html'
    return fname + '.html'

# returns (ok bool, has non-pick-one disagreement bool)
def testFile(fname):
    names = []
    nameIndexes = {}
    algorithms = [VRR(names), VRR2(names), IRV(names), PickOne(names)]
    with open(fname, 'r') as fin:
        votes, comments = processFile(algorithms, fin, args=None, names=names, nameIndexes=nameIndexes, rankings=True)
    if votes == 0:
        print('{} empty'.format(fname))
        return None, None, votes
    prevr = None
    preva = None
    html = io.StringIO()
    fail = False
    hasNonPickOne = False
    disagrees = []
    html.write('<h1>{}</h1>\n'.format(fname.replace('.nameq', '')))
    for alg in algorithms:
        html.write('<h2>{}</h2>\n'.format(alg.name()))
        xr = alg.getResults(html)
        if prevr is not None:
            if (len(prevr) == len(xr)) and (prevr[0][0] != xr[0][0]):
                disagreeHtml = f'\n<div class="votealgconflict"><i>{preva}</i> winner <b>{prevr[0][0]}</b> != <i>{alg.name()}</i> winner <b>{xr[0][0]}</b></div>\n'
                if 'Pick One' not in disagreeHtml:
                    hasNonPickOne = True
                disagrees.append(disagreeHtml)
                if not fail:
                    fail = True
                    print(preva, prevr)
                    print(alg.name(), xr)
                    print('{}: {} {} != {} {}'.format(fname, preva, prevr[0][0], alg.name(), xr[0][0]))
        prevr = xr
        preva = alg.name()
    for dh in disagrees:
        html.write(dh)
        fail = True
    if fail or True:
        # write html report to file
        outpath = outname(fname)
        print(outpath)
        with open(outpath, 'wt') as fout:
            fout.write(html.getvalue())
    return (not fail, hasNonPickOne, votes)

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('files', nargs='*', help='e.g. *.nameq')
    ap.add_argument('--dir', help='recursive find *.nameq')
    ap.add_argument('-v', '--verbose', action='store_true')
    args = ap.parse_args()
    count = 0
    failcount = 0
    p1count = 0

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    paths = list(args.files)
    if args.dir:
        for dirpath, dirnames, filenames in os.walk(args.dir):
            for fname in filenames:
                if fname.endswith('.nameq'):
                    paths.append(os.path.join(dirpath, fname))

    totalvotes = 0
    for fname in paths:
        print(fname)
        ok, hasNonPickOne, filevotes = testFile(fname)
        if ok is None:
            continue
        totalvotes += filevotes
        count += 1
        if hasNonPickOne:
            failcount += 1
        elif not ok:
            p1count += 1
    print(f'Done. {count} elections, {totalvotes} votes, {p1count} p1-different, {failcount} alg-different')

if __name__ == '__main__':
    main()
