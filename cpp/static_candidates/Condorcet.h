#include "RatedVotingSystem.h"

class Condorcet : public RatedVotingSystem {
public:
	Condorcet( int numCandidates );
	virtual ~Condorcet();

	virtual int voteRating( int* ranking );
	virtual int getWinners( int* choiceIndecies );
	virtual int write( int fd );
	virtual int read( int fd );
	virtual void htmlSummary( FILE* fout, char** names );
	virtual void print( FILE* fout, char** names );

	static VotingSystem* newCondorcet( int numc );
protected:
	unsigned int* talley;
	unsigned int* defeatCount;
};
