package org.bolson.vote;

/**
 * Election method based on summation of Yes/No "good enough" votes.
 * This class contains utility code for thresholding data at various points.
 @author Brian Olson
 */
public class Approval extends VotingSystem {
	public Approval( int numCandidates ) {
		super( numCandidates );
		talley = new int[numc];
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0;
		}
	}
	
	/** threshold for voteRating(int[]) */
	public int ithresh = 0;
	/** threshold for voteRating(float[]) */
	public float fthresh = 0.0f;
	/** threshold for voteRating(double[]) */
	public double dthresh = 0.0;
	/** If true then determine thresholds dynamically based on the mean rating for a specific ballot.
		NO_VOTE/NaN values in the rating are not counted toward the mean. */
	boolean voteOverMeanRating = true;
	
	public String name() {
		return "Approval";
	}
	
	public int voteRating( int rating[] )  {
		int i;
		if ( voteOverMeanRating ) {
			int mean = 0;
			for ( i = 0; i < numc; i++ ) if ( rating[i] != NO_VOTE ) {
				mean += rating[i];
			}
			mean = mean / numc;
			for ( i = 0; i < numc; i++ ) if ( (rating[i] != NO_VOTE) && (rating[i] > mean) ) {
				talley[i]++;
			}
			return 0;
		}
		for ( i = 0; i < numc; i++ ) if ( (rating[i] != NO_VOTE) && (rating[i] > ithresh) ) {
			talley[i]++;
		}
		return 0;
	}
	public int voteRating( float rating[] )  {
		int i;
		if ( voteOverMeanRating ) {
			float mean = 0;
			for ( i = 0; i < numc; i++ ) if ( rating[i] != NO_VOTE ) {
				mean += rating[i];
			}
			mean = mean / numc;
			for ( i = 0; i < numc; i++ ) if ( (rating[i] != NO_VOTE) && (rating[i] > mean) ) {
				talley[i]++;
			}
			return 0;
		}
		for ( i = 0; i < numc; i++ ) if ( !Float.isNaN(rating[i]) && (rating[i] > fthresh) ) {
			talley[i]++;
		}
		return 0;
	}
	public int voteRating( double rating[] )  {
		int i;
		if ( voteOverMeanRating ) {
			double mean = 0;
			for ( i = 0; i < numc; i++ ) if ( rating[i] != NO_VOTE ) {
				mean += rating[i];
			}
			mean = mean / numc;
			for ( i = 0; i < numc; i++ ) if ( (rating[i] != NO_VOTE) && (rating[i] > mean) ) {
				talley[i]++;
			}
			return 0;
		}
		for ( i = 0; i < numc; i++ ) if ( !Double.isNaN(rating[i]) && (rating[i] > dthresh)) {
			talley[i]++;
		}
		return 0;
	}
	/**
	Choices above (ranked less than or equal to) "(numc+1) / 2" are voted as "approved".
	@param ranking int[numc]
	@return 0 on success
	*/
	public int voteRanking( int ranking[] ) {
		int thresh = (numc+1) / 2;
		for ( int i = 0; i < numc; i++ ) if ( ranking[i] != NO_VOTE ) {
			if ( ranking[i] <= thresh ) {
				talley[i]++;
			}
		}
		return 0;
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
	
	protected int talley[];
	
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
			sb = new StringBuffer( "<table border=\"1\"><tr><th></th><th>Approval Summation</th></tr>\n" );
		} else {
			sb = new StringBuffer( "<table border=\"1\"><tr><th>Choice Index</th><th>Approval Summation</th></tr>\n" );
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
		return sb.toString();
	}
}
