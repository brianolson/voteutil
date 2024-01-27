#!/usr/bin/env python
# coding: utf-8
# Processing the NYC 2023-06-27 Primary Election RCV contests
#
# setup:
# curl -L -O https://www.vote.nyc/sites/default/files/pdf/election_results/2023/20230627Primary%20Election/cvr/2023P_CVR_Final.zip
# unzip 2023P_CVR_Final.zip
# ~/src/ve/bin/python3 /home/bolson/src/voteutil/python/voteutil/xlsxtocsv.py *xlsx
# ~/src/ve/bin/python3 boenyc2023.py

import glob
import csv
import re
import os
import urllib.parse
import logging


# In[2]:


files = glob.glob('*.csv')


# In[3]:


headers = {}
for fn in files:
    with open(fn) as fin:
        reader = csv.reader(fin)
        hrow = reader.__next__()
        for k in hrow:
            hl = headers.get(k)
            if hl is None:
                headers[k] = [fn]
            else:
                hl.append(fn)


# In[4]:


hpat = re.compile(r'(.*?)\s+Choice\s+(\d+)\s+of\s+(\d+)\s+(.*?)\s+\((\d+)\)')
def parseHeaderContest(cname):
    m = hpat.match(cname)
    if not m:
        return None,None,None,None,None
    office = m.group(1)
    rank = int(m.group(2))
    maxrank = int(m.group(3))
    region = m.group(4)
    idn = m.group(5)
    return office,rank,maxrank,region,idn

races = {}
for cname in headers.keys():
    #m = hpat.match(cname)
    #if not m:
    #    continue
    #office = m.group(1)
    #rank = int(m.group(2))
    #maxrank = int(m.group(3))
    #region = m.group(4)
    #idn = m.group(5)
    office,rank,maxrank,region,idn = parseHeaderContest(cname)
    if office is None:
        continue
    #print(office, rank, maxrank, region)
    orkey = (office,region)
    rec = races.get(orkey)
    if rec is None:
        rec = dict(ranks=[rank],maxrank=maxrank)
        races[orkey] = rec
    else:
        rec['ranks'].append(rank)
        assert(rec['maxrank']==maxrank)


# In[5]:


def assertComplete(rec):
    for i in range(1,rec['maxrank']+1):
        assert(i in rec['ranks'])
for orkey, rec in races.items():
    assertComplete(rec)


# In[6]:


print(sorted(races.keys()))


# In[7]:


namesById = {}
namefile = glob.glob("*_CandidacyID_To_Name.csv")[0]
with open(namefile, 'rt') as fin:
    reader = csv.reader(fin)
    header = reader.__next__()
    # CandidacyID,DefaultBallotName
    assert(header == ['CandidacyID','DefaultBallotName'])
    for row in reader:
        cid, name = row
        namesById[cid] = name


# In[8]:


len(namesById)


# In[9]:


datafiles = set(files)
datafiles.remove(namefile)


# In[12]:


def nycToNameq(fname):
    # map (office,region) to file
    outs = {}
    badids = set()
    outcounts = {}
    with open(fname) as fin:
        reader = csv.reader(fin)
        header = reader.__next__()
        cols = []
        outset = set()
        for ci, cname in enumerate(header):
            office,rank,maxrank,region,idn = parseHeaderContest(cname)
            if office is not None:
                cols.append((ci, (office,region),rank))
                outset.add((office,region))
        for orkey in outset:
            outname = '{}_{}.nameq'.format(orkey[0],orkey[1])
            outs[orkey] = open(outname, 'at')
        for row in reader:
            votes = {}
            for ci, orkey, rank in cols:
                name = namesById.get(row[ci])
                if name is not None:
                    vote = votes.get(orkey)
                    if vote is None:
                        votes[orkey] = {name:rank}
                    else:
                        vote[name] = rank
            for orkey, vote in votes.items():
                outs[orkey].write(urllib.parse.urlencode(vote) + '\n')
                outcounts[orkey] = outcounts.get(orkey,0) + 1
    for fout in outs.values():
        fout.close()
    if badids:
        logging.warning('unknown ids: %r', sorted(badids))
    #logging.debug('%s: %r', fname, outcounts)
    return outcounts


# In[13]:


for fname in glob.glob('*.nameq'):
    os.remove(fname)
for fname in datafiles:
    print(fname)
    outcounts = nycToNameq(fname)
    print('{}:{!r}'.format(fname, outcounts))
print('Done')


# In[1]:


# https://github.com/brianolson/voteutil
from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import processFile
import glob
import io
import urllib.parse

count = 0
failcount = 0

for fname in glob.glob('*.nameq'):
#for fname in ['DEM Council Member_25th Council District.nameq']:
    print(fname)
    names = []
    nameIndexes = {}
    algorithms = [VRR(names), VRR2(names), IRV(names), PickOne(names)]
    with open(fname, 'r') as fin:
        votes, comments = processFile(algorithms, fin, args=None, names=names, nameIndexes=nameIndexes, rankings=True)
    if votes == 0:
        print('{} empty'.format(fname))
        continue
    count += 1
    prevr = None
    preva = None
    html = io.StringIO()
    fail = False
    for alg in algorithms:
        html.write('<h1>{}</h1>\n'.format(alg.name()))
        xr = alg.getResults(html)
        if prevr is not None:
            if prevr[0][0] != xr[0][0]:
                if not fail:
                    print(preva, prevr)
                    print(alg.name(), xr)
                    print('{}: {} {} != {} {}'.format(fname, preva, prevr[0][0], alg.name(), xr[0][0]))
                fail = True
        prevr = xr
        preva = alg.name()
    if fail:
        failcount += 1
    if fail or True:
        outname = fname + '.html'
        print(outname)
        with open(outname, 'wt') as fout:
            fout.write(html.getvalue())
print('Done. {} elections, {} different'.format(count,failcount))
