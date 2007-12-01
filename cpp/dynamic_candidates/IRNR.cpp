#include "IRNR.h"
#include <math.h>
#include <assert.h>

IRNR::IRNR()
: NameVotingSystem(), storedVotes( NULL ), tally( NULL )
{}

int IRNR::voteRating( int numVotes, const NameVote* votes ) {
	storedVotes = newStoredIndexVoteNode( numVotes, votes, storedVotes );
	return 0;
}
int IRNR::voteIndexVotes( int numVotes, const IndexVote* votes ) {
	StoredIndexVoteNode* toret;
	toret = StoredIndexVoteNode::make(numVotes);
	assert(toret);
	if ( ! toret ) {
		return -1;
	}
	for ( int i = 0; i < numVotes; i++ ) {
		toret->vote[i] = votes[i];
	}
	toret->numVotes = numVotes;
	toret->next = storedVotes;
	storedVotes = toret;
	return 0;
}
int IRNR::getWinners( int numVotes, NameVote** votes ) {
	int numActive = numNames();
	assert(votes != NULL);
	if ((numVotes == 0) || (*votes == NULL)) {
		*votes = new NameVote[numActive];
		numVotes = numActive;
	}
	if ( tally != NULL ) {
		delete tally;
	}
	tally = new double[numNames()];
	bool* active = new bool[numNames()];
	int i;
	int toret = -1;
	for ( i = 0; i < numNames(); i++ ) {
		active[i] = true;
	}
	while ( numActive > 1 ) {
		// init tally and count it
		for ( i = 0; i < numNames(); i++ ) {
			tally[i] = 0.0;
		}
		{
			StoredIndexVoteNode* cur;
			cur = storedVotes;
			while ( cur != NULL ) {
				double sum;
				sum = 0.0;
				for ( i = 0; i < cur->numVotes; i++ ) {
					if ( active[cur->vote[i].index] ) {
						sum += fabs( cur->vote[i].rating );
					}
				}
				if ( sum > 0.0 ) {
					// many multiplies are faster, specially if there's a madd
					sum = 1.0 / sum;
					for ( i = 0; i < cur->numVotes; i++ ) {
						if ( active[cur->vote[i].index] ) {
							tally[cur->vote[i].index] += cur->vote[i].rating * sum;
						}
					}
				}
				cur = cur->next;
			}
		}
#if 0
		for ( i = 0; i < numNames(); i++ ) {
			printf("\t\"%s\" = %g\n", indexName( i ), tally[i] );
		}
#endif
		// disqualify loser(s)
		{
			double min;
			int tied;
			for ( i = 0; ! active[i]; i++ ) {
			}
			min = tally[i];
			tied = 1;
			i++;
			for ( ; i < numNames(); i++ ) {
				if ( ! active[i] ) {
					// skip
				} else if ( tally[i] < min ) {
					min = tally[i];
					tied = 1;
				} else if ( tally[i] == min ) {
					tied++;
				}
			}
			if ( tied == numActive ) {
				// last N are tied, return them all
				break;
			}
			for ( i = 0; i < numNames(); i++ ) {
				if ( tally[i] == min ) {
					//printf("disqualify \"%s\" with %g\n", indexName( i ), tally[i] );
					active[i] = false;
					numActive--;
					if ( numActive < numVotes ) {
						(*votes)[numActive].name = indexName( i );
						(*votes)[numActive].rating = tally[i];
					}
				}
			}
		}
	}
	toret = numActive;
	for ( i = 0; i < numNames(); i++ ) {
		if ( active[i] == true ) {
			numActive--;
			if ( numActive < numVotes ) {
				(*votes)[numActive].name = indexName( i );
				(*votes)[numActive].rating = tally[i];
			}
		}
	}
	delete [] active;
	return toret;
}
void IRNR::htmlSummary( FILE* fout ) {

}
void IRNR::print( FILE* fout ) {

}

IRNR::~IRNR() {
	if ( tally != NULL ) {
		delete tally;
	}
	StoredIndexVoteNode* cur;
	cur = storedVotes;
	while ( cur != NULL ) {
		cur = storedVotes->next;
		delete storedVotes;
		storedVotes = cur;
	}
	if ( private_index && index != NULL ) {
		delete index;
	}
}

NameVotingSystem* makeIRNR() {
	return new IRNR();
}

#if DO_MAIN
#include <assert.h>
struct defvotestruct {
	int numVotes;
	const NameVote* votes;
};
const NameVote dv1a[] = {
	{ "boo", 3.4 }, { "yo", 30.2 }, { "ni", -1.0 },
	{ "boo", 30.4 }, { "yo", -1.2 }, { "ni", 1.0 },
};
const struct defvotestruct dv1 = {
	3, dv1a
};
struct defvotestruct defaultVotes[] = {
	{ 3, dv1a + 0 },
	{ 3, dv1a + 3 },
	{ 0, 0 },
};

int main( int argc, char** argv ) {
	IRNR* it = new IRNR();
	NameVote winners[4];
	int err;
	for ( int i = 0; defaultVotes[i].numVotes; i++ ) {
		err = it->voteRating( defaultVotes[i].numVotes, defaultVotes[i].votes );
		assert( err == 0 );
	}
	err = it->getWinners( 4, winners );
	printf("err = %d\n", err );
	for ( int i = 0; i < 4; i++ ) {
		printf("%2d: %0.9g \"%s\"\n", i, winners[i].rating, winners[i].name );
	}
	return 0;
}
#endif
