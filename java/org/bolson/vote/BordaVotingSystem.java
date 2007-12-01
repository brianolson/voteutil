package org.bolson.vote;

/**
	Points-based-on-rank election method.
*/
public class BordaVotingSystem extends RankedVotingSystem {
	protected int talley[];
	boolean equalUnrankDistrib = false;// FIXME, implement
	public BordaVotingSystem( int numCandidates ) {
		super( numCandidates );
		talley = new int[numc];
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0;
		}
	}
	
	public int voteRanking( int ranking[] ) {
		int i;
		for ( i = 0; i < numc; i++ ) {
			if ( ranking[i] == Integer.MAX_VALUE ) {
				// unranked, give no points
				/* CHECK_VALUES */
			} else if ( (ranking[i] < 1) || (ranking[i] > numc) ) {
				return -1;
			} else {
				talley[i] = talley[i] + numc - ranking[i];
			}
		}
		return 0;
	}
	
	public String name() {
		return "Borda Point Summation";
	}
	
	public int[] getWinners() {
		int max = Integer.MIN_VALUE;
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
	
	public String htmlSummary( String names[] ) {
		StringBuffer sb;
		int max;
		boolean in[] = new boolean[numc];
		int maxi = 0,i;
		for ( i = 0; i < numc; i++ ) {
			in[i] = true;
		}
		if ( names != null ) {
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>Borda Point Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>Borda Point Summation</th></tr>\n" );
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
	
	public String toString( String names[] ) {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < talley.length; i++ ) {
			sb.append( names[i] ).append('\t');
			sb.append( talley[i] ).append('\n');
		}
		return sb.toString();
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < talley.length; i++ ) {
			sb.append( talley[i] ).append('\n');
		}
		return sb.toString();
	}
};
