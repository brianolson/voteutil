#!/usr/bin/python
#
# Utility to read ranked choice vote outputs as used around San Francisco and other places and convert them to name=value& records.
#
# usage:
# rcvToNameEq.py -m master.txt -b ballot.txt -o votes.nameq
#
# or if master and ballot files contain multiple contests, '%s' in the
# output name will be replaced with the name of the contest. e.g.
# -o votes_%s.nameq

import fileinput
import glob
import logging
import sys
import urllib
# Python2.7
urlencode = getattr(urllib, 'urlencode', None)
# Python3
if not urlencode:
    import urllib.parse
    urlencode = urllib.parse.urlencode

logger = logging.getLogger(__name__)

def hashinc(d, k):
    if k in d:
        d[k] += 1
    else:
        d[k] = 1

def charTruth(x):
    """t or T or 1, all else False"""
    return (x == 't') or (x == 'T') or (x == '1')

class MasterRecord(object):
    def __init__(self, field, row_id, name, list_order, candidate_contest_id, writein, provisional):
        self.field = field
        self.row_id = row_id
        self.name = name
        self.list_order = list_order
        self.candidate_contest_id = candidate_contest_id
        self.writein = writein
        self.provisional = provisional

def readMaster(f):
    for line in f:
        field = line[:10].strip()
        row_id = int(line[10:17], 10)
        name = line[17:67].strip()
        list_order = line[67:74]
        candidate_contest_id = line[74:81]
        writein = charTruth(line[81])
        provisional = charTruth(line[82])
        yield MasterRecord(field, row_id, name, list_order, candidate_contest_id, writein, provisional)

class Master:
    def __init__(self):
        self.candidates = {}
        self.contests = {}
        self.precincts = {}
    def load(self, path):
        unktypes = set()
        count = 0
        with open(path, 'rt') as fin:
            for row in readMaster(fin):
                count += 1
                if row.field == 'Candidate':
                    self.candidates[row.row_id] = row
                elif row.field == 'Contest':
                    self.contests[row.row_id] = row
                elif row.field == 'Precinct':
                    self.precincts[row.row_id] = row
                elif row.field == 'Tally Type':
                    pass # TODO: ignore for now, use?
                else:
                    unktypes.add(row.field)
        logger.debug('loaded %s rows of master', count)
        for ut in unktypes:
            logger.warning('unprocessed master record type %r', ut)

def makeCandidateNameIdMap(they):
    out = {}
    for x in they:
        if x.field != 'Candidate':
            continue
        assert x.row_id not in out
        out[x.row_id] = x.name
    return out

class RcvBallot(object):
    def __init__(self, contest, voter_id, serial, tally_type, precinct, rank, candidate, overvote, undervote):
        self.contest = contest
        self.voter_id = voter_id
        self.serial = serial
        self.tally_type = tally_type
        self.precinct = precinct
        self.rank = rank
        self.candidate = candidate
        self.overvote = overvote
        self.undervote = undervote

    def candidateName(self, nameMap):
        if self.candidate in nameMap:
            return nameMap[self.candidate]
        return self.candidate

def readBallot(f):
    for line in f:
        contest = int(line[0:7], 10)
        voter_id = line[7:16]
        serial = line[16:23]
        tally_type = line[23:26]
        precinct = line[26:33]
        rank = int(line[33:36], 10)
        candidate = int(line[36:43], 10)
        overvote = charTruth(line[43])
        undervote = charTruth(line[44])
        yield RcvBallot(contest, voter_id, serial, tally_type, precinct, rank, candidate, overvote, undervote)

def masterAndBallotsToNameEq(master, ballotstream, outpattern, ignoreUnknown=False):
    ma = Master()
    ma.load(master)
    if (len(ma.contests) > 1) and ('%s' not in outpattern):
        sys.stderr.write('files contain multiple contests ({!r}) so output file pattern needs "%s" in it to substitute contest name into (output pattern was {!r}\n'.format([x.name for x in ma.contests.values()], outpattern))
        sys.exit(1)
    outpaths = {contest:outpattern.replace('%s', mr.name) for contest,mr in ma.contests.items()}
    dupcheck = set()
    for op in outpaths.values():
        if op in dupcheck:
            logger.error("duplicate output paths at %r, does output pattern have %%s for contest name substitution? (-o is %r)", op, outpattern)
            sys.exit(1)
        dupcheck.add(op)
    outfs = {contest:open(outpath, 'wt') for contest, outpath in outpaths.items()}
    voteParts = {}
    partCount = 0
    for ballotPart in readBallot(ballotstream):
        vpkey = (ballotPart.contest, ballotPart.voter_id)
        parts = voteParts.get(vpkey)
        if parts is None:
            parts = {}
            voteParts[vpkey] = parts
        crec = ma.candidates.get(ballotPart.candidate)
        if not crec and ignoreUnknown:
            continue
        cname = (crec and crec.name) or 'None'
        parts[cname] = ballotPart.rank
        partCount += 1
    logger.debug('got %s voter_id from %s parts', len(voteParts), partCount)
    for vpkey, vdict in voteParts.items():
        contest = vpkey[0]
        out = outfs.get(contest)
        if not out:
            out = open(outpaths[contest], 'wt')
            outfs[contest] = out
        out.write(urlencode(vdict))
        out.write('\n')
    return

def main():
    # TODO: port to argparse; optparse is depricated
    import argparse
    argp = argparse.ArgumentParser()
    argp.add_argument('--master', '-m', dest='master', default=None)
    argp.add_argument('--ballots', '-b', dest='ballots', default=None)
    argp.add_argument('--ballotglob')
    argp.add_argument('--out', '-o', dest='outname', default=None)
    argp.add_argument('-v', '--verbose', action='store_true', default=False)
    argp.add_argument('--ignore-unknown', action='store_true', default=False)
    options = argp.parse_args()

    if options.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    logger.debug('wat')
    if options.ballotglob:
        ballotstream = fileinput.input(files=glob.glob(options.ballotglob))
    else:
        ballotstream = open(options.ballots, 'rt')
    masterAndBallotsToNameEq(options.master, ballotstream, options.outname, options.ignore_unknown)
    logger.debug('foo')
    return

if __name__ == '__main__':
    main()
