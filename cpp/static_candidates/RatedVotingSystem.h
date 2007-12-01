#ifndef RATED_VOTING_SYSTEM_H
#define RATED_VOTING_SYSTEM_H
/*!
@class RatedVotingSystem
* Interface for any voting system that inputs a voter's rankings of the choices.
* Possible implementations: Borda count, Condorcet, IRV (sucks).
*/

#include "VotingSystem.h"

class RatedVotingSystem : public VotingSystem {
public:
	/*!
	@function RatedVotingSystem
	* In addition to interfaced methods, there shall be a constructor taking
	* the integer number N of choices to be voted on. */
	RatedVotingSystem( int numCandidates ) : VotingSystem( numCandidates ) {};

	virtual ~RatedVotingSystem();

	/*!
	@function voteRating
	* @param ranking An array int[N].
	*	ranking[i] is the ranking of choice i.
	*/
	virtual int voteRating( int* rating ) = 0;

	/*!
	@function voteRanking
	* Convert to Rating and pass to voteRating();
	* @param ranking An array int[N].
	*	ranking[i] is the ranking of choice i.
	*/
	virtual int voteRanking( int* ranking );

	/*!
	@function write
	* Uses write(2) to save state to the file descriptor fd.
	* @result number of bytes written or -1 on error.
	*/
	virtual int write( int fd ) = 0;

	/*!
	@function read
	* Uses read(2) to restore state from the file descriptor fd.
	* @result number of bytes read or -1 on error.
	*/
	virtual int read( int fd ) = 0;
};

#endif
