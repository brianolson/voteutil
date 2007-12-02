package org.bolson.vote;
import java.util.Vector;

/**
 * Instant Runoff Normalized Ratings
 @author Brian Olson
 */
public class IRNR extends RatedVotingSystem {
	Vector votes = new Vector();
	int voterToTrack = -1;
	boolean showIntermedite = false;
	int[] winners = null;
	boolean rmsnorm = true;
	Vector debugHistory = null;
	StringBuffer debugStr = null;

    public IRNR( int numCandidates ) {
    	super( numCandidates );
		talley = new double[numc];
    }
	
	public String name() {
		return "Instant Runoff Normalized Ratings";
	}
	
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
//			} else if ( argv[i].equals( "" ) ) {
			} else {
				System.err.println( "IRNR.init: bogus arg \"" + argv[i] + '"' );
				return this;
			}
		}
		return this;
	}

    public int voteRating( int rating[] )  {
		votes.add( rating.clone() );
		winners = null;
		return 0;
    }
    public int voteRating( float rating[] )  {
		votes.add( rating.clone() );
		winners = null;
		return 0;
    }
    public int voteRating( double rating[] )  {
		votes.add( rating.clone() );
		winners = null;
		return 0;
    }
    
    public int[] getWinners() {
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		double max = Double.NEGATIVE_INFINITY;
		int i;
		int numWinners = 1;
		int numActive = numc;
		boolean active[] = new boolean[numc];
		int choiceIndecies[] = new int[numc];
		double dt[] = new double[numc];
		for ( int c = 0; c < numc; c++ ) {
			active[c] = true;
		}
		if ( debug ) {
			debugHistory = new Vector();
			debugStr = new StringBuffer();
		}
		while ( numActive > 1 ) {
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
			// sum up into talley
			for ( int v = 0; v < votes.size(); v++ ) {
				Object o;
				double ts;
				ts = 0.0;
				o = votes.elementAt( v );
				if ( o instanceof int[] ) {
					int[] ot = (int[])o;
					for ( int c = 0; c < numc; c++ ) if ( ot[c] != NO_VOTE && active[c] ) {
						//System.out.println("int[] ot[" + c + "] = " + ot[c] );
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
						talley[c] += ot[c] / ts;
					}
				} else if ( o instanceof float[] ) {
					float[] ot = (float[])o;
					for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Float.isNaN( ot[c] ) ) {
						//System.out.println("float[] ot[" + c + "] = " + ot[c] );
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
						//System.out.println("float[] ot[" + c + "] = " + ot[c] + " / (ts = " + ts + ") = " + (ot[c] / ts) );
						talley[c] += ot[c] / ts;
					}
					if ( v == voterToTrack ) {
						for ( int c = 0; c < numc; c++ ) {
							if ( active[c] ) {
								System.out.print( ot[c] + " / " + ts + " = " + (ot[c] / ts) + '\t' );
							} else {
								System.out.print( ot[c] + " disabled = 0\t" );
							}
						}
						System.out.println();
					}
				} else if ( o instanceof double[] ) {
					double[] ot = (double[])o;
					for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Double.isNaN( ot[c] ) ) {
						//System.out.println("double[] ot[" + c + "] = " + ot[c] );
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
						talley[c] += ot[c] / ts;
					}
				} else {
					System.err.println( "bogus vote \"" + o + '\"' );
				}
			}
			if ( debug ) {
				debugHistory.add( active.clone() );
				debugHistory.add( talley.clone() );
				for ( int c = 0; c < numc; c++ ) {
					if ( ! active[c] ) {
						debugStr.append('x');
					}
					debugStr.append( talley[c] );
					if ( (c + 1) < numc ) {
						debugStr.append( "\t" );
					}
				}
				debugStr.append('\n');
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
			if ( showIntermedite ) {
				System.out.println("num active " + numActive );
				for ( int c = 0; c < numc; c++ ) {
					int tci;
					tci = choiceIndecies[c];
					System.out.println("\tchoiceIndecies[" + c + "] = " + tci +
									   "\ttalley[" + tci + "] = " + talley[tci] +
									   "\tactive[" + tci + "] = " + active[tci] );
				}
			}
			if ( talley[choiceIndecies[0]] == talley[choiceIndecies[numActive-1]] ) {
				// N-way tie.
				numWinners = numActive;
				break;
			}
			active[choiceIndecies[numActive-1]] = false;
			numActive--;
			while ( (numActive > 1) &&
					(talley[choiceIndecies[numActive-1]] == talley[choiceIndecies[numActive]]) ) {
				// eliminate all who tied for last
				active[choiceIndecies[numActive-1]] = false;
				numActive--;
			}
		}
		winners = new int[numWinners];
		for ( i = 0; i < numWinners; i++ ) {
			winners[i] = choiceIndecies[i];
		}
		return (int[])winners.clone();
    }
	
	/** 
	Multi-seat IRNR.
	Everyone who votes for a winner shares in the distribution of the overvote and retains that fraction of their voting power.*/
	public int[] getWinners( /*java.io.PrintWriter out, */int numSeats ) {
		return null;
	}

    protected double talley[];
    
    public String toString() {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < talley.length; i++ ) {
			sb.append( talley[i] ).append('\n');
		}
		return sb.toString();
    }
	
    public String toString( String names[] ) {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < talley.length; i++ ) {
			sb.append( names[i] ).append('\t');
			sb.append( talley[i] ).append('\n');
		}
		return sb.toString();
    }
	
    public String htmlSummary( String names[] ) {
		StringBuffer sb;
		double max;
		boolean in[] = new boolean[numc];
		int maxi = 0,i;
		for ( i = 0; i < numc; i++ ) {
			in[i] = true;
		}
		if ( names != null ) {
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>IRNR Rating Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>IRNR Rating Summation</th></tr>\n" );
		}
		getWinners();
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
			in[i] = false;
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
	
	public String getDebugHTML( String[] names ) {
		int i = 0, j;
		if ( ! debug || debugHistory == null ) {
			return null;
		}
		int rounds = debugHistory.size() / 2;
		boolean[][] actives = new boolean[rounds][];
		double[][] tallies = new double[rounds][];
		while ( i < rounds ) {
			//boolean[] active;
			//double[] talley;
			actives[i] = (boolean[])debugHistory.elementAt( i * 2 );
			tallies[i] = (double[]) debugHistory.elementAt( i * 2 + 1 );
			i++;
		}
		StringBuffer toret = new StringBuffer( 2048 );
		toret.append("<pre>");
		toret.append(debugStr);
		toret.append("</pre>");
		toret.append("<TABLE BORDER=\"1\"><TR><TH></TH>");
		for ( j = 0; j < rounds; j++ ) {
			toret.append("<TH>").append(j).append("</TH>");
		}
		toret.append("</TR>");
		for ( i = 0; i < numc; i++ ) {
			toret.append("<TR>");
			toret.append("<TD>");
			toret.append(names[i]);
			toret.append("</TD>");
			for ( j = 0; j < rounds; j++ ) {
				toret.append("<TD>");
				if ( ! actives[j][i] ) {
					toret.append("<FONT COLOR=\"#999999\">");
				}
				toret.append( tallies[j][i] );
				if ( ! actives[j][i] ) {
					toret.append("</FONT>");
				}
				toret.append("</TD>");
			}
			toret.append("</TR>");
		}
		toret.append("</TABLE>");
		return toret.toString();
	}
};
