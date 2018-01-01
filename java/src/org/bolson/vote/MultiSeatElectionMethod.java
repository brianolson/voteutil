package org.bolson.vote;

/**
 Use this class to tag election method implementations that can sensibly elect multiple winners.
@author Brian Olson
*/
interface MultiSeatElectionMethod {
	/**
	 How many winners will we target when running getWinners() ?
	 @param seats number of openings to fill from this set of choices and votes.
	 @see org.bolson.vote.NameVotingSystem.getWinners()
	 */
	void setNumSeats(int seats);
}
