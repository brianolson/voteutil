#!/usr/bin/env python3

import argparse
import logging
import re
import sys
import urllib.parse

from irnrp import IRNRP
from vrr import VRR

logger = logging.getLogger(__name__)


def resultsToHtml(results, names, out):
    '''Process results array [(ci, votes), ...] to html text into out file'''
    if out is None:
        return
    out.write('<table class="results"><tr><th>Name</th><th>Votes</th></tr>\n')
    for ci, votes in results:
        if names and ci < len(names):
            name = names[ci]
        else:
            name = str(ci)
        out.write('<tr><td class="cn">{}</td><td class="vc">{}</td><tr>\n'.format(name, votes))
    out.write('</table>\n')



def processFile(algorithm, fin, args, names, nameIndexes, rankings=False):
    votes = 0
    comments = 0
    for line in fin:
        if not line:
            continue
        line = line.strip()
        if not line:
            continue
        if line[0] == '#' and not args.nocomment:
            comments += 1
            continue
        if line[0] == '*' and args.enable_repeat:
            m = REPEAT_RE.match(line)
            rcount = int(m.group(1))
            line = m.group(2)
        else:
            rcount = 1
        kvl = urllib.parse.parse_qs(line)
        indexRatingDict = {}
        for name, ratings in kvl.items():
            if len(ratings) == 0:
                continue
            ci = nameIndexes.get(name, None)
            if ci is None:
                ci = len(nameIndexes)
                nameIndexes[name] = ci
                names.append(name)
            if len(ratings) == 1:
                indexRatingDict[ci] = float(ratings[0])
            else:
                # warning, multiple votes for a choice, voting average of them
                indexRatingDict[ci] = sum(map(float, ratings)) / len(ratings)
        if rankings:
            maxrank = max(indexRatingDict.values()) + 1
            indexRatingDict = {k:(maxrank - v) for k,v in indexRatingDict.items()}
            assert min(indexRatingDict.values()) > 0, kvl
        while rcount > 0:
            algorithm.vote(indexRatingDict)
            votes += 1
            rcount -= 1
    return votes, comments
    

REPEAT_RE = re.compile(r'\*\s*(\d+)\s+(.*)')

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--seats', type=int, default=1, help='how many winners to elect (default 1)')
    ap.add_argument('--nocomment', action='store_true', default=False, help='ignore leading "#" chars on lines')
    ap.add_argument('--enable-repeat', action='store_true', default=False, help='enable repeat syntax "*N " at the start of a line')
    ap.add_argument('--rankings', action='store_true', default=False, help='treat input numbers as ranking 1st,2nd,3rd,etc')
    ap.add_argument('--html', help='file to write HTML explanation to')
    ap.add_argument('-o', '--out', dest='outpath', default=None, help='output text to file or "-" for stdout')
    ap.add_argument('votefile', nargs='*', help='votes as x-www-form-urlencoded query strings one per line, e.g.: name1=9&name2=3&name4=23')
    ap.add_argument('-v', '--verbose', action='store_true', default=False, help='verbose logging')
    args = ap.parse_args()

    if args.html:
        html = open(args.html, 'w')
    else:
        html = None

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    if args.outpath is None or args.outpath == '-':
        out = sys.stdout
    else:
        out = open(args.outpath, 'wt')

    nameIndexes = {}
    names = []
    #algorithm = IRNRP(seats=args.seats)
    algorithm = VRR(names)
    for fname in args.votefile:
        if fname == '-':
            votes, comments = processFile(algorithm, sys.stdin, args, names, nameIndexes, args.rankings)
            logger.info('finished %s votes from stdin', votes)
        else:
            with open(fname, 'r') as fin:
                votes, comments = processFile(algorithm, fin, args, names, nameIndexes, args.rankings)
            logger.info('finished vote file %s: %s votes', fname, votes)
    algorithm.names = names
    results = algorithm.getResults(html=html)
    if html is not None:
        resultsToHtml(results, names, html)
        html.close()


if __name__ == '__main__':
    main()

