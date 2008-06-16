#include "NamedVotingSystem.h"
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>

int debug = 0;

#if __ghs || __STRICT_ANSI__
char* strdup( const char* );
#endif

static int NameVote_cmp_qsort(const void* xa, const void* xb) {
	NameVote* a = (NameVote*)xa;
	NameVote* b = (NameVote*)xb;
	if ( a->rating > b->rating ) {
		return -1;
	}
	if ( a->rating < b->rating ) {
		return 1;
	}
	return strcmp( a->name, b->name );
}

void sortNameVotes(NameVote* they, int length) {
	qsort( they, length, sizeof(NameVote), NameVote_cmp_qsort );
}

void deleteNameIndexEntryTree( NameIndexEntry* x ) {
	if ( x->lt ) {
		deleteNameIndexEntryTree( x->lt );
	}
	if ( x->gt ) {
		deleteNameIndexEntryTree( x->gt );
	}
	free( (char*)x->name );
	free( x );
}

int nameIndex( NameIndex* ni, const char* name ) {
	NameIndexEntry* cur = ni->root;
	NameIndexEntry* prev = NULL;
	int cmp;
	if ( ni->root == NULL ) {
		ni->root = (NameIndexEntry*)malloc( sizeof(NameIndexEntry) );
		ni->names = (char**)malloc( sizeof(char*) );
		ni->names[0] = strdup( name );
		ni->root->name = ni->names[0];
		ni->root->index = 0;
		ni->root->lt = ni->root->gt = ni->root->up = NULL;
		ni->nextIndex = 1;
		return ni->root->index;
	}
	while ( cur != NULL ) {
		cmp = strcmp( cur->name, name );
		prev = cur;
		if ( cmp == 0 ) {
			return cur->index;
		} else if ( cmp < 0 ) {
			cur = cur->lt;
		} else /* cmp > 0 */ {
			cur = cur->gt;
		}
	}
	cur = (NameIndexEntry*)malloc( sizeof(NameIndexEntry) );
	if ( cur == NULL ) {
		return -1;
	}
	cur->index = ni->nextIndex;
	cur->name = strdup( name );
	cur->up = prev;
	cur->lt = cur->gt = NULL;
	if ( cmp < 0 ) {
		prev->lt = cur;
	} else /* cmp > 0 */ {
		prev->gt = cur;
	}
	ni->nextIndex++;
	ni->names = realloc( ni->names, sizeof(char*) * ni->nextIndex );
	assert(ni->names);
	ni->names[cur->index] = (char*)cur->name;
	return cur->index;
}

StoredIndexVoteNode* newStoredIndexVoteNode( int count ) {
	StoredIndexVoteNode* toret = (StoredIndexVoteNode*)malloc( sizeof(StoredIndexVoteNode) + (sizeof(IndexVote) * count));
	if ( toret != NULL ) {
		toret->numVotes = count;
		toret->next = NULL;
	}
	return toret;
}
StoredIndexVoteNode* dupStoredIndexVoteNode( const StoredIndexVoteNode* it ) {
	int i;
	StoredIndexVoteNode* toret = (StoredIndexVoteNode*)malloc( sizeof(StoredIndexVoteNode) + (sizeof(IndexVote) * it->numVotes));
	if ( toret == NULL ) { return toret; }
	toret->numVotes = it->numVotes;
	for ( i = 0; i < it->numVotes; i++ ) {
		toret->vote[i] = it->vote[i];
	}
	toret->next = NULL;
	return toret;
}

StoredIndexVoteNode* newStoredIndexVoteNodeFromVotes( int numVotes, const NameVote* votes, StoredIndexVoteNode* next, NameIndex* ni ) {
	StoredIndexVoteNode* toret;
	int i;
	toret = newStoredIndexVoteNode(numVotes);
	if ( toret == NULL ) {
		return toret;
	}
	for ( i = 0; i < numVotes; i++ ) {
		toret->vote[i].index = nameIndex( ni, votes[i].name );
		toret->vote[i].rating = votes[i].rating;
	}
	toret->next = next;
	toret->numVotes = numVotes;
	return toret;
}

void StoredIndexVoteNode_sort(StoredIndexVoteNode* it) {
	int i;
	int notdone = 1;
	// votes are probably short, bubblesort is easy
	while ( notdone ) {
		notdone = 0;
		for ( i = 0; i < it->numVotes - 1; ++i ) {
			if ( it->vote[i].rating < it->vote[i+1].rating ) {
				float trating = it->vote[i].rating;
				int tindex = it->vote[i].index;
				it->vote[i].rating = it->vote[i+1].rating;
				it->vote[i].index = it->vote[i+1].index;
				it->vote[i+1].rating = trating;
				it->vote[i+1].index = tindex;
				notdone = 1;
			}
		}
	}
}

int denibble( char c ) {
	if ( c >= '0' && c <= '9' ) {
		return c - '0';
	}
	if ( c >= 'a' && c <= 'f' ) {
		return (c - 'a') + 0xa;
	}
	if ( c >= 'A' && c <= 'F' ) {
		return (c - 'A') + 0xa;
	}
	return -1;
}

void strDePercentHexify( char* dest, const char* src ) {
	char c;
	while ( (c = *src) != '\0' ) {
		if ( c == '%' ) {
			*dest = (denibble( src[1] ) << 4) | denibble( src[2] );
			src += 2;
		} else {
			*dest = c;
		}
		dest++;
		src++;
	}
	*dest = c;
}

void strnDePercentHexify( char* dest, const char* src, int srclen ) {
	char c;
	while ( (c = *src) != '\0' && srclen > 0 ) {
		if ( c == '%' ) {
			*dest = (denibble( src[1] ) << 4) | denibble( src[2] );
			src += 2;
			srclen -= 2;
		} else {
			*dest = c;
		}
		dest++;
		src++;
		srclen--;
	}
	if ( srclen > 0 ) {
		*dest = c; // copy null terminate if src ends early
	}
}

/*!
returns length of dest output string not including trailing '\0'
*/
int percentHexify( char* dest, const char* src ) {
	int len = 0;
	char c;
	while ( (c = *src) != '\0' ) {
		if ( c == '%' ) {
			*dest = '%'; dest++;
			*dest = '2'; dest++;
			*dest = '5';
			len += 2;
		} else if ( c == '&' ) {
			*dest = '%'; dest++;
			*dest = '2'; dest++;
			*dest = '6';
			len += 2;
		} else if ( c == '=' ) {
			*dest = '%'; dest++;
			*dest = '3'; dest++;
			*dest = 'd';
			len += 2;
		} else {
			*dest = c;
		}
		len++;
		src++;
		dest++;
	}
	*dest = c;
	return len;
}

// count each non empty string separated by '&'
int countUrlVoteParts( const char* votestr ) {
	int numc = 0, any = 0;
	int len;
	for ( len = 0; votestr[len] != '\0'; len++ ) {
		if ( votestr[len] == '&' ) {
			any = 0;
		} else if ( ! any ) {
			numc++;
			any = 1;
		}
	}
	return numc;
}

/*!
Parse name1=r1&name2=r2[...] vote string.
name allowed to contain %hh hex escapes. '%' must be represented by "%25", '&' by "%26" and '=' by "%3d"
*/
// grr, this is so trivial in perl...
StoredIndexVoteNode* newStoredIndexVoteNodeFromURL( NameIndex* ni, const char* votestr ) {
	int numc = 0;
	int pos, nextbr;
	int namelen;
	int vi;
	StoredIndexVoteNode* toret;

	if ( votestr == NULL ) {
		return NULL;
	}
	if ( *votestr == '\0' ) {
		return newStoredIndexVoteNode(0);
	}

	numc = countUrlVoteParts( votestr );
	toret = newStoredIndexVoteNode(numc);
	pos = 0;
	for ( vi = 0; vi < numc; vi++ ) {
		int eq;
		char c;
		char* name;
		eq = 0;
		namelen = 0;
		nextbr = pos;
		do {
			nextbr++;
			c = votestr[nextbr];
			if ( c == '=' ) {
				namelen += nextbr - pos;
				eq = nextbr;
			} else if ( c == '%' && eq == 0 ) {
				namelen -= 2;
			}
		} while ( c != '\0' && c != '&' );

		name = (char*)malloc(namelen + 1);
		strnDePercentHexify( name, votestr + pos, eq - pos );
		name[namelen] = '\0';
		toret->vote[vi].index = nameIndex( ni, name );
		{
			char* endptr;
			const char* nptr = votestr + eq + 1;
			toret->vote[vi].rating = strtod( nptr, &endptr );
			if ( endptr == nptr ) {
				// conversion error
				int elen = nextbr - eq;
				char* erd = (char*)malloc( elen );
				assert(erd!=NULL);
				elen--;
				memcpy( erd, nptr, elen );
				erd[elen] = '\0';
				fprintf(stderr,"strtod could not parse rating \"%s\" for name \"%s\"\n", erd, name );
				toret->vote[vi].rating = nanf(NULL);
			}
		}
		free( name );
		pos = nextbr + 1;
	}
	return toret;
}


/*!
Parse name1=r1&name2=r2[...] vote string.
name must be a decimal integer parseable by strtol().
ratings must be parseable by strtod().
*/
// grr, this is so trivial in perl...
StoredIndexVoteNode* newStoredIndexVoteNodeFromIndexURL( const char* votestr ) {
	int len = 0;
	int numc = 0;
	int any = 0;
	int pos, nextbr;
	//int namelen;
	int vi;
	StoredIndexVoteNode* toret;

	if ( votestr == NULL ) {
		return NULL;
	}
	if ( *votestr == '\0' ) {
		return newStoredIndexVoteNode(0);
	}

	for ( ; votestr[len] != '\0'; len++ ) {
		if ( votestr[len] == '&' ) {
			any = 0;
		} else if ( ! any ) {
			numc++;
			any = 1;
		}
	}
	toret = newStoredIndexVoteNode(numc);
	pos = 0;
	for ( vi = 0; vi < numc; vi++ ) {
		int eq;
		char c;
		eq = 0;
		//namelen = 0;
		nextbr = pos;
		do {
			nextbr++;
			c = votestr[nextbr];
			if ( c == '=' ) {
				//namelen += nextbr - pos;
				eq = nextbr;
			} else if ( c == '%' && eq == 0 ) {
				//namelen -= 2;
			}
		} while ( c != '\0' && c != '&' );

		toret->vote[vi].index = strtol( votestr + pos, NULL, 10 );
		/* could check here that strtol converts the whole feild, eh. */
		{
			char* endptr;
			const char* nptr = votestr + eq + 1;
			toret->vote[vi].rating = strtod( nptr, &endptr );
			if ( endptr == nptr ) {
				// conversion error
				int elen = nextbr - eq;
				char* erd = (char*)malloc( elen );
				assert(erd != NULL);
				elen--;
				memcpy( erd, nptr, elen );
				erd[elen] = '\0';
				fprintf(stderr,"strtod could not parse rating \"%s\" for name \"%d\"\n", erd, toret->vote[vi].index );
				toret->vote[vi].rating = nanf(NULL);
			}
		}
		pos = nextbr + 1;
	}
	return toret;
}

/*!
return name1=r1&name2=r2[...] encoded string. malloc()s.
*/
char* storedIndexVoteToURL( NameIndex* ni, StoredIndexVoteNode* v ) {
	char* toret;
	int outlen = 0;
	int i;
	char fbuf[20];
	int opos;

	for ( i = 0; i < v->numVotes; i++ ) {
		const char* name;
		char c;
		int tl;
		name = indexName( ni , v->vote[i].index );
		assert( name != NULL );
		while ( (c = *name) != '\0' ) {
			if ( c == '=' || c == '&' || c == '%' ) {
				outlen += 3;
			} else {
				outlen++;
			}
			name++;
		}
		tl = snprintf( fbuf, sizeof(fbuf), "%.9g", v->vote[i].rating );
		//printf("outlen = %d: \"%s\".length = %d\n", outlen, fbuf, tl );
		outlen += tl;
	}
	outlen += v->numVotes + v->numVotes; // '=' and '&' chars, +1 for '\0'
	toret = (char*)malloc( outlen );
	if ( toret == NULL ) { return toret; }
	opos = 0;
	for ( i = 0; i < v->numVotes; i++ ) {
		opos += percentHexify( toret + opos, indexName( ni, v->vote[i].index ) );
		assert(opos < outlen);
		toret[opos] = '=';
		opos++;
		assert(opos < outlen);
		opos += sprintf( toret + opos, "%.9g", v->vote[i].rating );
		assert(opos < outlen);
		if ( i + 1 < v->numVotes ) {
			toret[opos] = '&';
			opos++;
		}
		assert(opos < outlen);
	}
	toret[opos] = '\0';
	return toret;
}

VirtualVotingSystem* newVirtualRawRating();
VirtualVotingSystem* newVirtualIRNR();
VirtualVotingSystem* newVirtualHistogram();
VirtualVotingSystem* newVirtualIRV();
VirtualVotingSystem* newVirtualVRR();
VirtualVotingSystem* newVirtualSTV();
//VirtualVotingSystem* newVirtual();

VirtualVotingSystemFactory votingSystemFatoryTable[] = {
	{
		newVirtualHistogram, "Histogram"
	},{
		newVirtualIRNR, "Instant Runoff Normalized Ratings"
	},{
		newVirtualVRR, "Virtual Round Robin"
	},{
		newVirtualRawRating, "Raw Rating Summation"
	},{
		newVirtualIRV, "Instant Runoff Voting"
	},{
		newVirtualSTV, "Single Transferrable Vote"
	},{
		NULL, NULL
	}
};
