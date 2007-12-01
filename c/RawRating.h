#ifndef RAW_RATING_H
#define RAW_RATING_H

#include "NamedVotingSystem.h"

struct RawRating {
	NameIndex* ni;
	int votes;
	double* tally;
	int tlen;
	NameVote* winners;
};
typedef struct RawRating RawRating;

RawRating* newRawRating();

int RawRating_voteRating( RawRating* it, int numVotes, const NameVote* votes );
int RawRating_voteStoredIndexVoteNode( RawRating* it, StoredIndexVoteNode* votes );
int RawRating_getWinners( RawRating* it, int numVotes, NameVote** winnersP );
void RawRating_htmlSummary( RawRating* it, FILE* fout );
void RawRating_print( RawRating* it, FILE* fout );
void RawRating_setSharedNameIndex( RawRating* it, NameIndex* ni );

void deleteRawRating( RawRating* );

VirtualVotingSystem* newVirtualRawRating();

#endif
