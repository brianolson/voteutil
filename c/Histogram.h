#ifndef HISTOGRAM_H
#define HISTOGRAM_H

#include "NamedVotingSystem.h"

struct Histogram {
	NameIndex* ni;
	float min, max, step;
	int buckets;
	int maxbucket;// = 0;
	int numc;
	int** they;
	int intmode;// = false;
	int lowfirst;// = false;
	/** bucketscale used at display time on bucket labels */
	float bucketscale;// = 1.0f;
	/** offset used at display time on bucket labels */
	float offset;// = 0.0f;
	/** doscaleoffset used at display time on bucket labels */
	int doscaleoffset;// = false;

	const char* labelname;// = "Rating";

	double minRecorded;// = Double.MAX_VALUE;
	double maxRecorded;// = Double.MAX_VALUE * -1.0;
	double sum;
	int votes;

	NameVote* winners;
};
typedef struct Histogram Histogram;

Histogram* newHistogram();
void initHistogram(Histogram*);

int Histogram_voteRating( Histogram* it, int numVotes, const NameVote* votes );
int Histogram_voteStoredIndexVoteNode( Histogram* it, StoredIndexVoteNode* votes );
int Histogram_getWinners( Histogram* it, int numVotes, NameVote** winnersP );
void Histogram_htmlSummary( Histogram* it, FILE* fout );
void Histogram_print( Histogram* it, FILE* fout );
void Histogram_setSharedNameIndex( Histogram* it, NameIndex* ni );

void deleteHistogram( Histogram* );

VirtualVotingSystem* newVirtualHistogram();

#endif
