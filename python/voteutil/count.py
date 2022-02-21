import logging
import re
import urllib.parse

logger = logging.getLogger(__name__)


write_in_pat = re.compile(r'^write.?in.?$', re.IGNORECASE)

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
    if names is None:
        names = []
    if nameIndexes is None:
        nameIndexes = {}
    votes = 0
    comments = 0
    nocomment = args and args.nocomment
    enable_repeat = args and args.enable_repeat
    for line in fin:
        if not line:
            continue
        line = line.strip()
        if not line:
            if voteEmptyLines:
                for algorithm in algorithms:
                    algorithm.vote({})
            continue
        if line[0] == '#' and not nocomment:
            comments += 1
            continue
        if line[0] == '*' and enable_repeat:
            m = REPEAT_RE.match(line)
            rcount = int(m.group(1))
            line = m.group(2)
        else:
            rcount = 1
        kvl = urllib.parse.parse_qs(line)
        indexRatingDict = {}
        for name, ratings in kvl.items():
            if len(ratings) == 0:
                continue
            if eraseWriteIn and write_in_pat.match(name):
                continue
            ci = nameIndexes.get(name, None)
            if ci is None:
                ci = len(nameIndexes)
                nameIndexes[name] = ci
                names.append(name)
            if len(ratings) == 1:
                indexRatingDict[ci] = float(ratings[0])
            else:
                # warning, multiple votes for a choice, voting average of them
                indexRatingDict[ci] = sum(map(float, ratings)) / len(ratings)
        if not indexRatingDict:
            # could have been filtered to nothing
            if voteEmptyLines:
                for algorithm in algorithms:
                    algorithm.vote({})
            continue
        if rankings:
            maxrank = max(indexRatingDict.values()) + 1
            indexRatingDict = {k:(maxrank - v) for k,v in indexRatingDict.items()}
            assert min(indexRatingDict.values()) > 0, kvl
        while rcount > 0:
            for algorithm in algorithms:
                algorithm.vote(indexRatingDict)
            votes += 1
            rcount -= 1
    logger.debug('name indexes %r', nameIndexes)
    logger.debug('names %r', names)
    return votes, comments
