#include "RankedVotingSystem.h"

class BordaVotingSystem : public RankedVotingSystem {
public:
	BordaVotingSystem( int numCandidates ) : RankedVotingSystem( numCandidates ) {
		talley = new int[numc];
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0;
		}
	}
	virtual ~BordaVotingSystem();

	virtual int voteRanking( int* ranking );
	virtual int getWinners( int* choiceIndecies );
	virtual int write( int fd );
	virtual int read( int fd );
	virtual void htmlSummary( FILE* fout, char** names );
	virtual void print( FILE* fout, char** names );

	static VotingSystem* newBordaVotingSystem( int numc );
protected:
	int* talley;
};
