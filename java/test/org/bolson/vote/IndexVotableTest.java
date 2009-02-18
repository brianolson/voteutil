package org.bolson.vote;

import org.junit.*;
import static org.junit.Assert.*;
import java.util.Random;

public class IndexVotableTest {
	NameVotingSystem.NameVote[][] votes;
	NameVotingSystem.IndexVoteSet[] ivotes;

	public void generateRandomVotes(int numc, int numv) {
		String[] names = new String[numc];
		for ( int i = 0; i < numc; i++ ) {
			names[i] = Integer.toString( i + 1 );
		}
		votes = new NameVotingSystem.NameVote[numv][];
		ivotes = new NameVotingSystem.IndexVoteSet[numv];
		Random r = new Random();
		for ( int v = 0; v < numv; v++ ) {
			NameVotingSystem.NameVote[] vote = new NameVotingSystem.NameVote[numc];
			ivotes[v] = new NameVotingSystem.IndexVoteSet(numc);
			for ( int c = 0; c < numc; c++ ) {
				vote[c] = new NameVotingSystem.NameVote( names[c], (float)r.nextGaussian() );
				ivotes[v].rating[c] = vote[c].rating;
				ivotes[v].index[c] = c;
			}
			votes[v] = vote;
		}
	}
	public void generateRandomIndexVotes(int numc, int numv) {
		ivotes = new NameVotingSystem.IndexVoteSet[numv];
		Random r = new Random();
		for ( int v = 0; v < numv; v++ ) {
			ivotes[v] = new NameVotingSystem.IndexVoteSet(numc);
			for ( int c = 0; c < numc; c++ ) {
				ivotes[v].rating[c] = (float)r.nextGaussian();
				ivotes[v].index[c] = c;
			}
		}
	}
	
	public void testSystem(Class ivc, int numc, int numv)
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		IndexVotable iv = (IndexVotable)ivc.newInstance();
		IndexVotable nv = (IndexVotable)ivc.newInstance();
		generateRandomVotes(numc, numv);
		for (int i = 0; i < numv; i++) {
			iv.voteIndexVoteSet(ivotes[i]);
			nv.voteRating(votes[i]);
		}
		NameVotingSystem.NameVote[] ivw = iv.getWinners();
		NameVotingSystem.NameVote[] nvw = nv.getWinners();
		boolean good = SummableVotingSystemTest.winnersEq(ivw, nvw);
		if (!good) {
			StringBuffer sb = new StringBuffer("index vote=\n");
			sbWinners(sb, ivw);
			sb.append("name vote=\n");
			sbWinners(sb, nvw);
			fail(sb.toString());
		}
	}
	public void testSystemBatch(Class ivc)
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testSystem(ivc, 2, 100);
		testSystem(ivc, 5, 1000);
		testSystem(ivc, 20, 5000);
	}
	
	public static StringBuffer sbWinners(StringBuffer sb, NameVotingSystem.NameVote[] winners) {
		for (int i = 0; i < winners.length; i++) {
			sb.append(winners[i].name);
			sb.append(": ");
			sb.append(winners[i].rating);
		}
		return sb;
	}
	
	@Test
	public void IRV()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testSystemBatch(org.bolson.vote.IRV.class);
	}
	@Test
	public void IRNR()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testSystemBatch(org.bolson.vote.IRNR.class);
	}
	@Test
	public void VRR()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testSystemBatch(org.bolson.vote.VRR.class);
	}
	
	public void testSpeed(IndexVotable x) {
		long start = System.currentTimeMillis();
		for (int i = 0; i < ivotes.length; i++) {
			x.voteIndexVoteSet(ivotes[i]);
		}
		NameVotingSystem.NameVote[] winners = x.getWinners();
		long end = System.currentTimeMillis();
		double vps = ivotes.length / ((double)(end - start) / 1000.0);
		System.out.println( x.name() + ": " + vps + " votes/second");
	}

	public static final int kSpeedNumc = 10;
	public static final int kSpeedNumv = 100000;

	@Test
	public void IRV_Speed() {
		generateRandomIndexVotes(kSpeedNumc, kSpeedNumv);
		testSpeed(new IRV());
	}
	@Test
	public void IRNR_Speed() {
		generateRandomIndexVotes(kSpeedNumc, kSpeedNumv);
		testSpeed(new IRNR());
	}
	@Test
	public void VRR_Speed() {
		generateRandomIndexVotes(kSpeedNumc, kSpeedNumv);
		testSpeed(new VRR());
	}

	public static void main( String[] argv ) {
		org.junit.runner.JUnitCore.main("org.bolson.vote.IndexVotableTest");
	}
}
