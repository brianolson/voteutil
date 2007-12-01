#include "RawRating.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

RawRating::~RawRating() {
	if ( talley ) {
		delete [] talley;
	}
}

inline int ratingCleanse( int r ) {
	return (( r == NO_VOTE ) ? 0 : r );
}

int RawRating::voteRating( int* rating ) {
	int i;
	for ( i = 0; i < numc; i++ ) {
		talley[i] += ratingCleanse( rating[i] );
	}
	return 0;
}

int RawRating::getWinners( int* choiceIndecies ) {
	long max = LONG_MIN;
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

int RawRating::write( int fd ) {
	return 0;
}

int RawRating::read( int fd ) {
	return 0;
}

void RawRating::htmlSummary( FILE* cur, char** names ) {
	long max;
	bool* in = new bool[numc];
	int maxi,i;
	for ( i = 0; i < numc; i++ ) {
		in[i] = true;
	}
	if ( names ) {
		fprintf( cur, "<table border=\"1\"><tr><th></th><th>Rating Summation</th></tr>\n" );
	} else {
		fprintf( cur, "<table border=\"1\"><tr><th>Choice Index</th><th>Rating Summation</th></tr>\n" );
	}
	while ( true ) {
		bool done;
		max = LONG_MIN;
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
			fprintf( cur, "<tr><td>%s</td><td>%ld</td></tr>\n", names[i], talley[i] );
		} else {
			fprintf( cur, "<tr><td>%d</td><td>%ld</td></tr>\n", i+1, talley[i] );
		}
	}
	fprintf( cur, "</table>\n" );
	delete [] in;
}

void RawRating::print( FILE* fout, char** names ) {
	for ( int i = 0; i < numc; i++ ) {
		if ( names ) {
			fprintf(fout,"%s\t%ld\n", names[i], talley[i] );
		} else {
			fprintf(fout,"%ld\n", talley[i] );
		}
	}
	fflush(fout);
}

void RawRating::squashToAcceptance( int* rating ) {
	for ( int i = 0; i < numc; i++ ) {
		if ( rating[i] >= 0 ) {
			rating[i] = 1;
		} else {
			rating[i] = 0;
		}
	}
}

VotingSystem* RawRating::newRawRating( int numc ) {
	return new RawRating( numc );
}
