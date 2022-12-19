#!/usr/bin/env python3


import csv
import logging
import re
import sys
import urllib.parse

logger = logging.getLogger(__name__)


# two header styles:
#Cast Vote Record,Precinct,Ballot Style,DEM Governor 1st Choice (5715),DEM Governor 2nd Choice (5718),DEM Governor 3rd Choice (5720),DEM Governor 4th Choice (5724),DEM Governor 5th Choice (5727),DEM Governor 6th Choice (5730),DEM Governor 7th Choice (5733),DEM Governor 8th Choice
#Cast Vote Record,Precinct,Ballot Style,Governor (D) 1st Choice,Governor (D) 2nd Choice,Governor (D) 3rd Choice,Governor (D) 4th Choice,Governor (D) 5th Choice,Governor (D) 6th Choice,Governor (D) 7th Choice,Governor (D) 8th Choice

# two candidate styles:
# "Mills, Janet T. (5463)"
# "Mills, Janet T."

namere = re.compile(r'(.*) \(\d+\)')

def nameFixup(name):
    m = namere.match(name)
    if m:
        return m.group(1)
    return name

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('csvfiles', nargs='*')
    ap.add_argument('-o', '--out', default=None)
    ap.add_argument('-v', '--verbose', action='store_true', default=False)
    args = ap.parse_args()

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    if args.out is None or args.out == '-':
        out = sys.stdout
    else:
        out = open(args.out, 'w')
    castVoteIds = set()
    choices = set()
    outcount = 0
    for path in args.csvfiles:
        with open(path, 'r') as fin:
            logger.debug('reading %r', path)
            reader = csv.reader(fin)
            header = next(reader)
            for row in reader:
                cvr = row[0]
                if cvr in castVoteIds:
                    logger.error('dup Cast Vote Record %r', cvr)
                    continue
                # 'overvote' and 'undervote' can be discarded
                params = []
                for i, ch in enumerate(row[3:]):
                    ch = nameFixup(ch)
                    choices.add(ch)
                    if ch == 'overvote' or ch == 'undervote':
                        continue
                    params.append( (ch, i+1) )
                castVoteIds.add(cvr)
                if not params:
                    continue
                out.write(urllib.parse.urlencode(params))
                out.write('\n')
                outcount += 1
    logger.debug('%r votes', len(castVoteIds))
    logger.debug('choices: %r', sorted(choices))
    sys.stderr.write('{} votes out\n'.format(outcount))
            
    return 0

if __name__ == '__main__':
    sys.exit(main())
