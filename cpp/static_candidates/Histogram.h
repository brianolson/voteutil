#ifndef HISTOGRAM_H
#define HISTOGRAM_H

#include <stdio.h>

/*!
@class Histogram
*/
class Histogram {
public:
	Histogram( int numcIn, int nbuck, double min, double max );
	~Histogram();

	void addRanking( int* rankings );
	void addRating( int* ratings );
	void addRating( float* ratings );
	void addRating( double* ratings );

	void print( FILE* fout, int style, char** names = (char**)0 );

	bool isRanking;
	int outerTableCols;

	const char* barImageUrl;

protected:
	/*! @var numc number of candidates */
	int numc;

	/*! @var b histogram buckets b[numc][blen] */
	int* b;
	/*! @var blen bucket length per choice */
	int blen;
	/*! @var center value of low bucket */
	double l;
	/*! @var center value of high bucket */
	double h;
	/*! @var totals sum of b[numc][blen] -> totals[numc] */
	int* totals;
	int maxbucket;
	int maxtotal;
	int numvotes;

	inline int bucketfy( int rate ) {
		if ( rate < l )
			return 0;
		if ( rate > h )
			return blen-1;
		return (int)((
			((rate - l) * (blen-1))
			/ (h - l))
			+ 0.5);
	}
	inline int bucketfy( double rate ) {
		if ( rate < l )
			return 0;
		if ( rate > h )
			return blen-1;
		return (int)((
			((rate - l) * (blen-1))
			/ (h - l))
			+ 0.5);
	}

	static const char* const defaultBarImageUrl;
};


#endif
