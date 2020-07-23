#!/usr/bin/env python3
#
# Process tables from Maine 2020 Primary

import csv
import logging
import sys
import urllib.parse

logger = logging.getLogger(__name__)

def headerRowToColumnRanks(header):
    colranks = {}
    for col, v in enumerate(header):
        if '1st Choice' in v:
            colranks[col] = 1
        elif '2nd Choice' in v:
            colranks[col] = 2
        elif '3rd Choice' in v:
            colranks[col] = 3
    if len(colranks) < 3:
        raise Exception('failed to find rank columns in header {!r}'.format(header))
    return colranks


def csvToNameq(fin, fout):
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
            ne.append( (name, rank) )
        if not ne:
            continue
        fout.write(urllib.parse.urlencode(ne) + '\n')

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('csvpaths', nargs='*')
    ap.add_argument('-o', '--out')
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
        with open(path, 'rt') as fin:
            csvToNameq(fin, out)
    return 0

if __name__ == '__main__':
    sys.exit(main())
