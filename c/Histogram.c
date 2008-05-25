#include "Histogram.h"
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <assert.h>

/*
	NameIndex* ni;
	float min, max, step;
	int buckets;
	int maxbucket = 0;
	int numc;
	int** they;
	public boolean intmode = false;
	public boolean lowfirst = false;
	public float bucketscale = 1.0f;
	public float offset = 0.0f;
	public boolean doscaleoffset = false;

	public String labelname = "Rating";

	double minRecorded = Double.MAX_VALUE;
	double maxRecorded = Double.MAX_VALUE * -1.0;
	double sum;
	int votes;
*/
int outerTableCols = 4;
const char* barImgUrl = "b.png";
int useSpan = 1;
int printPercents = 0;
int MAX_WIDTH = 100;
int MAXIMIZE_WIDTH = 1;

static void growHistTo( Histogram* it, int x ) {
	int** nt;
	if ( it->numc > x ) {
		return;
	}
	nt = (int**)realloc( it->they, sizeof(int*)*(x+1) );
	if ( nt == NULL ) {
		fprintf(stderr,"realloc to size %lu failed\n", sizeof(int*)*(x+1) );
		return;
	}
	it->they = nt;
	while ( it->numc <= x ) {
		it->they[it->numc] = (int*)malloc(sizeof(int)*it->buckets);
		assert(it->they[it->numc] != NULL);
		memset( it->they[it->numc], 0, sizeof(int)*it->buckets );
		it->numc++;
	}
}

void initHistogram( Histogram * it ) {
	it->buckets = 21;
	it->min = -10.5f;
	it->max = 10.5f;
	it->step = 1.0f;
	it->intmode = 1;

	it->numc = 0;
	it->they = (int**)malloc(sizeof(int*)*2);
	growHistTo( it, 2 );
	//it->intmode = 0;
	it->lowfirst = 0;
	it->bucketscale = 1.0;
	it->offset = 0.0;
	it->doscaleoffset = 0;
	it->labelname = "Rating";
	it->minRecorded = HUGE_VAL;
	it->maxRecorded = -HUGE_VAL;
	it->sum = 0;
	it->votes = 0;

	it->winners = NULL;
}

Histogram* newHistogram() {
	Histogram* toret = (Histogram*)malloc(sizeof(Histogram));
	if ( toret == NULL ) { return toret; }
	toret->ni = malloc(sizeof(NameIndex));
	if ( toret->ni == NULL ) {
		free( toret );
		return NULL;
	}
	initNameIndex(toret->ni);
	initHistogram( toret );
	return toret;
}

static void voteIndexRating( Histogram* it, int x, float rating ) {
	int* counts;
	if ( x >= it->numc ) {
		growHistTo( it, x );
	}
	it->sum += rating;
	if ( rating < it->minRecorded ) {
		it->minRecorded = rating;
	}
	if ( rating > it->maxRecorded ) {
		it->maxRecorded = rating;
	}
	counts = it->they[x];
	assert(counts!=NULL);
	if ( rating < it->min ) {
		counts[0]++;
		if ( counts[0] > it->maxbucket ) { it->maxbucket = counts[0]; }
	} else if ( rating >= it->max ) {
		counts[it->buckets-1]++;
		if ( counts[it->buckets-1] > it->maxbucket ) { it->maxbucket = counts[it->buckets-1]; }
	} else {
		/*
		0    1    2    3    4
		|----|----|----|----|----|
		min                      max
		5 buckets
		*/
		int j;
		j = floor( it->step * (rating - it->min) );
		counts[j]++;
		if ( counts[j] > it->maxbucket ) { it->maxbucket = counts[j]; }
	}
}

int Histogram_voteRating( Histogram* it, int numVotes, const NameVote* vote ) {
	int i;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	for ( i = 0; i < numVotes; i++ ) {
		int x;
		if ( isnan( vote[i].rating ) ) {
			continue;
		}
		x = nameIndex( it->ni, vote[i].name );
		voteIndexRating( it, x, vote[i].rating );
	}
	it->votes++;
	return 0;
}
int Histogram_voteStoredIndexVoteNode( Histogram* it, StoredIndexVoteNode* votes ) {
	int i;
	if ( it->winners != NULL ) {
		free( it->winners );
		it->winners = NULL;
	}
	for ( i = 0; i < votes->numVotes; i++ ) {
		int x;
		if ( isnan( votes->vote[i].rating ) ) {
			continue;
		}
		x = votes->vote[i].index;
		voteIndexRating( it, x, votes->vote[i].rating );
	}
	it->votes++;
	free( votes );
	return 0;
}
int Histogram_getWinners( Histogram* it, int numVotes, NameVote** winnersP ) {
	return -1;
}
static void histRow( Histogram* it, FILE* fout, int i, int hval, double scale ) {
	char valueLabel[30];
	float lmin = it->min;
	float lmax = it->max;
	float lstep = it->step;
	if ( it->doscaleoffset ) {
		lmin = lmin * it->bucketscale + it->offset;
		lmax = lmax * it->bucketscale + it->offset;
		lstep = lstep * it->bucketscale;
	}
	if ( it->intmode ) {
		snprintf( valueLabel, sizeof(valueLabel), "%d", (int)rint(i*lstep + lmin + 0.5) );
	} else if ( i == it->buckets - 1 ) {
		snprintf( valueLabel, sizeof(valueLabel), ">= %f", (lmax - (1.0/lstep)) );
	} else if ( i == 0 ) {
		snprintf( valueLabel, sizeof(valueLabel), "< %f", (lmin + (1.0/lstep)) );
	} else {
		snprintf( valueLabel, sizeof(valueLabel), "[%f .. %f)", (lmin + (i/lstep)), (lmin + ((i+1)/lstep)) );
	}
	if ( printPercents ) {
		fprintf( fout, "<tr><td>%s</td><td>%d</td></tr>\n", valueLabel, hval );
	} else {
		fprintf( fout, "<tr><td>%s", valueLabel );
		if ( useSpan ) {
			fprintf( fout, "</td><td><div style=\"background-color: #bb99ff; width: %d\">%d</div></td></tr>\n", (int)(hval * scale), hval );
		} else {
			fprintf( fout, "</td><td><img src=\"%s\" height=\"10\" width=\"%d\"> %d</td></tr>\n", barImgUrl, (int)(hval * scale), hval );
		}
	}
}
void Histogram_htmlSummary( Histogram* it, FILE* fout ) {
	double scale = 1.0;
	int oTC = 0;
	int maxBucket = (int)floor( it->step * (it->maxRecorded - it->min) );
	int minBucket = (int)floor( it->step * (it->minRecorded - it->min) );
	int c, i;

	if ( MAXIMIZE_WIDTH || it->maxbucket > MAX_WIDTH ) {
		scale = ((double)MAX_WIDTH) / ((double)it->maxbucket);
	}
	if ( outerTableCols > 1 ) {
		fprintf( fout, "<table border=\"1\">" );
	}
	if ( maxBucket > it->buckets - 1 ) {
		maxBucket = it->buckets - 1;
	}
	if ( minBucket < 0 ) {
		minBucket = 0;
	}
	for ( c = 0; c < it->numc; c++ ) {
		int* cc;
		int total;
		if ( outerTableCols > 1 ) {
			if ( oTC == 0 ) {
				fprintf( fout, "<tr>" );
			}
			fprintf( fout, "<td width=\"%d%%\">", 100 / outerTableCols );
		}
		fprintf( fout, "<table><tr><th colspan=\"2\">%s\n", indexName( it->ni, c ) );
		fprintf( fout, "</th></tr><tr><th>%s</th><th>Votes</th></tr>\n", it->labelname );
		cc = it->they[c];
		total = 0;
		if ( it->lowfirst ) {
			for ( i = minBucket; i <= maxBucket; i++ ) {
				int hval;
				hval = cc[i];
				total += hval;
				histRow( it, fout, i, hval, scale );
			}
		} else {
			for ( i = maxBucket; i >= minBucket; i-- ) {
				int hval;
				hval = cc[i];
				total += hval;
				histRow( it, fout, i, hval, scale );
			}
		}
		fprintf( fout, "<tr><td>total</td><td>%d</td></tr></table>\n", total );
		if ( outerTableCols > 1 ) {
			fprintf( fout, "</td>" );
			oTC = ( oTC + 1 ) % outerTableCols;
			if ( oTC == 0 ) {
				fprintf( fout, "</tr>\n" );
			}
		}
	}
	if ( outerTableCols > 1 ) {
		if ( oTC != 0 ) {
			fprintf( fout, "</tr>" );
		}
		fprintf( fout, "</table>\n" );
	}
}
void Histogram_print( Histogram* it, FILE* fout ) {
	fprintf(fout,"FIXME histogram plain text output\n");
}
DECLARE_STD_setSharedNameIndex( Histogram )
#if 0
void Histogram_setSharedNameIndex( Histogram* it, NameIndex* ni ) {
	if ( it->ni != NULL ) {
		clearNameIndex( it->ni );
		free( it->ni );
	}
	it->ni = ni;
}
#endif

void clearHistogram( Histogram* it ) {
	if ( it->winners != NULL ) {
		free( it->winners );
	}
	clearNameIndex( it->ni );
}
void deleteHistogram( Histogram* it ) {
	clearHistogram( it );
	free(it);
}

struct vvsrr {
	VirtualVotingSystem vvs;
	Histogram rr;
};
void Histogram_deleteVVS( VirtualVotingSystem* vvs ) {
	clearHistogram( (Histogram*)vvs->it );
	free( vvs );
}
void Histogram_setSeats( Histogram* it, int seats ) {
	// No-op, to allow Histogram to pose as a multi-winner method.
}

VirtualVotingSystem* newVirtualHistogram() {
	struct vvsrr* vr = (struct vvsrr*)malloc(sizeof(struct vvsrr));
	VirtualVotingSystem* toret = &vr->vvs;
	INIT_VVS_TYPE(Histogram);
	vr->vvs.setSeats = (vvs_setSeats)Histogram_setSeats;
	toret->close = Histogram_deleteVVS;
	toret->it = &vr->rr;
	initHistogram( &vr->rr );
	return toret;
}
