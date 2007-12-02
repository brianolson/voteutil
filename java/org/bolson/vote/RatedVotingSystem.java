package org.bolson.vote;
/**
 * Interface for any voting system that inputs a voter's ratings of the choices.
 * Supplies implementations of voteRanking which call voteRating
 @author Brian Olson
 */

public abstract class RatedVotingSystem extends VotingSystem {
	/* In addition to interfaced methods, there shall be a constructor taking 
	 * the integer number N of choices to be voted on. */
	public RatedVotingSystem( int numCandidates ) {
		super( numCandidates );
	};
	
	/**
	 * Convert to Rating and pass to voteRating();
	 * @param ranking An array int[numc].
	 *	ranking[i] is the ranking of choice i.
	 */
	public int voteRanking( int ranking[] ) {
		if ( ranking == null || ranking.length == 0 ) {
			return 0;
		}
		int rating[] = new int[numc];
		int i;
		for ( i = 0; i < numc; i++ ) {
			if ( ranking[i] == NO_VOTE ) {
				rating[i] = NO_VOTE;
			} else if ( ranking[i] == Integer.MAX_VALUE ) {
				rating[i] = 0; // unranked -> unrated
			} else {
				rating[i] = numc - ranking[i];
			}
		}
		return voteRating( rating );
	}
	public int voteRating( int rating[] )  {
		float nr[] = new float[numc];
		for ( int i = 0; i < numc; i++ ) {
			nr[i] = (float)rating[i];
		}
		return voteRating( nr );
	}
	public int voteRating( double rating[] )  {
		float nr[] = new float[numc];
		for ( int i = 0; i < numc; i++ ) {
			nr[i] = (float)rating[i];
		}
		return voteRating( nr );
	}
};

