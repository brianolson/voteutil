package org.bolson.vote;

/**
	Wrapper that makes all calls synchronized on the subordinate VotingSystem object.
 @author Brian Olson
*/
public class SynchronizedVSWrapper extends VotingSystem {
	VotingSystem wrapped;
	public SynchronizedVSWrapper( VotingSystem it ) {
		super( it.getNumberOfCandidates() );
		wrapped = it;
	}
    public int voteRating( int rating[] ) {
		synchronized ( wrapped ) {
			return wrapped.voteRating( rating );
		}
	}
    public int voteRating( float rating[] ) {
		synchronized ( wrapped ) {
			return wrapped.voteRating( rating );
		}
	}	
    public int voteRating( double rating[] ) {
		synchronized ( wrapped ) {
			return wrapped.voteRating( rating );
		}
	}
    public int voteRanking( int ranking[] ) {
		synchronized ( wrapped ) {
			return wrapped.voteRanking( ranking );
		}
	}
    public int[] getWinners() {
		synchronized ( wrapped ) {
			return wrapped.getWinners();
		}
	}
    public String toString( String names[] ) {
		synchronized ( wrapped ) {
			return wrapped.toString( names );
		}
	}
    public String htmlSummary( String names[] ) {
		synchronized ( wrapped ) {
			return wrapped.htmlSummary( names );
		}
	}
}
