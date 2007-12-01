#include "NameVotingSystem.h"
#include <string.h>
#include <stdlib.h>
#include <assert.h>
#include <math.h>


#if __ghs
extern "C" char* strdup( const char* );
#endif

NameIndex::NameIndex()
: root(NULL), nextIndex(1), names(NULL)
{}
NameIndex::~NameIndex() {
	if ( root != NULL ) {
		delete root;
		root = NULL;
	}
	if ( names != NULL ) {
		free( names );
		names = NULL;
	}
	nextIndex = 1;
}

int NameIndex::getIndex(const char* name) {
	Entry* cur = root;
	Entry* prev = NULL;
	int cmp;
	if ( root == NULL ) {
		names = (char**)malloc( sizeof(char*) * 2 );
		names[0] = strdup( name );
		root = new Entry(names[0], nextIndex);
		nextIndex++;
		return root->index;
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
	names = (char**)realloc( names, sizeof(char*) * nextIndex );
	assert(names);
	if ( names == NULL ) { return -1; }
	names[nextIndex] = (char*)cur->name;
	cur = new Entry(names[nextIndex], nextIndex);
	if ( cur == NULL ) { return -1; }
	cur->up = prev;
	if ( cmp < 0 ) {
		prev->lt = cur;
	} else /* cmp > 0 */ {
		prev->gt = cur;
	}
	nextIndex++;
	return cur->index;
}
const char* NameIndex::getName(int index) const {
	assert(index >= 0);
	assert(index < nextIndex);
	return names[index];
}
int NameIndex::getIndexConst(const char* name) const {
	Entry* cur = root;
	while ( cur != NULL ) {
		int cmp = strcmp( cur->name, name );
		Entry* prev = cur;
		if ( cmp == 0 ) {
			return cur->index;
		} else if ( cmp < 0 ) {
			cur = cur->lt;
		} else /* cmp > 0 */ {
			cur = cur->gt;
		}
	}
	return -1;
}

NameIndex::Entry::Entry(const char* name_in, int index_in)
: name(name_in), index(index_in), lt(NULL), gt(NULL), up(NULL)
{}
NameIndex::Entry::~Entry() {
	if ( lt ) {
		delete lt;
	}
	if ( gt ) {
		delete gt;
	}
	if ( name ) {
		free( const_cast<char*>(name) );
	}
}

int NameVotingSystem::voteRating( int numVotes, const NameVote* votes ) {
	IndexVote* toret = convertToIndexVote( numVotes, votes );
	int err = voteIndexVotes( numVotes, toret );
	delete toret;
	return err;
}

void NameVotingSystem::convertToIndexVote( int numVotes, const NameVote* votes, IndexVote* toret ) {
	for ( int i = 0; i < numVotes; i++ ) {
		toret[i].index = nameIndex( votes[i].name );
		toret[i].rating = votes[i].rating;
	}
}
IndexVote* NameVotingSystem::convertToIndexVote( int numVotes, const NameVote* votes ) {
	IndexVote* toret = new IndexVote[numVotes];
	convertToIndexVote( numVotes, votes, toret );
	return toret;
}
void NameVotingSystem::init( char** argv ) {
	/* default impl does nothing */
}

NameVotingSystem::~NameVotingSystem() {
}

#if 0
// don't define it, insist on implicit automatic binary copy
StoredIndexVoteNode::StoredIndexVoteNode(const StoredIndexVoteNode& it)
	: next(it.next), numVotes(it.numVotes), vote(it.vote) {
}
#endif

static int denibble( char c ) {
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

// count each non empty string separated by '&'
static int countUrlVoteParts( const char* votestr ) {
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
StoredIndexVoteNode* StoredIndexVoteNode::fromURL( NameIndex* ni, const char* votestr ) {
	int numc = 0;
	int pos, nextbr;
	int namelen;
	int vi;
	StoredIndexVoteNode* toret;
	
	if ( votestr == NULL ) {
		return NULL;
	}
	if ( *votestr == '\0' ) {
		return StoredIndexVoteNode::make(0);
	}
	
	numc = countUrlVoteParts( votestr );
	toret = StoredIndexVoteNode::make(numc);
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
		toret->vote[vi].index = ni->getIndex( name );
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

StoredIndexVoteNode* StoredIndexVoteNode::fromIndexURL( const char* votestr ) {
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
		return StoredIndexVoteNode::make(0);
	}
	
	for ( ; votestr[len] != '\0'; len++ ) {
		if ( votestr[len] == '&' ) {
			any = 0;
		} else if ( ! any ) {
			numc++;
			any = 1;
		}
	}
	toret = StoredIndexVoteNode::make(numc);
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
				toret->vote[vi].rating = NAN;
			}
		}
		pos = nextbr + 1;
	}
	return toret;
}

StoredIndexVoteNode* StoredIndexVoteNode::make(int numVotes) {
	return new(numVotes) StoredIndexVoteNode(numVotes);
}

void* StoredIndexVoteNode::operator new(size_t size, int count) {
	return malloc( size + (count * sizeof(IndexVote)) );
}

StoredIndexVoteNode* NameVotingSystem::newStoredIndexVoteNode( int numVotes, const NameVote* votes, StoredIndexVoteNode* next ) {
	StoredIndexVoteNode* toret;
	toret = StoredIndexVoteNode::make(numVotes);
	if ( toret == NULL ) {
		return toret;
	}
	convertToIndexVote( numVotes, votes, toret->vote );
	toret->next = next;
	toret->numVotes = numVotes;
	return toret;
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

/** return malloc()ed space full of URL representation of this SIVN */
char* StoredIndexVoteNode::toURLVoteString(NameIndex* ni) {
	char* toret;
	int outlen = 0;
	int i;
	char fbuf[20];
	int opos;
	
	// first pass, count length of eventual output
	for ( i = 0; i < numVotes; i++ ) {
		const char* name;
		char c;
		int tl;
		name = ni->getName( vote[i].index );
		assert( name != NULL );
		while ( (c = *name) != '\0' ) {
			if ( c == '=' || c == '&' || c == '%' ) {
				outlen += 3;
			} else {
				outlen++;
			}
			name++;
		}
		tl = snprintf( fbuf, sizeof(fbuf), "%.9g", vote[i].rating );
		//printf("outlen = %d: \"%s\".length = %d\n", outlen, fbuf, tl );
		outlen += tl;
	}
	outlen += numVotes + numVotes; // '=' and '&' chars, +1 for '\0'
	toret = (char*)malloc( outlen );
	if ( toret == NULL ) { return toret; }
	opos = 0;
	// second pass, fill in allocated output
	for ( i = 0; i < numVotes; i++ ) {
		opos += percentHexify( toret + opos, ni->getName( vote[i].index ) );
		assert(opos < outlen);
		toret[opos] = '=';
		opos++;
		assert(opos < outlen);
		opos += sprintf( toret + opos, "%.9g", vote[i].rating );
		assert(opos < outlen);
		if ( i + 1 < numVotes ) {
			toret[opos] = '&';
			opos++;
		}
		assert(opos < outlen);
	}
	toret[opos] = '\0';
	return toret;
}

/** shallow copy, same hard data, new next same pointer as old next */
StoredIndexVoteNode* StoredIndexVoteNode::clone() {
	StoredIndexVoteNode* toret = new(numVotes) StoredIndexVoteNode(*this);
	return toret;
}

void NameVotingSystem::setSharedNameIndex(NameIndex* ni) {
	index = ni;
	private_index = false;
}

#if DO_MAIN
#include <assert.h>

int main( int argc, char** argv ) {
	StoredIndexVoteNode* x;
	//assert( x = new StoredIndexVoteNode() );
	assert( x = new(5) StoredIndexVoteNode() );
}
#endif
