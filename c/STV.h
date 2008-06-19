#ifndef NAMED_STV_H
#define NAMED_STV_H

#include "NamedVotingSystem.h"

struct STV {
	StoredIndexVoteNode* storedVotes;
	NameIndex* ni;
	NameVote* winners;
	int numVotes;
	int seats;
	double deadVotes;
};
typedef struct STV STV;

STV* newSTV();

int STV_voteRating( STV* it, int numVotes, const NameVote* votes );
int STV_voteStoredIndexVoteNode( STV* it, StoredIndexVoteNode* votes );
int STV_getWinners( STV* it, int numVotes, NameVote** winnersP );
void STV_htmlSummary( STV* it, FILE* fout );
void STV_print( STV* it, FILE* fout );

void deleteSTV( STV* );

VirtualVotingSystem* newVirtualSTV();

#endif
