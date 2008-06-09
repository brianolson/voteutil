#include "IRV.h"
#include "RoundScore.h"
#include <stdlib.h>
#include <math.h>
#include <assert.h>

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

typedef struct TallyState {
	boolean active;
	int whole;
	double fractions;
	StoredIndexVoteNode* bucket;
} TallyState;


static __inline int min( int a, int b ) {
	if ( a < b ) return a; return b;
}
static int IRV_getWinnersInternal(
		IRV* it, int wlen, NameVote** winnersP, RoundScore** rounds ) {
	int numActive;
	int i;
	TallyState* they;
	int numc = it->ni->nextIndex;
	StoredIndexVoteNode* cur;
	StoredIndexVoteNode* exhausted = NULL;
	StoredIndexVoteNode* tiedVotes = NULL;
	RoundScore* round;

	if ( winnersP == NULL ) {
		return -1;
	}
	if ( it->winners != NULL ) {
		// have valid solution
		goto returnsolution;
	}
	if ( rounds != NULL ) {
		round = newRoundScore(numc);
		*rounds = round;
	} else {
		round = NULL;
	}
	numActive = numc;
	it->winners = (NameVote*)malloc( sizeof(NameVote)*numc );
	they = (TallyState*)malloc( sizeof(TallyState) * numc );
	assert( they != NULL );
	for ( i = 0; i < numc; i++ ) {
		they[i].active = true;
		they[i].whole = 0;
		they[i].fractions = 0.0;
		they[i].bucket = NULL;
	}
	cur = it->storedVotes;
	it->storedVotes = NULL;
	while ( numActive > 1 ) {
		for ( i = 0; i < numc; i++ ) {
			if ( they[i].active ) {
				they[i].fractions = 0.0;
			}
		}
		// bucketize a list
		while ( cur != NULL ) {
			StoredIndexVoteNode* next;
			int maxi;
			float max;
			next = cur->next;
			i = 0;
			while ( (i < cur->numVotes) && (! they[cur->vote[i].index].active) ) {
				i++;
			}
			if ( i == cur->numVotes ) {
				// exhausted ballot
				cur->next = exhausted;
				exhausted = cur;
			} else {
				int tied = 1;
				max = cur->vote[i].rating;
				maxi = cur->vote[i].index;
				i++;
				for ( ; i < cur->numVotes; i++ ) {
					if ( they[cur->vote[i].index].active ) {
						if ( cur->vote[i].rating > max ) {
							tied = 1;
							maxi = cur->vote[i].index;
							max = cur->vote[i].rating;
						} else if ( cur->vote[i].rating == max ) {
							tied++;
						}
					} 
				}
				if ( tied == 1 ) {
					cur->next = they[maxi].bucket;
					they[maxi].bucket = cur;
					they[maxi].whole++;
				} else {
					double tf = 1.0 / tied;
					for ( i = 0; i < cur->numVotes; ++i ) {
						if ( they[cur->vote[i].index].active ) {
							if ( cur->vote[i].rating == max ) {
								they[cur->vote[i].index].fractions += tf;
							}
						}
					}
					cur->next = tiedVotes;
					tiedVotes = cur;
				}
				if ( debug ) {
					printf("IRV bucketize \"%s\"<br>\n", indexName( it->ni, maxi ) );
				}
			}
			cur = next;
		}
		// disqualify loser(s)
		{
			double min;
			int tied;
			StoredIndexVoteNode* curend = NULL;
			cur = curend = tiedVotes;
			tiedVotes = NULL;
			for ( i = 0; (i < numc) && (! they[i].active); i++ ) {
			}
			assert(i < numc);
			min = they[i].whole + they[i].fractions;
			tied = 1;
			i++;
			for ( ; i < numc; i++ ) {
				if ( ! they[i].active ) {
					// skip
				} else if ( (they[i].whole + they[i].fractions) < min ) {
					min = (they[i].whole + they[i].fractions);
					tied = 1;
				} else if ( (they[i].whole + they[i].fractions) == min ) {
					tied++;
				}
			}
			if ( tied == numActive ) {
				// last N are tied, return them all
				break;
			}
			for ( i = 0; i < numc; i++ ) {
				if ( (they[i].whole + they[i].fractions) == min ) {
					they[i].active = false;
					if ( they[i].bucket == NULL ) {
						// nothing to rebucketize
					} else if ( curend == NULL ) {
						cur = they[i].bucket;
						curend = they[i].bucket;
					} else {
						while ( curend->next != NULL ) {
							curend = curend->next;
						}
						curend->next = they[i].bucket;
					}
					they[i].bucket = NULL;
					numActive--;
					it->winners[numActive].name = indexName( it->ni, i );
					it->winners[numActive].rating = (they[i].whole + they[i].fractions);
					if ( debug ) {
						printf("IRV dq \"%s\" = %f<br>\n", indexName( it->ni, i ), (they[i].whole + they[i].fractions) );
					}
					//printf("disqualify \"%s\" (%d) with %d, %d remain\n", indexName( it->ni, i ), i, (they[i].whole + they[i].fractions), numActive );
				}
			}
		}
		if ( round != NULL ) {
			for ( i = 0; i < numc; ++i ) {
				round->they[i].active = they[i].active;
				round->they[i].name = indexName( it->ni, i );
				round->they[i].tally = (they[i].whole + they[i].fractions);
			}
			if ( numActive > 1 ) {
				round->next = newRoundScore(numc);
				round = round->next;
			}
		}
		if ( debug ) {
			printf("IRV active = %d<br>\n", numActive );
		}
	}
	for ( i = 0; i < numc; i++ ) {
		if ( they[i].active == true ) {
			numActive--;
			it->winners[numActive].name = indexName( it->ni, i );
			it->winners[numActive].rating = (they[i].whole + they[i].fractions);
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
			cur->next = they[i].bucket;
		} else {
			// first non-null thing encountered!
			it->storedVotes = cur = they[i].bucket;
		}
	}
	free( they );
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
int IRV_getWinners( IRV* it, int wlen, NameVote** winnersP ) {
	return IRV_getWinnersInternal( it, wlen, winnersP, NULL );
}
static void printWinners( FILE* fout, NameVote* winners, int numw ) {
	int i;

	fprintf( fout, "<table border=\"1\"><tr><th>Name</th><th>Best IRV Count</th></tr>" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%d</td></tr>", winners[i].name, (int)(winners[i].rating) );
	}
	fprintf( fout, "</table>\n");
}
void IRV_htmlSummary( IRV* it, FILE* fout ) {
	int numw;
	NameVote* winners;

	numw = IRV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		return;
	}
	printWinners( fout, winners, numw );
}
void IRV_htmlExplain( IRV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	RoundScore* rounds = NULL;

	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	numw = IRV_getWinnersInternal( it, 0, &winners, &rounds );
	if ( numw <= 0 ) {
		deleteRoundScore( rounds );
		return;
	}
	RoundScore_HTMLTable( rounds, fout, it->winners );
	deleteRoundScore( rounds );
	printWinners( fout, winners, numw );
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
	toret->htmlExplain = (vvs_htmlSummary)IRV_htmlExplain;
	toret->it = &vr->rr;
	return toret;
}
