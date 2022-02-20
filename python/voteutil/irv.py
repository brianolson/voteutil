#!/usr/bin/env python3
#
# IRV considered harmful, only included for comparison

import logging

logger = logging.getLogger(__name__)

class IRV:
    """Instant Runoff Voting
IRV is a fundamentally flawed algorithm, do not use to decide anything, included for comparison.
"""
    def __init__(self, names=None, strictOvervote=True):
        self.votes = []
        self.names = names
        self.choices = set()
        self.strictOvervote = strictOvervote
    def name(self):
        return "IRV"
    def cname(self, a):
        if self.names and a < len(self.names):
            return self.names[a]
        return str(a)
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        self.votes.append(dict(indexRatingsDict))
        self.choices.update(indexRatingsDict.keys())
    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        if html:
            rounds = []
        losers = []
        dq = set()
        while True:
            counts = {}
            exhausted = 0
            overvote = 0
            for vote in self.votes:
                top = None
                tr = None
                isExhausted = True
                isTopTie = False
                for index, rating in vote.items():
                    if index in dq:
                        continue
                    isExhausted = False
                    if tr is None or rating > tr:
                        tr = rating
                        top = index
                        isTopTie = False
                    elif rating == tr:
                        isTopTie = True
                if isTopTie:
                    if self.strictOvervote:
                        # overvote; vote goes away forever
                        vote.clear()
                        top = None
                        overvote += 1
                        continue
                    else:
                        # TODO: split vote
                        pass
                if isExhausted:
                    exhausted += 1
                    continue
                if top is None:
                    continue
                counts[top] = counts.get(top, 0) + 1
            winners = sorted([(count, index) for index,count in counts.items()], reverse=True)
            thresh = (len(self.votes)-exhausted-overvote) / 2
            logger.debug('irv counts %r', counts)
            logger.debug('irv intermediate winners %r thresh=%f losers=%r', winners, thresh, losers)
            loser = winners[-1]
            if html:
                rounds.append(irvRound(counts, list(winners), list(losers), exhausted, overvote))
            if winners[0][0] > thresh:
                # Done
                winners += list(reversed(losers))
                if html:
                    html.write(self.html(rounds))
                return [(self.cname(i), count) for count,i in winners]
            dq.add(loser[1])
            losers.append(loser)
    def html(self, rounds):
        #return table string
        lastround = rounds[-1]
        logger.debug('irv html lr.w=%r lr.l=%r', lastround.winners, lastround.losers)
        winners = list(lastround.winners) + list(reversed(lastround.losers))
        #logger.debug('irv html winners %r', winners)
        #names = [self.cname(i) for count,i in winners]
        out = '<table border="1"><tr><td></td>'
        for i in range(len(rounds)):
            out += '<th>Round {}</th>'.format(i+1)
        out += '</tr>'
        for count, i in winners:
            out += '<tr><th>{}</th>'.format(self.cname(i))
            for r in rounds:
                rc = r.counts.get(i, 0)
                if rc == 0:
                    out += '<td></td>'
                else:
                    out += '<td>{}</td>'.format(rc)
            out += '</tr>'
        out += '<tr><th>exhausted</th>'
        for r in rounds:
            out += '<td>{}</td>'.format(r.exhausted)
        out += '</tr>'
        out += '<tr><th>overvote</th>'
        for r in rounds:
            out += '<td>{}</td>'.format(r.overvote)
        out += '</tr>'
        out += '</table>'
        return out


class irvRound:
    def __init__(self, counts, winners, losers, exhausted, overvote):
        self.counts = counts
        self.winners = winners
        self.losers = losers
        self.exhausted = exhausted
        self.overvote = overvote
