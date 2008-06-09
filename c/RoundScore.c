#include <stdlib.h>
#include <stdio.h>

#include "RoundScore.h"

RoundScore* newRoundScore(int numc) {
	RoundScore* toret = (RoundScore*)malloc( sizeof(RoundScore) + (sizeof(RoundPart) * (numc-1)) );
	toret->length = numc;
	toret->next = NULL;
	return toret;
}

void deleteRoundScore(RoundScore* it) {
	RoundScore* next;
	while ( it != NULL ) {
		next = it->next;
		free( it );
		it = next;
	}
}

static __inline int max( int a, int b ) {
	if ( a > b ) return a;
	return b;
}

void RoundScore_HTMLTable( RoundScore* rounds, FILE* fout, NameVote* winners ) {
	RoundScore* cur;
	int nrounds = 0;
	int i;
	int numc = 0;
	cur = rounds;
	while ( cur != NULL ) {
		nrounds++;
		numc = max( numc, cur->length );
		cur = cur->next;
	}
	if ( nrounds == 0 ) {
		return;
	}
	fprintf(fout, "<table border=\"1\"><tr>");
	for ( i = 0; i < nrounds; ++i ) {
		fprintf(fout, "<th colspan=\"2\">Round %d</th>", i + 1 );
	}
	fprintf(fout, "</tr>\n<tr>");
	for ( i = 0; i < nrounds; ++i ) {
		fprintf(fout, "<th>Name</th><th>Count</th>" );
	}
	fprintf(fout, "</tr>\n" );
	for ( i = 0; i < numc; ++i ) {
		fprintf(fout, "<tr>" );
		cur = rounds;
		while ( cur != NULL ) {
			RoundPart* rp;
			rp = NULL;
			if ( winners == NULL ) {
				rp = &(cur->they[i]);
			} else {
				int j;
				for ( j = 0; j < numc; ++j ) {
					if ( cur->they[j].name == winners[i].name ) {
						rp = &(cur->they[j]);
						break;
					}
				}
			}
			if ( rp == NULL ) {
				fprintf(fout, "<td></td><td></td>" );
			} else if ( rp->active ) {
				fprintf(fout, "<td>%s</td><td>%.2f</td>", rp->name, rp->tally );
			} else {
				fprintf(fout, "<td style=\"color:#999999\">%s</td><td>%.2f</td>", rp->name, rp->tally );
			}
			cur = cur->next;
		}
		fprintf(fout, "</tr>\n" );
	}
	fprintf(fout, "</table>\n" );
}
