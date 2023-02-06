#!/usr/bin/env python3

import logging

logger = logging.getLogger(__name__)

def dictinc(d, k, inc=1):
    d[k] = d.get(k, 0) + inc


class VRR2:
    """Virtual Round Robin
    aka Condorcet's Method

    Simplest implementation.
    Store all the votes.
    Process them once we know what choices we have.
    Stores O(C*V) data for C choices and V votes.
    """
    short_name = "vrr2"
    def __init__(self, names=None):
        self.votes = []
        self.names = names
        self.choices = set()
    def name(self):
        return "Virtual Round Robin (alt impl)"
    def cname(self, a):
        if self.names and a < len(self.names):
            return self.names[a]
        return str(a)
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        self.votes.append(indexRatingsDict)
        self.choices.update(indexRatingsDict.keys())
    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        counts = {}
        for vote in self.votes:
            ir = list(vote.items())
            for i in range(len(ir)):
                a = ir[i][0]
                av = ir[i][1]
                for j in range(i+1, len(ir)):
                    b = ir[j][0]
                    bv = ir[j][1]
                    if av > bv:
                        dictinc(counts, (a,b))
                    elif bv > av:
                        dictinc(counts, (b,a))
                for b in self.choices:
                    if b not in vote:
                        if av >= 0:
                            dictinc(counts, (a,b))
                        else:
                            dictinc(counts, (b,a))
        blockedDefeats = set()
        choices = list(self.choices)
        explainNotes = [] # add to html at end
        while True:
            defeats = {}
            for i, a in enumerate(choices):
                for j in range(i+1, len(choices)):
                    b = choices[j]
                    avb = counts.get( (a,b), 0)
                    bva = counts.get( (b,a), 0)
                    if avb > bva:
                        if (a,b) not in blockedDefeats:
                            dictinc(defeats, b)
                    elif bva > avb:
                        if (b,a) not in blockedDefeats:
                            dictinc(defeats, a)
            defeats = sorted([(defeats.get(k,0),k) for k in choices])
            if defeats[0][0] == 0:
                if len(defeats) > 1:
                    if defeats[1][0] != 0:
                        break # we have a winner
                else:
                    break # we have a winner
            # find weakest defeat and disable it
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
                        ivj = counts.get((i,j),0)
                        jvi = counts.get((j,i),0)
                        if ivj > jvi:
                            activeset.add(i)
                            activeGrew = True
            if len(activeset) == 1:
                # winner
                defeats[0][0] = 0
                break
            minstrength = len(self.votes)
            mins = []
            activelist = sorted(activeset)
            for i, a in enumerate(activelist):
                for b in activelist[i+1:]:
                    avb = counts.get((a,b), 0)
                    bva = counts.get((b,a), 0)
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
                break
            for hi,lo in mins:
                if html:
                    explainNotes.append('<div class="ep">{} {} > {} {}; ignored as weakest defeat</div>'.format(self.cname(hi), counts.get((hi,lo), 0), self.cname(lo), counts.get((lo,hi),0)))
                blockedDefeats.add((hi,lo))
            # end `while True` cycle resolution loop

        if html:
            html.write('<table border="1">\n<tr><td></td>')
            for i in range(len(defeats)):
                html.write('<th>{}</th>'.format(i+1))
            html.write('</tr>\n')
            for i, da in enumerate(defeats):
                defeatCount = da[0]
                a = da[1]
                html.write('<tr><th>({}) {}</th>'.format(i+1, self.cname(a)))
                for j, db in enumerate(defeats):
                    if j == i:
                        html.write('<td></td>')
                        continue
                    b = db[1]
                    avb = counts.get((a,b),0)
                    bva = counts.get((b,a),0)
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
