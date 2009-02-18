package org.bolson.vote;

import java.util.ArrayList;
import java.util.HashMap;

/**
Instant Runoff Normalized Ratings.
 <p>Given votes of ratings for choices, normalize so that each voter has a vote of equal magnitude.
 Sum up the normalized ratings.<br />
 If there are more than 2 choices, disqualify the one with the lowest sum.<br />
 Re-normalize the votes as if the disqualified candidates aren't there so that each voter still has a full magnitude vote.<br />
 Repeat sum, disqualify, re-normalize as needed.</p>
 @see <a href="http://bolson.org/voting/methods.html#IRNR">IRNR (bolson.org)</a>
 @author Brian Olson
*/
public class IRNR extends NameVotingSystem implements IndexVotable {
	/** Holds each passed in vote.
	 This would be ArrayList<IndexVoteSet> if I broke Java 1.4 compatibility. */
	protected ArrayList votes = new ArrayList();
	/** ArrayList<String> for lookup of name from index. */
	protected ArrayList indexNames = new ArrayList();
	/** ArrayList<TallyState> for lookup by index. */
	protected ArrayList indexTS = new ArrayList();
	/** Map names to TallyState instance. Could be HashMap<String,TallyState> */
	protected HashMap names = new HashMap();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;

	/** Get the TallyState for some name, or instantiate one if needed. */
	protected TallyState get(String name) {
		TallyState ts;
		ts = (TallyState)names.get( name );
		if ( ts != null ) {
			ts.count++;
		} else {
			int ni = names.size();
			ts = new TallyState( name, ni );
			names.put( name, ts );
			indexNames.add( name );
			indexTS.add( ts );
		}
		return ts;
	}

	/** Record vote.
	 Keeps a refence to passed in array.
	 */
	public void voteRating( NameVote[] vote ) {
		if ( vote == null ) {
			return;
		}
		IndexVoteSet iv = new IndexVoteSet(vote.length);
		for ( int i = 0; i < vote.length; i++ ) {
			TallyState ts = get(vote[i].name);
			iv.index[i] = ts.index;
			iv.rating[i] = vote[i].rating;
		}
		votes.add( iv );
		winners = null;
	}
	public void voteIndexVoteSet(IndexVoteSet vote) {
		if ( vote == null || vote.index.length == 0 ) {
			return;
		}
		int maxi = -1;
		for ( int i = 0; i < vote.index.length; i++ ) {
			if ( vote.index[i] > maxi ) {
				maxi = vote.index[i];
			}
		}
		while ( indexTS.size() <= maxi ) {
			get(Integer.toString(indexTS.size() + 1));
		}
		votes.add( vote );
		winners = null;
	}
	/**
	 Holds the internal count state for one choice.
	 */
	protected static class TallyState {
		public String name;
		public double tally;
		public boolean active;
		public int index;
		/** count is the number of ballots that include this name */
		public int count = 1;
		TallyState( String nin, int ii ) {
			name = nin;
			index = ii;
		}
		/**
		 Copy state for later verbose round descriptions.
		 fractions usage in copy is full count, not just fractions.
		 */
		public TallyState stateCopy() {
			TallyState x = new TallyState(name, index);
			x.tally = tally;
			x.active = active;
			x.count = count;
			return x;
		}
	}
	
	/**
	 Normalize to vector magnitude == 1.
	 If false then sum of absolute values == 1.
	 */
	protected boolean rmsnorm = true;
	public NameVote[] getWinners() {
		return getWinners(null);
	}
	public NameVote[] getWinners( StringBuffer explainb ) {
		boolean explain = explainb != null;
		ArrayList rounds = null;
		if ( explain ) {
			rounds = new ArrayList(); // ArrayList<TallyState[]>
		}
		if ( winners != null && ! explain ) {
			return winners;
		}
		Object[] nameoa = names.values().toArray();
		TallyState[] namea = new TallyState[nameoa.length];
		int i;
		for ( i = 0; i < nameoa.length; i++ ) {
			namea[i] = (TallyState)nameoa[i];
			namea[i].active = true;
		}
		nameoa = null;
		int numc = namea.length;
		if ( numc == 0 ) {
			if ( explain ) {
				explainb.append("no votes counted");
			}
			return winners = new NameVote[0];
		}
		int numWinners = 1;
		int numActive = numc;
		int choiceIndecies[] = new int[numc];
		winners = new NameVote[numc];
		while ( numActive > 1 ) {
			// per IR setup
			{
				numActive = 0;
				for ( int c = 0; c < numc; c++ ) {
					if ( namea[c].active ) {
						namea[c].tally = 0.0;
						choiceIndecies[numActive] = c;
						numActive++;
					}
				}
			}
			// sum up into tally
			for ( int v = 0; v < votes.size(); v++ ) {
				IndexVoteSet ot;
				double ts;
				boolean hasAny;
				ts = 0.0;
				ot = (IndexVoteSet)votes.get( v );
				hasAny = false;
				for ( int c = 0; c < ot.index.length; c++ ) {
					TallyState tts;
					tts = (TallyState)indexTS.get(ot.index[c]);
					if ( tts.active && ! Float.isNaN( ot.rating[c] ) ) {
						hasAny = true;
						if ( rmsnorm ) {
							ts += ot.rating[c] * ot.rating[c];
						} else {
							ts += Math.abs(ot.rating[c]);
						}
					}
				}
				if ( hasAny ) {
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					for ( int c = 0; c < ot.index.length; c++ ) {
						TallyState tts;
						tts = (TallyState)indexTS.get(ot.index[c]);
						if ( tts.active && ! Float.isNaN( ot.rating[c] ) ) {
							double tp;
							tp = ot.rating[c] / ts;
							if ( ! Double.isNaN( tp ) ) {
								tts.tally += tp;
							}
						}
					}
				}
			}
			// sort
			boolean notdone = true;
			while ( notdone ) {
				notdone = false;
				for ( int c = 1; c < numActive; c++ ) {
					if ( namea[choiceIndecies[c]].tally > namea[choiceIndecies[c-1]].tally ) {
						int ti = choiceIndecies[c];
						choiceIndecies[c] = choiceIndecies[c-1];
						choiceIndecies[c-1] = ti;
						notdone = true;
					}
				}
			}
			if (rounds != null) {
				TallyState[] ntv = new TallyState[namea.length];
				for ( i = 0; i < namea.length; i++ ) {
					ntv[i] = namea[i].stateCopy();
				}
				rounds.add(ntv);
			}
			if ( namea[choiceIndecies[0]].tally == namea[choiceIndecies[numActive-1]].tally ) {
				// N-way tie.
				numWinners = numActive;
				break;
			}
			numActive--;
			TallyState loser = namea[choiceIndecies[numActive]];
			loser.active = false;
			winners[numActive] = new NameVote( loser.name, (float)loser.tally );
			while ( (numActive > 0) &&
					(namea[choiceIndecies[numActive-1]].tally == namea[choiceIndecies[numActive]].tally) ) {
				// eliminate all who tied for last
				numActive--;
				loser = namea[choiceIndecies[numActive]];
				loser.active = false;
				winners[numActive] = new NameVote( loser.name, (float)loser.tally );
			}
		} // while numActive > 1
		for ( i = 0; i < numWinners; i++ ) {
			winners[i] = new NameVote( namea[choiceIndecies[i]].name, (float)namea[choiceIndecies[i]].tally );
		}
		if ( explain && rounds != null ) {
			roundsToHTML( explainb, rounds, winners );
		}
		return winners;
	}
	/** Used in htmlSummary. Default "0.00" */
	public static java.text.DecimalFormat ratingFormat = new java.text.DecimalFormat( "0.00" );
	public StringBuffer htmlSummary( StringBuffer sb ) {
		if ( winners == null ) {
			getWinners();
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>IRNR Rating</th></tr>" );
		for ( int i = 0; i < winners.length && winners[i] != null; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( winners[i].name );
			sb.append( "</td><td>" );
			ratingFormat.format( winners[i].rating, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
			//sb.append( winners[i].rating );
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public StringBuffer htmlExplain( StringBuffer sb ) {
	    getWinners( sb );
	    return htmlSummary( sb );
	}
	/**
	 @param sb where to print the table
	 @param rounds is ArrayList<TallyState[]> of intermediate state.
	 @return sb with stuff
	 */
	public static StringBuffer roundsToHTML( StringBuffer sb, ArrayList rounds, NameVote[] winners ) {
		sb.append("<table border=\"1\"><tr>");
		for ( int r = 0; r < rounds.size(); r++ ) {
			sb.append( "<th colspan=\"2\">Round " );
			sb.append( r + 1 );
			sb.append( "</th>");
		}
		sb.append( "</tr>\n<tr>" );
		for ( int r = 0; r < rounds.size(); r++ ) {
			sb.append( "<th>Name</th><th>Count</th>");
		}
		sb.append( "</tr>\n" );
		if ( (winners != null) && (winners.length > 0) ) {
			// present based on sorted order
			for ( int c = 0; c < winners.length; c++ ) {
				sb.append( "<tr>" );
				for ( int r = 0; r < rounds.size(); r++ ) {
					TallyState[] tv = (TallyState[])rounds.get(r);
					boolean found = false;
					for ( int i = 0; i < tv.length; i++ ) {
						if ( tv[i].name.equals( winners[c].name ) ) {
							found = true;
							if ( tv[i].active ) {
								sb.append( "<td>" );
							} else {
								sb.append( "<td style=\"color:#999999;\">" );
							}
							sb.append( tv[i].name );
							sb.append( "</td>" );
							if ( tv[i].active ) {
								sb.append( "<td>" );
							} else {
								sb.append( "<td style=\"color:#999999;\">" );
							}
							ratingFormat.format( tv[i].tally, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
							sb.append( "</td>" );
						}
					}
					if ( ! found ) {
						System.err.println( "round(" + r + "): could not find winners[" + c + "] \"" + winners[c].name + "\" in tv:" );
						for ( int i = 0; i < tv.length; i++ ) {
							System.err.println( tv[i].name + " = " + tv[i].tally );
						}
					}
				}
				sb.append( "</tr>\n" );
			}
		} else {
			sb.append( "<tr><td>FIXME: implement no-winner-data IRV explain</td></tr>\n" );
		}
		sb.append( "</table>\n" );
		return sb;
	}
	
	/** "Instant Runoff Normalized Ratings" */
	public String name() {
		return "Instant Runoff Normalized Ratings";
	}
	
	static {
		registerImpl( "IRNR", IRNR.class );
	}
};
