#!/usr/bin/env python
# coding: utf-8
#
# Alaska 2022 PRIMARY ELECTION – AUGUST 16, 2022
# https://www.elections.alaska.gov/election-results/e/?id=22prim
# https://www.elections.alaska.gov/results/22PRIM/CVR_Export_20220908084311.zip
# 6512608 bytes
# shasum -a 256
# 3d9ea12a0fa12064028057e02ad855ad63d349ccfb3b031afcdeb2eebfd1beab  CVR_Export_20220908084311.zip

import json
import urllib.parse


with open('CandidateManifest.json') as fin:
    candm = json.load(fin)

candById = {}
for cand in candm['List']:
    cid = cand['Id']
    if cid in candById:
        print('dup cand id {}: {} vs {}'.format(cid, json.dumps(cand), json.dumps(candById[cid])))
    candById[cid] = cand

with open('ContestManifest.json') as fin:
    contestm = json.load(fin)

contestById = {}
for cont in contestm['List']:
    contestById[cont['Id']] = cont

with open('CvrExport.json') as fin:
    votes = json.load(fin)

contestfiles = {}
for vote in votes['Sessions']:
    cards = vote['Original']['Cards']
    if len(cards) != 1:
        print('cards:\n{}'.format(json.dumps(vote)))
    for vcontest in cards[0]['Contests']:
        contid = vcontest['Id']
        cf = contestfiles.get(contid)
        if not cf:
            contest = contestById[contid]
            cf = open(contest['Description'].replace(' ', '_').replace('/', '|') + '.nameq', 'wt')
            contestfiles[contid] = cf
        cv = {}
        for mark in vcontest['Marks']:
            cand = candById[mark['CandidateId']]
            cv[cand['Description']] = mark['Rank']
            # TODO: check 'IsAmbiguous' and 'OutstackConditionIds':[]
        cf.write(urllib.parse.urlencode(cv) + '\n')

for contid, cf in contestfiles.items():
    cf.close()
