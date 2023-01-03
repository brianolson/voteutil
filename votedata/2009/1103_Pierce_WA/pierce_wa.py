#!/usr/bin/env python
# coding: utf-8

# "Candidate 0000440Jan Shabro                                        0000001000071400"
fieldLengths = [len("Candidate "), len("0000440"), len("Jan Shabro                                        "), len("0000001"), len("0000714"), 1,1]
def fieldSlicesFromLengths(lengths):
    start = 0
    slices = []
    for fl in lengths:
        slices.append((start, start+fl))
        start = start+fl
    return slices
fieldFirstAfter = fieldSlicesFromLengths(fieldLengths)
mrecs = []
with open('masterlookup.txt') as fin:
    for line in fin:
        row = []
        for start, after in fieldFirstAfter:
            row.append(line[start:after].strip())
        mrecs.append(row)

mheader = ('type', 'id', 'name', 'type-index', 'contest', 'write-in', 'provisional')

# id to name map
candidates = {}
for rec in mrecs:
    if rec[0] != 'Candidate':
        continue
    candidates[rec[1]] = rec[2]

ids = set([x[1] for x in mrecs])
print(sorted(ids))

# 000071400001543600000010050000002001000044100
# 000071400001543600000010050000002002000044000
# 000071400017999500000330050000297001000044100
# 000071400017999500000330050000297002000044000
# 000071400017999500000330050000297003000044200
# contest ballot    ?       ?   ?precinct? rank candidate ??
# 0000714 000015436 0000001 005 0000002 002 0000440 00
bidFieldLengths = [len("0000714"), len("000015436"), len("0000001"), 3, len("0000002"), 3, 7, 1, 1]
bidSlices = fieldSlicesFromLengths(bidFieldLengths)
def bidrow(line):
    out = []
    for start,after in bidSlices:
        out.append(line[start:after])
    return out
data = []
with open("bid.txt", "rt") as fin:
    for line in fin:
        data.append(bidrow(line))

ballots = {}
for row in data:
    _, ballot, _, _, _, rank, candidate, _, _ = row
    rank = int(rank,10)
    candidate = candidates.get(candidate)
    if candidate is None:
        continue
    v = ballots.get(ballot)
    if v is None:
        v = {candidate:rank}
        ballots[ballot] = v
    else:
        v[candidate] = rank

import urllib.parse
with open('auditor.nameq', 'wt') as fout:
    for vd in ballots.values():
        fout.write(urllib.parse.urlencode(vd) + '\n')
