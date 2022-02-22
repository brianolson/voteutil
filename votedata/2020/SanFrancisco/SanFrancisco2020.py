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


# In[2]:


def phtml(x):
    IPython.core.display.display(IPython.core.display.HTML(x))


# In[3]:


files = zf.infolist()


# In[4]:


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


# In[5]:


print('\n'.join(sorted([x.filename for x in otherFiles])))


# In[6]:


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


# In[7]:


rcvContests = {}
with zf.open('ContestManifest.json') as fin:
    ob = json.load(fin)
for rec in ob['List']:
    if rec['NumOfRanks'] > 1:
        rcvContests[rec['Id']] = rec


# In[8]:


print('\n'.join(['(ContestId={}) {}'.format(rc['Id'], rc['Description']) for rc in rcvContests.values()]))


# In[9]:


with zf.open('CvrExport_1.json') as fin:
    cvr = json.load(fin)


# In[10]:


print(dict(Version=cvr['Version'], ElectionId=cvr['ElectionId']))


# In[11]:


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
def dictflip(d):
    # flip a dict {k:v,...} to {v:[k,...],...}
    out = {}
    for k,v in d.items():
        l = out.get(k)
        if l is None:
            out[v] = [k]
        else:
            l.append(k)
    return out
count = 0
limit = 999999999  # limit for debugging
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
                    ambiguous = {}
                    for mark in cont['Marks']:
                        name = candidates[mark['CandidateId']]
                        if name == 'Write-in':
                            # San Francisco weirds their IRV count by pretending Write-In votes didn't happen.
                            # This non-obvious interpretation of the raw data is a compromise.
                            # On the plus side, user-error of writing-in a name they also voted for gets cleaned up.
                            # On the down side, the official results neglect the Write-In 'none of the above' vote, and I think that measure of dissatisfaction is important.
                            continue
                        if mark['IsVote']:
                            vote[name] = mark['Rank']
                        elif mark['IsAmbiguous']:
                            ambiguous[name] = mark['Rank']
                        elif False:
                            print(
                                '{}:warning: (TabulatorId={},BatchId={},RecordId={}) ContestId={} non vote mark: {!r}'.format(
                                    path, ses['TabulatorId'], ses['BatchId'], ses['RecordId'], cont['Id'], mark))
                    # if no unambiguous IsVote at a Rank, use IsAmbiguous mark(s) at that Rank
                    voteByRank = dictflip(vote)
                    ambiguousByRank = dictflip(ambiguous)
                    #logger.warning('vote %r vbr %r abr %r', vote, voteByRank, ambiguousByRank)
                    for rank, namelist in ambiguousByRank.items():
                        if voteByRank.get(rank) is None:
                            for aname in namelist:
                                vote[aname] = rank
                    line = urllib.parse.urlencode(vote) + '\n'
                    wnameq(cont['Id'], line)
                    count += 1
                    fcount += 1
            if count > limit:
                break
        if count > limit:
            break
    if count > limit:
        break
    #print('{}: {} votes'.format(path, fcount))
for fout in nameqouts.values():
    fout.close()
for fout in rawouts.values():
    fout.close()
print('Done: {} votes ({:.1f} seconds)'.format(count, time.time() - start))


# In[12]:


# generate HTML reports
import voteutil.rcvmatters
import glob

for fname in glob.glob('*.nameq'):
    voteutil.rcvmatters.testFile(fname)


# In[13]:


#voteutil.rcvmatters.testFile('BOARD OF SUPERVISORS DISTRICT 1.nameq')


# In[14]:


recs = []
with open('BOARD OF SUPERVISORS DISTRICT 1.json') as fin:
    for line in fin:
        recs.append(json.loads(line))


# In[15]:


ov = []
uv = []
for rec in recs:
    if rec['Overvotes']:
        ov.append(rec)
    if rec['Undervotes']:
        uv.append(rec)


# In[16]:


print('all votes')
print(len(recs))
print('"Overvotes"')
print(len(ov))
print('"Undervotes"')
print(len(uv))


# In[17]:


# official round one continuing 36076, non-transferrable 3833
# official round one blanks 3726
# official round one overvotes 107
3833+36076


# In[18]:


len(ov)+len(uv)


# In[19]:


nomarks = 0
for rec in recs:
    if not rec.get('Marks'):
        nomarks += 1
print(nomarks)


# In[20]:


novotes = 0
for rec in recs:
    marks = rec.get('Marks')
    if not marks:
        novotes += 1
        continue
    if not any([m['IsVote'] for m in marks]):
        novotes += 1
print(novotes)


# In[21]:


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


# In[22]:


# 347 overvotes == 347 votes with duplicate at rank=1


# In[23]:


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


# In[24]:


702+312+13508+6293+12383+1558+1320


# In[25]:


sum([x[1] for x in expected])


# In[26]:


dexpected = dict(expected)
def presult(rname, result):
    rd = dict(result)
    keys = sorted(set(list(dexpected.keys()) + list(rd.keys())))
    deltas = [(k,rd.get(k,0)-dexpected.get(k,0)) for k in keys]
    sabsd = sum([abs(d) for _,d in deltas])
    phtml('<b>{}, ({}) (err {})</b>'.format(rname, sum([x[1] for x in rd.items()]), sabsd))
    print('  ' + repr(sorted(rd.items())))
    print('  ' + repr([(k,rd.get(k,0)-dexpected.get(k,0)) for k in keys]))


# In[27]:


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
presult('firsts, all', firsts)
presult('firsts, removing Overvotes', firstsNoOvervotes)
presult('firsts, removing dups at rank 1, frdups={}'.format(frdups), firstsNoDups)


# In[28]:


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


# In[29]:


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


# In[30]:


print('highest rank (if not 1, then 2), erase Write-in')
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
    #if mina and ((not minv) or (mina < minv)):
    #    fr = va[mina]
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


# In[31]:


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


# In[32]:


print('example record')
print(json.dumps(recs[1], indent=2))


# In[33]:


with zf.open('OutstackConditionManifest.json') as fin:
    ocm = json.load(fin)


# In[34]:


outstackConditions = {x['Id']:x['Description'] for x in ocm['List']}


# In[35]:


outstackConditions


# In[36]:


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


# In[37]:


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


# In[38]:


print(errtcsv(expected, firstsNoDups))


# In[39]:


phtml(errthtml(expected, firstsNoDups))


# In[40]:


print(errthtml(expected, firstsNoDups))


# In[ ]:




