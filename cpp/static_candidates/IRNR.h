#ifndef IRNR_H
#define IRNR_H

#include "RatedVotingSystem.h"

class IRNR : public RatedVotingSystem {
public:
	IRNR( int numCandidates );
	virtual ~IRNR();

	virtual int voteRating( int* rating );
	virtual int getWinners( int* choiceIndecies );
	virtual int write( int fd );
	virtual int read( int fd );
	virtual void htmlSummary( FILE* fout, char** names );
	virtual void print( FILE* fout, char** names );

	static VotingSystem* newIRNR( int numc );
protected:
	double* talley;
	bool* active;
	bool rmsnorm;

	class Vote {
	public:
		Vote* next;
		int rating[1];

		static void* operator new( size_t s, int numc );

		Vote( int* r, int numc, Vote* n = 0 );
	};

	Vote* votes;
};

#endif
