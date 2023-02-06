#!/usr/bin/env python3

import logging

logger = logging.getLogger(__name__)

class VRR:
    """Virtual Round Robin
    aka Condorcet's Method

    Single pass method with flexible storage allowing choices to be
    defined as they come in. Stores only O(C^2) counts for C choices.
    """
    short_name = "vrr"

    def __init__(self, names=None, seats=None):
        # avb[n] contains the pairwise counts of n vs all choice indexes
        # less than n.
        # avb[n][0:n] is counts of n favored over lesser index;
        # avb[n][n:] is counts of a lesser index favored over n
        self.avb = {}
        # vs_unknown[a] = [a preferred over unknown, unknown preferred over a]
        self.vs_unknown = {}
        # list of names
        self.names = names
        self.votecount = 0
    def name(self):
        return "Virtual Round Robin"
    def cname(self, a):
        if self.names and a < len(self.names):
            return self.names[a]
        return str(a)
    def build_avb(self, a):
        # start out by copying from the unknown
        avb = [0] * (2 * (a))
        for b in range(a-1):
            vu = self.vs_unknown.get(b, (0,0))
            avb[b] = vu[1]
            avb[b + a] = vu[0]
        self.avb[a] = avb
        return avb
    def inc(self, a, b):
        "a is preferred over b one time"
        if a == b:
            assert false, "cannot increment a choice vs itself"
        if a > b:
            avb = self.avb.get(a) or self.build_avb(a)
            avb[b] += 1
        else:
            avb = self.avb.get(b) or self.build_avb(b)
            avb[b + a] += 1
    def get(self, a, b):
        if a > b:
            avb = self.avb.get(a)
            if not avb:
                return 0
            return avb[b]
        else:
            avb = self.avb.get(b)
            if not avb:
                return 0
            return avb[b + a]
    def inc_vs_unknown(self, a):
        vu = self.vs_unknown.get(a)
        if not vu:
            vu = [1, 0]
            self.vs_unknown[a] = vu
        else:
            vu[0] += 1
    def inc_unknown_vs(self, a):
        # for negative ratings, a is less desirable than an unknown
        vu = self.vs_unknown.get(a)
        if not vu:
            vu = [0, 1]
            self.vs_unknown[a] = vu
        else:
            vu[1] += 1
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        self.votecount += 1
        xv = list(indexRatingsDict.items())
        for a, va in enumerate(xv):
            for b in range(a + 1, len(xv)):
                vb = xv[b]
                if va[1] > vb[1]:
                    self.inc(va[0], vb[0])
                elif va[1] < vb[1]:
                    self.inc(vb[0], va[0])
                # else:
                #     logger.debug('tie rate')
            # vote vs unknown and unvoted based on rating sign.
            # unknown has a negative zero rating.
            if va[1] >= 0:
                self.inc_vs_unknown(va[0])
                # Everything is implicitly preferred over everything not voted on this ballot.
                ak = self.avb.keys()
                if ak:
                    for b in range(max(ak) + 1):
                        if b not in indexRatingsDict:
                            self.inc(va[0], b)
            elif va[1] < 0:
                self.inc_unknown_vs(va[0])

    def countDefeats(self, a, blockedDefeats=None):
        ak = self.avb.keys()
        if not ak:
            return 0
        count = 0
        for b in range(max(ak) + 1):
            if b == a:
                continue
            if self.get(a, b) < self.get(b, a):
                if (blockedDefeats is None) or ((b,a) not in blockedDefeats):
                    count += 1
        return count
    def disableWeakestDefeats(self, defeats, blockedDefeats, explainNotes):
        # maybe tweak contents of defeats and blockedDefeats
        # return True if done, False for another loop
        activeset = {defeats[0][1]}
        activeGrew = True
        while activeGrew:
            activeGrew = False
            for j in list(activeset):
                for i in range(0,len(self.names)):
                    if i in activeset:
                        continue
                    if (i,j) in blockedDefeats:
                        continue
                    ivj = self.get(i,j)
                    jvi = self.get(j,i)
                    if ivj > jvi:
                        activeset.add(i)
                        activeGrew = True
        if len(activeset) == 1:
            # winner
            logger.debug('activeset 1 %r', [self.cname(x) for x in activeset])
            return True
        minstrength = self.votecount
        mins = []
        activelist = sorted(activeset)
        for i, a in enumerate(activelist):
            for b in activelist[i+1:]:
                avb = self.get(a,b)
                bva = self.get(b,a)
                if avb > bva:
                    hi = a
                    lo = b
                    vhi = avb
                    vlo = bva
                else:
                    hi = b
                    lo = a
                    vhi = bva
                    vlo = avb
                if (hi,lo) in blockedDefeats:
                    continue
                # "winning votes" mode
                strength = vhi
                # TODO: "margins" mode
                # strength = vhi - vlo
                if strength < minstrength:
                    minstrength = strength
                    mins = [(hi,lo)]
                elif strength == minstrength:
                    mins.append((hi,lo))
        if len(mins) == len(activeset):
            # N-way tie, give up.
            names = [self.cname(x) for x in activeset]
            explainNotes.append('<div class="ep">{} are all tied!</div>'.format(', '.join(names)))
            return True
        for hi,lo in mins:
            explainNotes.append('<div class="ep">{} {} > {} {}; ignored as weakest defeat</div>'.format(self.cname(hi), self.get(hi,lo), self.cname(lo), self.get(lo,hi)))
            blockedDefeats.add((hi,lo))
        return False

    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        if not self.avb:
            if html:
                html.write('<p>no votes</p>')
            return []
        logger.debug('vrr results for %d votes', self.votecount)
        logger.debug('vrr names %r', self.names)
        if logger.isEnabledFor(logging.DEBUG):
            logger.debug('vrr vs unknown %r', [(self.cname(a), bb) for a,bb in self.vs_unknown.items()])
            for a in range(max(self.avb.keys()) + 1):
                aname = self.cname(a)
                logger.debug('vrr avb %s %r: %r', a, aname, self.avb.get(a))
        blockedDefeats = set()
        explainNotes = []
        while True:
            defeats = [(self.countDefeats(a, blockedDefeats), a) for a in range(max(self.avb.keys()) + 1)]
            defeats.sort()
            logger.debug('defeats %r', defeats)
            if defeats[0][0] == 0:
                if len(defeats) > 1:
                    if defeats[1][0] != 0:
                        break # we have a winner
                else:
                    break # we have a winner
            # find weakest defeat and disable it
            if self.disableWeakestDefeats(defeats, blockedDefeats, explainNotes):
                break
            logger.debug('blocked defeats %r', blockedDefeats)
        if html:
            html.write('<table border="1">\n<tr><td></td>')
            for i in range(len(defeats)):
                html.write('<th>{}</th>'.format(i+1))
            html.write('</tr>\n')
            for i, da in enumerate(defeats):
                a = da[1]
                html.write('<tr><th>({}) {}</th>'.format(i+1, self.cname(a)))
                for j, db in enumerate(defeats):
                    if j == i:
                        html.write('<td></td>')
                        continue
                    b = db[1]
                    avb = self.get(a,b)
                    bva = self.get(b,a)
                    style = ''
                    if avb > bva:
                        style = ' bgcolor="#bfb"'
                    elif bva > avb:
                        style = ' bgcolor="#fbb"'
                    html.write('<td{}>{}</td>'.format(style, avb))
                html.write('</tr>\n')
            html.write('</table>\n')
            for en in explainNotes:
                html.write(en)
        return [(self.cname(a), -d) for d,a in defeats]
