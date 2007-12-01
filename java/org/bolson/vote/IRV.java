package org.bolson.vote;
import java.util.Vector;

/**
 * Implements the Instant Runoff Voting, also known as Single Transferrable Vote.
 * There's wide debate about the flaws and virtues of this system.
 @see Condorcet
 @see IRNR
 */
public class IRV extends RankedVotingSystem {
	public IRV( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
		winnerBuckets = new Vector[numc + 2];
		active = new boolean[numc];
		myInit();
	}
	protected void myInit() {
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0.0;
			active[i] = true;
		}
		for ( int i = 0; i < winnerBuckets.length; i++ ) {
			winnerBuckets[i] = new Vector();
		}
	}
	boolean disqualifyTiedVotes = false;
	boolean duplicateTiedVotes = false;
	boolean splitTiedVotes = true;
	
	public String name() {
		if ( disqualifyTiedVotes ) {
			return "Instant Runoff Vote (tied-ranking votes are disqualified)";
		} else if ( duplicateTiedVotes ) {
			return "Instant Runoff Vote (tied-ranking votes are duplicated)";
		} else if ( splitTiedVotes ) {
			return "Instant Runoff Vote (tied-ranking votes are split)";
		}
		return "Instant Runoff Vote (tied rankings are undefined)";
	}
	
	/** if not null, then contains valid winners. set to null when adding a vote */
	int[] winners = null;
	protected void intoBucket( int ranking[] ) {
		int i;
		int mini = -1, min = numc + 1;
		int numAtRank = 1;
		if ( debug && dbtext != null ) {
			dbtext.append("ranking{ ");
		}
		for ( i = 0; i < numc; i++ ) {
			if ( debug && dbtext != null ) {
				if ( ranking[i] != NO_VOTE ) {
					dbtext.append( ranking[i] );
				} else {
					dbtext.append( '-' );
				}
				dbtext.append( ' ' );
			}
			if ( active[i] && (ranking[i] != NO_VOTE) ) {
				if ( ranking[i] < min ) {
					mini = i;
					min = ranking[i];
					numAtRank = 1;
				} else if ( ranking[i] == min ) {
					numAtRank++;
				}
			}
		}
		if ( debug && dbtext != null ) {
			dbtext.append( "} into bucket " ).append( mini ).append('\n');
		}
		if ( mini == -1 ) {
			// no active ranked choice
			winnerBuckets[numc].addElement( ranking );
		} else if ( numAtRank != 1 ) {
			winnerBuckets[numc+1].addElement( ranking );
			if ( disqualifyTiedVotes ) {
				// ignore the vote, add nothing to talley
			} else if ( duplicateTiedVotes ) {
				// add 1 to talley for each of the tied rankees
				for ( i = mini; i < numc; i++ ) {
					if ( ranking[i] == min ) {
						talley[i]++;
					}
				}
			} else if ( splitTiedVotes ) {
				// add 1/numAtRank to talley for each of the tied rankees
				double dt = 1.0 / numAtRank;
				for ( i = mini; i < numc; i++ ) {
					if ( ranking[i] == min ) {
						talley[i] += dt;
					}
				}
			}
		} else {
			talley[mini]++;
			winnerBuckets[mini].addElement( ranking );
		}
	}
	public int voteRanking( int ranking[] ) {
		winners = null;
		intoBucket( (int[])ranking.clone() );
		totalVotes++;
		return 0;
	}
	public void resetBuckets() {
		// collect all the old buckets
		Vector[] owb = (Vector[])winnerBuckets.clone();
		int i;
		myInit();
		// redistribute into new buckets
		for ( i = 0; i < owb.length; i++ ) {
			if ( owb[i] == null ) continue;
			for ( int j = 0; j < owb[i].size(); j++ ) {
				intoBucket( (int[])owb[i].elementAt( j ) );
			}
		}
	}
	StringBuffer dbtext = null;
	String dbTextSave = null;
	public int[] getWinners() {
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		if ( debug ) {
			dbtext = new StringBuffer();
		}
		double max;
		double min;
		int maxi = 0;
		int mini = 0;
		boolean loserTie = false;
		boolean winnerTie = false;
		int loseri = numc - 1;	// where to store next loser
		int winneri = 0;		// where to store next winner
		int i;
		resetBuckets();
		choiceIndecies = new int[numc];
		while ( winneri < loseri ) {
			max = -1e9;
			min = 1e9;
			if ( debug ) {
				dbtext.append( "winneri=" ).append( winneri ).append( ", loseri=" ).append( loseri ).append('\n');
			}
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				double si = talley[i];//winnerBuckets[i].size();
				if ( si > max ) {
					maxi = i;
					max = si;
					loserTie = false;
				} else if ( si == max ) {
					loserTie = true;
				}
				if ( si < min ) {
					mini = i;
					min = si;
					winnerTie = false;
				} else if ( si == min ) {
					winnerTie = true;
				}
			}
			if ( max == min ) {
				System.err.println( "max " + max + ", min " + min + ", winneri " + winneri + ", loseri " + loseri );
				winners = new int[loseri+1];
				for ( i = 0; i < numc; i++ ) {
					if ( active[i] && (talley[i] == max) ) {
						winners[winneri] = i;
						winneri++;
					}
				}
				finishDebugText("tie ending");
				return winners;
			} else if ( (talley[maxi] > totalVotes / 2) && false ) {
				//System.err.println( "promoting majority winner "+maxi+" with majority " + talley[maxi] );
				winners = new int[]{ maxi };
				finishDebugText("majority ending");
				return winners;
			} else for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				//System.err.println( "talley[" + i + "] = " + talley[i] );
				if ( talley[i] == min ) {
					if ( debug ) {
						dbtext.append( i ).append( " deactivated with tie for low score at ").append(min).append(", loseri ").append(loseri).append(", winneri " ).append( winneri ).append('\n');
					}
					choiceIndecies[loseri] = i;
					loseri--;
					active[i] = false;
					for ( int ri = winnerBuckets[i].size() - 1; ri >= 0; ri-- ) {
						intoBucket( (int[])(winnerBuckets[i].elementAt( ri )) );
					}
					winnerBuckets[i] = null;
				}
			}
			if ( disqualifyTiedVotes ) {
				// don't clear talley, it's fine
			} else if ( duplicateTiedVotes || splitTiedVotes ) {
				// reset talley to non-tied votes
				for ( i = 0; i < numc; i++ ) if ( active[i] ) {
					talley[i] = winnerBuckets[i].size();
				}
			}
			// may reprocess some votes that got moved into the tie bucket after disqualifying most recent loser, but that should be fine
			if ( winnerBuckets[numc+1].size() > 0 ) {
				Vector oldTiedVotes = winnerBuckets[numc+1];
				winnerBuckets[numc+1] = new Vector();
				for ( i = 0; i < oldTiedVotes.size(); i++ ) {
					intoBucket( (int[])(oldTiedVotes.elementAt( i )) );
				}
			}
		}
		if ( winneri != loseri ) {
			System.err.println( "IRV error: winneri "+winneri+", loseri "+loseri+"\n" );
		} else {
			//boolean onlyOne = true;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				choiceIndecies[winneri] = i;
				winneri++;
			}
		}
		if ( debug ) {
			dbtext.append("choiceIndecies:\n");
			for ( i = 0; i < numc; i++ ) {
				int ci;
				ci = choiceIndecies[i];
				dbtext.append( "\ttalley=" ).append( talley[ci] );
				if ( active[ci] ) {
					dbtext.append( "(active)" );
				}
				dbtext.append('\n');
			}
		}
		// count winners (who ties with first?)
		for ( numWinners = 1; (numWinners < talley.length) && (talley[choiceIndecies[numWinners-1]] == talley[choiceIndecies[numWinners]]); numWinners++ ) {}
		winners = new int[numWinners];
		for ( i = 0; i < numWinners; i++ ) {
			winners[i] = choiceIndecies[i];
		}
		finishDebugText("late ending");
		return winners;
	}
	private void finishDebugText(String comment) {
		if ( debug && dbtext != null ) {
			if ( comment != null ) {
				dbtext.append( comment );
			}
			dbTextSave = dbtext.toString();
			dbtext = null;
		}
	}
	
	public String getDebugText( String[] names ) {
		if ( winners == null ) {
			// dbTextSave not current, run getWinners()
			return null;
		}
		return dbTextSave;
	}
	/** 
	 Multi-seat IRV, aka STV.
	 In this case, "-split" refers to the overvote. With -split, votes for a winner maintain a fraction of their voting power proportional to the number of votes beyond the droop quota. By default a random set of ballots beyond the droop quota are transferred in whole to their remaining choices.
	 */
	public int[] getWinners( int numSeats ) {
		return null;
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
			toret = new StringBuffer( "<table border=\"1\"><tr><th></th><th>IRV Best Vote</th></tr>\n" );
		} else {
			toret = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>IRV Best Vote</th></tr>\n" );
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
	
	/**
	 Set what happens when a vote has a duplicate rating (among active choices)
	 -split|-dup|-dq
	 */
	public VotingSystem init( String[] argv ) {
		if ( argv == null ) return this;
		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i].equals("-split") ) {
				splitTiedVotes = true;
				disqualifyTiedVotes = false;
				duplicateTiedVotes = false;
			} else if ( argv[i].equals("-dup") ) {
				splitTiedVotes = false;
				disqualifyTiedVotes = false;
				duplicateTiedVotes = true;
			} else if ( argv[i].equals("-dq") ) {
				splitTiedVotes = false;
				disqualifyTiedVotes = true;
				duplicateTiedVotes = false;
			} else {
				System.err.println("IRV.init() bogus arg: "+argv[i]);
				return this;
			}
		}
		return this;
	}
	
	protected Vector votes;
	protected Vector winnerBuckets[];
	double talley[];
	boolean active[];
	int totalVotes;
	int numWinners;
	int choiceIndecies[] = new int[numc];
}
