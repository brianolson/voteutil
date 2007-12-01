package org.bolson.vote;
import java.util.LinkedList;

/**
 * Implements the Ranked Pairs method of ranked voting tabulation.
 * It is much like Condorcet.
 */
public class RankedPairs extends Condorcet {
	public RankedPairs( int nc ) {
		super( nc );
	}
	protected static class Pair {
		int winner;
		int loser;
		int score;
		public Pair( int w, int l, int s ) {
			winner = w;
			loser = l;
			score = s;
		}
	}
	/**
	breadth first search cycle detection
	 */
	static boolean hasCycle( Pair[] they, int lim, int startChoice ) {
		LinkedList searchq = new LinkedList();
		LinkedList nsq = null;
		int i;
		for ( i = 0; i < lim; i++ ) {
			if ( they[i].winner == startChoice ) {
				searchq.addLast( they[i] );
			}
		}
		while ( ! searchq.isEmpty() ) {
			nsq = new LinkedList();
			while ( ! searchq.isEmpty() ) {
				Pair cur;
				cur = (Pair)searchq.removeFirst();
				for ( i = 0; i < lim; i++ ) {
					if ( they[i].winner == cur.loser ) {
						if ( they[i].loser == startChoice ) {
							// cycle detected
							return true;
						}
						nsq.addLast( they[i] );
					}
				}
			}
			searchq = nsq;
		}
		return false;
	}
	public int[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		if ( debug ) {
			debugsb = new StringBuffer();
		} else {
			debugsb = null;
		}
		int j,k;
		Pair[] they = new Pair[numc*numc - numc];
		int ti = 0;
		// convert array to list of pairs
		for ( j = 0; j < numc; j++ ) {
			for ( k = j+1; k < numc; k++ ) {
				int vk, vj;
				vk = talley[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = talley[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ( winningVotes ) {
					they[ti] = new Pair( j, k, vj );
					ti++;
					they[ti] = new Pair( k, j, vk );
					ti++;
				} else /*if ( margins )*/ {
					they[ti] = new Pair( j, k, vj-vk );
					ti++;
					they[ti] = new Pair( k, j, vk-vj );
					ti++;
				}
			}
		}
		// sort (bubble sort, an n^2 algorithm on a numc^2 long list. n^4 baby! bad, oh well, lazy)
		boolean notdone = true;
		while ( notdone ) {
			notdone = false;
			for ( ti = 1; ti < they.length; ti++ ) {
				if ( they[ti].score > they[ti-1].score ) {
					Pair tp;
					tp = they[ti];
					they[ti] = they[ti-1];
					they[ti-1] = tp;
					notdone = true;
				}
			}
		}
		if ( debug & false ) {
			for ( ti = 0; ti < they.length; ti++ ) {
				debugsb.append(they[ti].winner);
				debugsb.append("\t>\t");
				debugsb.append(they[ti].loser);
				debugsb.append("\tby\t");
				debugsb.append(they[ti].score);
				debugsb.append('\n');
			}
		}
		// lock-in non cycle forming pairs
		int read = 0, write = 0;
		while ( read < they.length ) {
			if ( they[write].score <= 0 ) {
				break;
			}
			// FIXME need to detect when there are many in a row tied and abort if they together contribute a cycle
			if ( hasCycle( they, write + 1, they[write].winner ) ) {
				read++;
				if ( read < they.length ) {
					they[write] = they[read];
					they[read] = null;
				}
			} else {
				if ( debug ) {
					debugsb.append("lock-in ");
					debugsb.append(they[write].winner);
					debugsb.append("\t>\t");
					debugsb.append(they[write].loser);
					debugsb.append("\tby\t");
					debugsb.append(they[write].score);
					debugsb.append('\n');
				}
				write++;
				read++;
				if ( (read != write) && (read < they.length) ) {
					they[write] = they[read];
					they[read] = null;
				}
			}
		}
		// figure out who won
		// there should be one or a tie of undefeated choices.
		defeatCount = new int[numc];
		for ( ti = 0; ti < write; ti++ ) {
			defeatCount[they[ti].loser]++;
		}
		int mindc = defeatCount[0];
		int numatmindc = 1;
		int mindci = 0;
		if ( debug ) {
			debugsb.append("defeatCount={").append( defeatCount[0] );
		}
		for ( j = 1; j < numc; j++ ) {
			if ( debug ) {
				debugsb.append(", ").append( defeatCount[j] );
			}
			if ( defeatCount[j] < mindc ) {
				numatmindc = 1;
				mindci = j;
				mindc = defeatCount[j];
			} else if ( defeatCount[j] == mindc ) {
				numatmindc++;
			}
		}
		if ( debug ) {
			debugsb.append("}\n");
		}
		if ( numatmindc == 1 ) {
			winners = new int[]{ mindci };
			return winners;
		}
		winners = new int[numatmindc];
		k = 0;
		for ( j = mindci; j < numc; j++ ) {
			if ( defeatCount[j] == mindc ) {
				winners[k] = j;
				k++;
			}
		}
		return winners;
	}
	
	public String getDebugText( String[] names ) {
		if ( debugsb == null ) {
			return null;
		}
		return debugsb.toString();
	}

	public String name() {
		if ( winningVotes ) {
			return "Ranked Pairs, winning votes";
		} else {
			return "Ranked Pairs, marginal votes";
		}
	}
}
