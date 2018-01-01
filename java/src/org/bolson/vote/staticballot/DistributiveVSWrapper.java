package org.bolson.vote.staticballot;

/**
Utility class for running several VotingSystem instances at once.
 @author Brian Olson
 */
public class DistributiveVSWrapper extends VotingSystem {
	/**
	 * add VotingSystem-s to this and they will get all votes.
	 */
	public java.util.Vector they = new java.util.Vector();
	/**
	 * which one of they gets returned for getWinners, toString, htmlSummary.
	 * default to zero.
	 * it would probably be better if you kept track of the VotingSystem-s going into
	 * this and got results from them separately.
	 */
	public int primary = 0;
	public DistributiveVSWrapper( int numcIn ) {
		super( numcIn );
	}
    public int voteRating( int rating[] ) {
		int toret = 0;
		for ( int i = 0; i < they.size(); i++ ) {
			toret += ((VotingSystem)(they.elementAt(i))).voteRating( rating );
		}
		return toret;
	}
    public int voteRating( float rating[] ) {
		int toret = 0;
		for ( int i = 0; i < they.size(); i++ ) {
			toret += ((VotingSystem)(they.elementAt(i))).voteRating( rating );
		}
		return toret;
	}	
    public int voteRating( double rating[] ) {
		int toret = 0;
		for ( int i = 0; i < they.size(); i++ ) {
			toret += ((VotingSystem)(they.elementAt(i))).voteRating( rating );
		}
		return toret;
	}
    public int voteRanking( int ranking[] ) {
		int toret = 0;
		for ( int i = 0; i < they.size(); i++ ) {
			toret += ((VotingSystem)(they.elementAt(i))).voteRanking( ranking );
		}
		return toret;
	}
    public int[] getWinners() {
		return ((VotingSystem)(they.elementAt(primary))).getWinners();
	}
    public String toString( String names[] ) {
		return ((VotingSystem)(they.elementAt(primary))).toString( names );
	}
    public String htmlSummary( String names[] ) {
		return ((VotingSystem)(they.elementAt(primary))).htmlSummary( names );
	}
}
