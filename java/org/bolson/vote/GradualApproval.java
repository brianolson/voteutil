package org.bolson.vote;
import java.util.Vector;
/**
 * experimental election method, probably souldn't be used.
 @author Brian Olson
 */

public class GradualApproval extends RatedVotingSystem {
	Vector votes = new Vector();
	int[] winners = null;

    /** In addition to interfaced methods, there shall be a constructor taking 
     * the integer number N of choices to be voted on. */
    public GradualApproval( int numCandidates ) {
		super( numCandidates );
    };
	
	/**
	 * Arguments to modify behavior of GradualApproval
	 * @return this so that you can do
	 <pre>vs = (new VS(numc)).init( new String[]{"blah", "grunt"} );</pre>
	 */
	public VotingSystem init( String argv[] ) {
		// default impl, do nothing
		return super.init( argv );
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
	
    /**
		Do processing if necessary, return winners.
	 It might be a good idea to implement getWinners(java.io.PrintWriter) and implement this to call it with null.
     @return indecies of winners (hopefully 1 of them)
     */
    public int[] getWinners() {
		if ( winners != null ) {
			return (int[])winners.clone();
		}
		for ( int v = 0; v < votes.size(); v++ ) {
			// TODO FIXME WRITEME
			/*Object o;
			o = votes.elementAt( v );
			if ( o instanceof int[] ) {
				int[] ot = (int[])o;
			} else if ( o instanceof float[] ) {
				float[] ot = (float[])o;
			} else if ( o instanceof double[] ) {
				double[] ot = (double[])o;
			}*/
		}
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
