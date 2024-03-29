import logging
import re
import urllib.parse

logger = logging.getLogger(__name__)


write_in_pat = re.compile(r'^write.?in.?$', re.IGNORECASE)

class VoteProcessor:
    def __init__(self, algorithms, args=None, names=None, nameIndexes=None, rankings=False, voteEmptyLines=True, eraseWriteIn=True):
        '''algorithms: list/tuple of VRR, IRV, etc
        fin: file like object of url encoded lines name=value&...
        args: args.nocomment, args.enable_repeat
        names: list of names
        nameIndexes: {name:int, ...}
        rankings: interpret values as rankings (1=best), default as ratings (higher is better)

        return (number of votes counted, number of comment lines)
        After this, call [a.getResults() for a in algorithms]
        '''
        self.votes = 0
        self.comments = 0
        self.algorithms = algorithms
        self.nocomment = args and args.nocomment
        self.enable_repeat = args and args.enable_repeat
        self.names = names
        if self.names is None:
            self.names = []
        self.nameIndexes = nameIndexes
        if self.nameIndexes is None:
            self.nameIndexes = {}
        self.rankings = rankings
        self.voteEmptyLines = voteEmptyLines
        self.eraseWriteIn = eraseWriteIn
        # stats:
        self.blank = 0 # empty vote
        self.pickOne = 0 # only one candidate marked
        self.pickOneMany = 0 # only one candidate marked, and more than once
        self.reVote = 0 # same candidate voted more than once, but not alone

    def statsDict(self):
        out = {}
        for an in ('blank', 'pickOne', 'pickOneMany', 'reVote'):
            out[an] = getattr(self, an)
        return out

    def processFile(self, fin):
        "fin is a file-like object or other iterable over lines"
        votes = 0
        comments = 0
        lineno = 0
        for line in fin:
            lineno += 1
            try:
                voteCount, commentCount = self.processLine(line, lineno)
                votes += voteCount
                comments += commentCount
            except Exception as e:
                raise Exception(':{} {}'.format(lineno, e), e)
        logger.debug('name indexes %r', self.nameIndexes)
        logger.debug('names %r', self.names)
        return votes, comments
    def processLine(self, line, lineno):
        votes = 0
        comments = 0
        if not line:
            self.blank += 1
            return votes, comments
        line = line.strip()
        if not line:
            self.blank += 1
            if self.voteEmptyLines:
                for algorithm in self.algorithms:
                    algorithm.vote({})
            return votes, comments
        if line[0] == '#' and not self.nocomment:
            comments += 1
            return votes, comments
        if line[0] == '*' and self.enable_repeat:
            m = REPEAT_RE.match(line)
            rcount = int(m.group(1))
            line = m.group(2)
        else:
            rcount = 1
        kvl = urllib.parse.parse_qs(line)
        maxrank = None
        if self.rankings:
            for name, rankings in kvl.items():
                if len(rankings) == 0:
                    continue
                mr = max(map(int,rankings))
                if maxrank is None:
                    maxrank = mr
                else:
                    maxrank = max(mr, maxrank)
            if maxrank is not None:
                maxrank += 1
        indexRatingDict = {}
        reVote = False
        for name, ratings in kvl.items():
            if len(ratings) == 0:
                continue
            if self.eraseWriteIn and write_in_pat.match(name):
                continue
            ci = self.nameIndexes.get(name, None)
            if ci is None:
                ci = len(self.nameIndexes)
                self.nameIndexes[name] = ci
                self.names.append(name)
            if len(ratings) == 1:
                if self.rankings:
                    indexRatingDict[ci] = maxrank - float(ratings[0])
                else:
                    indexRatingDict[ci] = float(ratings[0])
            elif self.rankings:
                # this is so common we need to usually ignore it, but we should gather stats on it to measure how people actually use RCV ballots
                logger.debug(':%d multiple votes for choice %r, voting highest rank', lineno, name)
                reVote = True
                indexRatingDict[ci] = maxrank - float(min(ratings[0]))
            else:
                logger.warning(':%d multiple votes for choice %r, voting average of them', lineno, name)
                indexRatingDict[ci] = sum(map(float, ratings)) / len(ratings)
        if not indexRatingDict:
            # could have been filtered to nothing
            if self.voteEmptyLines:
                for algorithm in self.algorithms:
                    algorithm.vote({})
            return votes, comments
        if reVote:
            if len(indexRatingDict) == 1:
                self.pickOneMany += 1
            else:
                self.reVote += 1
        else:
            if len(indexRatingDict) == 1:
                self.pickOne += 1
        if self.rankings:
            assert min(indexRatingDict.values()) > 0, kvl
        while rcount > 0:
            for algorithm in self.algorithms:
                algorithm.vote(indexRatingDict)
            votes += 1
            rcount -= 1
        return votes, comments


def processFile(algorithms, fin, args=None, names=None, nameIndexes=None, rankings=False, voteEmptyLines=True, eraseWriteIn=True):
    '''algorithms: list/tuple of VRR, IRV, etc
    fin: file like object of url encoded lines name=value&...
    args: args.nocomment, args.enable_repeat
    names: list of names
    nameIndexes: {name:int, ...}
    rankings: interpret values as rankings (1=best), default as ratings (higher is better)

    return (number of votes counted, number of comment lines)
    After this, call [a.getResults() for a in algorithms]
    '''
    vp = VoteProcessor(algorithms, args=args, names=names, nameIndexes=nameIndexes, rankings=rankings, voteEmptyLines=voteEmptyLines, eraseWriteIn=eraseWriteIn)
    return vp.processFile(fin)
