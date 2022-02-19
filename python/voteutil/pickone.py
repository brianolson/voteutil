#!/usr/bin/env python3

import logging

logger = logging.getLogger(__name__)

class PickOne:
    """Pick One
    For comparison to VRR/IRV/etc.
    """

    def __init__(self, names=None, seats=None):
        self.names = names
        self.counts = {}
    def name(self):
        return "Pick One"
    def cname(self, a):
        if self.names and a < len(self.names):
            return self.names[a]
        return str(a)
    def vote(self, indexRatingsDict):
        '''vote() receives an 'index ratings dict', map from integer to number.
        String names will have been mapped to a dense range of ingeters starting at 0.
        votes shall be treated as immutable and may not be changed by an election algorithm.
        '''
        maxr = None
        maxi = None
        for i, r in indexRatingsDict.items():
            if (maxr is None) or (r > maxr):
                maxr = r
                maxi = i
        self.counts[maxi] = self.counts.get(maxi,0) + 1
    def getResults(self, html=None):
        '''Return ordered array of tuples [(name, votes), ...]
        If `html` is specified, .write() explanation HTML to it.
        '''
        vi = sorted([(v,i) for i,v in self.counts.items()], reverse=True)
        out = [(self.cname(x[1]), x[0]) for x in vi]
        if html:
            html.write('<table class="results"><tr><th>Name</th><th>Votes</th></tr>\n')
            for name, votes in out:
                html.write('<tr><td class="cn">{}</td><td class="vc">{}</td><tr>\n'.format(name, votes))
            html.write('</table>\n')
        return out
