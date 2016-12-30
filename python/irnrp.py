#!/usr/bin/env python3


class IRNRP:
    """Instant Runoff Normalized Ratings, Proportional"""

    def __init__(self, seats=None):
        if seats is None or seats <= 1:
            raise ValueError('IRNRP only makes sense for seats > 1')
        self.seats = seats
        self.votes = []
        self.names = None # self.names[i] = 'name of choice'

    def nameForChoiceIndex(self, ci):
        if self.names is not None and ci < len(self.names):
            return self.names[ci]
        return str(ci)
    
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        self.votes.append(indexRatingsDict)

    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        # archive for debugging
        oldcounts = []
        # get an initial count, find out how many choices there are
        count = []
        for ird in self.votes:
            ssum = 0.0
            for choiceIndex, rating in ird.items():
                while choiceIndex >= len(count):
                    count.append(0.0)
                ssum += abs(rating)
            for choiceIndex, rating in ird.items():
                count[choiceIndex] += rating / ssum
        vsum = len(self.votes)
        quota = vsum / (self.seats + 1)
        weight = [1.0] * len(count)
        numEnabled = len(count)

        winners = []
        winningCounts = []
        losers = []
        while numEnabled > self.seats:
            weightAdjusted = True
            weightAdjustmentCycleLimit = 10
            while weightAdjusted and weightAdjustmentCycleLimit >= 0:
                weightAdjusted = False
                weightAdjustmentCycleLimit -= 1

                # count
                oldcounts.append(count)
                count = [0.0] * len(count)
                vsum = _doCount(count, self.votes, weight)
                quota = vsum / (self.seats + 1)

                # find winners, maybe finish, de-weight according to surplus
                for ci, votes in enumerate(count):
                    if votes > quota:
                        if ci not in winners:
                            if html:
                                html.write('<div class="p">{} is winning with {} votes</div>\n'.format(self.nameForChoiceIndex(ci), int(votes)))
                            winners.append(ci)
                            winningCounts.append(votes)
                            if len(winners) >= self.seats:
                                return list(zip(winners, winningCounts)) + _everyoneElse(count, winners, weight) + losers
                        surplus = votes - quota
                        adjustment = surplus * ADJUSTMENT_RATE
                        adjustmentFraction = adjustment / votes
                        weight[ci] *= (1.0 - adjustmentFraction)
                        weightAdjusted = True
            if weightAdjusted:
                # we're not going to continue adjusting for now, but
                # we should reflect the last adjustment we made
                oldcounts.append(count)
                count = [0.0] * len(count)
                vsum = _doCount(count, self.votes, weight)
                quota = vsum / (self.seats + 1)

            # pick loser who doesn't make the cut
            mincount = None
            minci = None
            for ci, cv in enumerate(count):
                if weight[ci] < EPSILON:
                    continue  # already disabled
                if mincount is None or mincount > cv:
                    minci = ci
                    mincount = cv
            if html:
                html.write('<div class="p">{} is disabled with {} votes</div>'.format(self.nameForChoiceIndex(minci), mincount))
            losers = [(minci, mincount)] + losers
            weight[minci] = 0.0
            numEnabled -= 1
            # go back, do a count, reweight winners
                
        return list(zip(winners, winningCounts)) + _everyoneElse(count, winners, weight) + losers


ADJUSTMENT_RATE = 0.10
EPSILON = 1e-6


# helper function actually does the weighted, normalized count
def _doCount(count, votes, weight):
    vsum = 0.0
    for ird in votes:
        ssum = 0.0
        for choiceIndex, rating in ird.items():
            ssum += abs(rating) * weight[choiceIndex]
        if ssum < EPSILON:
            continue
        vsum += 1.0
        for choiceIndex, rating in ird.items():
            count[choiceIndex] += (rating * weight[choiceIndex]) / ssum
    return vsum


# compile the list of all choices neither winners nor losers, for
# building final return value at end.
def _everyoneElse(count, winners, weight):
    countCi = []
    for ci, cv in enumerate(count):
        if ci in winners:
            continue
        if weight[ci] < EPSILON:
            continue
        if cv < EPSILON:
            continue
        countCi.append( (cv, ci) )
    countCi.sort(reverse=True)
    return [(ci, cv) for cv,ci in countCi]
