#!/usr/bin/env python3

import sys
import urllib.parse

breedvleno = 0
lenovbreed = 0

for line in sys.stdin:
    line = line.strip()
    qs = urllib.parse.parse_qs(line)
    breed = int(qs.get('LONDON BREED', [99])[0])
    leno = int(qs.get('MARK LENO', [99])[0])
    if breed < leno:
        breedvleno += 1
    elif leno < breed:
        lenovbreed += 1
sys.stdout.write('{} breed > leno\n'.format(breedvleno))
sys.stdout.write('{} leno > breed\n'.format(lenovbreed))
