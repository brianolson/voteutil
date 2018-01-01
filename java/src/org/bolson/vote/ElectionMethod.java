package org.bolson.vote;

public interface ElectionMethod {
	public String getDebug();
	public int init(String[] argv);
	public void voteRating(NameVotingSystem.NameVote[] vote);
	public NameVotingSystem.NameVote[] getWinners();
	public StringBuffer htmlSummary(StringBuffer html);
	public String name();
	public StringBuffer htmlExplain(StringBuffer html);
}
