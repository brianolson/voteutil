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
	it->winners = NULL;
	it->explain = NULL;
	it->ni = malloc(sizeof(NameIndex));
	if ( it->ni == NULL ) {
		free( it->counts );
		return -1;
	}
	initNameIndex( it->ni );
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

static __inline void _setxy( VRR* it, int x, int y, int value ) {
	if (x > y) {
		it->counts[x][y] = value;
	} else {
		it->counts[y][y+x] = value;
	}
}
#define setxy( x, y, v ) _setxy( it, x, y, v )

static __inline void _incxy( VRR* it, int x, int y ) {
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

static int voteUnvoted( VRR* it, int votesLength, const NameVote* votes, int* indexes, NameIndexEntry* cur ) {
	int i;
	int unvoted = 1;
	for ( i = 0; i < votesLength; i++ ) {
		if ( indexes[i] == cur->index ) {
			unvoted = 0;
			break;
		}
	}
	if ( unvoted ) {
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		int j;
		for ( j = 0; j < votesLength; j++ ) {
			if ( votes[j].rating >= 0 ) {
				incxy( indexes[j], cur->index );
			} else {
				incxy( cur->index, indexes[j] );
			}
		}
	}
	i = 0;
	if ( cur->lt != NULL ) {
		i |= voteUnvoted( it, votesLength, votes, indexes, cur->lt );
	}
	if ( cur->gt != NULL ) {
		i |= voteUnvoted( it, votesLength, votes, indexes, cur->gt );
	}
	return i;
}
int VRR_voteRating( VRR* it, int votesLength, const NameVote* votes ) {
	int i, j;
	int indexes[votesLength];
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	// preload indexes, only do lookup n, not (n^2)/2
	for ( i = 0; i < votesLength; i++ ) {
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
	for ( i = 0; i < votesLength; i++ ) {
		for ( j = i + 1; j < votesLength; j++ ) {
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
	i = voteUnvoted( it, votesLength, votes, indexes, it->ni->root );
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
		double r = votes->vote[i].rating;
		x = votes->vote[i].index;
		// vote vs unvoted dummy
		if ( r >= 0 ) {
			incxy( x, it->numc );
		} else {
			incxy( it->numc, x );
		}
		for ( j = i + 1; j < votes->numVotes; j++ ) {
			if ( r > votes->vote[j].rating ) {
				incxy(x, votes->vote[j].index);
			} else if ( votes->vote[j].rating > r ) {
				incxy(votes->vote[j].index, x);
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
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	it->winners = (NameVote*)malloc( sizeof(NameVote)*it->numc );
	assert( it->winners != NULL );
	for ( i = 0; i < it->numc; i++ ) {
		it->winners[i].name = indexName( it->ni, i );
		it->winners[i].rating = 0.0 - defeatCount[i];
	}
	sortNameVotes( it->winners, it->numc );
}
static int VRR_returnWinners( VRR* it, int winnersLength, NameVote** winnersP ) {
	if ( *winnersP != NULL && winnersLength > 0 ) {
		// copy into provided return space
		NameVote* winners = *winnersP;
		int lim = it->numc < winnersLength ? it->numc : winnersLength;
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
static int VRR_CSSD( VRR* it, int winnersLength, NameVote** winnersP, int* defeatCount );
static int VRR_RankedPairs( VRR* it, int winnersLength, NameVote** winnersP, int* defeatCount );

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
static int VRR_plainCondorcet( VRR* it, int winnersLength, NameVote** winnersP, int** defeatCountP ) {
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
			return VRR_returnWinners( it, winnersLength, winnersP );
		}
	}
	*defeatCountP = defeatCount;
	return -1;
}

int VRR_getWinners( VRR* it, int winnersLength, NameVote** winnersP ) {
	return VRR_getWinnersCSSD( it, winnersLength, winnersP );
}
int VRR_getWinnersCSSD( VRR* it, int winnersLength, NameVote** winnersP ) {
	int* defeatCount;
	int result;

	if ( it->numc <= 0 ) {
		return -1;
	}
	result = VRR_plainCondorcet( it, winnersLength, winnersP, &defeatCount );
	if ( result >= 0 ) {
		return result;
	}
	return VRR_CSSD( it, winnersLength, winnersP, defeatCount );
}
int VRR_getWinnersRankedPairs( VRR* it, int winnersLength, NameVote** winnersP ) {
	int* defeatCount;
	int result;
	
	if ( it->numc <= 0 ) {
		return -1;
	}
	result = VRR_plainCondorcet( it, winnersLength, winnersP, &defeatCount );
	if ( result >= 0 ) {
		return result;
	}
	return VRR_RankedPairs( it, winnersLength, winnersP, defeatCount );
}

typedef struct minij {
	int ihi;
	int ilo;
} minij;

// I believe this correctly implements Cloneproof Schwartz Set Dropping, aka
// the Schulze method.
// http://wiki.electorama.com/wiki/Schulze_method
static int VRR_CSSD( VRR* it, int winnersLength, NameVote** winnersP, int* defeatCount ) {
	int numc = it->numc;
	int i, j;
	int* tally;
	int* ss;
	minij* mins;
	int numWinners;
	int mind; // minimum defeat, index and strength
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
	mins = malloc( sizeof(minij)*numc );
	assert( mins != NULL );
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
		mind = INT_MAX;
		tie = 0;
		if ( it->explain != NULL ) {
			assert(numWinners > 0);
			fprintf((FILE*)it->explain, "<p>Top choices: %s", indexName(it->ni, ss[0]));
			for ( i = 1; i < numWinners; ++i ) {
				fprintf((FILE*)it->explain, ", %s", indexName(it->ni, ss[i]) );
			}
			fprintf((FILE*)it->explain, "</p>");
		}
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
				int ihi, ilo;
				int vhi, vlo;
				int m;

				k = ss[ki];
				vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ((vk == -1) && (vj == -1)) {
					continue;
				}
				if ( vk > vj ) {
					ihi = k;
					ilo = j;
					vhi = vk;
					vlo = vj;
				} else /*if ( vj > vk )*/ {
					// A tie is indeed a weak defeat, probably the weakest, and
					// it doesn't matter in which direction it is considered for deletion.
					ihi = j;
					ilo = k;
					vhi = vj;
					vlo = vk;
				}
				if ( winningVotes ) {
					m = vhi;
				} else if ( margins ) {
					m = vhi - vlo;
				} else {
					assert(0);
				}
				if ( m < mind ) {
					tie = 1;
					mind = m;
					mins[0].ihi = ihi;
					mins[0].ilo = ilo;
				} else if ( m == mind ) {
					mins[tie].ihi = ihi;
					mins[tie].ilo = ilo;
					tie++;
				}
			}
		}
		if ( tie == 0 ) {
			if ( debug ) {
				fprintf(stderr, "tie = 0, no weakest defeat found to cancel\n");
			}
			goto finish;
		}
		// all are tied
		if ( tie == numWinners) {
			if ( debug ) {
				fprintf(stderr, "tie==numWinners==%d, mind=%d\n", tie, mind);
			}
			goto finish;
		}
		for ( i = 0; i < tie; ++i ) {
			int mindk = mins[i].ihi;
			int mindj = mins[i].ilo;
			if ( it->explain != NULL ) {
				fprintf((FILE*)it->explain, "<p>Weakest defeat is %s (%d) v %s (%d). %s has one fewer defeat.</p>\n",
						indexName(it->ni, mindk), tally[mindk*numc + mindj],
						indexName(it->ni, mindj), tally[mindj*numc + mindk],
						indexName(it->ni, mindj)
						);
			}
			tally[mindk*numc + mindj] = -1;
			tally[mindj*numc + mindk] = -1;
			defeatCount[mindj]--;
		}
		numWinners = getSchwartzSet( it, tally, defeatCount, ss );
		if ( numWinners == 1 ) {
			goto finish;
		}
		if ( debug ) {
			assert(numWinners > 0);
			fprintf(stderr, "ss={ %d", ss[0] );
			for ( j = 1; j < numWinners; j++ ) {
				fprintf(stderr, ", %d", ss[j] );
			}
			fprintf(stderr, " }\n");
		}
	}
	finish:
	free( mins );
	free( ss );
	free( tally );
	VRR_makeWinners( it, defeatCount );
	free( defeatCount );
	return VRR_returnWinners( it, winnersLength, winnersP );
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
					if ( debug ) {
						fprintf(stderr, "choice %d in bad schwartz set defeated by %d not in set\n", m, j);
					}
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
			if ( debug ) {
				fprintf(stderr, "choice %d in bad schwartz is defeated by all in set.\n", m);
			}
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
#ifndef NDEBUG
	if ( ! verifySchwartzSet( numc, tally, sset, numWinners ) ) {
		fprintf(stderr, "getSchwartzSet is returning an invalid Schwartz set!\n");
		assert(numWinners > 0);
		// TODO: it would be nice to dump the intermediate count table here
		fprintf(stderr, "bad sset: %d", sset[0] );
		for ( j = 1; j < numWinners; j++ ) {
			fprintf(stderr, ", %d", sset[j]);
		}
	}
#endif
	free(choiceIndecies);
	return numWinners;
}

typedef struct pair {
	// i always winner, j always loser.
	int i, j;
	int Vij, Vji;
	int active;
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
#if verbose
	int depth = 0;
	static char depthstr[] = "                                                            ";  // 60 spaces
	static char* depthstrEnd = depthstr + 60;
	char* prefix;
	{
		SearchStack* cur = up;
		while ( cur != NULL ) {
			depth++;
			cur = cur->prev;
		}
		assert(depth < 30);
		prefix = (depthstrEnd - (depth*2));
	}
#endif
	here.from = from;
	here.prev = up;
#if verbose
	fprintf(stderr, "%sfindpath(:%d) %d->%d\n", prefix, numranks-1, from, to);
#endif
	for ( i = 0; i < numranks; ++i ) {
		if ( !ranks[i].active ){
			continue;
		}
		if ( ranks[i].i == from ) {
			if ( ranks[i].j == to ) {
#if verbose
				fprintf(stderr, "%sfound %d->%d\n", prefix, from, to);
#endif
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
#if verbose
					fprintf(stderr, "%sthrough %d\n", prefix, ranks[i].j);
#endif
					maybepath = findPath(ranks, numranks, ranks[i].j, to, &here);
					if ( maybepath != 0 ) {
#if verbose
						fprintf(stderr, "%sfound %d->%d\n", prefix, from, to);
#endif
						return maybepath;
					} else {
#if verbose
						fprintf(stderr, "%sthrough %d fails\n", prefix, ranks[i].j);
#endif
					}
				}
			}
		}
	}
	return 0;
}

// http://wiki.electorama.com/wiki/Ranked_Pairs
int VRR_RankedPairs( VRR* it, int winnersLength, NameVote** winnersP, int* defeatCount ) {
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
				ranks[x].active = 1;
				x++;
			} else if ( ji > ij ) {
				ranks[x].i = j;
				ranks[x].j = i;
				ranks[x].Vij = ji;
				ranks[x].Vji = ij;
				ranks[x].active = 1;
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
	if ( it->explain != NULL ) {
		fprintf((FILE*)it->explain, "<p>Initial pair rankings:</p><table border=\"0\">\n" );
		for ( i = 0; i < x; ++i ) {
			fprintf((FILE*)it->explain, "<tr><td>%s</td><td>&gt;</td><td>%s</td><td>(%d > %d)</td></tr>\n",
					indexName(it->ni, ranks[i].i), indexName(it->ni, ranks[i].j), ranks[i].Vij, ranks[i].Vji);
		}
		fprintf((FILE*)it->explain, "</table>\n<p>");
	}
	i = 1;
	while ( i < x ) {
		int equivalenceLimit = i;
		while ( (equivalenceLimit + 1) < x ) {
			if (pair_cmp(ranks + equivalenceLimit, ranks + (equivalenceLimit + 1))) {
				break;
			}
			++equivalenceLimit;
		}
		if ( findPath(ranks, equivalenceLimit, ranks[i].j, ranks[i].i, NULL) ) {
			// drop this link as there is a pre-existing reverse path
			if ( verbose ) {
				fprintf(stderr, "DROP: %3d > %3d (%5d > %5d)\n", ranks[i].i, ranks[i].j, ranks[i].Vij, ranks[i].Vji);
			}
			if ( it->explain != NULL ) {
				fprintf((FILE*)it->explain, "DROP: %s > %s (%d > %d)<br />\n",
						indexName(it->ni, ranks[i].i), indexName(it->ni, ranks[i].j), ranks[i].Vij, ranks[i].Vji);
			}
			ranks[i].active = 0;
		}
		++i;
	}
	if ( it->explain != NULL ) {
		fprintf((FILE*)it->explain, "</p><p>Final pair rankings:</p><table border=\"0\">\n" );
		for ( i = 0; i < x; ++i ) {
			fprintf((FILE*)it->explain, "<tr%s><td>%s</td><td>&gt;</td><td>%s</td><td>(%d > %d)</td></tr>\n",
					(ranks[i].active ? "" : " style=\"color: #999999\""),
					indexName(it->ni, ranks[i].i), indexName(it->ni, ranks[i].j), ranks[i].Vij, ranks[i].Vji);
		}
		fprintf((FILE*)it->explain, "</table>\n");
	}
	for ( i = 0; i < numc; ++i ) {
		defeatCount[i] = 0;
	}
	for ( i = 0; i < x; ++i ) {
		if ( ranks[i].active ) {
			defeatCount[ranks[i].j]++;
		}
	}
	VRR_makeWinners( it, defeatCount );
	free( defeatCount );
	return VRR_returnWinners( it, winnersLength, winnersP );
}

static void VRR_condorcetTable( VRR* it, FILE* fout, int numw, NameVote* winners );
static void simpleWinnerTable( FILE* fout, int numw, NameVote* winners );

void VRR_htmlSummary( VRR* it, FILE* fout ) {
	int numw;
	NameVote* winners = NULL;

	if ( it->winners == NULL ) {
		numw = VRR_getWinners( it, 0, &winners );
	} else {
		numw = it->numc;
		winners = it->winners;
	}
	VRR_condorcetTable( it, fout, numw, winners );
	simpleWinnerTable( fout, numw, winners );
}
void VRR_htmlSummaryRankedPairs( VRR* it, FILE* fout ) {
	int numw;
	NameVote* winners = NULL;
	
	if ( it->winners == NULL ) {
		numw = VRR_getWinnersRankedPairs( it, 0, &winners );
	} else {
		numw = it->numc;
		winners = it->winners;
	}
	VRR_condorcetTable( it, fout, numw, winners );
	simpleWinnerTable( fout, numw, winners );
}
static void VRR_condorcetTable( VRR* it, FILE* fout, int numw, NameVote* winners ) {
	int i, xi, xj;
	int* indextr;

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
}
static void simpleWinnerTable( FILE* fout, int numw, NameVote* winners ) {
	int i;
	fprintf( fout, "<table border=\"1\">" );
	for ( i = 0; i < numw; i++ ) {
		fprintf( fout, "<tr><td>%s</td><td>%.0f</tr>", winners[i].name, winners[i].rating );
	}
	fprintf( fout, "</table>\n" );
}
static void VRR_htmlExplainCommon( VRR* it, FILE* fout, int (*f)(VRR*,int,NameVote**,int*) ) {
	int numw;
	NameVote* winners = NULL;
	int* defeatCount;

	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	
	numw = VRR_plainCondorcet( it, 0, &winners, &defeatCount );
	if ( numw >= 0 ) {
		VRR_condorcetTable( it, fout, numw, winners );
		simpleWinnerTable( fout, numw, winners );
		return;
	} else {
		VRR_makeWinners( it, defeatCount );
		VRR_condorcetTable( it, fout, it->numc, it->winners );
	}
	it->explain = fout;
	numw = f( it, 0, &winners, defeatCount );
	it->explain = NULL;
	simpleWinnerTable( fout, numw, winners );
}
void VRR_htmlExplain( VRR* it, FILE* fout ) {
	VRR_htmlExplainCommon( it, fout, VRR_CSSD );
}
void VRR_htmlExplainRankedPairs( VRR* it, FILE* fout ) {
	VRR_htmlExplainCommon( it, fout, VRR_RankedPairs );
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
	toret->htmlExplain = (vvs_htmlSummary)VRR_htmlExplain;
	toret->it = &vr->rr;
	initVRR(&(vr->rr));
	return toret;
}

VirtualVotingSystem* newVirtualRankedPairs() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(VRR);
	toret->htmlExplain = (vvs_htmlSummary)VRR_htmlExplainRankedPairs;
	toret->getWinners = (vvs_getWinners)VRR_getWinnersRankedPairs;
	toret->it = &vr->rr;
	initVRR(&(vr->rr));
	return toret;
}

