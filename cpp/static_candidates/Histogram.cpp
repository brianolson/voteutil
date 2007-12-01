#include "Histogram.h"
#include "VotingSystem.h"
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <math.h>
#include <assert.h>

Histogram::Histogram( int numcIn, int nbuck, double min, double max )
	: isRanking( true ), outerTableCols( 4 ),
	barImageUrl( defaultBarImageUrl ),
	numc( numcIn ), blen( nbuck ), l( min ), h( max ),
	maxbucket( 0 ), maxtotal( 0 ), numvotes( 0 )
{
	b = (int*)malloc( sizeof(int) * numc * nbuck );
	assert(b);
	memset( b, 0, sizeof(int) * numc * nbuck );
	totals = (int*)malloc( sizeof(int) * numc );
	assert(totals);
	memset( totals, 0, sizeof(int) * numc );
}

Histogram::~Histogram() {
	free( b );
}

void Histogram::addRanking( int* rankings ) {
	if ( b == NULL ) return;
	int i;
	bool anyvote = false;
	for ( i = 0; i < numc; i++ ) if ( rankings[i] != NO_VOTE  ) {
		int bu, bv;
		bu = bucketfy( rankings[i] );
		b[i*blen + bu] = bv = b[i*blen + bu] + 1;
		anyvote = true;
		if ( bv > maxbucket ) {
			maxbucket = bv;
		}
		totals[i]++;
		if ( totals[i] > maxtotal ) {
			maxtotal = totals[i];
		}
	}
	if ( anyvote ) {
		numvotes++;
	}
}
void Histogram::addRating( int* ratings ) {
	if ( b == NULL ) return;
	int i;
	bool anyvote = false;
#if 0
	static bool firstTime = true;
	if ( firstTime ) {
		firstTime = false;
		fprintf(stderr,"blen %d, l %f, h %f\n", blen, l, h );
		for ( i = 0; i < blen; i++ ) {
			fprintf(stderr,"bucketfy( %d ) => %d\n", i, bucketfy( i ) );
		}
	}
#endif
	for ( i = 0; i < numc; i++ ) if ( ratings[i] != NO_VOTE  ) {
		int bu, bv;
		bu = bucketfy( ratings[i] );
		b[i*blen + bu] = bv = b[i*blen + bu] + 1;
		anyvote = true;
		if ( bv > maxbucket ) {
			maxbucket = bv;
		}
		totals[i]++;
		if ( totals[i] > maxtotal ) {
			maxtotal = totals[i];
		}
	}
	if ( anyvote ) {
		numvotes++;
	}
}
#define ARF_BODY     if ( b == NULL ) return;\
int i;\
bool anyvote = false;\
for ( i = 0; i < numc; i++ ) if ( ! isnan( ratings[i] )  ) {\
	int bu, bv;\
	bu = bucketfy( ratings[i] );\
	b[i*blen + bu] = bv = b[i*blen + bu] + 1;\
	anyvote = true;\
	if ( bv > maxbucket ) {\
		maxbucket = bv;\
	}\
	totals[i]++;\
	if ( totals[i] > maxtotal ) {\
		maxtotal = totals[i];\
	}\
}\
if ( anyvote ) {\
	numvotes++;\
}

void Histogram::addRating( double* ratings ) {
	ARF_BODY
}
void Histogram::addRating( float* ratings ) {
	ARF_BODY
}

#ifndef MAX_WIDTH
#define MAX_WIDTH 100
#endif
#ifndef MAXIMIZE_WIDTH
#define MAXIMIZE_WIDTH 01
#endif

void Histogram::print( FILE* cur, int style, char** names ) {
	double bstep = (h - l) / (blen - 1);
	double base = l - bstep/2.0;
	double scale = 1.0;
	if ( MAXIMIZE_WIDTH || maxbucket > MAX_WIDTH ) {
		scale = ((double)MAX_WIDTH) / ((double)maxbucket);
	}
	if ( style == 2 ) {
		/* gnuplot style */
		fprintf( cur, "set data style histeps\n"
				"set terminal png color\n"
				"set xlabel \"Ranking\"\n"
				"set ylabel \"Votes\"\n"
				"set nolabel\n"
		);
		for ( int c = 0; c < numc; c++ ) {
			if ( names ) {
				fprintf( cur, "set title \'%s\'\n", names[c] );
				fprintf( cur, "set output \"%s.png\"\n", names[c] );
			} else {
				fprintf( cur, "set output \"%d.png\"\n", c );
			}
			fprintf( cur, "plot \"-\"\n" );
			for ( int i = 0; i < blen; i++ ) {
				int hval;
				hval = b[c*blen + i];
				fprintf( cur, "\t%g\t%d\n", l + i*bstep, hval );
			}
			fprintf( cur, "e\n" );
		}
		return;
	}
	int oTC = 0;
	if ( outerTableCols > 1 ) {
		fprintf( cur, "<table border=\"1\">" );
	}
	for ( int c = 0; c < numc; c++ ) {
		if ( outerTableCols > 1 ) {
			if ( oTC == 0 ) {
				fprintf( cur, "<tr>" );
			}
			fprintf( cur, "<td width=\"%d%%\">", 100 / outerTableCols );
		}
		if ( style == 3 || style == 4 ) {
			fprintf( cur, "<table><tr><th colspan=\"2\">" );
		}
		if ( names ) {
			fprintf( cur, "%s\n", names[c] );
		} else {
			fprintf( cur, "choice %d\n", c + 1 );
		}
		if ( style == 3 || style == 4 ) {
			fprintf( cur, "</th></tr><tr><th>Ranking</th><th>Votes</th></tr>\n" );
		}
		for ( int i = 0; i < blen; i++ ) {
			int hval;
			hval = b[c*blen + i];
			if ( style == 1 ) {
				fprintf( cur, "\t%f..%f:\t%d\n", base + i*bstep, base + (i+1)*bstep, hval );
			} else if ( style == 3 ) {
				fprintf( cur, "<tr><td>%g</td><td>%d</td></tr>\n", l + i*bstep, hval );
			} else if ( style == 4 ) {
				fprintf( cur, "<tr><td>%g</td><td><img src=\"%s\" height=\"10\" width=\"%d\"> %d</td></tr>\n", l + i*bstep, barImageUrl, (int)(hval * scale), hval );
			} else {
				fprintf( cur, "\t%g\t%d\n", l + i*bstep, hval );
			}
		}
		if ( style == 3 || style == 4 ) {
			fprintf( cur, "<tr><td>total</td><td>%d</td></tr></table>\n", totals[c] );
		} else {
			fprintf( cur, "\ttotal\t%d\n", totals[c] );
		}
		if ( outerTableCols > 1 ) {
			fprintf( cur, "</td>" );
			oTC = ( oTC + 1 ) % outerTableCols;
			if ( oTC == 0 ) {
				fprintf( cur, "</tr>\n" );
			}
		}
	}
	if ( outerTableCols > 1 ) {
		if ( oTC != 0 ) {
			fprintf( cur, "</tr>" );
		}
		fprintf( cur, "</table>\n" );
	}
}

const char* const Histogram::defaultBarImageUrl = "b.png";
