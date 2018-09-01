package org.bolson.vote;

import java.util.ArrayList;

/**
 * Virtual Round Robin election.
 * (Condorcet's method.)
 */
public class VRR2 extends NameVotingSystem {
    // avb[n] contains the pairwise counts of n vs all choice indexes less than n.
    // avb[n][0:n] is counts of n favored over lesser index;
    // avb[n][n:] is counts of a lesser index favored over n
    ArrayList<int[]> avb = new ArrayList<>();

    // vsUnknown[2*a] = a preferred over unknown
    // vsUnknown[(2*a) + 1] = unknown preferred over a
    ArrayList<Integer> vsUnknown = new ArrayList<>();

    NameIndex names = new NameIndex();

    private void vuInc(int i) {
        while (vsUnknown.size() <= i) {
            vsUnknown.add(0);
        }
        if (vsUnknown.size() == i) {
            vsUnknown.add(1);
        } else {
            vsUnknown.set(i, vsUnknown.get(i) + 1);
        }
    }
    /**
     * A has a rating >= 0, it is preferred to unknown options.
     * @param a
     */
    void incVsUnknown(int a) {
        vuInc(a * 2);
    }

    /**
     * a has a negative rating, unknown options are preferred to it.
     * @param a
     */
    void incUnknownVs(int a) {
        vuInc((a * 2) + 1);
    }

    int getVsUnknown(int a) {
        int i = a * 2;
        if (i < vsUnknown.size()) {
            return vsUnknown.get(i);
        }
        return 0;
    }
    int getUnknownVs(int a) {
        int i = (a * 2) + 1;
        if (i < vsUnknown.size()) {
            return vsUnknown.get(i);
        }
        return 0;
    }

    int[] getAvb(int a) {
        while (avb.size() <= a) {
            int l = avb.size();
            // build each with a copy of the unknown
            int[] x = new int[l*2];
            for (int b = 0; b < l; b++) {
                // l > b
                x[b] = getUnknownVs(b);
                // l < b
                x[l+b] = getVsUnknown(b);
            }
            avb.add(x);
        }
        return avb.get(a);
    }
    /**
     * A is preferred over B one time.
     * @param a
     * @param b
     */
    void inc(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException("cannot increment a choice over itself");
        }
        if (a > b) {
            int[] avb = getAvb(a);
            avb[b]++;
        } else {
            int[] avb = getAvb(b);
            avb[b+a]++;
        }
    }
    int get(int a, int b) {
        if (a > b) {
            int[] avb = getAvb(a);
            return avb[b];
        } else {
            int[] avb = getAvb(b);
            return avb[b+a];
        }
    }

    @Override
    public void voteRating(NameVote[] vote) {
        int[] nameIndexes = new int[vote.length];
        for (int i = 0; i < vote.length; i++) {
            nameIndexes[i] = names.index(vote[i].name);
			for (int j = 0; j < voteLength; j++) {
				if (nameIndexes[j] == nameIndexes[i]) {
					nameIndexes[i] = -1;
				}
			}
        }
        for (int i = 0; i < vote.length; i++) {
            int a = nameIndexes[i];
			if (a == -1) continue;
            for (int j = i + 1; j < vote.length; j++) {
                int b = nameIndexes[j];
				if (b == -1) continue;
                inc(a, b);
            }

            if (vote[i].rating >= 0) {
                // a is preferred to future unknown things not in this vote
                incVsUnknown(a);
            } else {
                // a is less preferred vs future unknown things
                incUnknownVs(a);
            }
            for (int b = 0; b < avb.size(); b++) {
                boolean found = false;
                for (int xb : nameIndexes) {
                    if (xb == b) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    if (vote[i].rating >= 0) {
                        // a is preferred to known things not in this vote
                        inc(a, b);
                    } else {
                        // negative rated a is less preferred than known things not in this vote
                        inc(b, a);
                    }
                }
            }
        }
    }

    int countDefeats(int a) {
        int count = 0;
        for (int b = 0; b < avb.size(); b++) {
            if (b == a) {
                continue;
            }
            if (get(a, b) < get(b, a)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public NameVote[] getWinners() {
        int[] defeats = new int[avb.size()];
        int mind = defeats.length;
        int mina = -1;
        int ties = 0;
        for (int a = 0; a < defeats.length; a++) {
            defeats[a] = countDefeats(a);
            if (defeats[a] < mind) {
                mind = defeats[a];
                mina = a;
                ties = 1;
            } else if (defeats[a] == mind) {
                ties++;
            }
        }
        if (ties > 1) {
            System.err.println("TODO: implement VRR2 cycle resolution");
        }
        NameVote[] out = new NameVote[avb.size()];
        for (int a = 0; a < out.length; a++) {
            out[a] = new NameVote(names.name(a), out.length + 1 - defeats[a]);
            //out[a].name = names.name(a);
            //out[a].rating = out.length + 1 - defeats[a];
        }
        java.util.Arrays.sort(out);
        return out;
    }

    @Override
    public StringBuffer htmlSummary(StringBuffer sb) {
        NameVote[] winners = getWinners();
        sb.append("<table border=\"1\"><tr><td></td>");
        for (int i = 0; i < winners.length; i++) {
            sb.append("<th>").append(i+1).append("</th>");
        }
        sb.append("</tr>");
        for (int i = 0; i < winners.length; i++) {
            int a = names.index(winners[i].name);
            sb.append("<tr><th>(").append(i + 1).append(") ").append(winners[i].name).append("</th>");
            for (int j = 0; j < winners.length; j++) {
                if (i == j) {
                    sb.append("<td></td>");
                    continue;
                }
                int b = names.index(winners[j].name);
                sb.append("<td>").append(get(a,b)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb;
    }

    @Override
    public String name() {
        return "Virtual Round Robin 2";
    }
    static {
        registerImpl( "VRR2", VRR2.class );
    }
}
