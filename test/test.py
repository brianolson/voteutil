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
    Impl('c', "../c/countvotes", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"]),
    #Impl('java', "java -jar ../java/vote.jar", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"]),
    Impl('go', "../go/countvotes/countvotes", ["irnr", "vrr", "raw"]),
    Impl('py', "../python/countvotes.py", ["irnr", "vrr", "raw"]),
]


def test_correctness(args):
    numChoices = random.randint(2,10)
    numVotes = random.randint(10,100000)
    names = [randname() for _ in range(numChoices)]
    votelines = [randneqvote(names) + '\n' for _ in range(numVotes)]
    votedata = ''.join(votelines)

    # results[method][impl name] = 'results'
    results = {}

    for imp in impls:
        try:
            cmd = [imp.cmd, '--test']
            logger.debug('run: %s', ' '.join(map(repr,cmd)))
            p = subprocess.Popen(cmd, shell=False, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
            outd, errd = p.communicate(votedata.encode())
        except:
            logger.error('failed running %r', cmd, exc_info=True)
            return 1
        outd = outd.decode()
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
    for methodName, ires in results.items():
        lastImpl = None
        lastResult = None
        for implName, result in ires.items():
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
        with open(fname, 'wt') as fout:
            fout.write(votedata)

    return failcount


def test_perf(args, numChoices, numVotes, out):
    names = [randname() for _ in range(numChoices)]
    votelines = [randneqvote(names) + '\n' for _ in range(numVotes)]
    votedata = ''.join(votelines)

    # results[method][impl name] = time
    results = {}

    errcount = 0

    for imp in impls:
        for methodName in imp.methods:
            try:
                cmd = imp.cmd + ' --disable-all --enable ' + methodName
                start = time.time()
                p = subprocess.Popen(cmd, shell=True, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
                outd, errd = p.communicate(votedata)
                end = time.time()
                dt = end - start
                if methodName not in results:
                    results[methodName] = {imp.name: dt}
                else:
                    results[methodName][imp.name] = dt
            except:
                logger.error('failed running %r', cmd, exc_info=True)
                errcount += 1
                if errcount >= 5:
                    return

    out.write('<table border="1">')
    for methodName, mr in results.items():
        ri = 0
        for implName, dt in mr.items():
            if ri == 0:
                out.write('<tr><td rowspan="{}">{}</td>'.format(len(mr), methodName))
            else:
                out.write('<tr>')
            ri += 1
            out.write('<td>{}</td><td>{:.2f}</td></tr>'.format(implName, dt))
    out.write('</table>\n')

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('-n', dest='runs', default=1, type=int)
    ap.add_argument('--perf', default=False, action='store_true', help='Test speed, not correctness.')
    ap.add_argument('--check', default=True, type=bool)
    ap.add_argument('--both', default=False, action='store_true')
    ap.add_argument('--maxbad', type=int, default=1)
    ap.add_argument('--err-prefix', default='errfneq')
    ap.add_argument('--verbose', default=False, action='store_true')
    args = ap.parse_args()

    if args.verbose:
        logging.basicConfig(level=logging.DEBUG)
    else:
        logging.basicConfig(level=logging.INFO)

    if args.check or args.both:
        for _ in range(args.runs):
            failcount = test_correctness(args)
            if failcount > 0:
                break

    if args.perf or args.both:
        test_perf(args, 20, 500000, sys.stdout)

if __name__ == '__main__':
    main()
