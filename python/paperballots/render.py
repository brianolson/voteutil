#!/usr/bin/python

import os

from jinja2 import Environment, FileSystemLoader, select_autoescape


def env():
    return Environment(
        loader=FileSystemLoader(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'templates')),
        trim_blocks=True,
        lstrip_blocks=True,
        autoescape=select_autoescape(['html', 'xml']),
        auto_reload=False
    )


if __name__ == '__main__':
    import argparse
    import json
    import sys
    ap = argparse.ArgumentParser()
    ap.add_argument("-i", "--in", dest="inpath", help="path to json ballot info")
    ap.add_argument("-d", "--outdir", dest="outdir", help="path to directory for output; will `mkdir -p`")
    args = ap.parse_args()

    if args.inpath and len(args.inpath) > 0:
        with open(args.inpath, "rt") as fin:
            ballotData = json.load(fin)
    else:
        ballotData = json.load(sys.stdin)
    outdir = args.outdir or os.getcwd()
    outdir = os.path.abspath(outdir)
    if not os.path.isdir(outdir):
        os.makedirs(outdir)
    env = env()
    template = env.get_template("ballot.html")
    with open(os.path.join(outdir, "ballot.html"), "wt") as fout:
        fout.write(template.render(ballotData))
    ovalt = env.get_template("oval.svg")
    height = 16
    width = 43
    thickness = 0.6
    innerWidth = width - (2 * thickness)
    innerHeight = height - (2 * thickness)
    ovalContext = {
        "Width": width,
        "Height": height,
        "LineThickness": thickness,
        "Xinset": thickness,
        "Yinset": thickness,
        "InnerWidth": innerWidth,
        "InnerHeight": innerHeight,
        "CornerRadius": min(innerWidth, innerHeight) * 0.47,
        "Xcenter": width / 2.0,
        "Ycenter": height / 2.0,
        "TextHeight": innerHeight * 0.8
    }
    maxChoices = max([len(z["Choices"]) for z in ballotData["Issues"]])
    for i in range(1, maxChoices + 1):
        ovalContext["Number"] = i
        with open(os.path.join(outdir, "{}.svg".format(i)), "wt") as fout:
            fout.write(ovalt.render(ovalContext))
