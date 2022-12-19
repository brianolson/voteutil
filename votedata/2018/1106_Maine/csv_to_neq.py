#!/usr/bin/env python3
#
# Process data from Maine election for Congressional District 2, 2018-11-06
# https://www.maine.gov/sos/cec/elec/results/results18.html#Nov6

import csv
from collections import Counter
import glob
import gzip
import logging
import re
import sys

from urllib.parse import urlencode

logger = logging.getLogger(__name__)


party_prefix_re = re.compile(r'^[A-Z]{3} ')
num_suffix_re = re.compile(r' \(\d+\)$')

# published data has bug with some votes for different punctuations of Mr Golden
NAME_FIXUP = {
    'Golden, Jared F. ': 'Golden, Jared F.',
    'Golden Jared F.': 'Golden, Jared F.',
}

def trimname(name):
    name = party_prefix_re.sub('', name)
    name = num_suffix_re.sub('', name)
    name = NAME_FIXUP.get(name, name)
    return name

HEADER_KEYS = (
    ('1st Choice', 1),
    ('2nd Choice', 2),
    ('3rd Choice', 3),
    ('4th Choice', 4),
    ('5th Choice', 5)
)

def main():
    logging.basicConfig(level=logging.INFO)
    #header = None
    #ncols = None
    out = sys.stdout
    allseen = Counter()
    for path in glob.glob('*.csv.gz'):
        with gzip.open(path, 'rt') as fin:
            reader = csv.reader(fin)
            nheader = next(reader)
            ncols = []
            for i, vh in enumerate(nheader):
                for match_text, n in HEADER_KEYS:
                    if match_text in vh:
                        ncols.append( (i, n) )
                        break
            for row in reader:
                vote = []
                seen = set()
                for col, rank in ncols:
                    name = trimname(row[col])
                    if name in ('undervote', 'overvote'):
                        continue
                    if name in seen:
                        continue
                    seen.add(name)
                    allseen[name] += 1
                    vote.append((name, rank))
                if vote:
                    out.write(urlencode(vote) + '\n')
    sys.stderr.write('all seen {!r}\n'.format(allseen))

if __name__ == '__main__':
    main()
