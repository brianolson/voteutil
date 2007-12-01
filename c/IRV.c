#include "IRV.h"
#include <stdlib.h>
#include <math.h>
#include <assert.h>

/*

struct IRV {
	StoredIndexVoteNode* storedVotes;
	double* tally;
};
typedef struct IRV IRV;
*/
IRV* newIRV() {
	IRV* toret = (IRV*)malloc( sizeof(IRV) );
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


int IRV_voteRating( IRV* it, int numVotes, const NameVote* votes ) {
	it->storedVotes = newStoredIndexVoteNodeFromVotes( numVotes, votes, it->storedVotes, it->ni );
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}
int IRV_voteStoredIndexVoteNode( IRV* it, StoredIndexVoteNode* votes ) {
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
int IRV_getWinners( IRV* it, int wlen, NameVote** winnersP ) {
	int numActive;
	int i;
	int* tally;
	boolean* active;
	StoredIndexVoteNode** buckets;
	int numc = it->ni->nextIndex;
	StoredIndexVoteNode* cur;
	StoredIndexVoteNode* exhausted = NULL;

	if ( winnersP == NULL ) {
		return -1;
	}
	if ( it->winners != NULL ) {
		// have valid solution
		goto returnsolution;
	}
	numActive = numc;
	it->winners = (NameVote*)malloc( sizeof(NameVote)*numc );
	tally = (int*)malloc( sizeof(int)*numc );
	assert( tally != NULL );
	active = (boolean*)malloc( sizeof(boolean)*numc );
	assert( active != NULL );
	buckets = (StoredIndexVoteNode**)malloc( sizeof(StoredIndexVoteNode*)*numc );
	assert( buckets != NULL );
	for ( i = 0; i < numc; i++ ) {
		active[i] = true;
		tally[i] = 0;
		buckets[i] = NULL;
	}
	cur = it->storedVotes;
	it->storedVotes = NULL;
	while ( numActive > 1 ) {
		// bucketize a list
		while ( cur != NULL ) {
			StoredIndexVoteNode* next;
			int mini;
			float min;
			next = cur->next;
			for ( i = 0; ! active[cur->vote[i].index] && i < cur->numVotes; i++ ) {
			}
			if ( i == cur->numVotes ) {
				// exhausted ballot
				cur->next = exhausted;
				exhausted = cur;
			} else {
				min = cur->vote[i].rating;
				mini = cur->vote[i].index;
				i++;
				for ( ; i < cur->numVotes; i++ ) {
					if ( active[cur->vote[i].index] && cur->vote[i].rating > min ) {
						mini = cur->vote[i].index;
						min = cur->vote[i].rating;
					}
				}
				cur->next = buckets[mini];
				buckets[mini] = cur;
				tally[mini]++;
				if ( debug ) {
					printf("IRV bucketize \"%s\"<br>\n", indexName( it->ni, mini ) );
				}
			}
			cur = next;
		}
		// disqualify loser(s)
		{
			float min;
			int tied;
			StoredIndexVoteNode* curend = NULL;
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
					active[i] = false;
					if ( buckets[i] == NULL ) {
						// nothing to rebucketize
					} else if ( curend == NULL ) {
						cur = buckets[i];
						curend = buckets[i];
						while ( curend->next != NULL ) {
							curend = curend->next;
						}
					} else {
						curend->next = buckets[i];
						while ( curend->next != NULL ) {
							curend = curend->next;
						}
					}
					buckets[i] = NULL;
					numActive--;
					it->winners[numActive].name = indexName( it->ni, i );
					it->winners[numActive].rating = tally[i];
					if ( debug ) {
						printf("IRV dq \"%s\" = %d<br>\n", indexName( it->ni, i ), tally[i] );
					}
					//printf("disqualify \"%s\" (%d) with %d, %d remain\n", indexName( it->ni, i ), i, tally[i], numActive );
				}
			}
		}
		if ( debug ) {
			printf("IRV active = %d<br>\n", numActive );
		}
	}
	for ( i = 0; i < numc; i++ ) {
		if ( active[i] == true ) {
			numActive--;
			it->winners[numActive].name = indexName( it->ni, i );
			it->winners[numActive].rating = tally[i];
		}
	}
	// re-bundle all the votes into storedVotes from cur, exhausted and buckets[]
	if ( cur != NULL ) {
		it->storedVotes = cur;
		while ( cur->next != NULL ) {
			cur = cur->next;
		}
		cur->next = exhausted;
	} else {
		it->storedVotes = cur = exhausted;
	}
	for ( i = 0; i < numc; i++ ) {
		if ( cur != NULL ) {
			while ( cur->next != NULL ) {
				cur = cur->next;
			}
			cur->next = buckets[i];
		} else {
			// first non-null thing encountered!
			it->storedVotes = cur = buckets[i];
		}
	}
	free( buckets );
	free( active );
	free( tally );
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
void IRV_htmlSummary( IRV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	int i;

	numw = IRV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		return;
	}
	fprintf( fout, "<table border=\"1\"><tr><th>Name</th><th>Best IRV Count</th></tr>" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%d</td></tr>", winners[i].name, (int)(winners[i].rating) );
	}
	fprintf( fout, "</table>\n");
}
void IRV_print( IRV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	int i;

	numw = IRV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		return;
	}
	fprintf( fout, "best IRV count\tname\n" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "%d\t%s\n", (int)(winners[i].rating), winners[i].name );
	}
}
DECLARE_STD_setSharedNameIndex( IRV )

void clearIRV( IRV* it ) {
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
void deleteIRV( IRV* it ) {
	clearIRV( it );
	free( it );
}

struct vvsrr {
	VirtualVotingSystem vvs;
	IRV rr;
};
void IRV_deleteVVS( VirtualVotingSystem* vvs ) {
	clearIRV( (IRV*)vvs->it );
	free( vvs );
}

VirtualVotingSystem* newVirtualIRV() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(IRV);
	toret->close = IRV_deleteVVS;
	toret->it = &vr->rr;
	return toret;
}
