#include "RatedVotingSystem.h"

class RawRating : public RatedVotingSystem {
public:
	RawRating( int numCandidates ) : RatedVotingSystem( numCandidates ) {
		talley = new long[numc];
		for ( int i = 0; i < numc; i++ ) {
			talley[i] = 0;
		}
	}
	virtual ~RawRating();

	virtual int voteRating( int* rating );
	virtual int getWinners( int* choiceIndecies );
	virtual int write( int fd );
	virtual int read( int fd );
	virtual void htmlSummary( FILE* fout, char** names );
	virtual void print( FILE* fout, char** names );

	void squashToAcceptance( int* rating );

	static VotingSystem* newRawRating( int numc );
protected:
	long* talley;
};
