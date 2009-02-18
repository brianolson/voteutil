package org.bolson.vote.staticballot;
import java.util.Vector;

/**
 * Coombs election method.
 * May be mathematically flawed, and is as computationally inconvenient as IRV.
 * Do not use.
 
 @see Condorcet
 @see IRNR
 @author Brian Olson
 */
public class Coombs extends RankedVotingSystem {
	
	protected Vector votes = new Vector();
	double talley[];
	double lastTalley[];
	boolean active[];
	int rankRound;
	boolean equalUnrankDistrib = true;
    static boolean nodebug = true;
	/** if not null, then contains valid winners. set to null when adding a vote */
	int[] winners = null;
	
    public Coombs( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
		active = new boolean[numc];
		lastTalley = new double[numc];
    }
	
	public String name() {
		return "Coombs' Method";
	}
	
    public int voteRanking( int ranking[] ) {
		winners = null;
		votes.add( (int[])ranking.clone() );
		return 0;
    }
    public int[] getWinners() {
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		double max = -1e9;
		double min = 1e9;
		int mini = 0;
		int i;
		for ( i = 0; i < numc; i++ ) {
			active[i] = true;
		}
		int numWinners;
		for ( rankRound = 1; rankRound <= numc; rankRound++ ) {
			int rrRank;
			rrRank = numc + 1 - rankRound;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				talley[i] = 0.0;
				lastTalley[i] = 0.0;
			}
			for ( i = 0; i < votes.size(); i++ ) {
				int[] cur;
				int numNR;
				boolean hasLast;
				int highRank;
				int highRankI;
				cur = (int[])votes.elementAt( i );
				numNR = 0;
				hasLast = false;
				highRank = 1000000;
				highRankI = -1;
				// find high and low
				for ( int bi = 0; bi < numc; bi++ ) if ( active[bi] ) {
					if ( cur[bi] == NO_VOTE ) {
						numNR++;
					} else {
						if ( cur[bi] == rrRank ) {
							hasLast = true;
							// talley low
							lastTalley[bi] += 1.0;
						}
						if ( cur[bi] < highRank ) {
							highRank = cur[bi];
							highRankI = bi;
						}
					}
				}
				// talley high
				if ( highRankI != -1 ) {
					talley[highRankI] += 1.0;
				}
				if ( equalUnrankDistrib ) {
					// distribute first and/or last
					double fract = 1.0 / numNR;
					if ( ! hasLast ) {
						// distribute low
						for ( int bi = 0; bi < numc; bi++ ) if ( active[bi] ) {
							if ( cur[bi] == NO_VOTE ) {
								lastTalley[bi] += fract;
							}
						}
					}
					if ( highRankI == -1 ) {
						// distribute high
						for ( int bi = 0; bi < numc; bi++ ) if ( active[bi] ) {
							if ( cur[bi] == NO_VOTE ) {
								talley[bi] += fract;
							}
						}
					}
				}
			}
			max = -1e9;
			min = -1e9;
			mini = -1;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				if ( talley[i] > max) {
					max = talley[i];
				}
				if ( lastTalley[i] > min ) {
					min = lastTalley[i];
					mini = i;
				}
			}
			if ( max > (votes.size() / 2) ) {
				break;
			}
			active[mini] = false;
		}
		// count winners (who ties with first?)
		numWinners = 0;
		for ( i = 0; i < numc; i++ ) {
			if ( talley[i] == max ) {
				numWinners++;
			}
		}
		winners = new int[numWinners];
		int wp = 0;
		for ( i = 0; i < numc; i++ ) {
			if ( talley[i] == max ) {
				winners[wp] = i;
				wp++;
			}
		}
		return winners;
    }
				
	public String htmlSummary( String names[] ) {
		StringBuffer toret;
		double max;
		boolean in[] = new boolean[numc];
		int maxi = 0, i;
		for ( i = 0; i < numc; i++ ) {
			in[i] = true;
		}
		if ( names != null ) {
			toret = new StringBuffer( "<table border=\"1\"><tr><th></th><th>Coombs Vote after round "+rankRound+"</th></tr>\n" );
		} else {
			toret = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>Coombs Vote after round "+rankRound+"</th></tr>\n" );
		}
		while ( true ) {
			boolean done;
			max = Integer.MIN_VALUE;
			done = true;
			for ( i = 0; i < numc; i++ ) {
				if ( in[i] && talley[i] > max ) {
					done = false;
					maxi = i;
					max = talley[i];
				}
			}
			if ( done ) break;
			i = maxi;
			in[i] = false;
			toret.append( "<tr><td>" );
			if ( names != null ) {
				toret.append( names[i] );
			} else {
				toret.append( i + 1 );
			}
			toret.append( "</td><td>" );
			toret.append( talley[i] );
			toret.append( "</td></tr>\n" );
		}
		toret.append( "</table>\n" );
		return toret.toString();
	}
	public String toString( String names[] ) {
		if ( names == null ) {
			return toString();
		}
		StringBuffer toret = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			toret.append( names[i] );
			toret.append( '\t' );
			toret.append( talley[i] );
			toret.append( '\n' );
		}
		return toret.toString();
	}
	public String toString() {
		StringBuffer toret = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			toret.append( talley[i] );
			toret.append( '\n' );
		}
		return toret.toString();
	}
}
