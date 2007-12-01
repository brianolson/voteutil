#include "VRR.h"
#include <limits.h>
#include <assert.h>

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

static int getSchwartzSet( VRR* it, int numc, int* tally, int* defeatCount, int* sset );
static int VRR_getWinnersCSSD( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount );

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

int VRR_getWinners( VRR* it, int numVotes, NameVote** winnersP ) {
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
	return VRR_getWinnersCSSD( it, numVotes, winnersP, defeatCount );
}

static int VRR_getWinnersCSSD( VRR* it, int numVotes, NameVote** winnersP, int* defeatCount ) {
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
	for ( i = 0; i < numc; i++ ) {
		for ( j = 0; j < numc; j++ ) {
			if ( i != j ) {
				tally[i*numc+j] = xy(i,j);
			} else {
				//tally[i*numc+j] = -1; // i==j should never be accessed
			}
		}
	}
	numWinners = getSchwartzSet( it, numc, tally, defeatCount, ss );
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
		numWinners = getSchwartzSet( it, numc, tally, defeatCount, ss );
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
						debugsb.append("choice ");
						debugsb.append(m);
						debugsb.append(" in bad schwartz set defeated by ");
						debugsb.append(j);
						debugsb.append(" not in set\n");
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
				debugsb.append("choice ");
				debugsb.append(m);
				debugsb.append(" in bad schwartz is defeated by all in set.\n");
			}
#endif
			return 0;
		}
	}
	// not disproven by exhaustive test, thus it's good
	return 1;
}

static int getSchwartzSet( VRR* it, int numc, int* tally, int* defeatCount, int* sset ) {
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
