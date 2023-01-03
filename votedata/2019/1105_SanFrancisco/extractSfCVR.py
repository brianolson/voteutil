import argparse
import glob
import json
import re
import os
import urllib.parse
import logging
import zipfile

from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import processFile

ap = argparse.ArgumentParser()
ap.add_argument('cvr')
args = ap.parse_args()

logger = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO)

# https://sfelections.sfgov.org/november-8-2022-election-results-detailed-reports
# https://www.sfelections.org/results/20221108/data/20221201/CVR_Export_20221201120428.zip

zf = zipfile.ZipFile(args.cvr)

files = zf.infolist()

cvrpat = re.compile(r'CvrExport_(\d+).json')
cvrs = {}
otherFiles = []
for zi in files:
    if zi.filename == 'CvrExport.json':
        cvrs[''] = zi
        continue
    m = cvrpat.match(zi.filename)
    if m:
        cvrs[int(m.group(1))] = zi
    else:
        otherFiles.append(zi)
print('\n'.join(sorted([x.filename for x in otherFiles])))
candidates = {}
with zf.open('CandidateManifest.json') as fin:
    ob = json.load(fin)
for rec in ob['List']:
    cont = rec['ContestId']
    cand = rec['Id']
    name = rec['Description']
    if cand in candidates:
        print('dup candidate {}'.format(cand))
    candidates[cand] = name
rcvContests = {}
with zf.open('ContestManifest.json') as fin:
    ob = json.load(fin)
for rec in ob['List']:
    if rec['NumOfRanks'] > 1:
        rcvContests[rec['Id']] = rec
print('\n'.join(['(ContestId={}) {}'.format(rc['Id'], rc['Description']) for rc in rcvContests.values()]))
# with zf.open('CvrExport_1.json') as fin:
#     cvr = json.load(fin)
# print(dict(Version=cvr['Version'], ElectionId=cvr['ElectionId']))

# main extract
rcvContestIds = set(rcvContests.keys())
nameqouts = {}
def wnameq(contestId, line):
    fout = nameqouts.get(contestId)
    if fout is None:
        cont = rcvContests[contestId]
        path = cont['Description'] + '.nameq'
        path = urllib.parse.quote_plus(path)
        fout = open(path, 'wt')
        nameqouts[contestId] = fout
    fout.write(line)
rawouts = {}
def wrawcont(contestId, rec):
    fout = rawouts.get(contestId)
    if fout is None:
        cont = rcvContests[contestId]
        path = cont['Description'] + '.json'
        path = urllib.parse.quote_plus(path)
        fout = open(path, 'wt')
        rawouts[contestId] = fout
    fout.write(json.dumps(rec) + '\n')
count = 0
for path in cvrs.values():
    print(path)
    with zf.open(path) as fin:
        raw = fin.read()
        #print(raw[:1000])
        cvr = json.loads(raw)
    print(path, dict(Version=cvr['Version'], ElectionId=cvr['ElectionId']))
    fcount = 0
    for ses in cvr['Sessions']:
        seso = ses['Original']
        cards = seso.get('Cards')
        if not cards:
            cards = [seso]
        for card in cards:
            for cont in card['Contests']:
                if cont['Id'] in rcvContestIds:
                    wrawcont(cont['Id'], cont)
                    vote = {}
                    for mark in cont['Marks']:
                        if mark['IsVote']:
                            name = candidates[mark['CandidateId']]
                            vote[name] = mark['Rank']
                        else:
                            print(
                                '{}:warning: (TabulatorId={},BatchId={},RecordId={}) ContestId={} non vote mark: {!r}'.format(
                                    path, ses['TabulatorId'], ses['BatchId'], ses['RecordId'], cont['Id'], mark))
                    line = urllib.parse.urlencode(vote) + '\n'
                    wnameq(cont['Id'], line)
                    count += 1
                    fcount += 1
    print('{}: {} votes'.format(path, fcount))
for fout in nameqouts.values():
    fout.close()
for fout in rawouts.values():
    fout.close()
print('Done: {} votes'.format(count))
