#!/usr/bin/env python3
#
# read a csv on input: state, pop
# write a csv to output: state, seats
#
# usage:
# python3 apportion.py -n 435 < state_pop.csv > state_seats.csv


import math


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
            priority = pop / math.sqrt(cur_seats * (cur_seats + 1))
            if (maxprio is None) or (priority > maxprio):
                maxprio = priority
                maxi = i
        out[maxi] += 1
        seats_set += 1
    return out

def main():
    import argparse
    import csv
    import sys
    ap = argparse.ArgumentParser()
    ap.add_argument('-n', dest='number', type=int, default=435, help='number of seats to apportion')
    args = ap.parse_args()
    reader = csv.reader(sys.stdin)
    statepops = []
    for row in reader:
        if row[0].lower() == 'state':
            # header row, ignore
            continue
        statepops.append(row)
    pops = [float(x[1]) for x in statepops]
    seats = apportion(args.number, pops)
    out = csv.writer(sys.stdout)
    for sp, ns in zip(statepops, seats):
        out.writerow([sp[0], ns])
    return 0

if __name__ == '__main__':
    main()
