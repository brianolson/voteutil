#ifndef ROUND_SCORE_H
#define ROUND_SCORE_H

#include <stdio.h>

#include "NamedVotingSystem.h"

typedef struct RoundPart {
	double tally;
	const char* name;
	int active;
} RoundPart;

typedef struct RoundScore {
	struct RoundScore* next;
	int length;
	RoundPart they[1];
} RoundScore;

RoundScore* newRoundScore(int numc);

/* delete list starting with it */
void deleteRoundScore(RoundScore* it);

/* Print HTML table of round score intermediate output.
 If winners is not NULL, order rounds output by winner order. */
void RoundScore_HTMLTable( RoundScore* rounds, FILE* fout, NameVote* winners );

#endif /* ROUND_SCORE_H */
