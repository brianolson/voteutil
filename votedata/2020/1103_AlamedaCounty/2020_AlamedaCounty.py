import csv
import logging
import re
import urllib.parse

logger = logging.getLogger(__name__)

choice_rank_re = re.compile(r'(.*)\((\d+)\)')

class AlamedaCounty2020:
    def __init__(self):
        # map contest name to file output
        self.outputs = {}

    def close(self):
        for fout in self.outputs.values():
            fout.close()
        self.outputs = {}

    def contest_out(self, contestname):
        sc = shortContest(contestname)
        fout = self.outputs.get(sc)
        if not fout:
            logger.info('contest: %s', contestname)
            fout = open(sc + '.nameq', 'wt')
            self.outputs[sc] = fout
        return fout

    def read_csv_file(self, fin):
        reader = csv.reader(fin)
        # h1: [election name, id]
        h1 = next(reader)
        # h2: contest name, repeated for each (h3)
        h2 = next(reader)
        # h3: choice name (within contest named in h2)
        h3 = next(reader)
        # metadata columns
        h4 = next(reader)
        colToContest = {col:contest for col,contest in nonemptycols(h2)}
        colToChoice = {col:contest for col,contest in nonemptycols(h3)}
        rcvChoicesByCol = {}
        for col,choice in nonemptycols(h3):
            contest = colToContest.get(col)
            if contest and ('RCV' in contest):
                m = choice_rank_re.match(choice)
                if m:
                    rcvChoicesByCol[col] = (m.group(1), m.group(2))
                else:
                    logger.error('[%d]\t%s\t%s', col, contest, choice)
        count = 0
        for row in reader:
            nec = nonemptycols(row)
            voteByContest = {}
            for col,v in nec:
                v = justMark(v)
                if v == '1':
                    contest = colToContest.get(col)
                    choiceRank = rcvChoicesByCol.get(col)
                    if contest and choiceRank:
                        da(voteByContest, contest, choiceRank)
            for contest, vote in voteByContest.items():
                self.contest_out(contest).write(urllib.parse.urlencode(vote) + '\n')
            count += 1
        return count

def justMark(v):
    if ' ' in v:
        return v.split()[0]
    return v

def shortContest(x):
    if '(RCV)' in x:
        return x.split('(RCV)')[0].strip()
    return x

def nonemptycols(row):
    out = []
    for i, v in enumerate(row):
        if (v is not None) and (v != ""):
            out.append((i,v))
    return out

def da(d, k, v):
    "dict append"
    l = d.get(k)
    if l:
        l.append(v)
    else:
        d[k] = [v]
    return d

def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument('csv_files', nargs='*')
    args = ap.parse_args()

    logging.basicConfig(level=logging.INFO)

    acp = AlamedaCounty2020()

    for path in args.csv_files:
        logger.info('%s ...', path)
        with open(path, 'rt') as fin:
            count = acp.read_csv_file(fin)
            logger.info('%s done, %d votes', path, count)
    acp.close()

if __name__ == '__main__':
    main()
