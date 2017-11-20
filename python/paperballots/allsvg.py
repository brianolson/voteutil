#!/usr/bin/env python
# generate a printable (and scannable) ballot, full page laid out in svg

prologueTemplate = '''<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
<svg
width="{widthIn}in"
height="{heightIn}in"
viewBox="0 0 {widthPx} {heightPx}"
id="svg2"
xmlns="http://www.w3.org/2000/svg" version="1.1">'''


ovalTemplate = '''<g transform="translate({left} {top})"><rect x="{Xinset}" y="{Yinset}" width="{InnerWidth}" height="{InnerHeight}" rx="{CornerRadius}" fill="none" stroke="#000" stroke-width="{LineThickness}" />{text}</g>'''
ovalTextTemplate = '''<text x="{Xcenter}" y="{Ycenter}" text-anchor="middle" dominant-baseline="central" font-size="{TextHeight}" fill="#aaa">{Number}</text>'''

def oval(text, left, top, width, height, lineThickness, textHeight=None, innerWidth=None, innerHeight=None, cornerRadius=None, xCenter=None, yCenter=None, xInset=None, yInset=None):
    if xInset is None:
        xInset = lineThickness
    if yInset is None:
        yInset = lineThickness
    if xCenter is None:
        xCenter = width / 2.0
    if yCenter is None:
        yCenter = height / 2.0
    if innerWidth is None:
        innerWidth = width - (2.0 * lineThickness)
    if innerHeight is None:
        innerHeight = height - (2.0 * lineThickness)
    if textHeight is None:
        textHeight = innerHeight * 0.8
    if cornerRadius is None:
        cornerRadius = min(innerWidth, innerHeight) * 0.47
    if text is not None:
        ovalSvg = ovalTextTemplate.format(Xcenter=xCenter, Ycenter=yCenter, TextHeight=textHeight, Number=text)
    else:
        ovalSvg = ''
    return ovalTemplate.format(left=left, top=top, Xinset=xInset, Yinset=yInset, InnerWidth=innerWidth, InnerHeight=innerHeight, CornerRadius=cornerRadius, LineThickness=lineThickness, text=ovalSvg)


# return svg text
#
# if coords is list, append to it values (an array so you can edit it later to translate x,y):
#   ['choice text', value, x, y, width, height]
def approvalLine(text, textHeight, ovalWidth=33, ovalHeight=13, ovalWPad=9, coords=None):
    ovalSvg = oval(None, 0, (textHeight / 2.0) - (ovalHeight / 2.0), ovalWidth, ovalHeight, 0.6)
    textSvg = '<text x="{x}" y="{y}" font-size="{fontSize}" dominant-baseline="central">{t}</text>'.format(x=ovalWPad + ovalWidth + ovalWPad, y=(textHeight / 2), t=text, fontSize=textHeight)
    if coords is not None:
        coords.append( [text, 1, 0, (textHeight / 2.0) - (ovalHeight / 2.0), ovalWidth, ovalHeight] )
    return ovalSvg + textSvg


# return svg text
#
# if coords is list, append to it values (an array so you can edit it later to translate x,y):
#   ['choice text', value, x, y, width, height]
def rankLine(nChoices, text, textHeight, ovalWidth=33, ovalHeight=13, ovalWPad=9, coords=None):
    parts = []
    for i in range(nChoices):
        parts.append(oval(str(i+1), (ovalWPad + ovalWidth) * i, (textHeight / 2.0) - (ovalHeight / 2.0), ovalWidth, ovalHeight, 0.6))
        if coords is not None:
            coords.append( [text, i+1, (ovalWPad + ovalWidth) * i, (textHeight / 2.0) - (ovalHeight / 2.0), ovalWidth, ovalHeight] )
    parts.append('<text x="{x}" y="{y}" font-size="{fontSize}" dominant-baseline="central">{t}</text>'.format(x=(ovalWPad + ovalWidth) * nChoices + ovalWPad, y=(textHeight / 2), t=text, fontSize=textHeight))
    return ''.join(parts)


def translateChoiceCoords(cclist, x, y):
    for cc in cclist:
        cc[2] += x
        cc[3] += y


if __name__ == '__main__':
    import argparse
    import json
    import sys
    ap = argparse.ArgumentParser()
    ap.add_argument('-c', '--coords', dest='coords', help='file to put bubble coordinates to in json')
    ap.add_argument('-o', '--out', dest='out', help='path to write svg to')
    args = ap.parse_args()

    doCoords = args.coords is not None

    if args.out:
        svgf = open(args.out, 'wt')
    else:
        svgf = sys.stdout
        
    widthIn = 7.5
    heightIn = 10
    dpi = 100
    widthPx = int(widthIn * dpi)
    heightPx = int(heightIn * dpi)
    
    svgf.write(prologueTemplate.format(widthIn=widthIn, heightIn=heightIn, widthPx=widthPx, heightPx=heightPx))
    #svgf.write('\n<rect x="0" y="0" width="7.5in" height="10in" stroke="#000" stroke-width="2" fill="none" />\n')
    #svgf.write('<rect x="0" y="0" width="{w}" height="{h}" stroke="#0f0" stroke-width="2" fill="none" />\n'.format(w=widthPx, h=heightPx))
    # corner register marks
    svgf.write('''<rect x="0" y="0" width="10" height="5" fill="#000" /><rect x="0" y="0" width="5" height="10" fill="#000" />''')
    svgf.write('''<rect x="{x}" y="0" width="10" height="5" fill="#000" /><rect x="{x2}" y="0" width="5" height="10" fill="#000" />'''.format(x=widthPx-10, x2=widthPx-5))
    svgf.write('''<rect x="0" y="{y}" width="10" height="5" fill="#000" /><rect x="0" y="{y2}" width="5" height="10" fill="#000" />'''.format(y=heightPx-5, y2=heightPx-10))
    svgf.write('''<rect x="{x}" y="{y}" width="10" height="5" fill="#000" /><rect x="{x2}" y="{y2}" width="5" height="10" fill="#000" />'''.format(y=heightPx-5, y2=heightPx-10, x=widthPx-10, x2=widthPx-5))

    choices = ["Episode I: The Phantom Menace", "Episode II: Attack of the Clones", "Episode III: Revenge of the Sith", "Episode IV: A New Hope", "Episode V: The Empire Strikes Back", "Episode VI: Return of the Jedi", "Episode VII: The Force Awakens"]
    ovalHeight = 13
    textHeight = 18
    rowHeight = max(ovalHeight, textHeight) + 6
    xpos = 10
    ypos = 10
    allChoiceCoords = []
    for choice in choices:
        choiceCoords = []
        svgf.write('<g transform="translate({x} {y})">'.format(x=xpos, y=ypos))
        svgf.write(rankLine(len(choices), choice, textHeight, ovalHeight=ovalHeight, coords=choiceCoords))
        svgf.write('</g>')
        translateChoiceCoords(choiceCoords, xpos, ypos)
        allChoiceCoords.extend(choiceCoords)
        ypos += rowHeight
    ypos += 10
    approvalChoices = ['Okay!', 'Sure!', 'Why not?', 'You betcha!', 'Hai!']
    for choice in approvalChoices:
        choiceCoords = []
        svgf.write('<g transform="translate({x} {y})">'.format(x=xpos, y=ypos))
        svgf.write(approvalLine(choice, textHeight, ovalHeight=ovalHeight, coords=choiceCoords))
        svgf.write('</g>')
        translateChoiceCoords(choiceCoords, xpos, ypos)
        allChoiceCoords.extend(choiceCoords)
        ypos += rowHeight
    svgf.write('</svg>\n')
    svgf.close()
    if args.coords:
        #sys.stderr.write(repr(allChoiceCoords))
        with open(args.coords, 'wt') as fout:
            json.dump(allChoiceCoords, fout)
