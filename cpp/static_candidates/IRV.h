/*
*  IRV.h
*  Instant Runoff Voting, though it sucketh
*
*  Created by Brian Olson on Wed Oct 22 2003.
*  Copyright (c) 2003 Brian Olson. All rights reserved.
*
*/
#include "RankedVotingSystem.h"

class IRVNode;

class IRV : public RankedVotingSystem {
public:
	IRV( int numCandidates );
	virtual ~IRV();

	virtual int voteRanking( int* ranking );
	virtual int getWinners( int* choiceIndecies );
	virtual int write( int fd );
	virtual int read( int fd );
	virtual void htmlSummary( FILE* fout, char** names );
	virtual void print( FILE* fout, char** names );

	static VotingSystem* newIRV( int numc );
	protected:
	IRVNode* votes;
	IRVNode** winnerBuckets;
	int* talley;
	bool* active;
	int totalVotes;

	void intoBucket( IRVNode* it );
};
