#!/usr/bin/env python
# coding: utf-8

# In[1]:


import glob
import io
import json
import re
import time
import os
import urllib.parse
import logging
import zipfile

import IPython.core.display

from voteutil.irv import IRV
from voteutil.vrr import VRR
from voteutil.vrr2 import VRR2
from voteutil.pickone import PickOne
from voteutil.count import processFile

logger = logging.getLogger(__name__)

# https://sfelections.sfgov.org/november-3-2020-election-results-detailed-reports
# Final Report
# https://www.sfelections.org/results/20201103/data/20201201/CVR_Export_20201201091840.zip
zf = zipfile.ZipFile('CVR_Export_20201201091840.zip')
# Preliminary Report 16
# https://www.sfelections.org/results/20201103/data/20201124/CVR_Export_20201124150514.zip-
# zf = zipfile.ZipFile('CVR_Export_20201124150514.zip')
# Preliminary Report 15
# https://www.sfelections.org/results/20201103/data/20201119/CVR_Export_20201119152920.zip
#zf = zipfile.ZipFile('CVR_Export_20201119152920.zip')
# Preliminary Report 14
# https://www.sfelections.org/results/20201103/data/20201117/CVR_Export_20201117160040.zip
#zf = zipfile.ZipFile('CVR_Export_20201117160040.zip')
# Preliminary Report 13
# https://www.sfelections.org/results/20201103/data/20201113/CVR_Export_20201113155705.zip
#zf = zipfile.ZipFile('CVR_Export_20201113155705.zip')
# Preliminary Report 12
# https://www.sfelections.org/results/20201103/data/20201112/CVR_Export_20201112161239.zip
#zf = zipfile.ZipFile('CVR_Export_20201112161239.zip')


# In[70]:


def phtml(x):
    IPython.core.display.display(IPython.core.display.HTML(x))


# In[2]:


files = zf.infolist()


# In[3]:


cvrpat = re.compile(r'CvrExport_(\d+).json')
cvrs = {}
otherFiles = []
for zi in files:
    #dirpath, fname = os.path.split(path)
    m = cvrpat.match(zi.filename)
    if m:
        cvrs[int(m.group(1))] = zi
    else:
        otherFiles.append(zi)


# In[4]:


print('\n'.join(sorted([x.filename for x in otherFiles])))


# In[5]:


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


# In[6]:


rcvContests = {}
with zf.open('ContestManifest.json') as fin:
    ob = json.load(fin)
for rec in ob['List']:
    if rec['NumOfRanks'] > 1:
        rcvContests[rec['Id']] = rec


# In[7]:


print('\n'.join(['(ContestId={}) {}'.format(rc['Id'], rc['Description']) for rc in rcvContests.values()]))


# In[8]:


with zf.open('CvrExport_1.json') as fin:
    cvr = json.load(fin)


# In[9]:


print(dict(Version=cvr['Version'], ElectionId=cvr['ElectionId']))


# In[10]:


# main extract
start = time.time()
rcvContestIds = set(rcvContests.keys())
nameqouts = {}
def wnameq(contestId, line):
    # keep name=rank&... url encoded votes, to file per contest
    fout = nameqouts.get(contestId)
    if fout is None:
        cont = rcvContests[contestId]
        path = cont['Description'] + '.nameq'
        fout = open(path, 'wt')
        nameqouts[contestId] = fout
    fout.write(line)
rawouts = {}
def wrawcont(contestId, rec):
    # keep json-per-line of RCV contest records, to file per contest
    fout = rawouts.get(contestId)
    if fout is None:
        cont = rcvContests[contestId]
        path = cont['Description'] + '.json'
        fout = open(path, 'wt')
        rawouts[contestId] = fout
    fout.write(json.dumps(rec) + '\n')
count = 0
for path in cvrs.values():
    #print(path)
    with zf.open(path) as fin:
        cvr = json.load(fin)
    fcount = 0
    for ses in cvr['Sessions']:
        for card in ses['Original']['Cards']:
            for cont in card['Contests']:
                if cont['Id'] in rcvContestIds:
                    wrawcont(cont['Id'], cont)
                    vote = {}
                    for mark in cont['Marks']:
                        if mark['IsVote']:
                            name = candidates[mark['CandidateId']]
                            vote[name] = mark['Rank']
                        elif False:
                            print(
                                '{}:warning: (TabulatorId={},BatchId={},RecordId={}) ContestId={} non vote mark: {!r}'.format(
                                    path, ses['TabulatorId'], ses['BatchId'], ses['RecordId'], cont['Id'], mark))
                    line = urllib.parse.urlencode(vote) + '\n'
                    wnameq(cont['Id'], line)
                    count += 1
                    fcount += 1
    #print('{}: {} votes'.format(path, fcount))
for fout in nameqouts.values():
    fout.close()
for fout in rawouts.values():
    fout.close()
print('Done: {} votes ({:.1f} seconds)'.format(count, time.time() - start))


# In[11]:


# generate HTML reports
import voteutil.rcvmatters
import glob

for fname in glob.glob('*.nameq'):
    voteutil.rcvmatters.testFile(fname)


# In[ ]:


voteutil.rcvmatters.testFile('BOARD OF SUPERVISORS DISTRICT 1.nameq.html')


# In[12]:


recs = []
with open('BOARD OF SUPERVISORS DISTRICT 1.json') as fin:
    for line in fin:
        recs.append(json.loads(line))


# In[13]:


ov = []
uv = []
for rec in recs:
    if rec['Overvotes']:
        ov.append(rec)
    if rec['Undervotes']:
        uv.append(rec)


# In[14]:


print('all votes')
print(len(recs))
print('"Overvotes"')
print(len(ov))
print('"Undervotes"')
print(len(uv))


# In[15]:


# official round one continuing 36076, non-transferrable 3833
# official round one blanks 3726
# official round one overvotes 107
3833+36076


# In[16]:


len(ov)+len(uv)


# In[17]:


nomarks = 0
for rec in recs:
    if not rec.get('Marks'):
        nomarks += 1
print(nomarks)


# In[18]:


novotes = 0
for rec in recs:
    marks = rec.get('Marks')
    if not marks:
        novotes += 1
        continue
    if not any([m['IsVote'] for m in marks]):
        novotes += 1
print(novotes)


# In[19]:


# find highest duplicate rank
rdups = {}
rcount = {}
amnc = {}
amvc = {}
for rec in recs:
    rc = {}
    for mark in rec.get('Marks',[]):
        rank = mark['Rank']
        if not mark['IsVote']:
            if mark['IsAmbiguous']:
                amnc[rank] = amnc.get(rank, 0) + 1
            continue
        if mark['IsAmbiguous']:
            amvc[rank] = amvc.get(rank, 0) + 1
        rcount[rank] = rcount.get(rank, 0) + 1
        rc[rank] = rc.get(rank, 0) + 1
    for rank, count in sorted(rc.items()):
        if count > 1:
            rdups[rank] = rdups.get(rank, 0) + 1
print('(rank,dups)...')
print(sorted(rdups.items()))
print('(rank, all votes at rank), ...')
print(sorted(rcount.items()))
print('(rank, ambiguous non-vote at rank), ...')
print(sorted(amnc.items()))
print('(rank, ambiguous vote at rank), ...')
print(sorted(amvc.items()))


# In[20]:


# 347 overvotes == 347 votes with duplicate at rank=1


# In[21]:


# https://www.sfelections.org/results/20201103/data/20201201/d1/20201201_d1_short.pdf
# Looking for official Round 1 counts:
# AMANDA INOCENCIO: 702
# ANDREW N. MAJALYA: 312
# CONNIE CHAN: 13508
# DAVID E. LEE: 6293
# MARJAN PHILHOUR: 12383
# SHERMAN R. D'SILVA: 1558
# VERONICA SHINZATO: 1320
expected = [
    ('AMANDA INOCENCIO', 702),
    ('ANDREW N. MAJALYA', 312),
    ('CONNIE CHAN', 13508),
    ('DAVID E. LEE', 6293),
    ('MARJAN PHILHOUR', 12383),
    ("SHERMAN R. D'SILVA", 1558),
    ('VERONICA SHINZATO', 1320),
]


# In[22]:


702+312+13508+6293+12383+1558+1320


# In[40]:


sum([x[1] for x in expected])


# In[71]:


dexpected = dict(expected)
def presult(rname, result):
    rd = dict(result)
    keys = sorted(set(list(dexpected.keys()) + list(rd.keys())))
    deltas = [(k,rd.get(k,0)-dexpected.get(k,0)) for k in keys]
    sabsd = sum([abs(d) for _,d in deltas])
    phtml('<b>{}, ({}) (err {})</b>'.format(rname, sum([x[1] for x in rd.items()]), sabsd))
    print('  ' + repr(sorted(rd.items())))
    print('  ' + repr([(k,rd.get(k,0)-dexpected.get(k,0)) for k in keys]))


# In[72]:


firsts = {}
firstsNoDups = {}
firstsNoOvervotes = {}
frdups = 0
for rec in recs:
    fr = []
    fra = []
    for mark in rec['Marks']:
        rank = mark['Rank']
        if rank != 1:
            continue
        name = candidates[mark['CandidateId']]
        if mark['IsVote']:
            fr.append(name)
        elif mark['IsAmbiguous']:
            fra.append(name)
    # if len(fr) == 0:
    #     fr = fra
    if len(fr) > 1:
        frdups += 1
    for name in fr:
        firsts[name] = firsts.get(name, 0) + 1
        if len(fr) == 1:
            firstsNoDups[name] = firstsNoDups.get(name, 0) + 1
        if not rec['Overvotes']:
            firstsNoOvervotes[name] = firstsNoOvervotes.get(name, 0) + 1
print('official final')
print(expected)
#print('firsts, all ({})'.format(sum([x[1] for x in firsts.items()])))
#print(sorted(firsts.items()))
#print('firsts, removing Overvotes ({})'.format(sum([x[1] for x in firstsNoOvervotes.items()])))
#print(sorted(firstsNoOvervotes.items()))
#print('firsts, removing dups at rank 1, frdups={} ({})'.format(frdups, sum([x[1] for x in firstsNoDups.items()])))
#print(sorted(firstsNoDups.items()))
presult('firsts, all', firsts)
presult('firsts, removing Overvotes', firstsNoOvervotes)
presult('firsts, removing dups at rank 1, frdups={}'.format(frdups), firstsNoDups)


# In[73]:


print('IsVote, but if no IsVote then IsAmbiguous')
firsts = {}
firstsNoDups = {}
firstsNoOvervotes = {}
frdups = 0
for rec in recs:
    fr = []
    fra = []
    for mark in rec['Marks']:
        rank = mark['Rank']
        if rank != 1:
            continue
        name = candidates[mark['CandidateId']]
        if mark['IsVote']:
            fr.append(name)
        elif mark['IsAmbiguous']:
            fra.append(name)
    if len(fr) == 0:
        fr = fra
    if len(fr) > 1:
        frdups += 1
    for name in fr:
        firsts[name] = firsts.get(name, 0) + 1
        if len(fr) == 1:
            firstsNoDups[name] = firstsNoDups.get(name, 0) + 1
        if not rec['Overvotes']:
            firstsNoOvervotes[name] = firstsNoOvervotes.get(name, 0) + 1
print('official final')
print(expected)
presult('firsts, all', firsts)
presult('firsts, removing Overvotes', firstsNoOvervotes)
presult('firsts, removing dups at rank 1, frdups={}'.format(frdups), firstsNoDups)


# In[74]:


print('highest rank (if not 1, then 2), and if no IsVote then IsAmbiguous')
firsts = {}
firstsNoDups = {}
firstsNoOvervotes = {}
frdups = 0
laterButNoFirst = 0
def da(d, k, v):
    # dict append
    l = d.get(k)
    if l is None:
        d[k] = [v]
    else:
        l.append(v)
for rec in recs:
    v = {}
    va = {}
    for mark in rec['Marks']:
        rank = mark['Rank']
        name = candidates[mark['CandidateId']]
        if mark['IsVote']:
            da(v,rank,name)
        elif mark['IsAmbiguous']:
            da(va,rank,name)
    if (not v) and (not va):
        continue
    minv = (v and min(v.keys())) or None
    mina = (va and min(va.keys())) or None
    if min(filter(None,[mina,minv])) != 1:
        laterButNoFirst += 1
    if mina and ((not minv) or (mina < minv)):
        fr = va[mina]
    elif minv and v:
        fr = v[minv]
    else:
        fr = []
    if len(fr) > 1:
        frdups += 1
    for name in fr:
        firsts[name] = firsts.get(name, 0) + 1
        if len(fr) == 1:
            firstsNoDups[name] = firstsNoDups.get(name, 0) + 1
        if not rec['Overvotes']:
            firstsNoOvervotes[name] = firstsNoOvervotes.get(name, 0) + 1
print('laterButNoFirst {}'.format(laterButNoFirst))
print('official final')
print(expected)
presult('firsts, all', firsts)
presult('firsts, removing Overvotes', firstsNoOvervotes)
presult('firsts, removing dups at rank 1, frdups={}'.format(frdups), firstsNoDups)


# In[76]:


print('highest rank (if not 1, then 2), and if no IsVote then IsAmbiguous, erase Write-in')
firsts = {}
firstsNoDups = {}
firstsNoOvervotes = {}
frdups = 0
laterButNoFirst = 0
def da(d, k, v):
    # dict append
    l = d.get(k)
    if l is None:
        d[k] = [v]
    else:
        l.append(v)
for rec in recs:
    v = {}
    va = {}
    for mark in rec['Marks']:
        rank = mark['Rank']
        name = candidates[mark['CandidateId']]
        if name == 'Write-in':
            continue
        if mark['IsVote']:
            da(v,rank,name)
        elif mark['IsAmbiguous']:
            da(va,rank,name)
    if (not v) and (not va):
        continue
    minv = (v and min(v.keys())) or None
    mina = (va and min(va.keys())) or None
    if min(filter(None,[mina,minv])) != 1:
        laterButNoFirst += 1
    if mina and ((not minv) or (mina < minv)):
        fr = va[mina]
    elif minv and v:
        fr = v[minv]
    else:
        fr = []
    if len(fr) > 1:
        frdups += 1
    for name in fr:
        firsts[name] = firsts.get(name, 0) + 1
        if len(fr) == 1:
            firstsNoDups[name] = firstsNoDups.get(name, 0) + 1
        if not rec['Overvotes']:
            firstsNoOvervotes[name] = firstsNoOvervotes.get(name, 0) + 1
print('laterButNoFirst {}'.format(laterButNoFirst))
print('official final')
print(expected)
presult('firsts, all', firsts)
presult('firsts, removing Overvotes', firstsNoOvervotes)
presult('firsts, removing dups at rank 1, dups={}'.format(frdups), firstsNoDups)


# In[27]:


print('example record')
print(json.dumps(recs[1], indent=2))


# In[28]:


with zf.open('OutstackConditionManifest.json') as fin:
    ocm = json.load(fin)


# In[29]:


outstackConditions = {x['Id']:x['Description'] for x in ocm['List']}


# In[30]:


outstackConditions


# In[31]:


oci = {}
moci = {}
def dil(d, l, v=1):
    for k in l:
        d[k] = d.get(k,0) + v
for rec in recs:
    dil(oci, rec['OutstackConditionIds'])
    for m in rec['Marks']:
        dil(moci, m['OutstackConditionIds'])
print('record OutstackConditionIds: {}'.format([(outstackConditions[x],c) for x,c in sorted(oci.items())]))
print('mark OutstackConditionIds: {}'.format([(outstackConditions[x],c) for x,c in sorted(moci.items())]))


# In[87]:


errt_header = ['','expected','actual','diff']
def smartsub(a,b):
    if a is None:
        return b
    if b is None:
        return '-{!r}'.format(a)
    return a - b
def errt(expected, actual):
    ed = dict(expected)
    ad = dict(actual)
    keys = sorted(set(list(ed.keys()) + list(ad.keys())))
    return [[k, ed.get(k), ad.get(k), smartsub(ad.get(k), ed.get(k))] for k in keys]
import csv
import io
def errtcsv(expected,actual):
    t = errt(expected,actual)
    out = io.StringIO()
    w = csv.writer(out)
    w.writerow(errt_header)
    for row in t:
        w.writerow(row)
    return out.getvalue()
def errthtml(expected,actual):
    t = errt(expected,actual)
    out = '<table><tr>' + ''.join(['<th>{}</th>'.format(x) for x in errt_header]) + '</tr>'
    for row in t:
        out += '<tr>{}</tr>'.format(''.join(['<td>{}</td>'.format(x) for x in row]))
    return out + '</table>'


# In[88]:


print(errtcsv(expected, firstsNoDups))


# In[89]:


phtml(errthtml(expected, firstsNoDups))


# In[90]:


print(errthtml(expected, firstsNoDups))


# In[ ]:




