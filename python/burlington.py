#!/usr/bin/env python3
#
# Parse "TrueBallot"/"ChoicePlus Pro" format used in Burlington VT, 2006 and 2009
# Convert to a single file 'vote.nameq' which is URL encoded name=rank&... lines for each vote.


import csv
import re
import sys
import urllib.parse

def main():
    includes = []
    incline = re.compile(r'^.INCLUDE\s+(.*)')
    cands = {}
    candline = re.compile(r'^.CANDIDATE ([^,]+),\s+"(.*)"')
    with open('2009 Burlington.in') as fin:
        for line in fin:
            m = candline.match(line)
            if m:
                cands[m.group(1)] = m.group(2)
                continue
            m = incline.match(line)
            if m:
                includes.append(m.group(1))
    print(cands)
    print(includes)

    # .BALLOT-FORMAT-FIELDS BALLOT-ID-ALPHA BALLOT-TOP-ALPHA IGNORE-FIELD IGNORE-FIELD RANKINGS-ALPHA
    voteline = re.compile(r'^[^,]+,[^,]+,[^,]+,\S+\s+(.*)')
    allvotes = []
    for incpath in includes:
        with open(incpath) as fin:
            lineno = 0
            for line in fin:
                lineno += 1
                if not line:
                    continue
                line = line.strip()
                if not line:
                    continue
                m = voteline.match(line)
                if not m:
                    sys.stderr.write("{}:{} could not process line \"{!r}\"\n".format(incpath, lineno, line))
                    continue
                cnames = m.group(1).split(',')
                nameranks = {}
                for x in cnames:
                    name,rank = cnameparse(x)
                    if '=' in name:
                        names = name.split('=')
                        for n in names:
                            n = cands.get(n,n)
                            nameranks[n] = rank
                    else:
                        name = cands.get(name,name)
                        nameranks[name] = rank
                allvotes.append(nameranks)
                #print(nameranks)
    print('{} votes'.format(len(allvotes)))
    with open('vote.nameq','w') as fout:
        for v in allvotes:
            fout.write(urllib.parse.urlencode(v) + '\n')

cnamere = re.compile(r'(.*?)\[(\d+)\]')

def cnameparse(x):
    m = cnamere.match(x)
    if m:
        return m.group(1), int(m.group(2))
    return x


if __name__ == '__main__':
    main()
