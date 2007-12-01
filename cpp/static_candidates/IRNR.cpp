#include "IRNR.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

IRNR::IRNR( int numCandidates ) :
	RatedVotingSystem( numCandidates ),
	rmsnorm( true ),
	votes( 0 )
{
	talley = new double[numc];
	active = new bool[numc];
}

IRNR::~IRNR() {
	if ( talley ) {
		delete [] talley;
	}
	if ( active ) {
		delete [] active;
	}
	while ( votes ) {
		Vote* tv;
		tv = votes->next;
		votes->next = NULL;
		delete votes;
		votes = tv;
	}
}

inline int ratingCleanse( int r ) {
	return (( r == NO_VOTE ) ? 0 : r );
}

int IRNR::voteRating( int* rating ) {
	votes = new(numc) Vote( rating, numc, votes );
	return 0;
}

int IRNR::getWinners( int* choiceIndecies ) {
	double max = -1e100;
	double min;
	int mini;
	int i;
	int numWinners;
	int numActive = numc;
	int numv;
	double epsilon;
	Vote* cv;
	for ( i = 0; i < numc; i++ ) {
		active[i] = true;
	}
	while ( numActive > 1 ) {
		for ( i = 0; i < numc; i++ ) if ( active[i] ) {
			talley[i] = 0;
		}
		cv = votes;
		numv = 0;
		while ( cv ) {
			double vs;
			vs = 0.0;
			numv++;
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				if ( rmsnorm ) {
					double tr;
					tr = cv->rating[i];
					vs += tr * tr;
				} else {
					vs += abs( cv->rating[i] );
				}
			}
			if ( rmsnorm ) {
				vs = sqrt( vs );
			}
			if ( vs != 0.0 ) {
				vs = 1.0 / vs;
				for ( i = 0; i < numc; i++ ) {
					if ( active[i] ) {
						talley[i] += cv->rating[i] * vs;
					}
				}
			}
			cv = cv->next;
		}
		if ( numv > 1000000 ) {
			epsilon = 1.0 / numv;
		} else {
			epsilon = 0.000001;
		}
		max = -1e100;
		min = 1e100;
		mini = -1;
		for ( i = 0; i < numc; i++ ) if ( active[i] ) {
			double tpe, tme;
			/* due to extra roundoff in this method, calculate ties to within epsilon */
			tpe = talley[i] + epsilon;
			tme = talley[i] - epsilon;
			if ( talley[i] <= min ) {
				min = talley[i];
				mini = i;
			}
			if ( tme > max ) {
				choiceIndecies[0] = i;
				numWinners = 1;
				max = talley[i];
			} else if ( (tme <= max) && (tpe >= max) ) {
				choiceIndecies[numWinners] = i;
				numWinners++;
			}
		}
		if ( numWinners == numActive ) {
			//printf("%d-way tie\n",numc);
			return numWinners;
		}
#if 0
		// debugging spew
		for ( i = 0; i < numc; i++ ) {
			printf("%.18lf ", talley[i] );
		}
		printf("\n\tdq %d numWinners %d min %lf max %lf\n", mini, numWinners, min, max );
#endif
		active[mini] = false;
		numActive--;
	}
	return numWinners;
}

int IRNR::write( int fd ) {
	return 0;
}

int IRNR::read( int fd ) {
	return 0;
}

void IRNR::htmlSummary( FILE* cur, char** names ) {
	double max;
	bool* in = new bool[numc];
	int maxi,i;
	for ( i = 0; i < numc; i++ ) {
		in[i] = true;
	}
	if ( names ) {
		fprintf( cur, "<table border=\"1\"><tr><th></th><th>IRNR Rating</th></tr>\n" );
	} else {
		fprintf( cur, "<table border=\"1\"><tr><th>Choice Index</th><th>IRNR Rating</th></tr>\n" );
	}
	while ( true ) {
		bool done;
		max = -1e100;
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
			fprintf( cur, "<tr><td>%s</td><td>%f</td></tr>\n", names[i], talley[i] );
		} else {
			fprintf( cur, "<tr><td>%d</td><td>%f</td></tr>\n", i+1, talley[i] );
		}
	}
	fprintf( cur, "</table>\n" );
	delete [] in;
}

void IRNR::print( FILE* fout, char** names ) {
	for ( int i = 0; i < numc; i++ ) {
		if ( names ) {
			fprintf(fout,"%s\t%f\n", names[i], talley[i] );
		} else {
			fprintf(fout,"%f\n", talley[i] );
		}
	}
	fflush(fout);
}

VotingSystem* IRNR::newIRNR( int numc ) {
	return new IRNR( numc );
}

IRNR::Vote::Vote( int* r, int numc, Vote* n ) : next( n ) {
	memcpy( rating, r, numc * sizeof(int) );
}

void* IRNR::Vote::operator new( size_t s, int numc ) {
	return ::operator new( (size_t)(s + numc * sizeof(int)) );
}
