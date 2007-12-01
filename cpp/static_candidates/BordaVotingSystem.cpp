#include "BordaVotingSystem.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

BordaVotingSystem::~BordaVotingSystem() {
	if ( talley ) {
		delete [] talley;
	}
}

int BordaVotingSystem::voteRanking( int* ranking ) {
	int i;
	for ( i = 0; i < numc; i++ ) {
		if ( ranking[i] == NO_VOTE ) {
			// unranked, give no points
#if NO_CHECK_VALUES
#else
		} else if ( (ranking[i] < 1) || (ranking[i] > numc) ) {
			return -1;
#endif
		} else {
			talley[i] += numc - ranking[i];
		}
		//printf("talley[%d] = %d\n", i, talley[i] );
	}
	return 0;
}

int BordaVotingSystem::getWinners( int* choiceIndecies ) {
	int max = INT_MIN;
	int i;
	int numWinners;
	for ( i = 0; i < numc; i++ ) {
		if ( talley[i] > max ) {
			choiceIndecies[0] = i;
			numWinners = 1;
			max = talley[i];
		} else if ( talley[i] == max ) {
			choiceIndecies[numWinners] = i;
			numWinners++;
		}
	}
	return numWinners;
}

int BordaVotingSystem::write( int fd ) {
	return 0;
}

int BordaVotingSystem::read( int fd ) {
	return 0;
}

void BordaVotingSystem::htmlSummary( FILE* fout, char** names ) {
	int max;
	bool* in = new bool[numc];
	int maxi,i;
	for ( i = 0; i < numc; i++ ) {
		in[i] = true;
	}
	if ( names ) {
		fprintf( fout, "<table border=\"1\"><tr><th></th><th>Borda Point Summation</th></tr>\n" );
	} else {
		fprintf( fout, "<table border=\"1\"><tr><th>Choice Index</th><th>Borda Point Summation</th></tr>\n" );
	}
	while ( true ) {
		bool done;
		max = INT_MIN;
		done = true;
		for ( i = 0; i < numc; i++ ) {
			if ( in[i] && talley[i] > max ) {
				done = false;
				maxi = i;
				max = talley[i];
			}
		}
		if ( done ) break;
		i = maxi;
		in[i] = false;
		if ( names ) {
			fprintf( fout, "<tr><td>%s</td><td>%d</td></tr>\n", names[i], talley[i] );
		} else {
			fprintf( fout, "<tr><td>%d</td><td>%d</td></tr>\n", i+1, talley[i] );
		}
	}
	fprintf( fout, "</table>\n" );
	delete [] in;
}

void BordaVotingSystem::print( FILE* fout, char** names ) {
	for ( int i = 0; i < numc; i++ ) {
		if ( names ) {
			fprintf(fout,"%s\t%d\n", names[i], talley[i] );
		} else {
			fprintf(fout,"%d\n", talley[i] );
		}
	}
	fflush(fout);
}

VotingSystem* BordaVotingSystem::newBordaVotingSystem( int numc ) {
	return new BordaVotingSystem( numc );
}
