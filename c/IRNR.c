#include "IRNR.h"
#include "RoundScore.h"
#include <stdlib.h>
#include <math.h>
#include <assert.h>

IRNR* newIRNR() {
	IRNR* toret = (IRNR*)malloc( sizeof(IRNR) );
	if ( toret == NULL ) { return toret; }
	toret->storedVotes = NULL;
	toret->winners = NULL;
	toret->ni = malloc(sizeof(NameIndex));
	if ( toret->ni == NULL ) {
		free( toret );
		return NULL;
	}
	initNameIndex( toret->ni );
	return toret;
}


int IRNR_voteRating( IRNR* it, int numVotes, const NameVote* votes ) {
	it->storedVotes = newStoredIndexVoteNodeFromVotes( numVotes, votes, it->storedVotes, it->ni );
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}
int IRNR_voteStoredIndexVoteNode( IRNR* it, StoredIndexVoteNode* votes ) {
	votes->next = it->storedVotes;
	it->storedVotes = votes;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}

typedef unsigned char boolean;
#define true ((boolean)1)
#define false ((boolean)0)
static __inline int min( int a, int b ) {
	if ( a < b ) return a; return b;
}

int rmsnorm = 1;

static void IRNR_calcWinners( IRNR* it, RoundScore** rounds ) {
	double* tally;
	boolean* active;
	int numActive;
	int i;
	int numc = it->ni->nextIndex;
	RoundScore* round;

	if ( rounds != NULL ) {
		round = newRoundScore(numc);
		*rounds = round;
	} else {
		round = NULL;
	}
	numActive = numc;
	it->winners = (NameVote*)malloc( sizeof(NameVote)*numc );
	tally = (double*)malloc( sizeof(double)*numc );
	assert( tally != NULL );
	active = (boolean*)malloc( sizeof(boolean)*numc );
	assert( active != NULL );
	for ( i = 0; i < numc; i++ ) {
		active[i] = true;
	}
	while ( numActive > 1 ) {
		// init tally and count it
		for ( i = 0; i < numc; i++ ) {
			if ( active[i] ) {
				tally[i] = 0.0;
			}
		}
		{
			StoredIndexVoteNode* cur;
			cur = it->storedVotes;
			while ( cur != NULL ) {
				double sum;
				sum = 0.0;
				for ( i = 0; i < cur->numVotes; i++ ) {
					if ( active[cur->vote[i].index] ) {
						if ( rmsnorm ) {
							sum += cur->vote[i].rating * cur->vote[i].rating;
						} else {
							sum += fabs( cur->vote[i].rating );
						}
					}
				}
				if ( sum > 0.0 ) {
					if ( rmsnorm ) {
						sum = sqrt( sum );
					}
					// many multiplies are faster, specially if there's a madd
					sum = 1.0 / sum;
					for ( i = 0; i < cur->numVotes; i++ ) {
						if ( active[cur->vote[i].index] ) {
							tally[cur->vote[i].index] += cur->vote[i].rating * sum;
						}
					}
				}
				cur = cur->next;
			}
		}
#if 0
		for ( i = 0; i < numActive; i++ ) {
			printf("\t\"%s\" = %g\n", indexName( i ), tally[i] );
		}
#endif
		// disqualify loser(s)
		{
			double min;
			int tied;
			for ( i = 0; ! active[i] && i < numc; i++ ) {
			}
			min = tally[i];
			tied = 1;
			i++;
			for ( ; i < numc; i++ ) {
				if ( ! active[i] ) {
					// skip
				} else if ( tally[i] < min ) {
					min = tally[i];
					tied = 1;
				} else if ( tally[i] == min ) {
					tied++;
				}
			}
			if ( tied == numActive ) {
				// last N are tied, return them all
				break;
			}
			for ( i = 0; i < numc; i++ ) {
				if ( tally[i] == min ) {
					//printf("disqualify \"%s\" with %g\n", indexName( i ), tally[i] );
					active[i] = false;
					numActive--;
					it->winners[numActive].name = indexName( it->ni, i );
					it->winners[numActive].rating = tally[i];
				}
			}
		}
		if ( round != NULL ) {
			for ( i = 0; i < numc; ++i ) {
				round->they[i].active = active[i];
				round->they[i].name = indexName( it->ni, i );
				round->they[i].tally = tally[i];
			}
			if ( numActive > 1 ) {
				round->next = newRoundScore(numc);
				round = round->next;
			}
		}
	}
	for ( i = 0; i < numc; i++ ) {
		if ( active[i] == true ) {
			numActive--;
			it->winners[numActive].name = indexName( it->ni, i );
			it->winners[numActive].rating = tally[i];
		}
	}
	free( active );
	free( tally );
}
int IRNR_getWinners( IRNR* it, int wlen, NameVote** winnersP ) {
	int i;
	int numc = it->ni->nextIndex;

	if ( winnersP == NULL ) {
		return -1;
	}
	if ( it->winners != NULL ) {
		// have valid solution
		goto returnsolution;
	}
	IRNR_calcWinners( it, NULL );
returnsolution:
	if ( *winnersP != NULL && wlen > 0 ) {
		// copy into provided return space
		NameVote* winners = *winnersP;
		int lim = min( numc, wlen );
		for ( i = 0; i < lim; i++ ) {
			winners[i] = it->winners[i];
		}
		return lim;
	} else {
		*winnersP = it->winners;
		return numc;
	}
}
void IRNR_htmlSummary( IRNR* it, FILE* fout ) {
	int i;
	int numc = it->ni->nextIndex;
	if ( ! it->winners ) {
		IRNR_calcWinners( it, NULL );
	}
	fprintf(fout, "<table border=\"1\"><tr><th>Name</th><th>IRNR Rating</th></tr>");
	for ( i = 0; i < numc; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%.2f</td></tr>", it->winners[i].name, it->winners[i].rating );
	}
	fprintf( fout, "</table>\n" );
}
void IRNR_htmlExplain( IRNR* it, FILE* fout ) {
	int i;
	int numc = it->ni->nextIndex;
	RoundScore* rounds;
	if ( ! it->winners ) {
		IRNR_calcWinners( it, &rounds );
	}
	RoundScore_HTMLTable( rounds, fout, it->winners );
	deleteRoundScore( rounds );
	fprintf(fout, "<table border=\"1\"><tr><th>Name</th><th>IRNR Rating</th></tr>");
	for ( i = 0; i < numc; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%.2f</td></tr>", it->winners[i].name, it->winners[i].rating );
	}
	fprintf( fout, "</table>\n" );
}
void IRNR_print( IRNR* it, FILE* fout ) {
	int i;
	int numc = it->ni->nextIndex;
	if ( ! it->winners ) {
		IRNR_calcWinners( it, NULL );
	}
	fprintf(fout, "#Name\tIRNR Rating\n");
	for ( i = 0; i < numc; i++ ) {
		fprintf( fout, "%s\t%.9g\n", it->winners[i].name, it->winners[i].rating );
	}
}
void IRNR_setSharedNameIndex( IRNR* it, NameIndex* ni ) {
	if ( it->ni != NULL ) {
		clearNameIndex( it->ni );
		free( it->ni );
	}
	it->ni = ni;
}

void clearIRNR( IRNR* it ) {
	StoredIndexVoteNode* cur;
	StoredIndexVoteNode* next;
	cur = it->storedVotes;
	while ( cur != NULL ) {
		next = cur->next;
		free( cur );
		cur = next;
	}
	clearNameIndex( it->ni );
}
void deleteIRNR( IRNR* it ) {
	clearIRNR( it );
	free( it );
}

struct vvsrr {
	VirtualVotingSystem vvs;
	IRNR rr;
};
void IRNR_deleteVVS( VirtualVotingSystem* vvs ) {
	clearIRNR( (IRNR*)vvs->it );
	free( vvs );
}

VirtualVotingSystem* newVirtualIRNR() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(IRNR);
	toret->htmlExplain = (vvs_htmlSummary)IRNR_htmlExplain;
	toret->it = &vr->rr;
	return toret;
}
