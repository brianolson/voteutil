#include "Condorcet.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define dbprint( fout ) for ( int i = 0; i < numc; i++ ) {\
	for ( int ii = 0; ii < numc; ii++ ) {\
		fprintf(fout,"%d\t", talley[i*numc + ii] );\
	}\
	fprintf(fout,"\n");\
}\
fflush(fout);

Condorcet::Condorcet( int numCandidates ) : RatedVotingSystem( numCandidates ) {
	talley = new unsigned int[numc*numc];
	defeatCount = new unsigned int[numc];
	for ( int i = 0; i < numc*numc; i++ ) {
		talley[i] = 0;
	}
}

Condorcet::~Condorcet() {
	if ( talley ) {
		delete [] talley;
	}
	if ( defeatCount ) {
		delete [] defeatCount;
	}
}

void runBeatpath( const unsigned int* talley, unsigned int* bpm, int numc, int depth );

inline int ratingCleanse( int r ) {
	return (( r == NO_VOTE ) ? 0 : r );
}

int Condorcet::voteRating( int* rating ) {
	int j, k;
	//    addRatingToHist( rating );
	// foreach pair of candidates, vote 1-on-1
	for ( j = 0; j < numc; j++ ) {
		int pj;
		pj = ratingCleanse( rating[j] );
		//	fprintf(stderr,"rating[%d] = %d\n", j, pj );
		for ( k = j + 1; k < numc; k++ ) {
			int pk;
			pk = ratingCleanse( rating[k] );
			if ( pk > pj ) {
				talley[k*numc + j]++;	// k beats j
				//		fprintf(stderr,"k %d (%d) beats j %d (%d)\n", k, pk, j, pj );
			} else if ( pj > pk ) {
				talley[j*numc + k]++;	// j beats k
				//		fprintf(stderr,"k %d (%d) loses j %d (%d)\n", k, pk, j, pj );
			}
		}
		//dbprint( stdout );
	}
	return 0;
}

int Condorcet::getWinners( int* choiceIndecies ) {
	int j,k;
	int numWinners;

	//dbprint( stdout );
	for ( j = 0; j < numc; j++ ) {
		defeatCount[j] = 0;
	}
	/* count raw defeats */
	for ( j = 0; j < numc; j++ ) {
		for ( k = j + 1; k < numc; k++ ) {
			unsigned int vk, vj;
			vk = talley[k*numc + j];	// k beat j vk times
			vj = talley[j*numc + k];	// j beat k vj times
			if ( vj > vk ) {
				defeatCount[k]++;
			} else if ( vj < vk ) {
				defeatCount[j]++;
			}
		}
	}
	numWinners = 0;
	/* count undefeated choices */
	for ( j = 0; j < numc; j++ ) {
		//fprintf( stderr, "%d defeated %d\n", j, defeatCount[j] );
		if ( defeatCount[j] == 0 ) {
			choiceIndecies[numWinners] = j;
			numWinners++;
		}
	}
	if ( numWinners == 1 ) {
		return numWinners;
	}
	unsigned int* bpm;
	int winsize = INT_MIN;
	bpm = new unsigned int[numc*numc];
	runBeatpath( talley, bpm, numc, 0 );
	for ( j = 0; j < numc; j++ ) {
		int winsizet;
		winsizet = INT_MIN;
		defeatCount[j] = 0;
		for ( k = 0; k < numc; k++ ) if ( k != j ) {
			int bpmt = bpm[j*numc + k];
			if ( bpmt == 0 ) {
				defeatCount[j]++;
			} else if ( bpmt > winsizet ) {
				winsizet = bpmt;
			}
		}
		if ( defeatCount[j] == 0 ) {
			if ( winsizet > winsize ) {
				choiceIndecies[0] = j;
				numWinners = 1;
				winsize = winsizet;
			} else if ( winsizet == winsize ) {
				choiceIndecies[numWinners] = j;
				numWinners++;
				//winsize = winsizet;
			}
		}
	}
#if 0
	if ( numWinners != 1 ) {
		printf("<pre>no Condorcet-beatpath winner, bpm array:\n");
		for ( j = 0; j < numc; j++ ) {
			printf("dc(%d)\t", defeatCount[j] );
			for ( k = 0; k < numc; k++ ) {
				printf("%d\t", bpm[j*numc + k] );
			}
			printf("\n");
		}
		printf("\ntalley array:\n");
		for ( j = 0; j < numc; j++ ) {
			for ( k = 0; k < numc; k++ ) {
				printf("%d\t", talley[j*numc + k] );
			}
			printf("\n");
		}
		printf("</pre>\n");
	}
#endif
	delete [] bpm;
	if ( numWinners == 0 ) {
#if 0
		// fall back to borda to get one
		RankedVotePickOne::runElection( winnerR, they, numv, numc );
#elif 0
		// FIXME write an option to return ties
#elif 0
		// another alternative, random pick from tie (not for real-world)
#else
		//	printf("no Condorcet-beatpath winner, going with 0\n");
#endif
	}
	return numWinners;
}

int Condorcet::write( int fd ) {
	return 0;
}

int Condorcet::read( int fd ) {
	return 0;
}

inline unsigned int umin( unsigned int a, unsigned int b ) {
	if ( a < b ) {
		return a;
	} else {
		return b;
	}
}
/**
bpm Beat Path Matrix, filled in as we go.
*/
void runBeatpath( const unsigned int* talley, unsigned int* bpm, int numc, int depth ) {
	int j, k;
	// foreach pair of candidates
	bool notDone = true;
	for ( j = 0; j < numc; j++ ) {
		bpm[j*numc + j] = 0;
		for ( k = j + 1; k < numc; k++ ) {
			int vj, vk;
			vk = talley[k*numc + j];	// k beat j vk times
			vj = talley[j*numc + k];	// j beat k vj times
			if ( vk > vj ) {
				bpm[k*numc + j] = vk;
				bpm[j*numc + k] = 0;
			} else if ( vj > vk ) {
				bpm[k*numc + j] = 0;
				bpm[j*numc + k] = vj;
			} else /* vj == vk */ {
#if 01
				bpm[k*numc + j] = 0;
				bpm[j*numc + k] = 0;
#endif
			}
		}
	}

	while ( notDone ) {
		notDone = false;
#if 0
		for ( j = 0; j < numc; j++ ) {
			for ( k = 0; k < numc; k++ ) {
				printf("%d\t", bpm[j*numc + k] );
			}
			printf("\n");
		}
		printf("\n");
#endif
		for ( j = 0; j < numc; j++ ) {
			for ( k = 0; k < numc; k++ ) if ( k != j ) {
				int vk;
				vk = bpm[k*numc + j];	// candidate k > j
				if ( vk != 0 ) {
					// sucessful beat, see if it can be used to get a better beat over another
					for ( int l = 0; l < numc; l++ ) if ( l != j && l != k ) { // don't care about self (k) or same (j)
						unsigned int vl;
						vl = umin( bpm[j*numc + l], vk );	// j > l
						if ( /*vl != 0 &&*/ vl > bpm[k*numc + l] ) {
							// better beat path found
							//			    printf("set bpm[%d * %d + %d] = %d\n", k, numc, l, vl);
							bpm[k*numc + l] = vl;
							notDone = true;
						}
					}
				}
			}
		}
	}
	for ( j = 0; j < numc; j++ ) {
		for ( k = j + 1; k < numc; k++ ) {
			int vj, vk;
			vk = bpm[k*numc + j];
			vj = bpm[j*numc + k];
			if ( vk > vj ) {
				//bpm[k*numc + j] = vk;
				bpm[j*numc + k] = 0;
			} else if ( vj > vk ) {
				bpm[k*numc + j] = 0;
				//bpm[j*numc + k] = vj;
			}
		}
	}
#if 0
	for ( j = 0; j < numc; j++ ) {
		for ( k = 0; k < numc; k++ ) {
			fprintf( stderr, "%d\t", bpm[j*numc + k] );
		}
		fprintf( stderr, "\n");
	}
#endif
}

VotingSystem* Condorcet::newCondorcet( int numc ) {
	return new Condorcet( numc );
}

void Condorcet::htmlSummary( FILE* fout, char** names ) {
	fprintf( fout, "<table border=\"1\"><tr><th>" );
	if ( names ) {
		//	fprintf( fout, "" );
	} else {
		fprintf( fout, "Choice Index" );
	}
	fprintf( fout, "</th><th colspan=\"%d\">times preferred over other choices</th></tr>\n", numc );
	for ( int i = 0; i < numc; i++ ) {
		fprintf( fout, "<tr><td>" );
		if ( names ) {
			fprintf( fout, names[i] );
		} else {
			fprintf( fout, "%d", i );
		}
		fprintf( fout, "</td>" );
		for ( int j = 0; j < numc; j++ ) {
			fprintf( fout, "<td>%d</td>", talley[i*numc + j] );
		}
		fprintf( fout, "</tr>\n" );
	}
	fprintf( fout, "</table>\n" );
#if 01
	fprintf( fout, "<table border=\"1\"><tr><th>" );
	if ( names ) {
		//	fprintf( fout, "" );
	} else {
		fprintf( fout, "Choice Index" );
	}
	fprintf( fout, "</th><th colspan=\"%d\">times defeated by another choice</th></tr>\n", numc );
	unsigned int mindef = INT_MAX;
	for ( int i = 0; i < numc; i++ ) {
		if ( defeatCount[i] < mindef ) {
			mindef = defeatCount[i];
		}
	}
	while ( mindef != INT_MAX ) {
		unsigned int nextmd;
		nextmd = INT_MAX;
		for ( int i = 0; i < numc; i++ ) {
			if ( defeatCount[i] == mindef ) {
				fprintf( fout, "<tr><td>" );
				if ( names ) {
					fprintf( fout, names[i] );
				} else {
					fprintf( fout, "%d", i );
				}
				fprintf( fout, "</td><td>%d</td></tr>\n", defeatCount[i] );
			} else if ( (defeatCount[i] > mindef) && (defeatCount[i] < nextmd) ) {
				nextmd = defeatCount[i];
			}
		}
		mindef = nextmd;
	}
	fprintf( fout, "</table>\n" );
#endif
}
void Condorcet::print( FILE* fout, char** names ) {
	for ( int i = 0; i < numc; i++ ) {
		for ( int ii = 0; ii < numc; ii++ ) {
			fprintf(fout,"%d\t", talley[i*numc + ii] );
		}
		fprintf(fout,"\n");
	}
	fflush(fout);
}
