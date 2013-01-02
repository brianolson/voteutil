#!/usr/bin/python

import copy
import logging


def getNameIndex(nameIndex, name):
    """Get or allocated numeric index for a string."""
    if (isinstance(name, int) or isinstance(name, long)) and (name > 0) and (name < len(nameIndex)):
        return name
    x = nameIndex.get(name)
    if x is not None:
        return x
    x = len(nameIndex)
    nameIndex[name] = x
    return x


# TODO: make nameIndex a smarter object with hash map string->int and array int->string?
# I think generally name->index happens a zillion times during voting, and
# index->name happens N times once at the end.
def getIndexName(nameIndex, index):
    """Get string name from numeric index. (slow, exhausting search)"""
    for k,v in nameIndex.iteritems():
        if v == index:
            return k
    return None


def _avb(tally, ai, bi):
    """Return count of (a over b)"""
    if ai == bi:
        raise ValueError("nonsense _avb %s == %s" % (ai, bi))
    if ai > bi:
        return tally[ai][bi]
    return tally[bi][bi+ai]


def _incavb(tally, ai, bi, value=1):
    if ai == bi:
        raise ValueError("nonsense _incavb %s == %s" % (ai, bi))
    if ai > bi:
        assert ai < len(tally)
        assert bi < len(tally[ai])
        tally[ai][bi] += value
    else:
        assert bi < len(tally)
        assert ai < len(tally[bi])
        tally[bi][bi+ai] += value


def _setavb(tally, ai, bi, value):
    if ai == bi:
        raise ValueError("nonsense _incavb %s == %s" % (ai, bi))
    if ai > bi:
        assert ai < len(tally)
        assert bi < len(tally[ai])
        tally[ai][bi] = value
    else:
        assert bi < len(tally)
        assert ai < len(tally[bi])
        tally[bi][bi+ai] = value


def _countDefeats(tally, includeDummy=False):
    """Return list corresponding to tally index of each choice's score, negative for each defeat. (-N..0]"""
    if includeDummy:
        olen = len(tally)
    else:
        olen = len(tally) - 1
    out = [0] * olen
    for ai in xrange(0, olen):
        for bi in xrange(ai + 1, olen):
            avb = _avb(tally, ai, bi)
            bva = _avb(tally, bi, ai)
            if avb > bva:
                out[bi] -= 1
            elif bva > avb:
                out[ai] -= 1
    return out


def _getSchwartzSet(tally):
    defeats = _countDefeats(tally)
    sset = [0]
    mindefeats = defeats[0]
    for xi in xrange(1,len(defeats)):
        if defeats[xi] > mindefeats:
            sset = [xi]
            mindefeats = defeats[xi]
        elif defeats[xi] == mindefeats:
            sset.append(xi)
    if mindefeats != 0:
        # the best was defeated by something, ensure it's in the set
        for ai in sset:
            for bi in xrange(0, len(tally)):
                if bi == ai:
                    continue
                if _avb(tally, bi, ai) > _avb(tally, ai, bi):
                    if bi not in sset:
                        sset.append(bi)
    ok, msg = _verifySchwartzSet(tally, sset)
    assert ok, msg
    return sset


def _verifySchwartzSet(tally, sset):
    """Return True if every member of sset beats everything not in sset. No member of sset is beaten by every other member of sset.
    Return (True, None) or (False, 'error message')"""
    # every member of sset beats everything not in sset
    for xi in sset:
        insetWinOrTie = 0
        insetLoss = 0
        for ai in xrange(0, len(tally)):
            if ai == xi:
                continue
            if _avb(tally, ai, xi) > _avb(tally, xi, ai):
                if ai in sset:
                    insetLoss += 1
                else:
                    return False, 'member of sset lost to something not in sset'
            elif (ai in sset) and (_avb(tally, xi, ai) > _avb(tally, ai, xi)):
                insetWinOrTie += 1
        if (insetWinOrTie == 0) and (insetLoss > 0):
            return False, 'member %s lost to all other members' % (xi,)
    return True, None


def _makeWinners(tally, nameIndex, defeats=None, includeDummy=False, topn=None):
    if defeats is None:
        defeats = _countDefeats(tally, includeDummy)
    tlen = len(tally) - 1
    out = []
    for name,index in nameIndex.iteritems():
        out.append( (name, tlen + defeats[index]) )
    out.sort(key=lambda x:x[1], reverse=True)
    if (topn is not None) and (topn < len(out)):
        return out[:topn]
    return out


# Hopefully no choice to be voted on will ever be this:
_dummy = 'DUMMY CHOICE NAME !1234@'


CSSD_MARGINS = 1
CSSD_BIGWINNER = 2

_CSSD_MODE = {
    'margins': CSSD_MARGINS,
    'margin': CSSD_MARGINS,
    'winningvotes': CSSD_BIGWINNER,
    'wv': CSSD_BIGWINNER,
    'bigwinner': CSSD_BIGWINNER,
}
    

class Condorcet(object):
    def __init__(self, **kwargs):
        # map {'choice name':int}
        # index into tally[x]
        self.nameIndex = {}

        # for x>y
        # tally[x][y] is count of x over y
        # tally[x][x+y] is count of y over x
        # tally[len(tally)-1] is the dummy choice
        # tally[x] has len (2*x)
        self.tally = [[]]

        # Data that could be rendered into a text or html exlpanation
        self.explainlog = []

        self.cssdmode = CSSD_BIGWINNER
        for k,v in kwargs:
            if k in _CSSD_MODE:
                self.cssdmode = _CSSD_MODE[k]
            if k == 'cssdmode' or k == 'mode':
                self.cssdmode = _CSSD_MODE[v]
    
    def _avb(self, ai, bi):
        """Return count of (a over b)"""
        return _avb(self.tally, ai, bi)

    def _incavb(self, ai, bi):
        _incavb(self.tally, ai, bi, 1)

    def countDefeats(self, includeDummy=False):
        return _countDefeats(self.tally, includeDummy)

    def _ensureTally(self, i):
        while len(self.tally) <= i+1:
            oldlen = len(self.tally) - 1
            newlen = oldlen + 1
            dummy = self.tally[oldlen]
            newrow = [0,0] * newlen
            for j in xrange(0, oldlen):
                newrow[j] = dummy[j]
                newrow[newlen + j] = dummy[oldlen + j]
            self.tally.append(newrow)

    def vote(self, vote):
        """'vote' is dict {name:rating} or {nameIndex:rating}"""
        indexratings = []
        maxi = None
        votedi = set()
        for k,v in vote.iteritems():
            i = getNameIndex(self.nameIndex, k)
            ir = (i, v)
            indexratings.append(ir)
            maxi = max(i, maxi)
            votedi.add(i)
        self._ensureTally(maxi)
        # Apply votes for choices voted on
        for ai in xrange(0, len(indexratings)):
            a = indexratings[ai]
            for bi in xrange(ai + 1, len(indexratings)):
                b = indexratings[bi]
                assert a[0] != b[0]
                if a[1] > b[1]:
                    self._incavb(a[0], b[0])
                elif b[1] > a[1]:
                    self._incavb(b[0], a[0])
        # All choices not voted on assumed to rate -0, lose to names with >=0 rating and beat <0 ratings.
        for xi in xrange(0, len(self.tally)):
            if xi in votedi:
                continue
            for bi in xrange(0, len(indexratings)):
                b = indexratings[bi]
                assert b[0] != xi
                if b[1] >= 0:
                    self._incavb(b[0], xi)
                else:
                    self._incavb(xi, b[0])

    def getResult(self, topn=None):
        """Return [('name',score),...]. May limit to topn (default total ranking of all choices)."""
        defeats = self.countDefeats()
        numzeroes = len(filter(lambda x: x == 0, defeats))
        if numzeroes == 1:
            return _makeWinners(self.tally, self.nameIndex, defeats, topn=topn)
        return self.getCSSDResult(topn)

    def getCSSDResult(self, topn=None):
        """CSSD Cycle resolution.
        I believe this correctly implements Cloneproof Schwartz Set Dropping, aka the Schulze method.
        http://wiki.electorama.com/wiki/Schulze_method"""
        sset = _getSchwartzSet(self.tally)
        ttally = copy.deepcopy(self.tally)
        sround = 0
        while True:
            # find weakest defeat between members of schwartz set
            mind = None
            tie = 0
            mins = []
            for ax in xrange(0, len(sset)):
                ai  = sset[ax]
                for bx in xrange(ax + 1, len(sset)):
                    bi = sset[bx]
                    avb = _avb(ttally, ai, bi)
                    bva = _avb(ttally, bi, ai)
                    if (avb == -1) and (bva == -1):
                        continue
                    if avb > bva:
                        ihi = ai
                        ilo = bi
                    else:
                        ihi = bi
                        ilo = ai
                    if self.cssdmode == CSSD_MARGINS:
                        win = abs(avb - bva)
                    elif self.cssdmode == CSSD_BIGWINNER:
                        win = max(avb, bva)
                    else:
                        assert False, 'bogus cssmode'
                    if (mind is None) or (win < mind):
                        mind = win
                        mins = [(ihi, ilo)]
                    elif mind == win:
                        mins.append( (ihi, ilo) )
            if not mins:
                logging.error('no defeat to drop')
                return _makeWinners(ttally, self.nameIndex, topn=topn)
            if len(mins) == len(sset):
                # all are tied!
                return _makeWinners(ttally, self.nameIndex, topn=topn)
            # adjust tally, discounting weak defeat(s)
            for ai, bi in mins:
                ## make it not a defeat by making it a tie
                #_setavb(ttally, ai, bi, _avb(ttally, bi, ai))
                _setavb(ttally, ai, bi, -1)
                _setavb(ttally, bi, ai, -1)
            sset = _getSchwartzSet(ttally)
            if len(sset) == 1:
                return _makeWinners(ttally, self.nameIndex, topn=topn)
            sround += 1
            if sround >= len(self.tally):
                logging.error("too many cssd rounds %s", sround)
                return _makeWinners(ttally, self.nameIndex, topn=topn)
        return _makeWinners(ttally, self.nameIndex, topn=topn)
        

if __name__ == '__main__':
    import unittest
    class BasicTest(unittest.TestCase):
        def test_messytie(self):
            """Test with three choices, exercises vote counting via dummy entry."""
            c = Condorcet()
            c.vote({'a':1})
            c.vote({'b':1})
            c.vote({'a':1, 'b': 2, 'c': 3})
            self.failUnlessEqual(c._avb(0,1), 1)
            self.failUnlessEqual(c._avb(1,0), 2)
            self.failUnlessEqual(c._avb(0,2), 1)
            self.failUnlessEqual(c._avb(1,2), 1)
            self.failUnlessEqual(c._avb(2,1), 1)
            self.failUnlessEqual(c._avb(2,0), 1)
            self.failUnlessEqual(c.countDefeats(includeDummy=True), [-1,0,0,-3])
            self.failUnlessEqual(c.getResult(), [('c',3), ('b',3), ('a',2)])
            # TODO: capture and expect logging.error

        def test_simplewinner(self):
            c = Condorcet()
            c.vote({'a':3, 'b':2})
            c.vote({'a':3, 'b':2})
            c.vote({'a':3, 'b':2})
            c.vote({'a':1, 'b':2})
            c.vote({'a':1, 'b':2, 'c':3})
            self.failUnlessEqual(c.getResult(), [('a',3), ('b',2), ('c',1)])
            self.failUnlessEqual(c.getResult(topn=1), [('a',3)])
            
    unittest.main()

