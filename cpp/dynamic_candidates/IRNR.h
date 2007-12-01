#ifndef NAMED_IRNR_H
#define NAMED_IRNR_H

#include "NameVotingSystem.h"

class IRNR : public NameVotingSystem {
public:
	IRNR();
	virtual int voteRating( int numVotes, const NameVote* votes );
	virtual int voteIndexVotes( int numVotes, const IndexVote* votes );
	virtual int getWinners( int numVotes, NameVote** votes );
	virtual void htmlSummary( FILE* fout );
	virtual void print( FILE* fout );

	virtual ~IRNR();
protected:
	StoredIndexVoteNode* storedVotes;
	double* tally;
};

NameVotingSystem* makeIRNR();

#endif
