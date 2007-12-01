#include "STV.h"
#include <stdlib.h>
#include <math.h>
#include <assert.h>

static int initSTV( STV* toret ) {
	toret->storedVotes = NULL;
	toret->winners = NULL;
	toret->numVotes = 0;
	toret->seats = 2;
	toret->ni = malloc(sizeof(NameIndex));
	if ( toret->ni == NULL ) {
		return -1;
	}
	initNameIndex( toret->ni );
	return 0;
}

STV* newSTV() {
	STV* toret = (STV*)malloc( sizeof(STV) );
	if ( toret == NULL ) { return toret; }
	if ( initSTV( toret ) != 0 ) {
		free( toret );
		return NULL;
	}
	return toret;
}

void STV_setSeats( STV* it, int seats ) {
	it->seats = seats;
}

int STV_voteRating( STV* it, int numVotes, const NameVote* votes ) {
	it->storedVotes = newStoredIndexVoteNodeFromVotes( numVotes, votes, it->storedVotes, it->ni );
	it->numVotes++;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}
int STV_voteStoredIndexVoteNode( STV* it, StoredIndexVoteNode* votes ) {
	votes->next = it->storedVotes;
	it->storedVotes = votes;
	it->numVotes++;
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
int STV_getWinners( STV* it, int wlen, NameVote** winnersP ) {
	int numActive;
	int numWinners;
	int i;
	double* tally;
	boolean* active;
	float* voterWeight;
	int* curvote;
	int numc = it->ni->nextIndex;
	StoredIndexVoteNode* cur;
	double quota;

	if ( winnersP == NULL ) {
		return -1;
	}
	if ( it->winners != NULL ) {
		// have valid solution
		goto returnsolution;
	}
	numActive = numc;
	numWinners = 0;
	it->winners = (NameVote*)malloc( sizeof(NameVote)*numc );
	tally = (double*)malloc( sizeof(double)*numc );
	assert( tally != NULL );
	active = (boolean*)malloc( sizeof(boolean)*numc );
	assert( active != NULL );
	for ( i = 0; i < numc; i++ ) {
		active[i] = true;
	}

	voterWeight = (float*)malloc( sizeof(float)*it->numVotes );
	assert( voterWeight != NULL );
	curvote = (int*)malloc( sizeof(int)*it->numVotes );
	assert( curvote != NULL );
	for ( i = 0; i < it->numVotes; i++ ) {
		voterWeight[i] = 1.0f;
	}
	quota = it->numVotes / ( it->seats + 1.0 );

	while ( numActive > 1 && numWinners < it->seats ) {
		int voteri;
		cur = it->storedVotes;
		voteri = 0;
		for ( i = 0; i < numc; i++ ) {
			tally[i] = 0.0;
		}

		// recount votes
		while ( cur != NULL ) {
			int maxi;
			float max;
			for ( i = 0; ! active[cur->vote[i].index] && i < cur->numVotes; i++ ) {
			}
			if ( i == cur->numVotes ) {
				// exhausted ballot, nothing to vote
			} else {
				max = cur->vote[i].rating;
				maxi = cur->vote[i].index;
				i++;
				for ( ; i < cur->numVotes; i++ ) {
					if ( active[cur->vote[i].index] && cur->vote[i].rating > max ) {
						maxi = cur->vote[i].index;
						max = cur->vote[i].rating;
					}
				}
				curvote[voteri] = maxi;
				tally[maxi] += voterWeight[voteri];
			}
			cur = cur->next;
			voteri++;
		}
		// commit winners or disqualify losers
		{
			double min, max;
			int mintied, maxtied;
			for ( i = 0; ! active[i] && i < numc; i++ ) {
			}
			max = min = tally[i];
			maxtied = mintied = 1;
			i++;
			for ( ; i < numc; i++ ) {
				if ( ! active[i] ) {
					continue;
				}
				if ( tally[i] < min ) {
					min = tally[i];
					mintied = 1;
				} else if ( tally[i] == min ) {
					mintied++;
				}
				if ( tally[i] > max ) {
					max = tally[i];
					maxtied = 1;
				} else if ( tally[i] == max ) {
					maxtied++;
				}
			}
			if ( maxtied == numActive ) {
				// last N are tied, return them all
				break;
			}
			if ( max > quota ) {
				double deweight = 1.0 - (quota / max);
				// promote winner(s)
				for ( i = 0; i < numc; i++ ) {
					if ( tally[i] == max ) {
						int v;
						// deweight by satisfaction
						for ( v = 0; v < it->numVotes; v++ ) {
							if ( curvote[v] == i ) {
								voterWeight[v] *= deweight;
							}
						}
						// record winner
						active[i] = false;
						numActive--;
						it->winners[numWinners].name = indexName( it->ni, i );
						it->winners[numWinners].rating = tally[i];
						if ( debug ) {
							printf("STV winner \"%s\" = %f<br>\n", indexName( it->ni, i ), tally[i] );
						}
						numWinners++;
					}
				}
			} else {
				// disqualify loser(s)
				for ( i = 0; i < numc; i++ ) {
					if ( tally[i] == min ) {
						active[i] = false;
						numActive--;
						it->winners[numWinners+numActive].name = indexName( it->ni, i );
						it->winners[numWinners+numActive].rating = tally[i];
						if ( debug ) {
							printf("STV dq \"%s\" = %f<br>\n", indexName( it->ni, i ), tally[i] );
						}
					}
				}
			}
		}
		if ( debug ) {
			printf("STV active = %d<br>\n", numActive );
		}
	}
	// record holdouts
	for ( i = 0; i < numc; i++ ) {
		if ( active[i] == true ) {
			numActive--;
			it->winners[numWinners+numActive].name = indexName( it->ni, i );
			it->winners[numWinners+numActive].rating = tally[i];
		}
	}
	free( active );
	free( tally );
	free( curvote );
	free( voterWeight );
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
void STV_htmlSummary( STV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	int i;

	numw = STV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		return;
	}
	fprintf( fout, "<table border=\"1\"><tr><th>Name</th><th>Best STV Count</th></tr>" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr bgcolor=\"%s\"><td>%s</td><td>%d</td></tr>", (i < it->seats) ? "#ccffcc" : "#ffcccc", winners[i].name, (int)(winners[i].rating) );
	}
	fprintf( fout, "</table>\n");
}
void STV_print( STV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	int i;

	numw = STV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		return;
	}
	fprintf( fout, "best STV count\tname\n" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "%d\t%s\n", (int)(winners[i].rating), winners[i].name );
	}
}
DECLARE_STD_setSharedNameIndex( STV )

void clearSTV( STV* it ) {
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
void deleteSTV( STV* it ) {
	clearSTV( it );
	free( it );
}

struct vvsrr {
	VirtualVotingSystem vvs;
	STV rr;
};
void STV_deleteVVS( VirtualVotingSystem* vvs ) {
	clearSTV( (STV*)vvs->it );
	free( vvs );
}

VirtualVotingSystem* newVirtualSTV() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(STV);
	vr->vvs.setSeats = (vvs_setSeats)STV_setSeats;
	if ( initSTV( &(vr->rr) ) != 0 ) {
		free( vr );
		return NULL;
	}
	toret->close = STV_deleteVVS;
	toret->it = &vr->rr;
	return toret;
}
