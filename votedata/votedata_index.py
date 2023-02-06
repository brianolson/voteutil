#!/usr/bin/env python3

import glob
import os

import jinja2.environment

with open('_index.html') as fin:
    tt = fin.read()

it = jinja2.environment.Template(tt)

def niceSize(b):
    if b > 2*1024*1024:
        return '{}MB'.format(b//(1024*1024))
    if b > 4*1024:
        return  '{}kB'.format(b//1024)
    return f'{b}B'

archives = glob.glob("archives/votedata*.tar.gz")
dataArchives = []
for apath in archives:
    item = {
        "url": apath,
        "fname": os.path.basename(apath),
        "size": niceSize(os.path.getsize(apath)),
    }
    dataArchives.append(item)

with open('_summary.html') as fin:
    summary = fin.read().strip()

with open('index.html', 'wt') as fout:
    fout.write(it.render(dataArchives=dataArchives, summary=summary))
