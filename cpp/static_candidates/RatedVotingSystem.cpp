#include "RatedVotingSystem.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int RatedVotingSystem::voteRanking( int* ranking ) {
#if __ghs || __STRICT_ANSI__
	int* rating = new int[numc];
#else
	int rating[numc];
#endif
	int i;
	for ( i = 0; i < numc; i++ ) {
		if ( ranking[i] == NO_VOTE ) {
			rating[i] = NO_VOTE; // unranked -> unrated
		} else {
			rating[i] = numc - ranking[i];
		}
	}
	int toret = voteRating( rating );
#if __ghs || __STRICT_ANSI__
	delete [] rating;
#endif
	return toret;
}


RatedVotingSystem::~RatedVotingSystem() {
}
