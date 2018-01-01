package org.bolson.vote.staticballot;

/**
 * It's like Condorcet, but based on ratings differentials.
 
 Pairwise Rating Differential Summation

 For N choices keep a talley matrix which is N by N.
 For each pair of choices in a rated vote:
	If choice A is rated higher than choice B, add (A's rating minus B's rating) to the talley entry (A,B)
	If choice B is rated higher than choice A, add (B's rating minus A's rating) to the talley entry (B,A)
 After all votes have been tallied, if (A,B) is greater than (B,A) then A has 'defeated' B.
 Examine all such relations and count up the defeats. The choice or choices with the fewest defeats win.
 @author Brian Olson
 */
public class PairwiseRatingDifferentialSummation extends RatedVotingSystem {
    protected double talley[];
	protected int defeatCount[];
	public int[] winners;
	String message = null;

    public PairwiseRatingDifferentialSummation( int numCandidates ) {
    	super( numCandidates );
		talley = new double[numc*numc];
		for ( int i = 0; i < talley.length; i++ ) {
			talley[i] = 0;
		}
    }

	public String name() {
		if ( noVoteImpliesNoPreference ) {
			return "Pairwise Rating Differential Summation, no-vote == no-pref";
		}
		return "Pairwise Rating Differential Summation";
	}

	boolean noVoteImpliesNoPreference = false;

    public int voteRating( int rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				int pk, pj;
				pk = rating[k];
				if ( pk == NO_VOTE ) {
					if ( noVoteImpliesNoPreference ) continue;
					pk = 0;
				}
				pj = rating[j];
				if ( pj == NO_VOTE ) {
					if ( noVoteImpliesNoPreference ) continue;
					pj = 0;
				}
				if ( pk > pj ) {
					talley[k*numc + j] += pk - pj;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k] += pj - pk;	// j beats k
				}
			}
		}
		return 0;
    }
    public int voteRating( float rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				float pk, pj;
				pk = rating[k];
				if ( Float.isNaN( pk ) ) {
					if ( noVoteImpliesNoPreference ) continue;
					pk = 0.0f;
				}
				pj = rating[j];
				if ( Float.isNaN( pj ) ) {
					if ( noVoteImpliesNoPreference ) continue;
					pj = 0.0f;
				}
				if ( pk > pj ) {
					talley[k*numc + j] += pk - pj;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k] += pj - pk;	// j beats k
				}
			}
		}
		return 0;
    }
    public int voteRating( double rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				double pk, pj;
				pk = rating[k];
				if ( Double.isNaN( pk ) ) {
					if ( noVoteImpliesNoPreference ) continue;
					pk = 0.0f;
				}
				pj = rating[j];
				if ( Double.isNaN( pj ) ) {
					if ( noVoteImpliesNoPreference ) continue;
					pj = 0.0f;
				}
				if ( pk > pj ) {
					talley[k*numc + j] += pk - pj;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k] += pj - pk;	// j beats k
				}
			}
		}
		return 0;
    }
    
    public int[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		/* ndefeats is "numc choose 2" or ((numc !)/((2 !)*((numc - 2)!))) */
		//int ndefeats = (numc*(numc-1))/2, dpos = 0;
		int j,k;
		int numWinners;
//		message = null;
		
		defeatCount = new int[numc];
		for ( j = 0; j < numc; j++ ) {
			defeatCount[j] = 0;
		}
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				double vk, vj;
				vk = talley[k*numc + j];	// k beat j by vk preference differential
				vj = talley[j*numc + k];	// j beat k by vj preference differential
				if ( vj > vk ) {
					defeatCount[k]++;
				} else if ( vj < vk ) {
					defeatCount[j]++;
				}
			}
		}
		//ndefeats = dpos;
		numWinners = 0;
		for ( j = 0; j < numc; j++ ) {
			if ( defeatCount[j] == 0 ) {
				numWinners++;
			}
		}
		/* FIXME, better cycle resolution */
		while ( numWinners == 0 ) {
			message = "cycle detected, falling back to minimum defeats";
			for ( j = 0; j < numc; j++ ) {
				defeatCount[j]--;
				if ( defeatCount[j] == 0 ) {
					numWinners++;
				}
			}
		}
		winners = new int[numWinners];
		int winneri = 0;
		for ( j = 0; j < numc; j++ ) {
			if ( defeatCount[j] == 0 ) {
				winners[winneri] = j;
			}
		}
		return winners;
    }

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			for ( int ii = 0; ii < numc; ii++ ) {
				sb.append( talley[i*numc + ii] );
				sb.append( '\t' );
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}    
	public String toString( String names[] ) {
		if ( names == null ) return toString();
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			sb.append( names[i] ).append( '\t' );
			for ( int ii = 0; ii < numc; ii++ ) {
				sb.append( talley[i*numc + ii] );
				sb.append( '\t' );
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}
	
	public String htmlSummary( String names[] ) {
		StringBuffer sb;
		if ( names != null ) {
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th colspan=\"" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th colspan=\"");
		}
		sb.append( numc );
		sb.append("\">Pairwise Rating Differential Sums</th></tr>\n" );
		for ( int i = 0; i < numc; i++ ) {
			sb.append("<tr><td>");
			if ( names != null ) {
				sb.append( names[i] );
			} else {
				sb.append( i );
			}
			sb.append("</td>");
			for ( int j = 0; j < numc; j++ ) {
				sb.append("<td>");
				sb.append(talley[i*numc + j]);
				sb.append("</td>");
			}
			sb.append("</tr>");
		}
		sb.append("</table>\n");
		if ( message != null ) {
			sb.append("<P>");
			sb.append(message);
			sb.append("<br>defeats at minimum:");
			for ( int i = 0; i < defeatCount.length; i++ ) {
				sb.append(' ');
				sb.append( defeatCount[i] );
			}
			sb.append("</P>");
			/*sb.append("<table border=\"1\"><tr><th>\n");
			if ( names != null) {
				sb.append("Choice Index");
			}
			sb.append("</th><th colspan=\"");
			sb.append( numc );
			sb.append("\">Beat-Path Results Array</th></tr>\n" );
			for ( int i = 0; i < numc; i++ ) {
				sb.append("<tr><td>");
				if ( names != null ) {
					sb.append( names[i] );
				} else {
					sb.append( i );
				}
				sb.append("</td>");
				for ( int j = 0; j < numc; j++ ) {
					sb.append("<td>");
					sb.append(bpm[i*numc + j]);
					sb.append("</td>");
				}
				sb.append("</tr>");
			}
			sb.append("</table>");*/
		}
		return sb.toString();
	}
}
