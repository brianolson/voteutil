#ifndef NAMED_IRNR_H
#define NAMED_IRNR_H

#include "NamedVotingSystem.h"

struct IRNR {
	StoredIndexVoteNode* storedVotes;
	NameIndex* ni;
	NameVote* winners;
};
typedef struct IRNR IRNR;

IRNR* newIRNR();

int IRNR_voteRating( IRNR* it, int numVotes, const NameVote* votes );
int IRNR_voteStoredIndexVoteNode( IRNR* it, StoredIndexVoteNode* votes );
int IRNR_getWinners( IRNR* it, int numVotes, NameVote** winnersP );
void IRNR_htmlSummary( IRNR* it, FILE* fout );
void IRNR_htmlExplain( IRNR* it, FILE* fout );
void IRNR_print( IRNR* it, FILE* fout );

void deleteIRNR( IRNR* );

VirtualVotingSystem* newVirtualIRNR();

#endif
