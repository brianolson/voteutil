#include "STV.h"
#include <math.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>

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
	StoredIndexVoteNode_sort( it->storedVotes );
	it->numVotes++;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}
int STV_voteStoredIndexVoteNode( STV* it, StoredIndexVoteNode* votes ) {
	StoredIndexVoteNode_sort( votes );
	votes->next = it->storedVotes;
	it->storedVotes = votes;
	it->numVotes++;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	return 0;
}

static __inline int min( int a, int b ) {
	if ( a < b ) return a; return b;
}
#define STATUS_DEAD 0
#define STATUS_ACTIVE 1
#define STATUS_ELECTED 2

// Per-choice status storage.
typedef struct TallyStatus {
	double tally;
	double weight;
	int status;
} TallyStatus;

// Intermediate status storage for htmlExplain
typedef struct RoundScore {
	struct RoundScore* next;
	double quota;
	double deadVote;
	TallyStatus ts[1];	// actually ts[numc]
} RoundScore;
static RoundScore* newRoundScore(int numc) {
	RoundScore* it = (RoundScore*)malloc( sizeof(RoundScore) + sizeof(TallyStatus)*(numc-1) );
	it->next = NULL;
	return it;
}

// Count all the votes.
// Clears TallyStatus before count.
// Distributes vote weight from each voter to each choice from top-rated down.
static double internalRecount( STV* it, TallyStatus* tally, double quota ) {
	StoredIndexVoteNode* cur;
	int i, j;
	double deadVotes = 0.0;
	int numc = it->ni->nextIndex;

	// reset tally
	for ( i = 0; i < numc; i++ ) {
		if ( tally[i].status != STATUS_DEAD ) {
			tally[i].tally = 0.0;
		}
	}

	cur = it->storedVotes;
	while ( cur != NULL ) {
		int tie;
		double tieWeight;
		double weight = 1.0;
		double dw;
		i = 0;
		// Add weight from this vote to top-rated chioice until this vote has been fully cast.
		while ( (i < cur->numVotes) && (weight > 0.0) ) {
			if ( tally[cur->vote[i].index].status == STATUS_DEAD ) {
			} else {
				tieWeight = 1.0;
				tie = 1;
				j = i + 1;
				// find any ties
				while ( (j < cur->numVotes) &&
					   (cur->vote[j].rating == cur->vote[i].rating) ) {
					if ( tally[cur->vote[j].index].status == STATUS_DEAD ) {
					} else if ( tally[cur->vote[j].index].status == STATUS_ACTIVE ) {
						tieWeight += 1.0;
					} else {
						assert( tally[cur->vote[j].index].status == STATUS_ELECTED );
						tieWeight += tally[cur->vote[j].index].weight;
					}
					j++;
				}
				if ( tie == 1 ) {
					// no tie, simple case.
					if ( tally[cur->vote[i].index].status == STATUS_ACTIVE ) {
						// Vote fully on non-elected choice.
						tally[cur->vote[i].index].tally += weight;
						weight = 0.0;
					} else {
						// Vote fractionally on elected choices.
						assert( tally[cur->vote[i].index].status == STATUS_ELECTED );
						dw = tally[cur->vote[i].index].weight * weight;
						tally[cur->vote[i].index].tally += dw;
						weight -= dw;
					}
				} else {
					// There are things tied for the active level in this vote.
					// Split the vote across the tie-voted choices.
					double weightLoss = 0.0;
					j = i;
					while ( (j < cur->numVotes) &&
						   (cur->vote[j].rating == cur->vote[i].rating) ) {
						if ( tally[cur->vote[j].index].status == STATUS_DEAD ) {
						} else if ( tally[cur->vote[j].index].status == STATUS_ACTIVE ) {
							dw = weight * ( 1.0 / tieWeight );
							tally[cur->vote[j].index].tally += dw;
							weightLoss += dw;
						} else {
							assert( tally[cur->vote[j].index].status == STATUS_ELECTED );
							dw = weight * (tally[cur->vote[j].index].weight / tieWeight);
							tally[cur->vote[j].index].tally += dw;
							weightLoss += dw;
						}
						j++;
					}
					weight -= weightLoss;
				}
			}
			i++;
		}
		if ( weight > 0.0 ) {
			deadVotes += weight;
		}
		cur = cur->next;
	}
	// assert some simple sanity checks.
	for ( i = 0; i < numc; i++ ) {
		if ( tally[i].status == STATUS_ELECTED ) {
			assert(tally[i].tally >= quota);
		}
	}
	return deadVotes;
}

/*
 sum(votes_for_c)*deweight_c == quota
	aka
 deweight_c = quota / sum(vote_for_c)
 
 first place:
	vote_for_c = deweight_c
 second place:
	vote_for_c2 = deweight_c * deweight_c2
 third place:
	vote_for_c3 = deweight_c * deweight_c2 * deweight_c3
 
 There's probably a closed form linear algebra solution to this, but it might
 inlvolve a matrix ((n+1) choose (seats)) columns wide.
 
 So instead, iterate.
 Stop when enough are elected or if there isn't enough extra vote in the top N to elect an (N+1)th.
 */
static int settleWinners( STV* it, TallyStatus* tally, double quota ) {
	int winnersFound = 0;
	int numc = it->ni->nextIndex;
	int i;
	double max = 0.0, submax;
	int maxi, submaxi;
	int tie = -1, subtie;
	double surplus;
	while ( winnersFound < it->seats ) {
		int newWinners;
		newWinners = 0;
		i = 0;
		// find first active
		surplus = 0.0;
		maxi = -1;
		for ( i = 0; i < numc; ++i ) {
			if ( tally[i].status == STATUS_ELECTED ) {
				surplus += tally[i].tally - quota;
			} else if ( tally[i].status == STATUS_ACTIVE ) {
				if ( (maxi == -1) || (tally[i].tally > max) ) {
					submaxi = maxi;
					submax = max;
					subtie = tie;
					maxi = i;
					max = tally[i].tally;
					tie = 1;
				} else if ( tally[i].tally == max ) {
					tie++;
				}
				if ( tally[i].tally > quota ) {
					newWinners++;
				}
			}
		}
		if ( maxi == -1 ) {
			// nothing active
			return winnersFound;
		}
		if ( ((max + surplus) < quota) || (winnersFound + newWinners > it->seats) ) {
			// Highest found can't win yet, or too-many-way tie for winners.
			// Bail and try a DQ phase.
			return winnersFound;
		}
		for ( i = 0; i < numc; ++i ) {
			if ( (tally[i].status == STATUS_ACTIVE) && (tally[i].tally > quota) ) {
				tally[i].status = STATUS_ELECTED;
				winnersFound++;
				tally[i].weight = 1.00000001 * (quota / tally[i].tally);
				surplus += tally[i].tally - quota;
			}
		}
		if ( winnersFound < it->seats ) {
			it->deadVotes = internalRecount( it, tally, quota );
			quota = (it->numVotes - it->deadVotes) / ( it->seats + 1.0 );
		}
	}
	return winnersFound;
}

static int STV_getWinnersInternal( STV* it, int wlen, NameVote** winnersP, RoundScore** roundsP ) {
	int numActive;
	int numWinners;
	int i;
	int numc = it->ni->nextIndex;
	double quota;
	TallyStatus* ts;
	RoundScore* rounds = NULL;

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
	assert( it->winners != NULL );
	ts = (TallyStatus*)malloc( sizeof(TallyStatus)*numc );
	assert( ts != NULL );

	for ( i = 0; i < numc; i++ ) {
		ts[i].weight = 1.0;
		ts[i].status = STATUS_ACTIVE;
	}
	
	quota = it->numVotes / ( it->seats + 1.0 );

	while ( numActive > 1 && numWinners < it->seats && numActive > it->seats ) {
		// Setup explain storage, if needed.
		if ( roundsP != NULL ) {
			if ( rounds == NULL ) {
				rounds = newRoundScore( numc );
				*roundsP = rounds;
			} else {
				rounds->next = newRoundScore( numc );
				rounds = rounds->next;
			}
		}
		
		it->deadVotes = internalRecount( it, ts, quota );
		quota = (it->numVotes - it->deadVotes) / ( it->seats + 1.0 );
		numWinners = settleWinners( it, ts, quota );
		assert(numWinners <= it->seats);
		
		// Copy status to explain storage, if needed.
		if ( rounds != NULL ) {
			memcpy(rounds->ts, ts, sizeof(TallyStatus) * numc);
			rounds->quota = quota;
			rounds->deadVote = it->deadVotes;
		}
		if ( numWinners < it->seats ) {
			double min = 0.0;
			int mini = -1;
			int tied = 1;
			for ( i = 0; i < numc; ++i ) {
				if ( ts[i].status != STATUS_DEAD ) {
					if ( (mini == -1) || (ts[i].tally < min) ) {
						min = ts[i].tally;
						mini = i;
						tied = 1;
					} else if ( ts[i].tally == min ) {
						tied++;
					}
				}
			}
			assert(mini != -1);
			if ( numActive - tied < it->seats ) {
				// fail!
				fprintf(stderr, "STV failed, too many tied for disqualification\n");
			}
			for ( i = 0; i < numc; i++ ) {
				if ( ts[i].tally == min ) {
					ts[i].status = STATUS_DEAD;
					numActive--;
					it->winners[numActive].name = indexName( it->ni, i );
					it->winners[numActive].rating = ts[i].tally;
					if ( debug ) {
						printf("STV dq \"%s\" = %f<br>\n", indexName( it->ni, i ), ts[i].tally );
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
		if ( ts[i].status != STATUS_DEAD ) {
			numActive--;
			it->winners[numActive].name = indexName( it->ni, i );
			it->winners[numActive].rating = ts[i].tally;
		}
	}
	sortNameVotes( it->winners, numc );
	free( ts );
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
int STV_getWinners( STV* it, int wlen, NameVote** winnersP ) {
	return STV_getWinnersInternal( it, wlen, winnersP, NULL );
}

static void STV_resultTable( STV* it, FILE* fout, NameVote* winners, int numw ) {
	int i;
	fprintf( fout, "<table border=\"1\"><tr><th>Name</th><th>Best STV Count</th></tr>" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr bgcolor=\"%s\"><td>%s</td><td>%d</td></tr>", (i < it->seats) ? "#ccffcc" : "#ffcccc", winners[i].name, (int)(winners[i].rating) );
	}
	fprintf( fout, "</table>\n");
}

void STV_htmlSummary( STV* it, FILE* fout ) {
	int numw;
	NameVote* winners;

	numw = STV_getWinners( it, 0, &winners );
	if ( numw <= 0 ) {
		fprintf( fout, "<p>STV returns no winners.</p>\n" );
		return;
	}
	STV_resultTable( it, fout, winners, numw );
}
static const char greyStyle[] = " style=\"color:#999999;\"";
static const char greenStyle[] = " style=\"color:#00cc00;\"";
void STV_htmlExplain( STV* it, FILE* fout ) {
	int numw;
	NameVote* winners;
	int i, c, numc;
	RoundScore* rounds = NULL;
	RoundScore* cr;
	int numrounds = 0;
	
	numw = STV_getWinnersInternal( it, 0, &winners, &rounds );
	if ( numw <= 0 ) {
		fprintf( fout, "<p>STV returns no winners.</p>\n" );
		return;
	}

	cr = rounds;
	while ( cr != NULL ) {
		numrounds++;
		cr = cr->next;
	}
	numc = it->ni->nextIndex;

	// Top row. Round numbers.
	fprintf( fout, "<table border=\"1\"><tr><th rowspan=\"2\">Name</th>");
	for ( i = 0; i < numrounds; ++i ) {
		fprintf( fout, "<th colspan=\"2\">Round %d</th>", i+1 );
	}
	fprintf( fout, "</tr>\n<tr>" );
	// Second row. Column labels.
	for ( i = 0; i < numrounds; ++i ) {
		fprintf( fout, "<th>Tally</th><th>Weight</th>" );
	}
	fprintf( fout, "</tr>\n" );
	// Data rows, in order of winners.
	for ( c = 0; c < numw; ++c ) {
		cr = rounds;
		fprintf( fout, "<tr><td>%s</td>", winners[c].name );
		while ( cr != NULL ) {
			const char* style;
			for ( i = 0; i < numc; ++i ) {
				if ( winners[c].name == indexName( it->ni, i ) ) {
					break;
				}
			}
			assert(i < numc);
			style = (cr->ts[i].status == STATUS_DEAD) ? greyStyle :
				(cr->ts[i].status == STATUS_ELECTED) ? greenStyle : "";
			fprintf( fout, "<td%s>%.2f</td><td%s>%.2f</td>", style, cr->ts[i].tally, style, cr->ts[i].weight );
			cr = cr->next;
		}
		fprintf( fout, "</tr>\n" );
	}
	// Bottom row. Summary data.
	fprintf( fout, "<tr><td></td>" );
	cr = rounds;
	while ( cr != NULL ) {
		fprintf( fout, "<td colspan=\"2\">Total Vote: %.2f (%.2f dead)<br>Quota: %.2f</td>",
			it->numVotes - cr->deadVote, cr->deadVote, cr->quota );
		cr = cr->next;
	}
	fprintf( fout, "</tr>\n</table>\n" );

	STV_resultTable( it, fout, winners, numw );
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
	vr->vvs.htmlExplain = (vvs_htmlSummary)STV_htmlExplain;
	if ( initSTV( &(vr->rr) ) != 0 ) {
		free( vr );
		return NULL;
	}
	toret->it = &vr->rr;
	return toret;
}
