#!/usr/bin/python
#
# Utility to read ranked choice vote outputs as used around San Francisco and other places and convert them to name=value& records.

import sys
import urllib


def hashinc(d, k):
	if k in d:
		d[k] += 1
	else:
		d[k] = 1

def charTruth(x):
	"""t or T or 1, all else False"""
	return (x == 't') or (x == 'T') or (x == '1')

class MasterRecord(object):
	def __init__(self, field, id, name, list_order, candidate_contest_id, writein, provisional):
		self.field = field
		self.id = id
		self.name = name
		self.list_order = list_order
		self.candidate_contest_id = candidate_contest_id
		self.writein = writein
		self.provisional = provisional

def readMaster(f):
	for line in f:
		field = line[:10].strip()
		id = line[10:17]
		name = line[17:67].strip()
		list_order = line[67:74]
		candidate_contest_id = line[74:81]
		writein = charTruth(line[81])
		provisional = charTruth(line[82])
		yield MasterRecord(field, id, name, list_order, candidate_contest_id, writein, provisional)

def makeCandidateNameIdMap(they):
	out = {}
	for x in they:
		if x.field != 'Candidate':
			continue
		assert x.id not in out
		out[x.id] = x.name
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
		contest = line[0:7]
		voter_id = line[7:16]
		serial = line[16:23]
		tally_type = line[23:26]
		precinct = line[26:33]
		rank = int(line[33:36])
		candidate = line[36:43]
		overvote = charTruth(line[43])
		undervote = charTruth(line[44])
		yield RcvBallot(contest, voter_id, serial, tally_type, precinct, rank, candidate, overvote, undervote)

def masterAndBallotsToNameEq(master, ballots):
	mf = open(master, 'r')
	masterlines = list(readMaster(mf))
	idName = makeCandidateNameIdMap(masterlines)
	voteParts = {}
	partCount = 0
	for ballotPart in readBallot(open(ballots, 'r')):
		if not ballotPart.voter_id in voteParts:
			voteParts[ballotPart.voter_id] = {}
		cname = ballotPart.candidateName(idName)
		voteParts[ballotPart.voter_id][cname] = ballotPart.rank
		partCount += 1
	print 'got %d voter_id from %d parts' % (len(voteParts), partCount)
	for vdict in voteParts.itervalues():
		yield urllib.urlencode(vdict)

def main():
	import optparse
	argp = optparse.OptionParser()
	argp.add_option('--master', '-m', dest='master', default=None)
	argp.add_option('--ballots', '-b', dest='ballots', default=None)
	argp.add_option('--out', '-o', dest='outname', default=None)
	(options, args) = argp.parse_args()
	if options.outname:
		out = file(options.outname, 'w')
	else:
		out = sys.stdout
	for neq in masterAndBallotsToNameEq(options.master, options.ballots):
		out.write(neq)
		out.write('\n')
	out.close()

if __name__ == '__main__':
	main()
