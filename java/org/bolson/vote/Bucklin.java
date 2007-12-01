package org.bolson.vote;
import java.util.Vector;

/**
 * Bucklin election method.
 * May be mathematically flawed, and is as computationally inconvenient as IRV.
 */
public class Bucklin extends RankedVotingSystem {
	
	protected Vector votes = new Vector();
	double talley[];
	int rankRound;
	boolean equalUnrankDistrib = true;
	static boolean nodebug = true;
	/** if not null, then contains valid winners. set to null when adding a vote */
	int[] winners = null;
	
	public Bucklin( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
	}
	public VotingSystem init( String[] argv ) {
		if ( argv == null ) return this;
		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i].equals("-distrib") ) {
				equalUnrankDistrib = true;
			} else if ( argv[i].equals("-trunc") ) {
				equalUnrankDistrib = false;
				//			} else if ( argv[i].equals("") ) {
			} else {
				System.err.println("Bucklin init bogus arg: "+argv[i]);
				return this;
			}
		}
		return this;
	}
	
	public String name() {
		if ( equalUnrankDistrib ) {
			return "Bucklin election method, non-voted ranks are distributed";
		} else {
			return "Bucklin election method";
		}
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
		double max = -1.9;
		int min;
		int maxi = 0;
		int mini = 0;
		boolean loserTie = false;
		boolean winnerTie = false;
		int loseri = numc - 1;	// where to store next loser
		int winneri = 0;		// where to store next winner
		int i;
		for ( i = 0; i < numc; i++ ) {
			talley[i] = 0.0;
		}
		for ( rankRound = 1; rankRound <= numc; rankRound++ ) {
			for ( i = 0; i < votes.size(); i++ ) {
				int[] cur;
				int numNR;
				cur = (int[])votes.elementAt( i );
				numNR = 0;
				for ( int bi = 0; bi < numc; bi++ ) {
					if ( cur[bi] == NO_VOTE ) {
						numNR++;
					} else if ( cur[bi] == rankRound ) {
						talley[bi]++;
						numNR = 0;
						break;
					}
				}
				if ( equalUnrankDistrib && (numNR != 0) ) {
					double fract = 1.0 / numNR;
					for ( int bi = 0; bi < numc; bi++ ) {
						if ( cur[bi] == NO_VOTE ) {
							talley[bi] += fract;
						}
					}
				}
			}
			max = talley[0];
			maxi = 0;
			for ( i = 1; i < numc; i++ ) {
				if ( talley[i] > max ) {
					max = talley[i];
					maxi = i;
				}
			}
			if ( max > (votes.size() / 2) ) {
				break;
			}
		}
		// count winners (who ties with first?)
		int numWinners = 0;
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
			toret = new StringBuffer( "<table border=\"1\"><tr><th></th><th>Bucklin Vote after round "+rankRound+"</th></tr>\n" );
		} else {
			toret = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>Bucklin Vote after round "+rankRound+"</th></tr>\n" );
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
