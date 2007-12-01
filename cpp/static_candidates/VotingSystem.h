#ifndef VOTING_SYSTEM_H
#define VOTING_SYSTEM_H

#include <stdio.h>

/*!
	@class VotingSystem
	Interface for any voting system that inputs a voter's rankings of the choices.
	Possible implementations: Borda count, Condorcet, IRV (sucks).
*/

#define NO_VOTE ((int)0x80000000)

class VotingSystem {
public:
	/*!
	@function VotingSystem
	In addition to interfaced methods, there shall be a constructor taking
	the integer number N of choices to be voted on. */
	VotingSystem( int numCandidates )
	: numc( numCandidates )
	{};
	virtual ~VotingSystem();

	/*!
	@function voteRating
	* @param rating An array int[N].
	*	rating[i] is the rating of choice i.
	*  A rating can be any int value. Higher means more preferred.
	*  0 for unrated.
	*/
	virtual int voteRating( int* rating ) = 0;
	/*!
	@function voteRanking
	* @param ranking An array int[N].
	*	ranking[i] is the ranking of choice i.
	*  rankings are 1 (most preferred) through N (least).
	*  INT_MAX (or N) for unspecified.
	*/
	virtual int voteRanking( int* ranking ) = 0;

	/*!
	@function getWinners
	* @param choiceIndecies choiceIndecies[0] through choiceIndecies[number of tied winners - 1]
	*	will be set to the index [0..N-1] of the winning choice(s). Other positions undefined.
	* @result number of tied winners (hopefully 1)
	*/
	virtual int getWinners( int* choiceIndecies ) = 0;

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

	/*!
	@function htmlSummary
	* @result a ascii-html summary of the state of the voting system
	*		(most likely at end, to show results)
	*/
	virtual void htmlSummary( FILE* fout, char** names = (char**)0 ) = 0;

	/*!
	@function print
	* Uses write(2) to save state to the file descriptor fd.
	* @result number of bytes written or -1 on error.
	*/
	virtual void print( FILE* fout, char** names = (char**)0 ) = 0;

	/*!
	@function init
	Default implementation does nothing.
	@param argv NULL terminated array of strings. May be modified by init()
	*/
	virtual void init( char** argv );
protected:
	/*! @var numc number of candidates */
	int numc;
};

#endif
