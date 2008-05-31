#include "VRR.h"
#include <limits.h>
#include <assert.h>

#ifndef verbose
#define verbose 0
#else
#define verbose 1
#endif

static void printCounts( FILE* out, VRR* it );

static int initVRR( VRR* it ) {
	it->counts = (int**)malloc( sizeof(int**) );
	if ( it->counts == NULL ) {
		return -1;
	}
	it->numc = 0;
	it->ni = malloc(sizeof(NameIndex));
	if ( it->ni == NULL ) {
		free( it->counts );
		return -1;
	}
	initNameIndex( it->ni );
	it->winners = NULL;
	return 0;
}

VRR* newVRR() {
	VRR* it = (VRR*)malloc( sizeof(VRR) );
	if ( it == NULL ) {
		return it;
	}
	if ( initVRR( it ) != 0 ) {
		free( it );
		return NULL;
	}
	return it;
}

/**
	counts laid out in concentric rings.
	x, y and z indicate higher indexed counts.
	A 0 x y z
	B x 0 y z
	C y y 0 z
	D z z z 0
*/

// x defeats y
#define xy( x, y ) ( (x > y) ? (it->counts[x][y]) : (it->counts[y][y+x]) )
#define setxy( x, y, v ) do { if (x > y) { it->counts[x][y] = (v); }else{ it->counts[y][y+x] = (v); } } while (0)
//#define incxy( x, y ) do { if (x > y) { it->counts[x][y]++; }else{ it->counts[y][y+x]++; } } while (0)

static /*__inline*/ void _incxy( VRR* it, int x, int y ) {
	if (x > y) {
		it->counts[x][y]++;
	} else {
		it->counts[y][y+x]++;
	}
}
#define incxy( x, y ) _incxy( it, x, y )

int growVRR( VRR* it, int x ) {
	if ( debug ) {
		printf("growVRR %d\n", x );
	}
	{
		int** tc = (int**)realloc( it->counts, sizeof(int*) * (x+2) );
		if ( tc == NULL ) {
			fprintf(stderr,"realloc VRR counts array fails\n");
			return -1;
		}
		it->counts = tc;
	}
	while ( x >= it->numc ) {
		int ni = it->numc + 1;
		int j;
		it->counts[ni] = (int*)malloc( sizeof(int) * ni * 2 );
		assert( it->counts[ni] != NULL );
		it->counts[ni][it->numc] = 0;
		it->counts[ni][it->numc+ni] = 0;
		for ( j = 0; j < it->numc; j++ ) {
			it->counts[ni][j] = it->counts[it->numc][j];
			it->counts[ni][j + ni] = it->counts[it->numc][j + it->numc];
		}
		it->numc = ni;
	}
	return 0;
}

static int voteUnvoted( VRR* it, int numVotes, const NameVote* votes, int* indexes, NameIndexEntry* cur ) {
	int i;
	int unvoted = 1;
	for ( i = 0; i < numVotes; i++ ) {
		if ( indexes[i] == cur->index ) {
			unvoted = 0;
			break;
		}
	}
	if ( unvoted ) {
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		int j;
		for ( j = 0; j < numVotes; j++ ) {
			if ( votes[j].rating >= 0 ) {
				incxy( indexes[j], cur->index );
			} else {
				incxy( cur->index, indexes[j] );
			}
		}
	}
	i = 0;
	if ( cur->lt != NULL ) {
		i |= voteUnvoted( it, numVotes, votes, indexes, cur->lt );
	}
	if ( cur->gt != NULL ) {
		i |= voteUnvoted( it, numVotes, votes, indexes, cur->gt );
	}
	return i;
}
int VRR_voteRating( VRR* it, int numVotes, const NameVote* votes ) {
	int i, j;
	int indexes[numVotes];
	// preload indexes, only do lookup n, not (n^2)/2
	for ( i = 0; i < numVotes; i++ ) {
		int x;
		x = nameIndex( it->ni, votes[i].name );
		if ( x > it->numc ) {
			int err;
			err = growVRR( it, x );
			if ( err != 0 ) {
				return err;
			}
		}
		indexes[i] = x;
		// vote vs unvoted dummy
		if ( votes[i].rating >= 0 ) {
			incxy( x, it->numc + 1 );
		} else {
			incxy( it->numc + 1, x );
		}
	}
	for ( i = 0; i < numVotes; i++ ) {
		for ( j = i + 1; j < numVotes; j++ ) {
			if ( votes[i].rating > votes[j].rating ) {
				incxy(indexes[i],indexes[j]);
			} else if ( votes[j].rating > votes[i].rating ) {
				incxy(indexes[j],indexes[i]);
			} else {
				// tie rating policy?
			}
		}
	}
	// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
	i = voteUnvoted( it, numVotes, votes, indexes, it->ni->root );
	return i;
}
static int voteUnvotedSIVN( VRR* it, const StoredIndexVoteNode* votes, NameIndexEntry* cur ) {
	int i;
	int unvoted = 1;
	for ( i = 0; i < votes->numVotes; i++ ) {
		if ( votes->vote[i].index == cur->index ) {
			unvoted = 0;
			break;
		}
	}
	if ( unvoted ) {
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		int j;
		if ( debug ) {
			printf("unvoted %d \"%s\"\n", cur->index, indexName( it->ni, cur->index ) );
		}
		for ( j = 0; j < votes->numVotes; j++ ) {
			if ( votes->vote[j].rating >= 0 ) {
				incxy( votes->vote[j].index, cur->index );
			} else {
				incxy( cur->index, votes->vote[j].index );
			}
		}
	}
	i = 0;
	if ( cur->lt != NULL ) {
		i |= voteUnvotedSIVN( it, votes, cur->lt );
	}
	if ( cur->gt != NULL ) {
		i |= voteUnvotedSIVN( it, votes, cur->gt );
	}
	return i;
}
int VRR_voteStoredIndexVoteNode( VRR* it, StoredIndexVoteNode* votes ) {
	int i, j;
	// preload indexes, get growth out of the way, vote against dummy
	for ( i = 0; i < votes->numVotes; i++ ) {
		int x;
		x = votes->vote[i].index;
		if ( x >= it->numc ) {
			int err;
			err = growVRR( it, x );
			if ( err != 0 ) {
				return err;
			}
		}
	}
	for ( i = 0; i < votes->numVotes; i++ ) {
		int x;
		x = votes->vote[i].index;
		// vote vs unvoted dummy
		if ( votes->vote[i].rating >= 0 ) {
			incxy( x, it->numc );
		} else {
			incxy( it->numc, x );
		}
	}
	for ( i = 0; i < votes->numVotes; i++ ) {
		for ( j = i + 1; j < votes->numVotes; j++ ) {
			if ( votes->vote[i].rating > votes->vote[j].rating ) {
				incxy(votes->vote[i].index,votes->vote[j].index);
			} else if ( votes->vote[j].rating > votes->vote[i].rating ) {
				incxy(votes->vote[j].index,votes->vote[i].index);
			} else {
				// tie rating policy?
			}
		}
	}
	// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
	i = voteUnvotedSIVN( it, votes, it->ni->root );
	return i;
}

static void countDefeats( VRR* it, int* defeatCount ) {
	int i, j;
	for ( i = 0; i < it->numc; i++ ) {
		defeatCount[i] = 0;
	}
	for ( i = 0; i < it->numc; i++ ) {
		for ( j = i + 1; j < it->numc; j++ ) {
			int ij, ji;
			ij = xy(i,j);
			ji = xy(j,i);
			if ( ij > ji ) {
				defeatCount[j]++;
			} else if ( ji > ij ) {
				defeatCount[i]++;
			} else {
				// tie policy?
			}
		}
	}
}
void VRR_makeWinners( VRR* it, int* defeatCount ) {
	int i;
	int notdone;
	it->winners = (NameVote*)malloc( sizeof(NameVote)*it->numc );
	assert( it->winners != NULL );
	for ( i = 0; i < it->numc; i++ ) {
		it->winners[i].name = indexName( it->ni, i );
		it->winners[i].rating = 0.0 - defeatCount[i];
	}
	// sort
	notdone = 1;
	while ( notdone ) {
		notdone = 0;
		for ( i = 1; i < it->numc; i++ ) {
			if ( it->winners[i-1].rating < it->winners[i].rating ) {
				float rating; const char* name;
				rating = it->winners[i].rating;
				name = it->winners[i].name;
				it->winners[i].rating = it->winners[i-1].rating;
				it->winners[i].name = it->winners[i-1].name;
				it->winners[i-1].rating = rating;
				it->winners[i-1].name = name;
				notdone = 1;
			}
		}
	}
}
int VRR_returnWinners( VRR* it, int numVotes, NameVote** winnersP ) {
	if ( *winnersP != NULL && numVotes > 0 ) {
		// copy into provided return space
		NameVote* winners = *winnersP;
		int lim = it->numc < numVotes ? it->numc : numVotes;
		int i;
		for ( i = 0; i < lim; i++ ) {
			winners[i] = it->winners[i];
		}
		return lim;
	} else {
		*winnersP = it->winners;
		return it->numc;
	}
}

static int getSchwartzSet( VRR* it, int* tally, int* defeatCount, int* sset );
static int VRR_CSSD( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount );
static int VRR_RankedPairs( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount );

int winningVotes = 1;
int margins = 0;
//int debug = 0;

static void printCounts( FILE* out, VRR* it ) {
	int i, j;
	for ( i = 0; i < it->numc; i++ ) {
		for ( j = 0; j < it->numc; j++ ) {
			if ( i == j ) {
				fprintf(out,"   ");
			} else {
				fprintf(out,"%2d ", xy(i,j) );
			}
		}
		printf("\n");
	}
}

static void printTally( FILE* out, int* tally, int numc ) {
	int i, j;
	for ( i = 0; i < numc; i++ ) {
		for ( j = 0; j < numc; j++ ) {
			fprintf(out,"%2d ", tally[i*numc + j] );
		}
		fprintf(out,"\n");
	}
}

// If there is a simple undefeated Condorcet winner, that is enough. Return the winner.
static int VRR_plainCondorcet( VRR* it, int numVotes, NameVote** winnersP, int** defeatCountP ) {
	int* defeatCount;
	int i;
	
	defeatCount = (int*)malloc( sizeof(int)*it->numc );
	assert( defeatCount != NULL );
	if ( debug ) {
		printCounts( stdout, it );
	}
	countDefeats( it, defeatCount );
	for ( i = 0; i < it->numc; i++ ) {
		if ( defeatCount[i] == 0 ) {
			// winner
			VRR_makeWinners( it, defeatCount );
			free( defeatCount );
			return VRR_returnWinners( it, numVotes, winnersP );
		}
	}
	*defeatCountP = defeatCount;
	return -1;
}

int VRR_getWinners( VRR* it, int numVotes, NameVote** winnersP ) {
	return VRR_getWinnersCSSD( it, numVotes, winnersP );
}
int VRR_getWinnersCSSD( VRR* it, int numVotes, NameVote** winnersP ) {
	int* defeatCount;

	int result = VRR_plainCondorcet( it, numVotes, winnersP, &defeatCount );
	if ( result >= 0 ) {
		return result;
	}
	return VRR_CSSD( it, numVotes, winnersP, defeatCount );
}
int VRR_getWinnersRankedPairs( VRR* it, int numVotes, NameVote** winnersP ) {
	int* defeatCount;
	
	int result = VRR_plainCondorcet( it, numVotes, winnersP, &defeatCount );
	if ( result >= 0 ) {
		return result;
	}
	return VRR_RankedPairs( it, numVotes, winnersP, defeatCount );
}

// I believe this correctly implements Cloneproof Schwartz Set Dropping, aka
// the Schulze method.
// http://wiki.electorama.com/wiki/Schulze_method
static int VRR_CSSD( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount ) {
	int numc = it->numc;
	int i, j;
	int* tally;
	int* ss;
	int numWinners;
	int mindj, mindk, mind; // minimum defeat, index and strength
	int tie = 0;

	if ( debug ) {
		fprintf(stderr,"pre cssd defeat count:");
		for ( i = 0; i < numc; i++ ) {
			fprintf(stderr," %d", defeatCount[i] );
		}
		fprintf(stderr,"\n");
	}

	tally = malloc( sizeof(int)*numc*numc );
	assert( tally != NULL );
	ss = malloc( sizeof(int)*numc );
	assert( ss != NULL );
	// Copy original VRR tally into local tally where defeats can be deleted.
	for ( i = 0; i < numc; i++ ) {
		for ( j = 0; j < numc; j++ ) {
			if ( i != j ) {
				tally[i*numc+j] = xy(i,j);
			} else {
				//tally[i*numc+j] = -1; // i==j should never be accessed
			}
		}
	}
	numWinners = getSchwartzSet( it, tally, defeatCount, ss );
	while ( 1 ) {
		int ji, ki;
		int defeatCountIncIndex = -1;
		mind = INT_MAX;
		mindj = -1;
		mindk = -1;
		tie = 0;
		if ( debug ) {
			printTally(stdout,tally,numc);
			fprintf(stderr,"ss (%d):", numWinners );
			for ( i = 0; i < numWinners; i++ ) {
				fprintf(stderr," %d", ss[i] );
			}
			fprintf(stderr,"\n");
		}
		// find weakest defeat between members of schwartz set
		for ( ji = 0; ji < numWinners - 1; ji++ ) {
			j = ss[ji];
			for ( ki = ji + 1; ki < numWinners; ki++ ) {
				int k;
				int vj, vk;
				k = ss[ki];
				vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ( vj > vk ) {
					if ( winningVotes ) {
						if ( vj < mind ) {
							mind = vj;
							mindj = j;
							mindk = k;
							tie = 1;
							defeatCountIncIndex = k;
						} else if ( vj == mind ) {
							tie++;
						}
					} else if ( margins ) {
						int m = vj - vk;
						if (m < mind) {
							mind = m;
							mindj = j;
							mindk = k;
							tie = 1;
							defeatCountIncIndex = k;
						} else if ( m == mind ) {
							tie++;
						}
					}
				} else if ( vk > vj ) {
					if ( winningVotes ) {
						if ( vk < mind ) {
							mind = vk;
							mindj = j;
							mindk = k;
							tie = 1;
							defeatCountIncIndex = j;
						} else if ( vk == mind ) {
							tie++;
						}
					} else if ( margins ) {
						int m = vk - vj;
						if (m < mind) {
							mind = m;
							mindj = j;
							mindk = k;
							tie = 1;
							defeatCountIncIndex = j;
						} else if ( m == mind ) {
							tie++;
						}
					}
				}
			}
		}
		if ( tie == 0 ) {
#if 0
			if ( debug ) {
				debugsb.append("tie = 0, no weakest defeat found to cancel\n");
			}
#endif
			goto finish;
		}
		// all are tied
		if ( tie == numWinners) {
#if 0
			if ( debug ) {
				debugsb.append("tie==numWinners, mind=").append(mind).append(", mindj=").append(mindj).append(", mindk=").append(mindk).append('\n');
			}
#endif
			goto finish;
		}
		tally[mindk*numc + mindj] = 0;
		tally[mindj*numc + mindk] = 0;
		defeatCount[defeatCountIncIndex]--;
#if 0
		if ( debug ) {
			debugfprintf( fout, mindk ).append( '/' ).append( mindj ).append(" = 0\n");
			htmlTable( debugsb, numc, tally, "intermediate", null );
		}
#endif
		numWinners = getSchwartzSet( it, tally, defeatCount, ss );
//		ss = getSchwartzSet( numc, tally, defeatCount, debugsb );
		if ( numWinners == 1 ) {
			goto finish;
		}
#if 0
		if ( debug ) {
			debugsb.append("ss={ ");
			debugfprintf( fout, ss[0] );
			for ( j = 1; j < numWinners; j++ ) {
				debugsb.append(", ");
				debugfprintf( fout, ss[j] );
			}
			debugsb.append(" }\n");
		}
#endif
	}
	finish:
	free( ss );
	free( tally );
	VRR_makeWinners( it, defeatCount );
	free( defeatCount );
	return VRR_returnWinners( it, numVotes, winnersP );
}



int verifySchwartzSet( int numc, int* tally, int* ss, int numWinners ) {
#ifndef NDEBUG
	int i, j, k;
	for ( i = 0; i < numWinners; i++ ) {
		int m, innerDefeats;
		m = ss[i];
		// check for defeats by choices outside the set
		for ( j = 0; j < numc; j++ ) {
			int notinset;
			notinset = 1;
			for ( k = 0; k < numWinners; k++ ) {
				if ( ss[k] == j ) {
					notinset = 0;
					break;
				}
			}
			if ( notinset ) {
				int vm, vj;
				vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
				vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
				if ( vj > vm ) {
#if 0
					if ( debugsb != null ) {
						fprintf(stderr, "choice %d in bad schwartz set defeated by %d not in set\n", m, j);
					}
#endif
					return 0;
				}
			}
		}
		// check if defated by all choices inside the set
		innerDefeats = 0;
		for ( k = 0; k < numWinners; k++ ) {
			int j;
			j = ss[k];
			if ( m != j ) {
				int vm, vj;
				vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
				vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
				if ( vj > vm ) {
					innerDefeats++;
				}
			}
		}
		if ( (innerDefeats > 0) && (innerDefeats == numWinners - 1) ) {
#if 0
			if ( debugsb != null ) {
				fprintf(stderr, "choice %d in bad schwartz is defeated by all in set.\n", m);
			}
#endif
			return 0;
		}
	}
#endif /* ! NDEBUG */
	// not disproven by exhaustive test, thus it's good
	return 1;
}

static int getSchwartzSet( VRR* it, int* tally, int* defeatCount, int* sset ) {
	int numc = it->numc;
	// start out set with first choice (probabbly replace it)
	int i,j,k;
	int mindefeats = defeatCount[0];
	int numWinners = 1;
	int* choiceIndecies = malloc(sizeof(int)*numc);
	assert( choiceIndecies != NULL );
	choiceIndecies[0] = 0;
	for ( j = 1; j < numc; j++ ) {
		if ( defeatCount[j] < mindefeats ) {
			choiceIndecies[0] = j;
			numWinners = 1;
			mindefeats = defeatCount[j];
		} else if ( defeatCount[j] == mindefeats ) {
			choiceIndecies[numWinners] = j;
			numWinners++;
		}
	}
	if ( mindefeats != 0 ) {
		// the best there is was defeated by some choice, make sure that is added to the set
		for ( i = 0; i < numWinners; i++ ) {
			// foreach k in set of least defeated ...
			k = choiceIndecies[i];
			for ( j = 0; j < numc; j++ ) if ( k != j ) {
				int vk, vj;
				vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ( vj > vk ) {
					// j defeats k, j must be in the set
					int gotj = 0;
					int si;
					for ( si = 0; si < numWinners; si++ ) {
						if ( choiceIndecies[si] == j ) {
							gotj = 1;
							break;
						}
					}
					if ( ! gotj ) {
						choiceIndecies[numWinners] = j;
						numWinners++;
					}
				}
			}
		}
	}
	for ( j = 0; j < numWinners; j++ ) {
		sset[j] = choiceIndecies[j];
	}
	if ( ! verifySchwartzSet( numc, tally, sset, numWinners ) ) {
#if 0
		//System.err.println("getSchwartzSet is returning an invalid Schwartz set!");
		if ( debugsb != null ) {
			htmlTable( debugsb, numc, tally, "tally not met by schwartz set", null );
			debugfprintf( fout, "bad sset: " );
			debugfprintf( fout, sset[0] );
			for ( j = 1; j < sset.length; j++ ) {
				debugsb.append(", ");
				debugsb.append(sset[j]);
			}
		}
#else
		fprintf(stderr,"verifySchwartzSet failed\n");
#endif
	}
	free(choiceIndecies);
	return numWinners;
}

typedef struct pair {
	// i always winner, j always loser.
	int i, j;
	int Vij, Vji;
} pair;

//  1 == a higher
//  0 == tie
// -1 == b higher
static __inline int pair_cmp(const pair* a, const pair* b) {
	if (a->Vij > b->Vij) {
		return 1;
	} else if (a->Vij < b->Vij) {
		return -1;
	} else {
		if (a->Vji < b->Vji) {
			return 1;
		} else if (a->Vji > b->Vji) {
			return -1;
		} else {
			return 0;
		}
	}
}
static int pair_cmp_qsort(const void* a, const void* b) {
	return pair_cmp((const pair*)a, (const pair*)b) * -1;
}

// Depth first search of fixed ranks to find established cycles.
typedef struct SeachStack {
	int from;
	struct SeachStack* prev;
} SearchStack;

int findPath(pair* ranks, int numranks, int from, int to, SearchStack* up) {
	SearchStack here;
	int i;
	here.from = from;
	here.prev = up;
	for ( i = 0; i < numranks; ++i ) {
		if ( ranks[i].i == from ) {
			if ( ranks[i].j == to ) {
				return 1;
			} else {
				int maybepath;
				int beenthere = 0;
				SearchStack* cur = up;
				while ( cur != NULL ) {
					if ( cur->from == ranks[i].j ) {
						beenthere = 1;
						break;
					}
					cur = cur->prev;
				}
				if ( ! beenthere ) {
					maybepath = findPath(ranks, numranks, ranks[i].j, to, &here);
					if ( maybepath != 0 ) {
						return maybepath;
					}
				}
			}
		}
	}
	return 0;
}

// http://wiki.electorama.com/wiki/Ranked_Pairs
int VRR_RankedPairs( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount ) {
	pair* ranks;
	int numc = it->numc;
	int i, j;
	int x = 0;

	ranks = (pair*)malloc( sizeof(pair) * ((numc * numc) / 2) );
	for ( i = 0; i < numc; ++i ) {
		for ( j = i + 1; j < numc; ++j ) {
			int ij, ji;
			ij = xy(i,j);
			ji = xy(j,i);
			if ( ij > ji ) {
				ranks[x].i = i;
				ranks[x].j = j;
				ranks[x].Vij = ij;
				ranks[x].Vji = ji;
				x++;
			} else if ( ji > ij ) {
				ranks[x].i = j;
				ranks[x].j = i;
				ranks[x].Vij = ji;
				ranks[x].Vji = ij;
				x++;
			} else {
				// tie policy?
			}
		}
	}
	qsort(ranks, x, sizeof(pair), pair_cmp_qsort);
	if ( verbose ) {
		for ( i = 0; i < x; ++i ) {
			fprintf(stderr, "%3d > %3d (%5d > %5d)\n", ranks[i].i, ranks[i].j, ranks[i].Vij, ranks[i].Vji);
		}
	}
	for ( i = 1; i < x; ++i ) {
		if ( findPath(ranks, i, ranks[i].i, ranks[i].j, NULL) ) {
			// drop this link as there is a pre-existing reverse path
			if ( verbose ) {
				fprintf(stderr, "DROP: %3d > %3d (%5d > %5d)\n", ranks[i].i, ranks[i].j, ranks[i].Vij, ranks[i].Vji);
			}
			x--;
			for ( j = i; j < x; ++j ) {
				ranks[j] = ranks[j+1];
			}
		}
	}
	{
		// make winners
		NameVote* winners;
		int winc = 0;
		it->winners = (NameVote*)malloc( sizeof(NameVote)*it->numc );
		winners = it->winners;
		for ( i = 0; i < x; ++i ) {
			int foundw;
			foundw = 0;
			for ( j = 0; j < i; ++j ) {
				if ( ranks[j].i == ranks[i].i ) {
					foundw = 1;  // already emitted
				}
			}
			if ( foundw == 0 ) {
				assert(winc < numc);
				winners[winc].name = indexName( it->ni, ranks[i].i );
				winners[winc].rating = numc - winc;
				winc++;
			}
		}
		assert(winc == numc);
	}
	free(ranks);
	return VRR_returnWinners( it, numVotes, winnersP );
}

void VRR_htmlSummary( VRR* it, FILE* fout ) {
	int numw;
	int i, xi, xj;
	NameVote* winners = NULL;
	int* indextr;

	if ( it->winners == NULL ) {
		numw = VRR_getWinners( it, 0, &winners );
	} else {
		numw = it->numc;
		winners = it->winners;
	}
	fprintf( fout, "<table border=\"1\"><tr><td></td>" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<td>(%d)</td>", i );
	}
	fprintf( fout, "</tr>" );
	indextr = (int*)malloc( sizeof(int)*numw );
	assert( indextr != NULL );
	for ( i = 0; i < numw; i++ ) {
		indextr[i] = nameIndex( it->ni, winners[i].name );
	}
	for ( xi = 0; xi < numw; xi++ ) {
		i = indextr[xi];
		fprintf( fout, "<tr><td>(%d) %s</td>", xi, winners[xi].name );
		for ( xj = 0; xj < numw; xj++ ) {
			int j;
			j = indextr[xj];
			if ( i == j ) {
				fprintf( fout, "<td></td>" );
			} else {
				int thisw, otherw;
				thisw = xy(i,j);
				otherw = xy(j,i);
				if ( thisw > otherw ) {
					fprintf( fout, "<td bgcolor=\"#bbffbb\">");
				} else if ( thisw < otherw ) {
					fprintf( fout, "<td bgcolor=\"#ffbbbb\">");
				} else {
					fprintf( fout, "<td>");
				}
				fprintf( fout, "%d</td>", thisw );
			}
		}
		fprintf( fout, "</tr>" );
	}
	free( indextr );
	indextr = NULL;
	fprintf( fout, "</table>" );
	fprintf( fout, "<table border=\"1\">" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%.0f</tr>", winners[i].name, winners[i].rating );
	}
	fprintf( fout, "</table>\n" );
}
void VRR_print( VRR* it, FILE* fout ) {

}
DECLARE_STD_setSharedNameIndex( VRR )

void clearVRR( VRR* it ) {
	int i;
	for ( i = 1; i <= it->numc; i++ ) {
		free( it->counts[i] );
	}
	free( it->counts );
	it->counts = NULL;
	it->numc = 0;
	if ( it->winners != NULL ) {
		free( it->winners );
	}
	clearNameIndex( it->ni );
}
void deleteVRR( VRR* it ) {
	clearVRR( it );
	free(it);
}

struct vvsrr {
	VirtualVotingSystem vvs;
	VRR rr;
};
void VRR_deleteVVS( VirtualVotingSystem* vvs ) {
	clearVRR( (VRR*)vvs->it );
	free( vvs );
}

VirtualVotingSystem* newVirtualVRR() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(VRR);
	toret->close = VRR_deleteVVS;
	toret->it = &vr->rr;
	return toret;
}

VirtualVotingSystem* newVirtualRankedPairs() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(VRR);
	toret->getWinners = (vvs_getWinners)VRR_getWinnersRankedPairs;
	toret->close = VRR_deleteVVS;
	toret->it = &vr->rr;
	return toret;
}

