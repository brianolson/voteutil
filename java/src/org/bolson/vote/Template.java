package org.bolson.vote;

/**
 Empty source for you to copy for making a new election method implementation.
 @author Somebody
 */
public class Template extends NameVotingSystem {
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
		return "Template";
	}
};
