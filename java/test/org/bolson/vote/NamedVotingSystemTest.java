package org.bolson.vote;

import org.junit.*;
import static org.junit.Assert.*;

public class NamedVotingSystemTest {

	/** Test casting one vote on a NameVotingSystem class.
	The one vote cast will be 0 for all but one choice which will get 1.
	The one picked will be tested in all positions for votes length 1 to 30. */
	public static void castOneVote( Class nvc )
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		// for a variety of ballot sizes
		for ( int choices = 1; choices < 30; choices++ ) {
			// make sure it works in any position
			for ( int pick = 0; pick < choices; pick++ ) {
				NameVotingSystem it = (NameVotingSystem)nvc.newInstance();
				NameVotingSystem.NameVote[] vote = new NameVotingSystem.NameVote[choices];
				String winnerName = null;
				// init the new vote
				for ( int namei = 0; namei < choices; namei++ ) {
					float rating = 0.0f;
					String name = new String(new char[]{(char)(' ' + namei)});
					if ( namei == pick ) {
						rating = 1.0f;
						winnerName = name;
					}
					vote[namei] = new NameVotingSystem.NameVote( name, rating );
				}
				assertNotNull(winnerName);
				it.voteRating( vote );
				NameVotingSystem.NameVote[] winners = it.getWinners();
				assertEquals(winners[0].name, winnerName);
			}
		}
	}
	
	public static void castNoVote( Class nvc )
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		NameVotingSystem it = (NameVotingSystem)nvc.newInstance();
		NameVotingSystem.NameVote[] winners = it.getWinners();
		assertNotNull(winners);
		assertEquals(winners.length,0);
	}

	static final int kFiftyPercentVotes = 1000;
	static final String kWinnerName = "winner";
	static final String kRunnerUpName = "runner up";

	protected static void oneFiftyFiftyVote(NameVotingSystem it, int pick, int choices, java.util.Random rand, String winnerName) {
		NameVotingSystem.NameVote[] vote = new NameVotingSystem.NameVote[choices];
		// init the new vote
		for ( int namei = 0; namei < choices; namei++ ) {
			float rating;
			String name;
			if ( namei == pick ) {
				rating = 1.0f;
				name = winnerName;
			} else {
				rating = rand.nextFloat() - 1.0f;
				name = Integer.toString(namei, 36);
			}
			vote[namei] = new NameVotingSystem.NameVote( name, rating );
		}
		assertNotNull(winnerName);
		it.voteRating( vote );
	}
	
	/** Test casting many votes vs many votes + 1.
	Votes cast will be [-1.0..0.0) with one 1.0 rating per vote.
	All positional combos of the 50% and the 50%+1 candidate will be tested.
	Votes lengths vary 2 to 30. */
	public static void castFiftyFiftyPlusOne( Class nvc )
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		// new seed every day
		java.util.Random rand = new java.util.Random( System.currentTimeMillis() / (24*60*60*1000) );
		// for a variety of ballot sizes
		for ( int choices = 2; choices <= 10; choices++ ) {
			// make sure it works in any position
			for ( int pick = 0; pick < choices; pick++ ) {
				for ( int runnerup = 0; runnerup < choices; runnerup++ ) {
					if ( runnerup == pick ) {
						continue;
					}
					NameVotingSystem it = (NameVotingSystem)nvc.newInstance();
					for ( int i = 0; i < kFiftyPercentVotes+1; i++ ) {
						oneFiftyFiftyVote(it, pick, choices, rand, kWinnerName);
					}
					for ( int i = 0; i < kFiftyPercentVotes; i++ ) {
						oneFiftyFiftyVote(it, runnerup, choices, rand, kRunnerUpName);
					}
					NameVotingSystem.NameVote[] winners = it.getWinners();
					assertEquals(winners[0].name, kWinnerName);
					assertEquals(winners[1].name, kRunnerUpName);
				}
			}
		}
	}
	
	/*static final Class[] knownVotingSystems = {
		org.bolson.vote.IRNR.class,
		org.bolson.vote.IRV.class,
		org.bolson.vote.Approval.class,
		org.bolson.vote.Raw.class,
		org.bolson.vote.VRR.class,
	};

	@Test(timeout=1000)
	public void castOneVoteAllMethods()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		for ( int i = 0; i < knownVotingSystems.length; i++ ) {
			castOneVote( knownVotingSystems[i] );
		}
	}

	@Test(timeout=100)
	public void castNoVoteAllMethods()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		for ( int i = 0; i < knownVotingSystems.length; i++ ) {
			NameVotingSystem it = (NameVotingSystem)knownVotingSystems[i].newInstance();
			NameVotingSystem.NameVote[] winners = it.getWinners();
			assertNotNull(winners);
			assertEquals(winners.length,0);
		}
	}*/
	
	public static void testNameVotingSystem( Class nvc )
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		castOneVote( nvc );
		castNoVote( nvc );
		castFiftyFiftyPlusOne( nvc );
	}
	
	@Test
	public void IRNR()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testNameVotingSystem( org.bolson.vote.IRNR.class );
	}
	@Test
	public void IRV()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testNameVotingSystem( org.bolson.vote.IRV.class );
	}
	@Test
	public void Approval()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testNameVotingSystem( org.bolson.vote.Approval.class );
	}
	@Test
	public void VRR()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testNameVotingSystem( org.bolson.vote.VRR.class );
	}
	@Test
	public void Raw()
	throws java.lang.InstantiationException, java.lang.IllegalAccessException {
		testNameVotingSystem( org.bolson.vote.Raw.class );
	}
	
	public static void main( String[] argv ) {
		org.junit.runner.JUnitCore.main("org.bolson.vote.VotingSystemTest");
	}
}
