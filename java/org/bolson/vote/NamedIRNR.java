package org.bolson.vote;

import java.util.Vector;
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
public class NamedIRNR extends NameVotingSystem {
	/** Holds each passed in vote.
	 This would be Vector<NameVote[]> if I broke Java 1.4 compatibility. */
	protected Vector votes = new Vector();
	/** Map names to TallyState instance. Could be HashMap<String,TallyState> */
	protected HashMap names = new HashMap();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;

	/** Record vote.
	 Keeps a refence to passed in array.
	 */
	public void voteRating( NameVote[] vote ) {
		if ( vote == null ) {
			return;
		}
		votes.add( vote );
		for ( int i = 0; i < vote.length; i++ ) {
			TallyState ts;
			ts = (TallyState)names.get( vote[i].name );
			if ( ts != null ) {
				ts.count++;
			} else {
				names.put( vote[i].name, new TallyState( vote[i].name ) );
			}
		}
		winners = null;
	}
	
	/**
	 Holds the internal count state for one choice.
	 */
	protected static class TallyState {
		public String name;
		public double tally;
		public boolean active;
		/** count is the number of ballots that include this name */
		public int count = 1;
		TallyState( String nin ) {
			name = nin;
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
		boolean debug = false;
		boolean explain = explainb != null;
		java.io.PrintWriter out = null;
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
		double max = Double.NEGATIVE_INFINITY;
		int numWinners = 1;
		int numActive = numc;
		int choiceIndecies[] = new int[numc];
		double dt[] = new double[numc];
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
				NameVote[] ot;
				double ts;
				boolean hasAny;
				ts = 0.0;
				ot = (NameVote[])votes.elementAt( v );
				hasAny = false;
				for ( int c = 0; c < ot.length; c++ ) {
					TallyState tts;
					tts = (TallyState)names.get( ot[c].name );
					if ( tts.active && ! Float.isNaN( ot[c].rating ) ) {
						hasAny = true;
						if ( rmsnorm ) {
							ts += ot[c].rating * ot[c].rating;
						} else {
							ts += Math.abs(ot[c].rating);
						}
					}
				}
				if ( hasAny ) {
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					for ( int c = 0; c < ot.length; c++ ) {
						TallyState tts;
						tts = (TallyState)names.get( ot[c].name );
						if ( tts.active && ! Float.isNaN( ot[c].rating ) ) {
							double tp;
							tp = ot[c].rating / ts;
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
			if ( explain ) {
				explainb.append("<p>");
				explainb.append( numActive );
				explainb.append(" active</p><table border=\"1\">");
				for ( int c = 0; c < numActive; c++ ) {
					explainb.append("<tr><td>");
					explainb.append(namea[choiceIndecies[c]].name);
					explainb.append("</td><td>");
					explainb.append(namea[choiceIndecies[c]].tally);
					explainb.append("</td></tr>\n");
				}
				explainb.append("</table><hr>\n");
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
	    return sb;
	}
	/** "Instant Runoff Normalized Ratings" */
	public String name() {
		return "Instant Runoff Normalized Ratings";
	}
	
	static {
		registerImpl( "IRNR", NamedIRNR.class );
	}
};
