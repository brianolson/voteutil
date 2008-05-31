#ifndef NAMED_VRR_H
#define NAMED_VRR_H

#include "NamedVotingSystem.h"

struct VRR {
	int** counts;
	int numc;
	NameIndex* ni;
	NameVote* winners;
};
typedef struct VRR VRR;

VRR* newVRR();

int VRR_voteRating( VRR* it, int numVotes, const NameVote* votes );
int VRR_voteStoredIndexVoteNode( VRR* it, StoredIndexVoteNode* votes );
int VRR_getWinners( VRR* it, int numVotes, NameVote** winnersP );
int VRR_getWinnersCSSD( VRR* it, int numVotes, NameVote** winnersP );
int VRR_getWinnersRankedPairs( VRR* it, int numVotes, NameVote** winnersP );
void VRR_htmlSummary( VRR* it, FILE* fout );
void VRR_print( VRR* it, FILE* fout );

void deleteVRR( VRR* );

// VRR which uses CSSD for cycle resolution
VirtualVotingSystem* newVirtualVRR();

// VRR which uses Ranked Pairs for cycle resolution
VirtualVotingSystem* newVirtualRankedPairs();

#endif
