package org.bolson.vote;

/**
A voting system that can add up results from partial counts of the total vote.
 For example, precincts send their partial sum to a central tabulating location which produces the final result.
 SummableVotingSystem implementations should almost certainly also implement Serializable.
 @author Brian Olson
 */
public interface SummableVotingSystem {
	/** Vote a partial tally into this voting system.
	@param other another SummableVotingSystem. Most likely it will only work if it is the same class as this.
	@throws ClassCastExepcion if other isn't the same or compatible with this
	*/
	public void accumulateSubVote( SummableVotingSystem other ) throws ClassCastException;
}
