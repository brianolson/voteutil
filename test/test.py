#!/usr/bin/python


'''./test.py [--tmpdir dir][--keep-temps][--perf][--check][--both][-n runs]
          [--urlhandler cmd]

    --tmpdir dir      defaults to using perl tempdir() otherwise
    --keep-temps      do not delete temporary files if all tests pass (normally
                      kept on failures)
    --perf            Test speed, not correctness.
    --check           Test correctness, not speed (default).
    --both            Test both speed and correctness.
    --maxbad n        Maximum number of tests that can fail before stopping.
    -n runs           Number fo times to repeat correctness test.
    --urlhandler cmd  Execute `cmd \$url` for each failed test explain output.
'''

import argparse
import logging
import random
import subprocess
import sys
import time

from randomneq import randname, randneqvote
#import randomneq
#randneqvote = randneq.randneqvote


logger = logging.getLogger(__name__)

longToShort = {
	"Instant Runoff Normalized Ratings": "irnr",
	"Virtual Round Robin": "vrr",
	"Virtual Round Robin, Ranked Pairs Resolution": "rp",
	"Raw Rating Summation": "raw",
	"Instant Runoff Voting": "irv",
	"Single Transferrable Vote": "stv",
}

class Impl(object):
    def __init__(self, name, cmd, methods):
        self.name = name
        self.cmd = cmd
        self.methods = methods

impls = [
    Impl('c', "../c/countvotes --test", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"]),
    Impl('java', "java -jar ../java/vote.jar --test", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"]),
    Impl('go', "../go/countvotes/countvotes --test", ["irnr", "vrr", "raw"]),
]


def test_correctness(args):
    num_choices = random.randint(2,10)
    num_votes = random.randint(10,100000)
    names = [randname() for _ in xrange(num_choices)]
    votelines = [randneqvote(names) + '\n' for _ in xrange(num_votes)]
    votedata = ''.join(votelines)

    # results[method][impl name] = 'results'
    results = {}
    #methods = set()

    for imp in impls:
        try:
            p = subprocess.Popen(imp.cmd, shell=True, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
            outd, errd = p.communicate(votedata)
        except:
            logger.error('failed running %r', imp.cmd, exc_info=True)
            return 1
        for line in outd.splitlines():
            line = line.strip()
            ab = line.split(':')
            if len(ab) != 2:
                logger.error('bad line from %s: %r', imp.name, line)
                continue
            methodName, result = ab
            methodName = methodName.strip()
            result = result.strip()
            if methodName not in results:
                results[methodName] = {imp.name: result}
            else:
                results[methodName][imp.name] = result
            #methods.add(methodName)

    failcount = 0
    for methodName, ires in results.iteritems():
        lastImpl = None
        lastResult = None
        for implName, result in ires.iteritems():
            if lastResult is not None:
                if lastResult != result:
                    logger.error('%s: (%s %s) != (%s %s)', methodName, lastImpl, lastResult, implName, result)
                    failcount += 1
            lastResult = result
            lastImpl = implName

    # TODO: write votedata to file for more analysis
    if failcount > 0:
        fname = args.err_prefix + time.strftime('%Y%m%d_%H%M%S')
        logger.error('writing problem votes to %r', fname)
        with open(fname, 'wb') as fout:
            fout.write(votedata)

    return failcount
                    


def main():
    logging.basicConfig(level=logging.INFO)
    ap = argparse.ArgumentParser()
    ap.add_argument('-n', dest='runs', default=1, type=int)
    ap.add_argument('--perf', default=False, action='store_true', help='Test speed, not correctness.')
    ap.add_argument('--check', default=True, type=bool)
    ap.add_argument('--both', default=False, action='store_true')
    ap.add_argument('--maxbad', type=int, default=1)
    ap.add_argument('--err-prefix', default='errfneq')
    args = ap.parse_args()

    if args.check or args.both:
        for _ in xrange(args.runs):
            failcount = test_correctness(args)
            if failcount > 0:
                break

if __name__ == '__main__':
    main()
