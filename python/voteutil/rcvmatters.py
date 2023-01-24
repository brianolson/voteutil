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
import time
import zipfile

from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import VoteProcessor

logger = logging.getLogger(__name__)

def outname(fname):
    if fname.endswith('.nameq'):
        return fname[:-6] + '.html'
    return fname + '.html'

def htmlOn(args, fail):
    if args.html is None:
        return fail
    hl = args.html.lower()
    if (hl == '') or (hl == 'on'):
        return True
    if hl == 'off':
        return False
    return fail

class statsVoter:
    short_name = "stats"
    def __init__(self, names=None, seats=None):
        self.names = names
        self.seats = seats
        # stats:
        self.tieVote = 0 # A == B
    def name(self):
        return "Statistics"
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        byRating = {}
        for index, rating in indexRatingsDict.items():
            if rating in byRating:
                self.tieVote += 1
                break
            byRating[rating] = index
        pass
    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        pass

# add add dict b into dict d
# return updated dict d
def dictsum(d, b):
    for k, v in b.items():
        if v is True:
            v = 1
        elif v is False:
            v = 0
        d[k] = d.get(k, 0) + v
    return d

def newerthan(a, b):
    """Makefile stile, return True if a newerthan b, or b not exist"""
    if not os.path.exists(b):
        return True
    if not os.path.exists(a):
        return False
    return os.path.getmtime(a) > os.path.getmtime(b)

# returns stats dict
# stats = { 'votes': int, 'hasNonPickOne': bool, 'rcvDisagree': bool,
## from count.py VoteProcessor{}
# 'blank': int, 'pickOne': int, 'pickOneMany': int, 'reVote': int,
# }
def testFile(args, fname, force=False):
    fdir, fbasename = os.path.split(fname)
    rootname = fbasename.replace('.nameq', '')
    statpath = os.path.join(fdir, '.' + fbasename + '.stats.json')
    if (not force) and (not newerthan(fname, statpath)):
        with open(statpath, 'rt') as fin:
            stats = json.load(fin)
        return stats
    names = []
    nameIndexes = {}
    algorithms = [VRR(names), VRR2(names), IRV(names), PickOne(names)]
    with open(fname, 'r') as fin:
        vp = VoteProcessor(algorithms, args=None, names=names, nameIndexes=nameIndexes, rankings=True, voteEmptyLines=True, eraseWriteIn=True)
        votes, comments = vp.processFile(fin)
        stats = vp.statsDict()
        stats['votes'] = votes
        #votes, comments = processFile(algorithms, fin, args=None, names=names, nameIndexes=nameIndexes, rankings=True)
    if votes == 0:
        print('{} empty'.format(fname))
        with open(statpath, 'wt') as fout:
            json.dump(stats, fout)
        return stats
    # prevr[alg.name()] = alg.getResults()
    prevr = {}
    html = io.StringIO()
    # hasNonPickOne: some rcv algorithm disagrees with 'pick one'
    hasNonPickOne = False
    # rcvDisagree: two rcv algorithms disagree with each other
    rcvDisagree = False
    disagrees = []
    html.write('<h1>{}</h1>\n'.format(rootname))
    html.write('\n<div class="results">\n')
    for alg in algorithms:
        html.write('<h2>{}</h2>\n'.format(alg.name()))
        xr = alg.getResults(html)
        algname = alg.name()
        for pn, pr in prevr.items():
            if (len(pr) == len(xr)) and (pr[0][0] != xr[0][0]):
                # accumulate disagreements to put at the end
                disagreeHtml = f'\n<div class="votealgconflict"><i>{pn}</i> winner <b>{pr[0][0]}</b> != <i>{algname}</i> winner <b>{xr[0][0]}</b></div>\n'
                disagrees.append(disagreeHtml)
                if (algname == 'Pick One') or (pn == 'Pick One'):
                    hasNonPickOne = True
                elif not rcvDisagree:
                    rcvDisagree = True
                    print(pn, pr)
                    print(alg.name(), xr)
                    print('{}: {} {} != {} {}'.format(fname, pn, pr[0][0], alg.name(), xr[0][0]))
        prevr[algname] = xr
    html.write('\n</div><!-- end results -->\n')
    if disagrees:
        html.write('\n<div class="disagrees">\n')
        for dh in disagrees:
            html.write(dh)
        html.write('\n</div><!-- end disagrees -->\n')
    if htmlOn(args, rcvDisagree):
        # write html report to file
        outpath = outname(fname)
        print(outpath)
        with open(outpath, 'wt') as fout:
            fout.write('''<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>{title}</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
'''.format(title=rootname))
            # TODO: also write inner fragment to separate file for other composition?
            fout.write(html.getvalue())
            fout.write('''</body>
</html>
''')
    if hasNonPickOne:
        stats['hasNonPickOne'] = hasNonPickOne
    if rcvDisagree:
        stats['rcvDisagree'] = True
    with open(statpath, 'wt') as fout:
        json.dump(stats, fout)
    return stats

def main():
    start = time.time()
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('files', nargs='*', help='e.g. *.nameq')
    ap.add_argument('--dir', help='recursive find *.nameq')
    ap.add_argument('-v', '--verbose', default=False, action='store_true')
    ap.add_argument('--html', default=None, help='--html=on/off/x "--html" is "on"')
    ap.add_argument('--report', help='path to write html report to')
    ap.add_argument('--summary', help='summary fragment html path')
    ap.add_argument('--recount', default=False, action='store_true', help='recount all .nameq even if stats cache exists')
    args = ap.parse_args()

    count = 0
    rcvDisagreecount = 0
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

    paths.sort()
    # totalvotes = 0
    statsByPath = {}
    statsum = {}
    for fname in paths:
        print(fname)
        fstats = testFile(args, fname, force=args.recount)
        dictsum(statsum, fstats)
        statsByPath[fname] = fstats
        count += 1
        # totalvotes += fstats['votes']
        # if fstats.get('rcvDisagree'):
        #     rcvDisagreecount += 1
        # if fstats.get('hasNonPickOne'):
        #     p1count += 1
    logger.debug('statsum %r', statsum)
    if args.report:
        with open(args.report, 'wt') as fout:
            fout.write('''<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>RCV Election Analysis</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body>
''')
            fout.write('<h1>Contests</h1>')
            fout.write('<table><tr><th>tags:</th></tr><tr><th>!PO</th><td>If only the top-rank choice was voted, the pick-one winner would be different than the RCV winner</td></tr><tr><th>!RCV</th><td>The Ranked Choice Vote winner differs depending on the RCV algorithm used</td></tr></table>')
            fout.write('<table>')
            fout.write('<tr><th>file</th><th>tags</th><th>votes</th><th>picked one</th><th>picked one repeatedly</th><th>picked one again (and others)</th><th>blank</th></tr>')
            for fname in [''] + paths + ['']:
                if fname == '':
                    fstats = statsum
                    fnamer = f'TOTAL ({count} files)'
                    fnameh = '#'
                else:
                    fstats = statsByPath[fname]
                    fnamer = fname.replace('.nameq', '')
                    fnameh = fname.replace('.nameq', '.html')
                hasNonPickOne = fstats.get('hasNonPickOne')
                rcvDisagree = fstats.get('rcvDisagree')
                tags = []
                if rcvDisagree:
                    if isinstance(rcvDisagree, bool):
                        tags.append('<span class="Rcv">!RCV</span>')
                    else:
                        tags.append(f'<span class="Rcv">!RCV * {rcvDisagree}</span>')
                if hasNonPickOne:
                    if isinstance(hasNonPickOne, bool):
                        tags.append('<span class="npo">!PO</span>')
                    else:
                        tags.append(f'<span class="npo">!PO * {hasNonPickOne}</span>')
                if tags:
                    tags = ' '.join(tags)
                else:
                    tags = ''
                nvotes = fstats['votes']
                nblank = fstats['blank']
                npickOne = fstats['pickOne']
                npickOneMany = fstats['pickOneMany']
                nrevote = fstats['reVote']
                fout.write(f'<tr><td><a href="{fnameh}">{fnamer}</a></td><td>{tags}</td><td>{nvotes}</td><td>{npickOne}</td><td>{npickOneMany}</td><td>{nrevote}</td><td>{nblank}</td></tr>')
            fout.write('</table>')
            fout.write('</body></html>\n')
    if args.summary:
        with open(args.summary, 'wt') as fout:
            fout.write(f'<div class="vsum">{count} files, {statsum["votes"]} votes</div><div class="vsum">{statsum.get("rcvDisagree",0)} had RCV algorithm disagreement</div><div class="vsum">{statsum.get("hasNonPickOne",0)} had RCV outcome different than just voting for first choice</div>')
    dt = time.time() - start
    print(f'Done. {count} elections, {statsum["votes"]} votes, {statsum["hasNonPickOne"]} p1-different, {statsum["rcvDisagree"]} alg-different; # ({dt:.2f} seconds)')


if __name__ == '__main__':
    main()
