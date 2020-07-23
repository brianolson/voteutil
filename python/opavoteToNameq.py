#!/usr/bin/env python3
#
# https://www.opavote.com/help/overview#blt-file-format

import sys
import urllib.parse

def foo(fname, out):
    rankings = []
    with open(fname, 'rt') as fin:
        fi = iter(fin)
        line = next(fi)
        candidates, seats = [int(x.strip()) for x in line.split()]
        for line in fi:
            vparts = [int(x.strip()) for x in line.split()]
            if vparts[0] == 0:
                break
            if vparts[0] != 1:
                sys.stderr.write("can't deal with weight !=1: %r\n", line)
                sys.exit(1)
            if vparts[-1] != 0:
                sys.stderr.write("vote doesn't end with 0: %r\n", line)
                sys.exit(1)
            rankings.append(vparts[1:-1])
        cnames = []
        title = None
        for line in fin:
            line = line.strip()
            if len(cnames) < candidates:
                if line[0] == '"' and line[-1] == '"':
                    line = line[1:-1]
                cnames.append(line)
            elif title is None:
                title = line
            else:
                sys.stderr.write("unexpected line at end: %r\n", line)
                sys.exit(1)

    for r in rankings:
        rate = candidates + 1
        parts = []
        for ci in r:
            parts.append((cnames[ci-1], rate))
            rate -= 1
        out.write(urllib.parse.urlencode(parts) + '\n')

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('infile', nargs='?')
    ap.add_argument('-i', '--in', dest='inname', default=None)
    ap.add_argument('-o', '--out', default=None)
    args = ap.parse_args()

    inname = args.infile or args.inname
    outname = args.out
    if outname is None or outname == '-':
        out = sys.stdout
    else:
        out = open(outname, 'wt')
    foo(inname, out)
    return

if __name__ == '__main__':
    main()
