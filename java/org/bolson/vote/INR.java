package org.bolson.vote;
import java.util.Vector;

/**
* Iterated Normalized Ratings
 I don't know how useful this is. Maybe it's always the same as straight rating summation. dunno.
 @author Brian Olson
 */
public class INR extends RatedVotingSystem {
	Vector votes = new Vector();
	int voterToTrack = -1;
	boolean showIntermedite = false;
	int[] winners = null;
	boolean rmsnorm = true;
	double inactiveThreshold = 0.05;
	double stepSize = 0.05;
	Vector debugSteps = null;

	public static String doubleToStrLen( double d, int l ) {
		String toret = Double.toString( d );
		if ( toret.length() > l ) {
			return toret.substring( 0, l );
		}
		return toret;
	}
    public INR( int numCandidates ) {
    	super( numCandidates );
		talley = new double[numc];
    }
	
	public String name() {
		return "Iterated Normalized Ratings";
	}
	
	public VotingSystem init( String argv[] ) {
		// default impl, do nothing
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
				System.err.println( "INR.init: bogus arg \"" + argv[i] + '"' );
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
		if ( debug ) {
			winners = null;
			debugSteps = new Vector();
		}
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		double max = Double.NEGATIVE_INFINITY;
		int i;
		int numWinners = 1;
		int numActive = numc;
		boolean active[] = new boolean[numc];
		//int choiceIndecies[] = new int[numc];
		double dt[] = new double[numc];
		double oneMinusStepSize = 1.0 - stepSize;
		double[] solution;
		int bored = 10000;
		solution = new double[numc];
		for ( int c = 0; c < numc; c++ ) {
			active[c] = true;
			solution[c] = 1.0;
		}
		while ( (numActive > 1) && (--bored > 0) ) {
			// per IR setup
			{
				int curact = 0;
				for ( int c = 0; c < numc; c++ ) {
					if ( active[c] ) {
						talley[c] = 0;
						//choiceIndecies[curact] = c;
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
							double otc;
							otc = ot[c] * solution[c];
							ts += otc * otc;
						} else {
							ts += Math.abs(ot[c] * solution[c]);
						}
					}
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					for ( int c = 0; c < numc; c++ ) if ( ot[c] != NO_VOTE && active[c] && ts != 0 ) {
						talley[c] += ot[c] * solution[c] / ts;
					}
				} else if ( o instanceof float[] ) {
					float[] ot = (float[])o;
					for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Float.isNaN( ot[c] ) ) {
						//System.out.println("float[] ot[" + c + "] = " + ot[c] );
						if ( rmsnorm ) {
							double otc;
							otc = ot[c] * solution[c];
							ts += otc * otc;
						} else {
							ts += Math.abs(ot[c] * solution[c]);
						}
					}
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Float.isNaN( ot[c] ) && ts != 0 ) {
						//System.out.println("float[] ot[" + c + "] = " + ot[c] + " / (ts = " + ts + ") = " + (ot[c] / ts) );
						talley[c] += ot[c] * solution[c] / ts;
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
							double otc;
							otc = ot[c] * solution[c];
							ts += otc * otc;
						} else {
							ts += Math.abs(ot[c] * solution[c]);
						}
					}
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					for ( int c = 0; c < numc; c++ ) if ( active[c] && ! Double.isNaN( ot[c] ) && ts != 0 ) {
						talley[c] += ot[c] * solution[c] / ts;
					}
				} else {
					System.err.println( "bogus vote \"" + o + '\"' );
				}
			}
			// find new solution[]
			double tsum;
			tsum = 0.0;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				tsum += talley[i];
			}
			tsum /= numActive;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				solution[i] = ((talley[i] - tsum) * stepSize) + solution[i];
				if ( solution[i] < inactiveThreshold ) {
					active[i] = false;
					numActive--;
				}
			}
			if ( debug ) {
				debugSteps.add( talley.clone() );
				debugSteps.add( solution.clone() );
			}
		}
		if ( numActive == 0 ) {
			winners = new int[numc];
			for ( i = 0; i < numc; i++ ) {
				winners[i] = i;
			}
		} else {
			winners = new int[numActive];
			int winneri = 0;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				winners[winneri] = i;
				winneri++;
			}
		}
		return (int[])winners.clone();
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
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>INR Rating Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>INR Rating Summation</th></tr>\n" );
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
		if ( ! debug || debugSteps == null ) {
			return null;
		}
		StringBuffer toret = new StringBuffer( 4096 );
		int numSteps = debugSteps.size() / 2;
		double[][] tallies = new double[numSteps][];
		double[][] solutions = new double[numSteps][];
		int i, j;
		for ( i = 0; i < numSteps; i++ ) {
			tallies[i] = (double[])debugSteps.elementAt( i*2 );
			solutions[i] = (double[])debugSteps.elementAt( i*2 + 1 );
		}
		toret.append("<TABLE BORDER=\"1\"><TR>");
		for ( j = 0; j < numc; j++ ) {
			toret.append("<TH>");
			if ( names != null ) {
				toret.append( names[j] );
			}
			toret.append("</TH>");
		}
		toret.append("</TR>");
		for ( i = 0; i < numSteps; i++ ) {
			toret.append("<TR>");
			double[] talley;
			double[] solution;
			talley = tallies[i];
			solution = solutions[i];
			for ( int c = 0; c < numc; c++ ) {
				toret.append("<TD>");
				if ( solution[c] < inactiveThreshold ) {
					toret.append("<FONT COLOR=\"#999999\">");
				}
				toret.append( doubleToStrLen( talley[c], 6 ) );
				toret.append("<br>");
				toret.append( doubleToStrLen( solution[c], 6 ) );
				if ( solution[c] < inactiveThreshold ) {
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
