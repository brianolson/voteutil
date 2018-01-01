package org.bolson.vote.staticballot;
import java.util.Vector;
/**
 * Single Transferrable Vote, IRV for multi-seat elections.
 * @see <a href="http://en.wikipedia.org/wiki/Single_transferable_vote">Wikipedia: Single Transferrable Vote</a>
 @author Brian Olson
 */

public class STV extends RankedVotingSystem {
	/** if not null, then contains valid winners. set to null when adding a vote */
	int[] winners = null;
	Vector[] winnerBuckets;
	double[] talley;
	boolean[] active;
	int totalVotes;
	StringBuffer debugsb = null;
	
	// ranking built up during getWinners
	int[] choiceIndecies;
	
	boolean disqualifyTiedVotes = false;
	boolean duplicateTiedVotes = false;
	boolean splitTiedVotes = true;
	
	static final int DROOP = 1;
	static final int HARE = 2;
	static final int IMPERIALI = 3;
	
	int quotaStyle = DROOP;
	
	public static double quota( int style, double votes, int seats ) {
		switch ( style ) {
			case DROOP:
				return (votes / (seats+1.0)) + 1.0;
			case HARE:
				return votes / seats;
			case IMPERIALI:
				return votes / (seats + 2.0);
			default:
				return Double.NaN;
		}
	}
	
	double quota( double votes, int seats ) {
		return quota( quotaStyle, votes, seats );
	}
	
	static boolean nodebug = true;
	
	/** In addition to interfaced methods, there shall be a constructor taking 
	 * the integer number N of choices to be voted on. */
	public STV( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
		winnerBuckets = new Vector[numc + 2];
		active = new boolean[numc];
		myInit();
	};
	protected void myInit() {
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0.0;
			active[i] = true;
		}
		for ( int i = 0; i < winnerBuckets.length; i++ ) {
			winnerBuckets[i] = new Vector();
		}
	}
	
	/**
	 * Arguments to modify behavior of STV
	 * @return this so that you can do
	 <pre>vs = (new VS(numc)).init( new String[]{"blah", "grunt"} );</pre>
	 */
	public VotingSystem init( String argv[] ) {
		int i = 0, j = 0;
		while ( i < argv.length ) {
			boolean used;
			used = true;
			if ( argv[i] == null ) {
			} else if ( argv[i].equals("droop") ) {
				quotaStyle = DROOP;
			} else if ( argv[i].equals("hare") ) {
				quotaStyle = HARE;
			} else if ( argv[i].equals("imperiali") ) {
				quotaStyle = IMPERIALI;
			} else {
				if ( j != i ) {
					argv[j] = argv[i];
				}
				j++;
				used = false;
			}
			if ( used ) {
				argv[i] = null;
			}
			i++;
		}
		return super.init( argv );
	}
	
	protected static class Ballot {
		int ranking[];
		double weight;
		
		public Ballot( int r[] ) {
			ranking = (int[])r.clone();
			weight = 1.0;
		}
	}
	
	protected void intoBucket( Ballot b ) {
		int i;
		int mini = -1, min = numc + 1;
		int numAtRank = 1;
		StringBuffer sb = null;
		if ( ! nodebug ) {
			sb = new StringBuffer("ranking{ ");
		}
		for ( i = 0; i < numc; i++ ) {
			if ( nodebug ) {
			} else {
				if ( b.ranking[i] != NO_VOTE ) {
					sb.append( b.ranking[i] );
				} else {
					sb.append( '-' );
				}
				sb.append( ' ' );
			}
			if ( active[i] && (b.ranking[i] != NO_VOTE) ) {
				if ( b.ranking[i] < min ) {
					mini = i;
					min = b.ranking[i];
					numAtRank = 1;
				} else if ( b.ranking[i] == min ) {
					numAtRank++;
				}
			}
		}
		if ( ! nodebug ) {
			sb.append( "} into bucket " ).append( mini );
			System.out.println( sb );
		}
		if ( mini == -1 ) {
			// no active ranked choice
			winnerBuckets[numc].addElement( b );
		} else if ( numAtRank != 1 ) {
			winnerBuckets[numc+1].addElement( b );
			if ( disqualifyTiedVotes ) {
				// ignore the vote, add nothing to talley
			} else if ( duplicateTiedVotes ) {
				// add weight to talley for each of the tied rankees
				for ( i = mini; i < numc; i++ ) {
					if ( b.ranking[i] == min ) {
						talley[i] += b.weight;
					}
				}
			} else if ( splitTiedVotes ) {
				// add weight/numAtRank to talley for each of the tied rankees
				double dt = b.weight / numAtRank;
				for ( i = mini; i < numc; i++ ) {
					if ( b.ranking[i] == min ) {
						talley[i] += dt;
					}
				}
			}
		} else {
			talley[mini] += b.weight;
			winnerBuckets[mini].addElement( b );
		}
	}
	protected void intoBucket( Vector bv ) {
		for ( int i = 0; i < bv.size(); i++ ) {
			Ballot b;
			b = (Ballot)bv.elementAt(i);
			intoBucket( b );
		}
	}
	protected void intoBucket( Vector bv, double weightmod ) {
		for ( int i = 0; i < bv.size(); i++ ) {
			Ballot b;
			b = (Ballot)bv.elementAt(i);
			b.weight *= weightmod;
			intoBucket( b );
		}
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
				intoBucket( (Ballot)owb[i].elementAt( j ) );
			}
		}
	}
	public void recountBuckets() {
		int i, j;
		Ballot b;
		Vector cv;
		for ( i = 0; i < numc; i++ ) {
			cv = winnerBuckets[i];
			if ( cv == null ) continue;
			talley[i] = 0.0;
			for ( j = 0; j < cv.size(); j++ ) {
				b = (Ballot)cv.elementAt(j);
				talley[i] += b.weight;
			}
		}
		Vector oldTiedVotes = winnerBuckets[numc+1];
		winnerBuckets[numc+1] = new Vector();
		for ( i = 0; i < oldTiedVotes.size(); i++ ) {
			// old ties may have been broked by disqualification, redistribute
			intoBucket( (Ballot)(oldTiedVotes.elementAt( i )) );
		}
	}
	
	/**
	 * @param ranking An array int[N].
	 *	ranking[i] is the ranking of choice i.
	 *  rankings are 1 (most preferred) through N (least).
	 *  INT_MAX, N, or NO_VOTE for unspecified.
	 * @return 0 on success
	 */
	public int voteRanking( int ranking[] ) {
		winners = null;
		intoBucket( new Ballot( ranking ) );
		totalVotes++;
		return 0;
	}
	
	/**
	 Do processing if necessary, return winners.
	 It might be a good idea to implement getWinners(java.io.PrintWriter) and implement this to call it with null.
	 * @return indecies of winners (hopefully 1 of them)
	 */
	public int[] getWinners() {
		return null;
	}
	
	/**
	 Do processing if necessary, return winners.
	 Default implementation returns null so that not all systems have to be multi-seat systems.
	 * @param numSeats the number of seats available. 
	 * @return indecies of winners, hopefully numSeats of them but in case of ties there may be more and in case of some internal schism there may be fewer. Check .length to be sure.
	 */
	public int[] getWinners( int numSeats ) {
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		if ( debug ) {
			debugsb = new StringBuffer();
		}
		double max;
		double min;
		int numAtMin;
		int numAtMax;
		int loseri = numc - 1;	// where to store next loser
		int winneri = 0;		// where to store next winner
		int i;
		choiceIndecies = new int[numc];
		double winQuota = quota( totalVotes, numSeats );//(((double)totalVotes) / (numSeats+1.0)) + 1.0;
		resetBuckets();
		while ( winneri <= loseri ) {
			max = -1e9;
			min = 1e9;
			numAtMin = 0;
			numAtMax = 0;
			if ( debug ) {
				for ( i = 0; i < numc; i++ ) {
					debugsb.append( i );
					debugsb.append( ": " );
					if ( active[i] ) {
						debugsb.append("  active ");
					} else {
						debugsb.append("inactive ");
					}
					debugsb.append( talley[i] );
					debugsb.append( '\n' );
				}
			}
			for ( i = 0; i < numc; i++ ) {if ( active[i] ) {
				double si = talley[i];//winnerBuckets[i].size();
				if ( si > max ) {
					max = si;
					numAtMax = 1;
				} else if ( si == max ) {
					numAtMax++;
				}
				if ( si < min ) {
					min = si;
					numAtMin = 1;
				} else if ( si == min ) {
					numAtMin++;
				}
			}}
			if ( max >= winQuota ) {
				// we have winners, pass them, redistribute votes.
				int oldwinneri = winneri;
				double weightmod = 1.0 - (winQuota / max);
				for ( i = 0; i < numc; i++ ) {if ( active[i] ) {
					if ( talley[i] == max ) {
						choiceIndecies[winneri] = i;
						winneri++;
						active[i] = false;
						if ( debug ) {
							debugsb.append("index ").append(i).append(" passes quota ").append(winQuota).append(" with tally ").append(talley[i]).append('\n');
						}
					}
				}}
				// assert( winneri - oldwinneri == numAtMax )
				for ( i = oldwinneri; i < winneri; i++ ) {
					intoBucket( winnerBuckets[choiceIndecies[i]], weightmod );
					winnerBuckets[choiceIndecies[i]] = null;
				}
			} else {
				int oldloseri = loseri;
				for ( i = 0; i < numc; i++ ) if ( active[i] ) {
					//System.err.println( "talley[" + i + "] = " + talley[i] );
					if ( talley[i] == min ) {
						//System.err.println( i + "deactivated with tie for low score at "+min+", loseri "+loseri+", winneri " + winneri );
						//fprintf( stderr, "%d deactivated with tie for low score at %d, loseri %d, winneri %d\n", i, min, loseri, winneri );
						choiceIndecies[loseri] = i;
						loseri--;
						active[i] = false;
						if ( debug ) {
							debugsb.append("index ").append(i).append(" disqualified as loser with tally ").append(talley[i]).append('\n');
						}
					}
				}
				// assert( oldloseri - loseri == numAtMin )
				for ( i = loseri+1; i <= oldloseri; i++ ) {
					intoBucket( winnerBuckets[choiceIndecies[i]] );
					winnerBuckets[choiceIndecies[i]] = null;
				}
			}
			if ( winnerBuckets[numc+1].size() > 0 ) {
				// to not repeatedly count fractional votes which were added to lists they weren't in, fresh count for all.
				recountBuckets();
			}
		}
		if ( winneri != loseri ) {
			System.err.println( "IRV error: winneri "+winneri+", loseri "+loseri+"\n" );
		}
		// count winners (who ties with the last actual winner?)
		int numWinners;
		for ( numWinners = numSeats; (numWinners < talley.length) && (talley[choiceIndecies[numWinners-1]] == talley[choiceIndecies[numWinners]]); numWinners++ ) {}
		winners = new int[numWinners];
		for ( i = 0; i < numWinners; i++ ) {
			winners[i] = choiceIndecies[i];
		}
		return winners;
	}
	
	/**
	 * A more interesting representation.
	 * Include the names of the choices voted upon in the representation.
	 * @param names The names of the choices.
	 * @return state, with names!
	 */
	public String toString( String names[] ) {
		return toString();
	}
	
	/**
	 * A fancy html representation.
	 * Include the names of the choices voted upon in the representation.
	 * @param names The names of the choices. May be null.
	 * @return state, with names!
	 */
	public String htmlSummary( String names[] ) {
		StringBuffer toret;
		double max;
		boolean in[] = new boolean[numc];
		int maxi = 0, i;
		for ( i = 0; i < numc; i++ ) {
			in[i] = true;
		}
		if ( names != null ) {
			toret = new StringBuffer( "<table border=\"1\"><tr><th></th><th>STV Best Vote</th></tr>\n" );
		} else {
			toret = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>STV Best Vote</th></tr>\n" );
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
	
	public String name() {
		if ( disqualifyTiedVotes ) {
			return "Single Transferrable Vote (tied-ranking votes are disqualified)";
		} else if ( duplicateTiedVotes ) {
			return "Single Transferrable Vote (tied-ranking votes are duplicated)";
		} else if ( splitTiedVotes ) {
			return "Single Transferrable Vote (tied-ranking votes are split)";
		}
		return "Single Transferrable Vote (tied rankings are undefined)";
	}
}
