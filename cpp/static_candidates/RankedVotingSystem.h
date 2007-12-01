#ifndef RANKED_VOTING_SYSTEM_H
#define RANKED_VOTING_SYSTEM_H
/**
Interface for any voting system that inputs a voter's rankings of the choices.
Possible implementations: Borda count, Condorcet, IRV (sucks).
*/

#include "VotingSystem.h"

class RankedVotingSystem : public VotingSystem {
public:
	/* In addition to interfaced methods, there shall be a constructor taking
	* the integer number N of choices to be voted on. */
	RankedVotingSystem( int numCandidates ) : VotingSystem( numCandidates ) {};

	virtual ~RankedVotingSystem();

	/**
	* @param ranking An array int[N].
	*	ranking[i] is the ranking of choice i.
	*/
	virtual int voteRanking( int* ranking ) = 0;

	/**
	* Converts rating to ranking and passes to voteRanking.
	* @param ranking An array int[N].
	*	ranking[i] is the ranking of choice i.
	*/
	virtual int voteRating( int* rating );

public:
	/**
	* Uses write(2) to save state to the file descriptor fd.
	* @returns number of bytes written or -1 on error.
	*/
	virtual int write( int fd ) = 0;

	/**
	* Uses read(2) to restore state from the file descriptor fd.
	* @returns number of bytes read or -1 on error.
	*/
	virtual int read( int fd ) = 0;
};

#endif
