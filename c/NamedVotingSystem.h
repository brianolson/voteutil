#ifndef NAME_VOTING_SYSTEM_H
#define NAME_VOTING_SYSTEM_H

#include <stdio.h>
#include <stdlib.h>

#if __cplusplus
extern "C" {
#define NVSINLINE static inline
#else
#define NVSINLINE static __inline
#endif

/*!
@class NameVotingSystem
Voting System interface that allows voting name=value pairs.
Implementations should be write-in capable and allow new names to be specified on any vote.
A simple name indexing system is provided in this base class.
Interfaces are not thread safe. Access to an instance must be externally synchronized.
*/

#define NO_VOTE ((int)0x80000000)

struct NameVoteStruct {
	const char* name;
	float rating;
};
typedef struct NameVoteStruct NameVote;

/* A more compact struct than NameVote. For implementations that need to hold all votes in memory. */
struct IndexVoteStruct {
	int index;
	float rating;
};
typedef struct IndexVoteStruct IndexVote;

struct StoredIndexVoteNode {
	struct StoredIndexVoteNode* next;
	int numVotes;
	IndexVote vote[1];

};
typedef struct StoredIndexVoteNode StoredIndexVoteNode;

StoredIndexVoteNode* newStoredIndexVoteNode(int count);
StoredIndexVoteNode* dupStoredIndexVoteNode( const StoredIndexVoteNode* );

/* Convert Rankings <-> Ratings.
 * Symmetric function can be used to restore original rankings data from stored ratings.
 * rating = numVotes - ranking
 * ranking = numVotes  - rating
 */
NVSINLINE void convertRankingsAndRatings(StoredIndexVoteNode* v) {
	int i;
	for (i = 0; i < v->numVotes; ++i) {
		// 1st place gets N-1, last place gets 0.
		v->vote[i].rating = v->numVotes - v->vote[i].rating;
	}
}

struct NameIndexEntry {
	const char* name;
	int index;
	struct NameIndexEntry* lt;
	struct NameIndexEntry* gt;
	struct NameIndexEntry* up;
};
typedef struct NameIndexEntry NameIndexEntry;

void deleteNameIndexEntryTree( NameIndexEntry* x );

struct NameIndex {
	NameIndexEntry* root;
	int nextIndex;
	char** names;
};
typedef struct NameIndex NameIndex;


NVSINLINE void initNameIndex( NameIndex* ni ) {
	ni->root = NULL;
	ni->names = NULL;
	ni->nextIndex = 0;
}
NVSINLINE void clearNameIndex( NameIndex* ni ) {
	if ( ni->root != NULL ) {
		deleteNameIndexEntryTree( ni->root );
		ni->root = NULL;
	}
	if ( ni->names != NULL ) {
		free( ni->names );
		ni->names = NULL;
	}
	ni->nextIndex = 0;
}
	/*!
		@function nameIndex
	 strdup()s passed in name argument to keep a copy.
		@param name name to look up or create index for
		@result index of name supplied
	*/
int nameIndex( NameIndex* ni, const char* name );

	/*!
		@function indexName
		@param index index of name to lookup
		@result a name or NULL if invalid index
	*/
NVSINLINE const char* indexName( NameIndex* ni, int index ) {
	if ( index < 0 || index >= ni->nextIndex ) {
		return NULL;
	}
	return ni->names[index];
}


StoredIndexVoteNode* newStoredIndexVoteNodeFromVotes( int numVotes, const NameVote* votes, StoredIndexVoteNode* next, NameIndex* ni );

/* parse name1=rate1&name2=rate2&name3=rate3, allocate and fill a StoredIndexVoteNode. */
StoredIndexVoteNode* newStoredIndexVoteNodeFromURL( NameIndex* ni, const char* votestr );
/*!
	@function newStoredIndexVoteNodeFromIndexURL
	Parse i1=r1&i2=r2[...] vote string.
	indecies must be a decimal integer parseable by strtol().
	ratings must be parseable by strtod().
*/
StoredIndexVoteNode* newStoredIndexVoteNodeFromIndexURL( const char* votestr );
/* allocate and create name1=rate1&name2=rate2&name3=rate3 from a StoredIndexVoteNode. */
char* storedIndexVoteToURL( NameIndex* ni, StoredIndexVoteNode* v );

/* should be created by a factory method and destroyed by close() */

typedef struct VirtualVotingSystem VirtualVotingSystem;
	
/* For all methods below, first argument 'it' is the 'it' member of VirtualVotingSystem. */
struct VirtualVotingSystem {
	/* Passed in pointer is not retained (it would be converted to a StoreIndexVoteNode if needed) */
	int (*voteRating)( void* it, int votesLength, const NameVote* votes );

	/* Ownership for 'votes' parameter goes to the callee and must not be accessed by caller after passing in.
	 * 'votes' must be malloc() allocated memory that callee may free(). 
	 * */
	int (*voteStoredIndexVoteNode)( void* it, StoredIndexVoteNode* votes );

	/*  */
	int (*getWinners)( void* it, int winnersLength, NameVote** winnersP );

	void (*htmlSummary)( void* it, FILE* fout );

	// Like summary, but with verbose behind-the-scenes step-by-step detail.
	// Defaults to htmlSummary as per INIT_VVS_TYPE macro below.
	void (*htmlExplain)( void* it, FILE* fout );

	// Plain text summary. Unimplemented in most methods.
	void (*print)( void* it, FILE* fout );

	void (*setSharedNameIndex)( void* it, NameIndex* ni );

	void (*close)( VirtualVotingSystem* it );// delete

	// For multi-seat methods. May be NULL.
	void (*setSeats)( void* it, int seats );

	/* Opaque context holding election method state. Pass this to all of above methods. */
	void* it;
};

typedef int(*vvs_voteRating)(void*,int,const NameVote*);
typedef int (*vvs_voteStoredIndexVoteNode)( void* it, StoredIndexVoteNode* votes );
typedef int (*vvs_getWinners)( void* it, int numVotes, NameVote** winnersP );
typedef void (*vvs_htmlSummary)( void* it, FILE* fout );
typedef void (*vvs_setSharedNameIndex)( void* it, NameIndex* ni );
typedef void (*vvs_print)( void* it, FILE* fout );
typedef void (*vvs_setSeats)( void* it, int seats );

#define DECLARE_STD_setSharedNameIndex( type ) void type##_setSharedNameIndex( type* it, NameIndex* ni ) {\
	if ( it->ni != NULL ) {\
		clearNameIndex( it->ni );\
		free( it->ni );\
	}\
	it->ni = ni;\
}

// Helper macro for setting up a newVirtual<Type>()
// All the typecasting allows definiton of code to
// use actual type while being called as void*
#define INIT_VVS_TYPE(type) toret->voteRating = (vvs_voteRating)type##_voteRating; \
toret->voteStoredIndexVoteNode = (vvs_voteStoredIndexVoteNode)type##_voteStoredIndexVoteNode; \
toret->getWinners = (vvs_getWinners)type##_getWinners; \
toret->htmlSummary = (vvs_htmlSummary)type##_htmlSummary; \
toret->htmlExplain = (vvs_htmlSummary)type##_htmlSummary; \
toret->print = (vvs_print)type##_print;\
toret->setSharedNameIndex = (vvs_setSharedNameIndex)type##_setSharedNameIndex;\
toret->setSeats = NULL;

struct VirtualVotingSystemFactory {
	VirtualVotingSystem* (*make)();
	const char* name;
//	struct VirtualVotingSystemFactory* next;
};
typedef struct VirtualVotingSystemFactory VirtualVotingSystemFactory;

// terminated with a NULL,NULL entry
extern VirtualVotingSystemFactory votingSystemFatoryTable[];

extern int debug;

#if __cplusplus
}
#endif

#endif
