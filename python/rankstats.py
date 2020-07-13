#!/usr/bin/env python3

import argparse
import logging
import sys
import time
import urllib.parse

logger = logging.getLogger(__name__)

# dict append
# maintain {k:[v,...]}
def da(d, k, v):
    they = d.get(k)
    if they:
        they.append(v)
    else:
        d[k] = [v]

def splitbyrank(votes, rank):
    bynth = {}
    for v in votes:
        for c,r in v.items():
            if r == rank:
                da(bynth, c, v)
    return bynth

def sortbynth(bynth):
    return sorted([(len(sv), c, sv) for c, sv in bynth.items()], reverse=True)

htmlout = None

class Outf:
    def __init__(self, out):
        self.out = out
    def __call__(self, fmt, *args, **kwargs):
        if self.out is None:
            return
        self.out.write(fmt.format(*args, **kwargs))

def main():
    start = time.time()
    ap = argparse.ArgumentParser()
    ap.add_argument('votefile', nargs='*', help='votes as x-www-form-urlencoded query strings one per line, e.g.: name1=9&name2=3&name4=23')
    ap.add_argument('--nocomment', action='store_true', default=False, help='ignore leading "#" chars on lines')
    ap.add_argument('--html', default=None, help='path to write html to')
    ap.add_argument('-v', '--verbose', action='store_true', default=False, help='verbose logging')
    args = ap.parse_args()
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)
    text = Outf(sys.stdout)
    if args.html:
        html = Outf(open(args.html, 'w'))
    else:
        html = Outf(None)
    votes = []
    for fname in args.votefile:
        fstart = time.time()
        if fname == '-':
            fin = sys.stdin
        else:
            fin = open(fname, 'r')
        for line in fin:
            if not line:
                continue
            line = line.strip()
            if not line:
                continue
            if line[0] == '#' and not args.nocomment:
                comments += 1
                continue
            kvl = {k:int(v) for k,v in urllib.parse.parse_qsl(line)}
            votes.append(kvl)
        fin.close()
    text('{} votes\n', len(votes))
    byfirst = splitbyrank(votes, 1)
    text('\nFirsts and Seconds:\n')
    #bfs = [(len(sv), c, sv) for c, sv in byfirst.items()]
    #bfs.sort(reverse=True)
    html('<table border="1"><tr><th colspan="2">First</th><th colspan="2">Second</a></tr>')
    for lsv, c, sv in sortbynth(byfirst):
        text('{}\t{}\n', lsv, c)
        bysecond = splitbyrank(sv, 2)
        html('<tr><td rowspan="{}">{}</td><td rowspan="{}">{}</td>', len(bysecond), lsv, len(bysecond), c)
        firstrow = True
        for ls2, c2, s2 in sortbynth(bysecond):
            text('\t{}\t{}\n', ls2, c2)
            if not firstrow:
                html('<tr>')
            html('<td>{}</td><td>{}</td></tr>', ls2, c2)
            firstrow = False
    html('</table>')
    return

if __name__ == '__main__':
    main()
