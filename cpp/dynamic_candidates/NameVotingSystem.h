#ifndef NAME_VOTING_SYSTEM_H
#define NAME_VOTING_SYSTEM_H

#include <stdio.h>

/*!
@class NameVotingSystem
Voting System interface that allows voting name=value pairs.
Implementations should be write-in capable and allow new names to be specified on any vote.
A simple name indexing system is provided in this base class.
Interfaces are not thread safe. Access to an instance must be externally synchronized.
*/

#define NO_VOTE ((int)0x80000000)

class NameVote {
public:
	const char* name;
	float rating;
};

/* A more compact struct than NameVote. For implementations that need to hold all votes in memory. */
class IndexVote {
public:
	int index;
	float rating;
};

class NameIndex;

/* The stored vote from one voter; names substituted with name-indexes to save space. */
class StoredIndexVoteNode {
public:
	static StoredIndexVoteNode* make(int numVotes);

	StoredIndexVoteNode* next;
	int numVotes;
	IndexVote vote[1];

	void* operator new(size_t size, int count);

	/** return malloc()ed space full of URL representation of this SIVN */
	char* toURLVoteString(NameIndex* ni);

	/** shallow copy, same hard data, new next same pointer as old next */
	StoredIndexVoteNode* clone();

	/** Parse URL vote (name=value&...) into a SIVN */
	static StoredIndexVoteNode* fromURL(NameIndex* ni, const char* line);
	/** Parse index URL vote (index=value&...) into a SIVN */
	static StoredIndexVoteNode* fromIndexURL(const char* line);
private:
	StoredIndexVoteNode(int numVotesIn) : numVotes(numVotesIn){}
	//StoredIndexVoteNode(const StoredIndexVoteNode& it);// don't define it, insist on automatic bin copy
};


class NameIndex {
protected:
	class Entry;
public:

	Entry* root;
	int nextIndex;
	char** names;
	NameIndex();
	~NameIndex();

	void clear();

	// get index of name, allocating index if name not already seen
	int getIndex(const char* name);
	// return name or null
	const char* getName(int index) const;

	// get index of name, return -1 if name not already seen
	int getIndexConst(const char* name) const;
protected:
	class Entry {
public:
		const char* name;
		int index;
		Entry* lt;
		Entry* gt;
		Entry* up;

		Entry(const char* name_in, int index_in);
		~Entry();
		void clear();
	};

};

void NameVotesFromURLVotes( int* numVotes, NameVote** votes, const char** urlvotes );

class NameVotingSystem {
public:

	/*!
	@function voteRating
	Default to creating IndexVote[] and calling voteIndexVotes().
	If a StoredIndexVoteNode will be created, it'd be faster to override this and do that here.
	* @param numVotes length of votes array
	* @param votes array with name=rating pairs.
	* @result 0 on success, -1 otherwise.
	*/
	virtual int voteRating( int numVotes, const NameVote* votes );

	virtual int voteIndexVotes( int numVotes, const IndexVote* votes ) = 0;

	inline int voteStoredIndexVoteNode( StoredIndexVoteNode* si ) {
		return voteIndexVotes( si->numVotes, si->vote );
	}
	/*!
	@function getWinners
	* @param numVotes length of *votes array
	* @param pointer to votes array which receives winners. if numVotes==0 or *votes==NULL, return malloc()ed memory
	*  Implementation may optionally fill the array with ordered results past the winner(s).
	* @result number of tied winners (hopefully 1)
	*/
	virtual int getWinners( int numVotes, NameVote** votes ) = 0;

	/*!
	@function htmlSummary
	Only valid after getWinners() and before any further vote*() calls.
	Prints an ascii-html summary of the state of the voting system
	(most likely at end, to show results)
	*/
	virtual void htmlSummary( FILE* fout ) = 0;

	/*!
	@function print
	Only valid after getWinners() and before any further vote*() calls.
	*/
	virtual void print( FILE* fout ) = 0;

	/*!
	@function init
	Default implementation does nothing.
	@param argv NULL terminated array of strings. May be modified by init()
	*/
	virtual void init( char** argv );
	
	virtual void setSharedNameIndex(NameIndex* ni);
protected:
	/*!
	@function nameIndex
	@param name name to look up or create index for
	@result index of name supplied
	*/
	inline int nameIndex( const char* name ) {
		return index->getIndex( name );
	}

	/*!
	@function indexName
	@param index index of name to lookup
	@result a name or NULL if invalid index
	*/
	inline const char* indexName( int x ) const {
		return index->getName( x );
	}

	NameIndex* index;
	bool private_index;

	void convertToIndexVote( int numVotes, const NameVote* src, IndexVote* dest );
	IndexVote* convertToIndexVote( int numVotes, const NameVote* votes );
public:

	inline int numNames() {
		if ( index == NULL ) {
			return 0;
		}
		return index->nextIndex;
	}

	void setIndex(NameIndex* index_in) {
		index = index_in;
	}
	const NameIndex* getIndex();

	NameVotingSystem() : index( NULL ), private_index( false ) {};

	virtual ~NameVotingSystem();

	StoredIndexVoteNode* newStoredIndexVoteNode( int numVotes, const NameVote* votes, StoredIndexVoteNode* next );
};

#endif
