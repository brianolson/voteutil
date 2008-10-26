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
	
	/** Returns true if identical name orderings.
	Doesn't check for equal ratings because roundoff could make that silly. */
	public static boolean winnersEq(NameVotingSystem.NameVote[] a, NameVotingSystem.NameVote[] b) {
		if ((a == null) && (b == null)) {
			return true;
		}
		if (a == null) {
			return false;
		}
		if (b == null) {
			return false;
		}
		if (a.length != b.length) {
			return false;
		}
		for (int i = 0; i < a.length; i++) {
			if (!a[i].name.equals(b[i].name)) {
				return false;
			}
		}
		return true;
	}
	
	public static void runPartElection(
			SummableVotingSystem dest, NameVotingSystem.NameVote[][] votes,
			int first, int afterLast) {
		for (int i = first; i < afterLast; i++) {
			dest.voteRating(votes[i]);
		}
	}
	
	public static void runCombos(Class nvc, int numc, int numv)
			throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		SummableVotingSystem whole, a, b;
		NameVotingSystem.NameVote[][] votes = generateRandomVotes(numc, numv);
		whole = (SummableVotingSystem)nvc.newInstance();
		for (int i = 0; i < votes.length; i++) {
			whole.voteRating(votes[i]);
		}
		NameVotingSystem.NameVote[] wholeWinners = whole.getWinners();
		int[] splits = new int[]{0, 1, votes.length/2, votes.length-1, votes.length};
		// Test summing {0, 1, half, all-1, all} into {the other half}
		for (int split: splits) {
			a = (SummableVotingSystem)nvc.newInstance();
			b = (SummableVotingSystem)nvc.newInstance();
			for (int i = 0; i < split; i++) {
				a.voteRating(votes[i]);
			}
			for (int i = split; i < votes.length; i++) {
				b.voteRating(votes[i]);
			}
			b.accumulateSubVote(a);
			assert(winnersEq(wholeWinners, b.getWinners()));
		}
		// Test summing 2..9 fragments together and getting same result as one instance.
		for (int fragcount = 3; fragcount < 10; fragcount++) {
			SummableVotingSystem[] fragment = new SummableVotingSystem[fragcount];
			for (int f = 0; f < fragcount; f++) {
				fragment[f] = (SummableVotingSystem)nvc.newInstance();
				int start = (f * numc) / fragcount;
				int end = ((f + 1) * numc) / fragcount;
				for (int i = start; i < end; i++) {
					fragment[f].voteRating(votes[i]);
				}
			}
			for (int f = 1; f < fragcount; f++) {
				fragment[0].accumulateSubVote(fragment[f]);
			}
			assert(winnersEq(wholeWinners, fragment[0].getWinners()));
		}
	}
	
	@Test
	public void Raw()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		runCombos(org.bolson.vote.NamedRaw.class, 2, 100);
		runCombos(org.bolson.vote.NamedRaw.class, 5, 1000);
		runCombos(org.bolson.vote.NamedRaw.class, 20, 1000);
	}
	
	@Test
	public void VRR()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		runCombos(org.bolson.vote.NamedVRR.class, 2, 100);
		runCombos(org.bolson.vote.NamedVRR.class, 5, 1000);
		runCombos(org.bolson.vote.NamedVRR.class, 20, 1000);
	}

	public static void main( String[] argv ) {
		org.junit.runner.JUnitCore.main("org.bolson.vote.SummableVotingSystemTest");
	}
}
