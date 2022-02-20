#!/usr/bin/env python3

import argparse
import logging
import re
import sys
import time

from irnrp import IRNRP
from irnr import IRNR
from irv import IRV
from vrr import VRR
from vrr2 import VRR2
from count import processFile

logger = logging.getLogger(__name__)

vclasses = [IRNR, IRV, VRR, VRR2]

def vcByShortName(sn):
    for vc in vclasses:
        if sn == vc.short_name:
            return vc
    return None

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


REPEAT_RE = re.compile(r'\*\s*(\d+)\s+(.*)')

def main():
    start = time.time()
    ap = argparse.ArgumentParser()
    ap.add_argument('--seats', type=int, default=1, help='how many winners to elect (default 1)')
    ap.add_argument('--nocomment', action='store_true', default=False, help='ignore leading "#" chars on lines')
    ap.add_argument('--enable-repeat', action='store_true', default=False, help='enable repeat syntax "*N " at the start of a line')
    ap.add_argument('--rankings', action='store_true', default=False, help='treat input numbers as ranking 1st,2nd,3rd,etc')
    #ap.add_argument('--html', help='file to write HTML explanation to')
    ap.add_argument('-o', '--out', dest='outpath', default=None, help='output text to file or "-" for stdout')
    ap.add_argument('--full-html', action='store_true', default=False)
    ap.add_argument('--no-full-html', action='store_false', dest='full_html')
    ap.add_argument('--disable-all', action='store_true', default=False)
    ap.add_argument('--enable-all', action='store_true', default=False)
    ap.add_argument('--enable', action='append', default=[])
    ap.add_argument('--disable', action='append', default=[])
    ap.add_argument('--explain', action='store_true', default=False)
    ap.add_argument('--test', action='store_true', default=False)
    ap.add_argument('-i', action='append', default=[])
    ap.add_argument('votefile', nargs='*', help='votes as x-www-form-urlencoded query strings one per line, e.g.: name1=9&name2=3&name4=23')
    ap.add_argument('-v', '--verbose', action='store_true', default=False, help='verbose logging')
    args = ap.parse_args()

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    if args.full_html:
        logger.warning('TODO implement --full-html')
    if args.disable_all:
        enabled = []
    if args.enable_all:
        enabled = list(vclasses)
    if args.enable:
        ennames = sorted(set(args.enable))
        for en in ennames:
            vc = vcByShortName(en)
            if vc is None:
                sys.stderr.write('could not enable unknown algorithm {!r}\n'.format(en))
                sys.exit(1)
                return
            enabled.append(vc)
    if args.disable:
        disnames = sorted(set(args.disable))
        for d in disnames:
            # TODO? silently fails on unknown disables, `--disable WHARRGARBL`
            for vc in enabled:
                if vc.short_name == d:
                    enabled.remove(vc)
                    break
    if args.explain:
        logger.warning('TODO implement --explain')

    if args.outpath is None or args.outpath == '-':
        out = sys.stdout
    else:
        out = open(args.outpath, 'wt')

    if args.test:
        html = None
    else:
        html = out

    nameIndexes = {}
    names = []
    algorithms = [vc(names) for vc in enabled]
    if args.seats > 1:
        algorithms.append(IRNRP(names, seats=args.seats))
    input_paths = args.votefile + args.i
    if not input_paths:
        # if nothing else, then read one 'file' of vote data from stdin
        input_paths = ['-']
    for fname in input_paths:
        fstart = time.time()
        if fname == '-':
            votes, comments = processFile(algorithms, sys.stdin, args, names, nameIndexes, args.rankings, fname="-")
            dt = fstart - time.time()
            logger.info('finished %s votes from stdin in %0.2f seconds', votes, dt)
        else:
            with open(fname, 'r') as fin:
                votes, comments = processFile(algorithms, fin, args, names, nameIndexes, args.rankings, fname=fname)
            dt = time.time() - fstart
            logger.debug('finished vote file %s: %s votes in %0.2f seconds', fname, votes, dt)
    for algorithm in algorithms:
        if html:
            html.write('<h2>{}</h2>\n'.format(algorithm.name()))
        results = algorithm.getResults(html=html)
        logger.debug('%s: %r', algorithm.short_name, results)
        if args.test:
            out.write('{}: {}\n'.format(algorithm.name(), ', '.join([names[r[0]] for r in results])))
    if html:
        html.close()
    logger.debug('done in %0.2f seconds', time.time() - start)


if __name__ == '__main__':
    main()
