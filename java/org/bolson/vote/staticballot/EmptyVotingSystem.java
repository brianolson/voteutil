package org.bolson.vote.staticballot;
/**
 * Empty code for copying before making a new VotingSystem.
 @author Somebody
 */

public class EmptyVotingSystem extends VotingSystem {
    /** In addition to interfaced methods, there shall be a constructor taking 
     * the integer number N of choices to be voted on. */
    public EmptyVotingSystem( int numCandidates ) {
		super( numCandidates );
    };
	
	/**
	 * Arguments to modify behavior of EmptyVotingSystem
	 * @return this so that you can do
	 <pre>vs = (new VS(numc)).init( new String[]{"blah", "grunt"} );</pre>
	 */
	public VotingSystem init( String argv[] ) {
		// default impl, do nothing
		return super.init( argv );
	}
	
    /**
     * @param rating An array int[numc].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( int rating[] ) {
		return 1;
	}
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( float rating[] ) {
		return 1;
	}
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public int voteRating( double rating[] ) {
		return 1;
	}
    /**
     * @param ranking An array int[N].
     *	ranking[i] is the ranking of choice i.
     *  rankings are 1 (most preferred) through N (least).
     *  INT_MAX, N, or NO_VOTE for unspecified.
     * @return 0 on success
     */
    public int voteRanking( int ranking[] ) {
		return 1;
	}

    /**
		Do processing if necessary, return winners.
	 It might be a good idea to implement getWinners(java.io.PrintWriter) and implement this to call it with null.
     @return indecies of winners (hopefully 1 of them)
     */
    public int[] getWinners() {
		return null;
	}

	/**
		Do processing if necessary, return winners.
	 Default implementation returns null so that not all systems have to be multi-seat systems.
	 <p>It is recommended to cache the return value in such a way that calling this function again does not do complex recalculation.
	 That cache should be cleared if voteRating or voteRanking is called.
	 @param numSeats the number of seats available. 
	 @return indecies of winners, hopefully numSeats of them but in case of ties there may be more and in case of some internal schism there may be fewer. Check .length to be sure.
     */
    public int[] getWinners( int numSeats ) {
		return null;
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
		return toString();
	}

	public String name() {
		return toString();
	}
}
