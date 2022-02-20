#!/usr/bin/python

import random
import sys

az = [chr(x) for x in range(ord('a'),ord('z')+1)]

def randname(length=6):
    return ''.join([random.choice(az) for _ in range(length)])

def randneqvote(names, bias=None, joiner='&'):
    if bias is None:
        return joiner.join([k + '=' + str(random.random()) for k in names])
    else:
        return joiner.join([k + '=' + str(random.random() * bias[k]) for k in names])

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('-n', '-v', dest='num', type=int, default=10, help='number of votes')
    ap.add_argument('--numnames', type=int, default=4)
    ap.add_argument('-F', dest='joiner', default='&')
    ap.add_argument('-i', dest='indexnames', default=False, action='store_true')
    ap.add_argument('--nobias', default=False, action='store_true')
    ap.add_argument('--namelen', default=6, type=int)
    args = ap.parse_args()

    if args.indexnames:
        names = [str(x) for x in range(args.numnames)]
    else:
        names = [randname(args.namelen) for _ in range(args.numnames)]

    bias = {k:1.0 for k in names}
    if not args.nobias:
        bias[names[1]] = 1.2
        bias[names[3]] = 1.4

    for _ in range(args.num):
        sys.stdout.write(randneqvote(names, bias, args.joiner) + '\n')


if __name__ == '__main__':
    main()
