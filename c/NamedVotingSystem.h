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
struct VirtualVotingSystem {
	int (*voteRating)( void* it, int numVotes, const NameVote* votes );
	/*! assumed to retain or free() votes passed in. because it may free(), votes must not be accessed after call.  */
	int (*voteStoredIndexVoteNode)( void* it, StoredIndexVoteNode* votes );
	int (*getWinners)( void* it, int numVotes, NameVote** winnersP );
	void (*htmlSummary)( void* it, FILE* fout );
	void (*print)( void* it, FILE* fout );
	void (*setSharedNameIndex)( void* it, NameIndex* ni );
	void (*close)( VirtualVotingSystem* it );// delete
	void (*setSeats)( void* it, int seats );

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

// helper macro for setting up a newVirtual<Type>()
#define INIT_VVS_TYPE(type) toret->voteRating = (vvs_voteRating)type##_voteRating; \
toret->voteStoredIndexVoteNode = (vvs_voteStoredIndexVoteNode)type##_voteStoredIndexVoteNode; \
toret->getWinners = (vvs_getWinners)type##_getWinners; \
toret->htmlSummary = (vvs_htmlSummary)type##_htmlSummary; \
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
