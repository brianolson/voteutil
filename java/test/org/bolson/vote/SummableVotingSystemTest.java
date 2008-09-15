package org.bolson.vote;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.Random;

/**
 Test SummableVotintgSystem implementations.
 The primary test is to run random vote data sets and compare fragmented versions summed up against running the whole set in one instance.
 */
public class SummableVotingSystemTest {
	public static NameVotingSystem.NameVote[][] generateRandomVotes(int numc, int numv) {
		String[] names = new String[numc];
		for ( int i = 0; i < numc; i++ ) {
			names[i] = Integer.toString( i + 10, 36 );
		}
		NameVotingSystem.NameVote[][] out = new NameVotingSystem.NameVote[numv][];
		Random r = new Random();
		for ( int v = 0; v < numv; v++ ) {
			NameVotingSystem.NameVote[] vote = new NameVotingSystem.NameVote[numc];
			for ( int c = 0; c < numc; c++ ) {
				vote[c] = new NameVotingSystem.NameVote( names[c], (float)r.nextGaussian() );
			}
			out[v] = vote;
		}
		return out;
	}
	
	// TODO: Test summing {empty, one vote, many} into {empty, one vote, many} and getting same result as one instance.
	// TODO: Test summing 2..9 fragments together and getting same result as one instance.
}
