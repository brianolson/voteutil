package org.bolson.vote;
/**
 * Interface for any voting system that inputs a voter's rankings of the choices.
 * Supplies implementations of voteRating which call voteRanking
 @author Brian Olson
 */

public abstract class RankedVotingSystem extends VotingSystem {
	/* In addition to interfaced methods, there shall be a constructor taking 
	 * the integer number N of choices to be voted on. */
	public RankedVotingSystem( int numCandidates ) {
		super( numCandidates );
	};
	
	/**
	 * Converts rating to ranking and passes to voteRanking.
	 * @param rating An array int[numc].
	 *	rating[i] is the rating of choice i. greater values are better.
	 */
	public int voteRating( int rating[] )  {
		int ci[] = new int[numc], rank[] = new int[numc];
		int i;
		boolean done = false;
		for ( i = 0; i < numc; i++ ) {
			ci[i] = i;
		}
		while ( ! done ) {
			done = true;
			for ( i = 0; i < numc - 1; i++ ) {
				if ( rating[ci[i]] < rating[ci[i+1]] ) {
					int t;
					t = ci[i];
					ci[i] = ci[i+1];
					ci[i+1] = t;
					done = false;
				}
			}
		}
		for ( i = 0; i < numc; i++ ) {
			rank[ci[i]] = i + 1;
			while ( ((i+1) < numc) && (rating[ci[i]] == rating[ci[i+1]]) ) {
				rank[ci[i+1]] = rank[ci[i]];
				i++;
			}
		}
		return voteRanking( rank );
	}
	public int voteRating( float rating[] )  {
		int ci[] = new int[numc], rank[] = new int[numc];
		int i;
		boolean done = false;
		for ( i = 0; i < numc; i++ ) {
			ci[i] = i;
		}
		while ( ! done ) {
			done = true;
			for ( i = 0; i < numc - 1; i++ ) {
				if ( rating[ci[i]] < rating[ci[i+1]] ) {
					int t;
					t = ci[i];
					ci[i] = ci[i+1];
					ci[i+1] = t;
					done = false;
				}
			}
		}
		for ( i = 0; i < numc; i++ ) {
			rank[ci[i]] = i + 1;
		}
		return voteRanking( rank );
	}
	public int voteRating( double rating[] )  {
		int ci[] = new int[numc], rank[] = new int[numc];
		int i;
		boolean done = false;
		for ( i = 0; i < numc; i++ ) {
			ci[i] = i;
		}
		while ( ! done ) {
			done = true;
			for ( i = 0; i < numc - 1; i++ ) {
				if ( rating[ci[i]] < rating[ci[i+1]] ) {
					int t;
					t = ci[i];
					ci[i] = ci[i+1];
					ci[i+1] = t;
					done = false;
				}
			}
		}
		for ( i = 0; i < numc; i++ ) {
			rank[ci[i]] = i + 1;
		}
		return voteRanking( rank );
	}
	
};
