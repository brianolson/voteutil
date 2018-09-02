#!/usr/bin/env python3
#
# read a csv on input: state, pop
# write a csv to output: state, seats
#
# usage:
# to apportion US House seats across the states:
# python3 apportion.py -n 435 < state_pop.csv > state_seats.csv
#
# to apportion a smaller set of toy populations and explore apportionment algorithms:
# python3 apportion.py --pops 15,44,368,573 -n 9


import math
import sys


# US House algorithm, "the method of equal proportions"
# https://en.wikipedia.org/wiki/United_States_congressional_apportionment
#
# int seats
# int[] populations
#
# returns apportionment in list parallel to populations list
#
# apportion(12,[60,30,10])
def apportion(seats, populations, start_alloc=1):
    out = [start_alloc] * len(populations)
    seats_set = sum(out)
    while seats_set < seats:
        maxprio = None
        maxi = None
        for i, pop in enumerate(populations):
            cur_seats = out[i]
            if cur_seats == 0:
                priority = math.inf
            else:
                priority = pop / math.sqrt(cur_seats * (cur_seats + 1))
            if (maxprio is None) or (priority > maxprio):
                maxprio = priority
                maxi = i
        out[maxi] += 1
        seats_set += 1
    return out

# int seats
# int[] populations
#
# returns apportionment in list parallel to populations list
def nbresenham(seats, populations, start_alloc=0):
    # modified from bresenham's algorithm for linear interpolation
    # https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    #
    # Option 1: processes each pass through populations to assign to always in static initial population (descending) order.
    #
    # Right now I prefer Option 2 below. -- bolson 2018-09-02
    #
    # TODO: this probably does the wrong thing for start_alloc>0
    popIndexes = [(pop,i) for i,pop in enumerate(populations)]
    popIndexes.sort(reverse=True)
    D = [(2*px[0] - popIndexes[0][0]) for px in popIndexes[1:]]
    out = [start_alloc] * len(populations)
    while True:
        out[popIndexes[0][1]] += 1
        if sum(out) >= seats:
            return out
        for pop, i in popIndexes[1:]:
            xD = D[i-1]
            if xD > 0:
                out[i] += 1
                if sum(out) >= seats:
                    return out
                xD = xD - (2 * popIndexes[0][0])
            xD = xD + (2 * pop)
            D[i-1] = xD



class NB2PopState:
    def __init__(self, index, pop, start_alloc=0):
        self.index = index
        self.pop = pop
        self.D = None
        self.apportioned = start_alloc

# int seats
# int[] populations
#
# returns apportionment in list parallel to populations list
def nbresenham2(seats, populations, start_alloc=0):
    # modified from bresenham's algorithm for linear interpolation
    # https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
    #
    # Bresenham's algorithm is frequently applied as a computer
    # graphics method for drawing a line of pixels from 0,0 to x,y. In
    # this case 'y' is the population of the most populous set (state,
    # district, proportional constituency, etc), 'x' is the population
    # of some other set. If we had as many seats as there were people,
    # we would step through this algorithm and draw a line all the way
    # to the end. Instead we count how many steps we have taken (seats
    # apportioned) and stop when we hit the seats limit. Because
    # Bresenham's algorithm closely follows the line we have a fair
    # linear interpolation constrained by integer steps.
    #
    # Option 2: processes each pass through populations in dynamic priority order based on how high the D error score is
    #
    # TODO: this probably does the wrong thing for start_alloc>0
    state = [NB2PopState(i,pop,start_alloc) for i,pop in enumerate(populations)]
    mostPopulous = state[0]
    for xs in state[1:]:
        if xs.pop > mostPopulous.pop:
            mostPopulous = xs
    everyoneElse = []
    for xs in state:
        if xs.index == mostPopulous.index:
            continue
        xs.D = (2 * xs.pop) - mostPopulous.pop
        everyoneElse.append(xs)

    apportionedSum = sum([xs.apportioned for xs in state])
    while True:
        mostPopulous.apportioned += 1
        apportionedSum += 1
        if apportionedSum >= seats:
            break
        # sort by D to find which other population is furthest behind
        # the line and needs to be caught up by adding to its
        # apportionment.
        dsort = sorted([(xs.D, xs) for xs in everyoneElse], reverse=True)
        for D, xs in dsort:
            if D > 0:
                xs.apportioned += 1
                apportionedSum += 1
                if apportionedSum >= seats:
                    break
                D -= 2 * mostPopulous.pop
            xs.D = D + (2 * xs.pop)
        if apportionedSum >= seats:
            break
    return [xs.apportioned for xs in state]


def seatPerPop(populations, apportionment):
    return [(a/p) for p, a in zip(populations, apportionment)]


def numt(nums):
    return ' '.join(['{:10.6g}'.format(x) for x in nums])


def runToyPops(popdesc, seats, out=sys.stdout):
    pops = [int(x) for x in popdesc.split(',')]
    ha = apportion(seats, pops, 0)
    na = nbresenham(seats, pops, 0)
    nb2 = nbresenham2(seats, pops, 0)

    out.write('target p/s {:10.6g}\n'.format(seats/sum(pops)))
    out.write('pops      {}\n'.format(numt(pops)))
    out.write('house     {}\n'.format(numt(ha)))
    out.write('house p/s {}\n'.format(numt(seatPerPop(pops,ha))))
    out.write('nb 1      {}\n'.format(numt(na)))
    out.write('nb 1  p/s {}\n'.format(numt(seatPerPop(pops,na))))
    out.write('nb 2      {}\n'.format(numt(nb2)))
    out.write('nb 2  p/s {}\n'.format(numt(seatPerPop(pops,nb2))))

def readCsvPops(fin, fout, seats):
    reader = csv.reader(fin)
    statepops = []
    for row in reader:
        if row[0].lower() == 'state':
            # header row, ignore
            continue
        statepops.append(row)
    pops = [float(x[1]) for x in statepops]
    apportionedSeats = apportion(seats, pops)
    out = csv.writer(fout)
    for sp, ns in zip(statepops, apportionedSeats):
        out.writerow([sp[0], ns])


def main():
    import argparse
    import csv
    import sys
    ap = argparse.ArgumentParser()
    ap.add_argument('-n', dest='number', type=int, default=435, help='number of seats to apportion')
    ap.add_argument('-p', '--pops', default=None, help='comma separated list of population numbers')
    args = ap.parse_args()

    if args.pops:
        return runToyPops(args.pops, args.number)
    else:
        return readCsvPops(sys.stdin, sys.stdout, args.number)

if __name__ == '__main__':
    main()
