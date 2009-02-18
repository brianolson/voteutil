package org.bolson.vote.staticballot;
import java.util.Vector;

/**
 * Instant Runoff Normalized Ratings Proportional
 * @see org.bolson.vote.IRNR
 @author Brian Olson
 */

public class IRNRP extends RatedVotingSystem {
	Vector votes = new Vector();
	int voterToTrack = -1;
	boolean showIntermedite = false;
	int[] winners = null;
	boolean rmsnorm = true;
	boolean bolsonQuota = false;
    protected double talley[];
	int[] doneIndecies = null;
	StringBuffer debugsb = null;

    /** In addition to interfaced methods, there shall be a constructor taking 
     * the integer number N of choices to be voted on. */
    public IRNRP( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
    };
	
	/**
	 * Arguments to modify behavior of IRNRP
	 * @return this so that you can do
	 <pre>vs = (new VS(numc)).init( new String[]{"blah", "grunt"} );</pre>
	 */
	public VotingSystem init( String argv[] ) {
		if ( argv == null ) return this;
		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i].equals( "track" ) ) {
				i++;
				voterToTrack = Integer.parseInt( argv[i] );
			} else if ( argv[i].equals( "show" ) ) {
				showIntermedite = true;
			} else if ( argv[i].equals( "rmsnorm" ) || argv[i].equals( "l2norm" ) || argv[i].equals( "sphericalNorm" ) ) {
				rmsnorm = true;
			} else if ( argv[i].equals( "linearNorm" ) || argv[i].equals( "l1norm" ) || argv[i].equals( "manhattanNorm" ) ) {
				rmsnorm = false;
			} else if ( argv[i].equals( "bolson" ) ) {
				bolsonQuota = true;
//			} else if ( argv[i].equals( "" ) ) {
			} else {
				System.err.println( "IRNR.init: bogus arg \"" + argv[i] + '"' );
				return this;
			}
		}
		return this;
	}
	
    /**
     * @param rating An array int[numc].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( int rating[] ) {
		votes.add( rating.clone() );
		winners = null;
		return 0;
	}
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( float rating[] ) {
		votes.add( rating.clone() );
		winners = null;
		return 0;
	}
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( double rating[] ) {
		votes.add( rating.clone() );
		winners = null;
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

	protected void getScaledDoubleVote( double[] dt, Object o, double weight, boolean[] active ) {
		double ts = 0.0;
		if ( o instanceof int[] ) {
			int[] ot = (int[])o;
			for ( int c = 0; c < numc; c++ ) if ( ot[c] != NO_VOTE && active[c] ) {
				if ( debug && false ) {
					debugsb.append("int[] ot[").append( c ).append("] = ").append(ot[c]).append('\n');
				}
				if ( rmsnorm ) {
					ts += ot[c] * ot[c];
				} else {
					ts += Math.abs(ot[c]);
				}
			}
			if ( rmsnorm ) {
				ts = Math.sqrt( ts );
			}
			for ( int c = 0; c < numc; c++ ) if ( ot[c] != NO_VOTE && active[c] && ts != 0 ) {
				dt[c] = weight * ot[c] / ts;
			}
		} else if ( o instanceof float[] ) {
			float[] ot = (float[])o;
			for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Float.isNaN( ot[c] ) ) {
				if ( debug ) {
					debugsb.append("float[] ot[").append( c ).append("] = ").append(ot[c]).append('\n');
				}
				if ( rmsnorm ) {
					ts += ot[c] * ot[c];
				} else {
					ts += Math.abs(ot[c]);
				}
			}
			if ( rmsnorm ) {
				ts = Math.sqrt( ts );
			}
			for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Float.isNaN( ot[c] ) && ts != 0 ) {
				if ( debug ) {
					debugsb.append("float[] ot[").append( c ).append("] = ").append(ot[c]).append(" / (ts = ").append(ts).append(") = ").append(ot[c] / ts).append('\n');
				}
				dt[c] = weight * ot[c] / ts;
			}
		} else if ( o instanceof double[] ) {
			double[] ot = (double[])o;
			for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Double.isNaN( ot[c] ) ) {
				if ( debug ) {
					debugsb.append("double[] ot[").append( c ).append("] = ").append(ot[c]).append('\n');
				}
				if ( rmsnorm ) {
					ts += ot[c] * ot[c];
				} else {
					ts += Math.abs(ot[c]);
				}
			}
			if ( rmsnorm ) {
				ts = Math.sqrt( ts );
			}
			for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Double.isNaN( ot[c] ) && ts != 0 ) {
				dt[c] = weight * ot[c] / ts;
			}
		} else {
			System.err.println( "bogus vote \"" + o + '\"' );
		}
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
		int i;
		int numActive = numc;
		boolean active[] = new boolean[numc];
		int choiceIndecies[] = new int[numc];
		double dt[] = new double[numc];
		double winQuota;
		int numv = votes.size();
		double[] ballotWeights = new double[numv];
		int loseri = numc - 1;	// where to store next loser
		int winneri = 0;		// where to store next winner
		doneIndecies = new int[numc];
		double totalVote;

		for ( int c = 0; c < numc; c++ ) {
			active[c] = true;
		}
		for ( i = 0; i < ballotWeights.length; i++ ) {
			ballotWeights[i] = 1.0;
		}
		while ( winneri <= loseri ) {
			// per IR setup
		{
			int curact = 0;
			for ( int c = 0; c < numc; c++ ) {
				if ( active[c] ) {
					talley[c] = 0;
					choiceIndecies[curact] = c;
					curact++;
				}
			}
		}
			totalVote = 0.0;
			// sum up into talley
			for ( int v = 0; v < votes.size(); v++ ) {
				getScaledDoubleVote( dt, votes.elementAt( v ), ballotWeights[v], active );
				for ( int c = 0; c < numc; c++ ) {
					if ( active[c] ) {
						talley[c] += dt[c];
						totalVote += dt[c];
					}
				}
				if ( v == voterToTrack && debug ) {
					for ( int c = 0; c < numc; c++ ) {
						if ( active[c] ) {
							debugsb.append( dt[c] );
							debugsb.append( '\t' );
						} else {
							debugsb.append( "disabled = 0\t" );
						}
					}
					debugsb.append('\n');
				}
			}
			if ( bolsonQuota ) {
				winQuota = (totalVote / ((numSeats-winneri)+1.0)) + 1.0;
			} else {
				winQuota = totalVote / (numSeats-winneri);
			}
			if ( debug ) {
				for ( int c = 0; c < numc; c++ ) {
					if ( ! active[c] ) {
						debugsb.append('x');
					}
					debugsb.append( talley[c] );
					debugsb.append( '\t' );
				}
				debugsb.append("totalVote ");
				debugsb.append(totalVote);
				debugsb.append("\tquota ");
				debugsb.append(winQuota);
				debugsb.append('\n');
			}
			// sort
			boolean notdone = true;
			while ( notdone ) {
				notdone = false;
				for ( int c = 1; c < numActive; c++ ) {
					if ( talley[choiceIndecies[c]] > talley[choiceIndecies[c-1]] ) {
						int ti = choiceIndecies[c];
						choiceIndecies[c] = choiceIndecies[c-1];
						choiceIndecies[c-1] = ti;
						notdone = true;
					}
				}
			}
			if ( showIntermedite && debug ) {
				debugsb.append("num active ");
				debugsb.append( numActive );
				debugsb.append('\n');
				for ( int c = 0; c < numc; c++ ) {
					int tci;
					tci = choiceIndecies[c];
					debugsb.append("\tchoiceIndecies[").append(c).append("] = ").append(tci).append(
"\ttalley[").append(tci).append("] = ").append(talley[tci]).append(
"\tactive[").append(tci).append("] = ").append(active[tci]).append('\n');
				}
			}
			if ( talley[choiceIndecies[0]] > winQuota ) {
				int winner = choiceIndecies[0];
				// ? scale weight based on original or current vote for winner?
				// going with "current" for now.

				// scaling factor based on overvote
				double weightmod = 1.0 - (winQuota / talley[winner]);

				for ( int v = 0; v < votes.size(); v++ ) {
					getScaledDoubleVote( dt, votes.elementAt( v ), ballotWeights[v], active );
					// scale down in proportion to this ballot's vote for this winner and this winner's overvote
					ballotWeights[v] *= weightmod * dt[winner];
				}
				doneIndecies[winneri] = winner;
				active[winner] = false;
				winneri++;
				numActive--;
				if ( debug ) {
					debugsb.append( winner).append(" beats quota ").append(winQuota).append(" with tally ").append(talley[winner]).append('\n');
				}
			} else {
				// disqualify loser
				int loser = choiceIndecies[numActive-1];
				active[loser] = false;
				numActive--;
				doneIndecies[loseri] = loser;
				loseri--;
				if ( debug ) {
					debugsb.append( loser).append(" disqualified with tally ").append(talley[loser]).append('\n');
				}
				while ( (numActive > 1) && false &&
						(talley[choiceIndecies[numActive-1]] == talley[choiceIndecies[numActive]]) ) {
					// eliminate all who tied for last
					active[choiceIndecies[numActive-1]] = false;
					numActive--;
				}
			}
			if ( false ) {
				// old code
			if ( talley[choiceIndecies[0]] == talley[choiceIndecies[numActive-1]] ) {
				break;
			}
			}
		}
		winners = new int[numSeats];
		for ( i = 0; i < numSeats; i++ ) {
			winners[i] = doneIndecies[i];
		}
		return (int[])winners.clone();
    }
	
    /**
     * A more interesting representation.
     * Include the names of the choices voted upon in the representation.
     * @param names The names of the choices.
     * @return state, with names!
     */
    public String toString( String names[] ) {
		return super.toString();
	}
			
	public String getDebugText( String names[] ) {
		if ( ! debug ) return null;
		return debugsb.toString();
	}

    /**
     * A fancy html representation.
     * Include the names of the choices voted upon in the representation.
     * @param names The names of the choices. May be null.
     * @return state, with names!
     */
    public String htmlSummary( String names[] ) {
		StringBuffer sb;
		boolean in[] = new boolean[numc];
		int i;
		for ( i = 0; i < numc; i++ ) {
			in[i] = true;
		}
		if ( names != null ) {
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>IRNR Rating Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>IRNR Rating Summation</th></tr>\n" );
		}
		getWinners();
		/*
		 int maxi = 0;
		 while ( true ) {
			boolean done;
			max = Double.NEGATIVE_INFINITY;
			done = true;
			for ( i = 0; i < numc; i++ ) {
				if ( in[i] ) {
					if ( (talley[i] > max) || done ) {
						done = false;
						maxi = i;
						max = talley[i];
					}
				}
			}
			if ( done ) break;
			i = maxi;
			in[i] = false;*/
		for ( int di = 0; di < doneIndecies.length; di++ ) {
			i = doneIndecies[di];
			sb.append( "<tr><td>" );
			if ( names != null ) {
				sb.append( names[i] );
			} else {
				sb.append( i+1 );
			}
			sb.append( "</td><td>" );
			sb.append( talley[i] );
			sb.append( "</td></tr>\n" );
		}
		sb.append( "</table>\n" );
		return sb.toString();
	}

	public String name() {
		StringBuffer toret = new StringBuffer( "Instant Runoff Normalized Ratings" );
		if ( bolsonQuota ) {
			toret.append(", quota=(totalVote/(seatsOpen+1.0)) + 1.0" );
		} else {
			toret.append(", quota=totalVote/seatsOpen" );
		}
		if ( rmsnorm ) {
			toret.append(", rms normalized");
		} else {
			toret.append(", normalized");
		}
		return toret.toString();
	}
}
