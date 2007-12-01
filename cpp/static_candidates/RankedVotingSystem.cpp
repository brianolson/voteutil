#include "RankedVotingSystem.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// convert to ranking (sort)
int RankedVotingSystem::voteRating( int* rating ) {
#if __ghs || __STRICT_ANSI__
	int* ci = new int[numc];
	int* rank = new int[numc];
#else
	int ci[numc], rank[numc];
#endif
	int i;
	bool done = false;
	for ( i = 0; i < numc; i++ ) {
		ci[i] = i;
	}
	while ( ! done ) {
		done = true;
		for ( i = 0; i < numc - 1; i++ ) {
			if ( rating[ci[i]] < rating[ci[i+1]] ) {
				int t;
				t = ci[i];
				ci[i] = ci[i+1];
				ci[i+1] = t;
				done = false;
			}
		}
	}
	for ( i = 0; i < numc; i++ ) {
		rank[ci[i]] = i + 1;
		while ( ((i+1) < numc) && (rating[ci[i]] == rating[ci[i+1]]) ) {
			rank[ci[i+1]] = rank[ci[i]];
			i++;
		}
	}
#if 01
	for ( i = 0; i < numc; i++ ) {
		if ( rating[i] == NO_VOTE ) {
			rank[i] = NO_VOTE;
		}
	}
#endif
	int toret = voteRanking( rank );
#if __ghs || __STRICT_ANSI__
	delete [] ci;
	delete [] rank;
#endif
	return toret;
}

RankedVotingSystem::~RankedVotingSystem() {
}
