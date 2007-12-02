package org.bolson.vote;

/**
 * Raw summation of voter's ratings.
 * External limits must be established.
 @author Brian Olson
 */
public class RawRating extends RatedVotingSystem {
	public RawRating( int numCandidates ) {
		super( numCandidates );
		talley = new double[numc];
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0;
		}
	}
	boolean normalize = false;
	boolean normalizeL2 = false;
	boolean stretch = false;
	
	public String name() {
		return "Raw Rating Summation";
	}
	
	public int voteRating( int rating[] )  {
		int i;
		for ( i = 0; i < numc; i++ ) if ( rating[i] != NO_VOTE ) {
			talley[i] += rating[i];
		}
		return 0;
	}
	public int voteRating( float rating[] )  {
		int i;
		for ( i = 0; i < numc; i++ ) if ( !Float.isNaN(rating[i]) ) {
			talley[i] += rating[i];
		}
		return 0;
	}
	public int voteRating( double rating[] )  {
		int i;
		for ( i = 0; i < numc; i++ ) if ( !Double.isNaN(rating[i]) ) {
			talley[i] += rating[i];
		}
		return 0;
	}
	
	public int[] getWinners() {
		double max = Double.NEGATIVE_INFINITY;
		int i;
		int numWinners = 0;
		int choiceIndecies[] = new int[numc];
		for ( i = 0; i < numc; i++ ) {
			if ( talley[i] > max ) {
				choiceIndecies[0] = i;
				numWinners = 1;
				max = talley[i];
			} else if ( talley[i] == max ) {
				choiceIndecies[numWinners] = i;
				numWinners++;
			}
		}
		int winners[] = new int[numWinners];
		for ( i = 0; i < numWinners; i++ ) {
			winners[i] = choiceIndecies[i];
		}
		return winners;
	}
	
	
	void squashToAcceptance( int rating[] ) {
		for ( int i = 0; i < numc; i++ ) {
			if ( rating[i] >= 0 ) {
				rating[i] = 1;
			} else {
				rating[i] = 0;
			}
		}
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
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>Raw Rating Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>Raw Rating Summation</th></tr>\n" );
		}
		while ( true ) {
			boolean done;
			max = Double.NEGATIVE_INFINITY;
			done = true;
			for ( i = 0; i < numc; i++ ) {
				if ( in[i] && (talley[i] > max) ) {
					done = false;
					maxi = i;
					max = talley[i];
				}
			}
			if ( done ) {
				//sb.append(max);
				break;
			}
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
		/*for ( i = 0; i < numc; i++ ) {
		 sb.append( in[i] ).append(' ').append( talley[i] ).append(", ");
		 }*/
		return sb.toString();
	}
}
