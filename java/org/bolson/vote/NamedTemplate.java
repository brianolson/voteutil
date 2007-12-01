package org.bolson.vote;

public class NamedTemplate extends NameVotingSystem {
	/* public int init( String[] argv ) {
	} */
	public void voteRating( NameVote[] vote ) {
		
	}
	public NameVote[] getWinners() {
		return null;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		return sb;
	}
	public String name() {
		return "NamedTemplate";
	}
};
