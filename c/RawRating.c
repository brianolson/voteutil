#include "RawRating.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <assert.h>

RawRating* newRawRating() {
	RawRating* toret = (RawRating*)malloc(sizeof(RawRating));
	if ( toret == NULL ) { return toret; }
	toret->ni = malloc(sizeof(NameIndex));
	if ( toret->ni == NULL ) {
		free( toret );
		return NULL;
	}
	initNameIndex(toret->ni);
	toret->votes = 0;
	toret->tally = NULL;
	toret->tlen = 0;
	toret->winners = NULL;
	return toret;
}

int RawRating_voteRating( RawRating* it, int numVotes, const NameVote* votes ) {
	int i;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	for ( i = 0; i < numVotes; i++ ) {
		int x;
		if ( isnan( votes[i].rating ) ) {
			continue;
		}
		x = nameIndex( it->ni, votes[i].name );
		if ( x >= it->tlen ) {
			double* tt;
			tt = (double*)realloc( it->tally, sizeof(double)*(x+1) );
			if ( tt == NULL ) {
				fprintf(stderr,"realloc returned null, vote not recorded\n");
				return -1;
			}
			it->tally = tt;
			while ( it->tlen <= x ) {
				it->tally[it->tlen] = 0.0;
				it->tlen++;
			}
		}
		it->tally[x] += votes[i].rating;
	}
	it->votes++;
	return 0;
}
int RawRating_voteStoredIndexVoteNode( RawRating* it, StoredIndexVoteNode* votes ) {
	int i;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	for ( i = 0; i < votes->numVotes; i++ ) {
		int x;
		if ( isnan( votes->vote[i].rating ) ) {
			continue;
		}
		x = votes->vote[i].index;
		if ( x >= it->tlen ) {
			double* tt;
			tt = (double*)realloc( it->tally, sizeof(double)*(x+1) );
			if ( tt == NULL ) {
				fprintf(stderr,"realloc returned null, vote not recorded\n");
				free( votes );
				return -1;
			}
			it->tally = tt;
			while ( it->tlen <= x ) {
				it->tally[it->tlen] = 0.0;
				it->tlen++;
			}
		}
		it->tally[x] += votes->vote[i].rating;
	}
	it->votes++;
	free( votes );
	return 0;
}
static void RawRating_calcWinners( RawRating* it ) {
	int notdone = 1;
	int i;
	it->winners = (NameVote*)malloc(sizeof(NameVote)*it->tlen);
	assert( it->winners != NULL );
	for ( i = 0; i < it->tlen; i++ ) {
		it->winners[i].name = indexName( it->ni, i );
		it->winners[i].rating = it->tally[i];
	}
	// sort
	while ( notdone ) {
		notdone = 0;
		for ( i = 1; i < it->tlen; i++ ) {
			if ( it->winners[i].rating > it->winners[i-1].rating ) {
				float rating = it->winners[i].rating;
				const char* name = it->winners[i].name;
				it->winners[i].rating = it->winners[i-1].rating;
				it->winners[i].name = it->winners[i-1].name;
				it->winners[i-1].rating = rating;
				it->winners[i-1].name = name;
				notdone = 1;
			}
		}
	}
}
int RawRating_getWinners( RawRating* it, int numVotes, NameVote** winnersP ) {
	int i;
	if ( numVotes > 0 && *winnersP != NULL ) {
		int lim = numVotes < it->tlen ? numVotes : it->tlen;
		NameVote* winners = *winnersP;
		// copy to provided space
		for ( i = 0; i < lim; i++ ) {
			winners[i].name = indexName( it->ni, i );
			winners[i].rating = it->tally[i];
		}
		return lim;
	}
	if ( it->winners == NULL ) {
		RawRating_calcWinners( it );
	}
	*winnersP = it->winners;
	return it->tlen;
}
void RawRating_htmlSummary( RawRating* it, FILE* fout ) {
	int i;
	if ( it->winners == NULL ) {
		RawRating_calcWinners( it );
	}
	fprintf(fout, "<table border=\"1\"><tr><th>Name</th><th>Raw Rating Summation</th></tr>");
	for ( i = 0; i < it->tlen; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%.2f</td></tr>", it->winners[i].name, it->winners[i].rating );
	}
	fprintf( fout, "</table>\n" );
}
void RawRating_print( RawRating* it, FILE* fout ) {
	int i;
	if ( it->winners == NULL ) {
		RawRating_calcWinners( it );
	}
	fprintf(fout, "#Name\tRaw Rating Summation\n");
	for ( i = 0; i < it->tlen; i++ ) {
		fprintf( fout, "%s\t%.9g\n", it->winners[i].name, it->winners[i].rating );
	}
}
DECLARE_STD_setSharedNameIndex( RawRating )
#if 0
void RawRating_setSharedNameIndex( RawRating* it, NameIndex* ni ) {
	if ( it->ni != NULL ) {
		clearNameIndex( it->ni );
		free( it->ni );
	}
	it->ni = ni;
}
#endif

void clearRawRating( RawRating* it ) {
	if ( it->winners != NULL ) {
		free( it->winners );
	}
	if ( it->tally != NULL ) {
		free( it->tally );
	}
	clearNameIndex( it->ni );
}
void deleteRawRating( RawRating* it ) {
	clearRawRating( it );
	free(it);
}

struct vvsrr {
	VirtualVotingSystem vvs;
	RawRating rr;
};
void RawRating_deleteVVS( VirtualVotingSystem* vvs ) {
	clearRawRating( (RawRating*)vvs->it );
	free( vvs );
}

VirtualVotingSystem* newVirtualRawRating() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(RawRating);
	toret->close = RawRating_deleteVVS;
	toret->it = &vr->rr;
	return toret;
}
