#!/usr/bin/env python3
#
# Process tables from Maine 2020 Primary

import csv
import logging
import re
import sys
import urllib.parse

logger = logging.getLogger(__name__)

choiceKeys = [
    ('1st Choice', 1),
    ('2nd Choice', 2),
    ('3rd Choice', 3),
    ('1st choice', 1),
    ('2nd choice', 2),
    ('3rd choice', 3),
]

for i in range(1,99):
    choiceKeys.append(('Choice {}'.format(i), i))

def headerRowToColumnRanks(header):
    colranks = {}
    for col, v in enumerate(header):
        lv = v.lower()
        for ck, cv in choiceKeys:
            if ck in v:
                colranks[col] = cv
                break
    if len(colranks) < 3:
        raise Exception('failed to find rank columns in header {!r}'.format(header))
    return colranks


cnd1 = re.compile(r'\s+\(CND\d+\)')
def nameJunkTrimCnd1(name):
    return cnd1.sub('', name)

def csvToNameq(fin, fout, args):
    reader = csv.reader(fin)
    header = None
    colranks = None
    for row in reader:
        if header is None:
            header = row
            colranks = headerRowToColumnRanks(header)
            continue
        ne = []
        for col,rank in colranks.items():
            name = row[col]
            if name == 'undervote':
                continue
            if name == 'overvote':
                continue
            if args.name_junk_trim_cnd1:
                name = nameJunkTrimCnd1(name)
            name = name.strip()
            ne.append( (name, rank) )
        if not ne:
            fout.write('\n')
        else:
            fout.write(urllib.parse.urlencode(ne) + '\n')

def outname(path):
    if path.endswith('.csv'):
        return path[:-4] + '.nameq'
    return path + '.nameq'

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('csvpaths', nargs='*')
    ap.add_argument('-o', '--out')
    ap.add_argument('--each', default=False, action='store_true', help='each *.csv becomes a *.nameq')
    ap.add_argument('--encoding', default='utf-8')
    ap.add_argument('--name-junk-trim-cnd1', default=False, action='store_true')
    ap.add_argument('--verbose', default=False, action='store_true')
    args = ap.parse_args()
    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)
    out = sys.stdout
    if args.out:
        out = open(args.out, 'wt')
    if not args.csvpaths:
        csvToNameq(sys.stdin, out)
        return 0
    for path in args.csvpaths:
        if args.each:
            outpath = outname(path)
            logger.debug('%s -> %s', path, outpath)
            try:
                with open(outpath, 'wt') as fout:
                    with open(path, 'rt', encoding=args.encoding) as fin:
                        csvToNameq(fin, fout, args)
            except Exception as e:
                logger.error('%s: %s', path, e, exc_info=True)
        else:
            logger.debug('%s -> %s', path, args.out)
            with open(path, 'rt', encoding=args.encoding) as fin:
                csvToNameq(fin, out, args)
    return 0

if __name__ == '__main__':
    sys.exit(main())
