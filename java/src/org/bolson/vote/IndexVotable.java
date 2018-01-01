package org.bolson.vote;

/**
This class should be implemented by something which also extends NameVotingSystem.
@see NameVotingSystem
*/
public interface IndexVotable extends ElectionMethod {
	/**
	Vote a set of ratings.
	@see NameVotingSystem#voteRating(NameVotingSystem.NameVote[])
	@param vote a set of (name,rating) pairs
	*/
	public void voteIndexVoteSet(NameVotingSystem.IndexVoteSet vote);
}
