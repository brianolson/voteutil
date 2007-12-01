#ifndef NAMED_IRNR_H
#define NAMED_IRNR_H

#include "NamedVotingSystem.h"

struct IRV {
	StoredIndexVoteNode* storedVotes;
	NameIndex* ni;
	NameVote* winners;
};
typedef struct IRV IRV;

IRV* newIRV();

int IRV_voteRating( IRV* it, int numVotes, const NameVote* votes );
int IRV_voteStoredIndexVoteNode( IRV* it, StoredIndexVoteNode* votes );
int IRV_getWinners( IRV* it, int numVotes, NameVote** winnersP );
void IRV_htmlSummary( IRV* it, FILE* fout );
void IRV_print( IRV* it, FILE* fout );

void deleteIRV( IRV* );

VirtualVotingSystem* newVirtualIRV();

#endif
