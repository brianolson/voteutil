/*
*  IRV.cpp
*  Instant Runoff Voting, though it sucketh
*
*  Created by Brian Olson on Wed Oct 22 2003.
*  Copyright (c) 2003 Brian Olson. All rights reserved.
*
*/

#include "IRV.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#ifndef verbose
#define verbose 0
#endif

class IRVNode {
public:
	IRVNode* next;
	/* actually rankings[numc] */
	int ranking[1];
};

IRV::IRV( int numCandidates )
: RankedVotingSystem( numCandidates ), votes( NULL ), totalVotes( 0 )
{
	talley = new int[numc];
	winnerBuckets = new IRVNode*[numc+1];
	active = new bool[numc];
	for ( int c = 0; c < numc; c++ ) {
		talley[c] = 0;
		active[c] = true;
		winnerBuckets[c] = NULL;
	}
	winnerBuckets[numc] = NULL;
}

IRV::~IRV() {
	if ( talley ) {
		delete [] talley;
	}
	if ( active ) {
		delete [] active;
	}
	if ( winnerBuckets ) {
		for ( int c = 0; c <= numc; c++ ) {
			IRVNode* cur;
			IRVNode* next;
			cur = winnerBuckets[c];
			while ( cur != NULL ) {
				next = cur->next;
				free( cur );
				cur = next;
			}
			winnerBuckets[c] = NULL;
		}
		delete [] winnerBuckets;
	}
}

void IRV::intoBucket( IRVNode* it ) {
	int i;
	int mini = -1, min = numc + 1;
	for ( i = 0; i < numc; i++ ) {
		if ( active[i] && (it->ranking[i] != NO_VOTE) && (it->ranking[i] < min) ) {
			mini = i;
			min = it->ranking[i];
		}
	}
	if ( mini == -1 ) {
		it->next = winnerBuckets[numc];
		winnerBuckets[numc] = it;
	} else {
		talley[mini]++;
		it->next = winnerBuckets[mini];
		winnerBuckets[mini] = it;
	}
}

int IRV::voteRanking( int* ranking ) {
	IRVNode* tn = (IRVNode*)malloc( sizeof(IRVNode) + (sizeof(int) * numc) );
	assert(tn);
	memcpy( tn->ranking, ranking, (sizeof(int) * numc) );
	tn->next = votes;
	votes = tn;
	totalVotes++;
	return 0;
}

int IRV::getWinners( int* choiceIndecies ) {
	int max = talley[0];
	int min = talley[0];
	int maxi = 0;
	int loseri = numc - 1;	// where to store next loser
	int winneri = 0;		// where to store next winner
	int i;
	int numWinners;
	for ( int c = 0; c < numc; c++ ) {
		talley[c] = 0;
		active[c] = true;
		IRVNode* cur;
		IRVNode* next;
		cur = winnerBuckets[c];
		while ( cur != NULL ) {
			next = cur->next;
			cur->next = votes;
			votes = cur;
			cur = next;
		}
		winnerBuckets[c] = NULL;
	}
	int votecheck = 0;
	while ( votes ) {
		IRVNode* n;
		n = votes->next;
		intoBucket( votes );
		votecheck++;
		votes = n;
	}
	if ( verbose ) {
		fprintf(stderr,"%d votes, %d into buckets\n", totalVotes, votecheck );
	}
	while ( winneri < loseri ) {
		max = INT_MIN;
		min = INT_MAX;
		for ( i = 0; i < numc; i++ ) if ( active[i] ) {
			if ( verbose ) {
				fprintf(stderr,"%d:\t%d\n", i, talley[i] );
			}
			if ( talley[i] > max ) {
				maxi = i;
				max = talley[i];
			}
			if ( talley[i] < min ) {
				min = talley[i];
			}
		}
		if ( talley[maxi] > totalVotes / 2 ) {
			if ( verbose ) {
				fprintf( stderr, "promoting majority winner %d with majority %d\n", maxi, talley[maxi] );
			}
			choiceIndecies[winneri] = maxi;
			active[maxi] = false;
			winneri++;
			break;
		} else if ( max == min ) {
			// N way tie!
			for ( i = 0; i < numc; i++ ) if ( active[i] ) {
				choiceIndecies[winneri] = i;
				winneri++;
			}
			break;
		} else for ( i = 0; i < numc; i++ ) if ( active[i] ) {
			if ( talley[i] == min ) {
				if ( verbose ) {
					fprintf( stderr, "%d deactivated with tie for low score at %d, loseri %d, winneri %d\n", i, min, loseri, winneri );
				}
				choiceIndecies[loseri] = i;
				loseri--;
				active[i] = false;
				while ( winnerBuckets[i] ) {
					IRVNode* n;
					n = winnerBuckets[i]->next;
					intoBucket( winnerBuckets[i] );
					winnerBuckets[i] = n;
				}
			}
		}
	}
	while ( winneri < loseri ) {
		for ( i = 0; i < numc; i++ ) if ( active[i] ) {
			choiceIndecies[winneri] = i;
			winneri++;
		}
	}
	for ( numWinners = 1; talley[numWinners-1] == talley[numWinners]; numWinners++ ) {
	}
	return numWinners;
}

int IRV::write( int fd ) {
	return 0;
}

int IRV::read( int fd ) {
	return 0;
}

void IRV::htmlSummary( FILE* fout, char** names ) {
	int max;
	bool* in = new bool[numc];
	int maxi,i;
	for ( i = 0; i < numc; i++ ) {
		in[i] = true;
	}
	if ( names ) {
		fprintf( fout, "<table border=\"1\"><tr><th></th><th>IRV Best Vote</th></tr>\n" );
	} else {
		fprintf( fout, "<table border=\"1\"><tr><th>Choice Index</th><th>IRV Best Vote</th></tr>\n" );
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

void IRV::print( FILE* fout, char** names ) {
	for ( int i = 0; i < numc; i++ ) {
		if ( names ) {
			fprintf(fout,"%s\t%d\n", names[i], talley[i] );
		} else {
			fprintf(fout,"%d\n", talley[i] );
		}
	}
	fflush(fout);
}

VotingSystem* IRV::newIRV( int numc ) {
	return new IRV( numc );
}
