package org.bolson.vote;
import java.util.Vector;
import java.util.HashMap;

/**
 Instant Runoff Voting.
 Do not use, IRV is bad, VRR is better, IRNR is my favorite.
 
 @see NamedVRR
 @see NamedIRNR
 @author Brian Olson
 */
public class NamedIRV extends NameVotingSystem {
	/** Map names to TallyState instance. Could be HashMap<String,TallyState> */
	protected HashMap they = new HashMap();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;
	/** Holds each passed in vote.
	 This would be Vector<NameVote[]> if I broke Java 1.4 compatibility. */
	protected Vector votes = new Vector();
	protected Vector deadVotes = new Vector();
	protected Vector tiedVotes = new Vector();

	public NamedIRV() {
	}

	/**
	 Holds the internal count state for one choice.
	 */
	protected static class TallyState {
		public String name;
		public double tally = 0.0;
		public boolean active = true;
		public Vector votes = new Vector();
		public TallyState( String nin ) {
			name = nin;
		}
		public TallyState stateCopy() {
			TallyState x = new TallyState(name);
			x.tally = tally;
			x.active = active;
			return x;
		}
	}
	
	protected TallyState get( String name ) {
		TallyState v = (TallyState)they.get( name );
		if ( v == null ) {
			v = new TallyState( name );
			they.put( name, v );
		}
		return v;
	}
	public void voteRating( NameVote[] vote ) {
		if ( vote == null || vote.length == 0 ) {
			return;
		}
		winners = null;
		for ( int i = 0; i < vote.length; i++ ) {
			// causes creation of TallyState for name if not already existing
			TallyState v = get( vote[i].name );
		}
		votes.add( vote );
	}
	protected void bucketize( NameVote[] vote ) {
		float max = Float.NaN;
		int tied = 1;
		int i = 0;
		TallyState v = null, maxv = null;
		for ( ; i < vote.length; i++ ) {
			v = get( vote[i].name );
			if ( v.active ) {
				maxv = v;
				max = vote[i].rating;
				i++;
				break;
			}
		}
		if ( maxv == null ) {
			// none of the names in this vote are active
			deadVotes.add( vote );
			return;
		}
		for ( ; i < vote.length; i++ ) {
			v = get( vote[i].name );
			if ( ! v.active ) {
				// ignore
			} else if ( vote[i].rating > max ) {
				max = vote[i].rating;
				maxv = v;
				tied = 1;
			} else if ( vote[i].rating == max ) {
				tied++;
			}
		}
		if ( tied == 1 ) {
			maxv.votes.add( vote );
		} else {
			double fract = 1.0 / tied;
			for ( i = 0; i < vote.length; i++ ) {
				if ( vote[i].rating == max ) {
					v = get( vote[i].name );
					v.tally += fract;
				}
			}
			tiedVotes.add( vote );
		}
	}
	
	public NameVote[] getWinners() {
		return getWinners(null);
	}
	public NameVote[] getWinners(StringBuffer explain) {
		if ( winners != null ) {
			return winners;
		}
		TallyState[] tv = new TallyState[they.size()];
		int i = 0;
		java.util.Iterator ti = they.values().iterator();
		votes.addAll( deadVotes );
		deadVotes.clear();
		int numActive = 0;
		while ( ti.hasNext() ) {
			TallyState ts = (TallyState)ti.next();
			votes.addAll( ts.votes );
			ts.votes.clear();
			ts.tally = 0;
			ts.active = true;
			numActive++;
			tv[i] = ts;
			i++;
		}
		while ( votes.size() > 0 ) {
		    bucketize( (NameVote[])votes.remove(votes.size()-1) );
		}
		winners = new NameVote[tv.length];
		Vector rounds = null;  // Vector<TallyState[]>
		if (explain != null) {
			rounds = new Vector();
		}
		// numActive == tv.length
		while ( numActive > 1 ) {
			double min = 0;
			int mini = 0;
			for ( i = 0; i < tv.length; i++ ) {
				if ( tv[i].active ) {
					min = tv[i].votes.size() + tv[i].tally;
					mini = i;
					break;
				}
			}
			for ( ; i < tv.length; i++ ) {
				double tm = tv[i].votes.size() + tv[i].tally;
				if ( tv[i].active && (tm < min) ) {
					min = tm;
					mini = i;
				}
			}
			tv[mini].active = false;
			if (rounds != null) {
				TallyState[] ntv = new TallyState[tv.length];
				for ( i = 0; i < tv.length; i++ ) {
					ntv[i] = tv[i].stateCopy();
				}
				rounds.add(ntv);
			}
			numActive--;
			winners[numActive] = new NameVote( tv[mini].name, (float)min );
			if ( numActive > 1 ) {
				// redistribute disqualified votes
				while ( tv[mini].votes.size() > 0 ) {
					bucketize( (NameVote[])tv[mini].votes.remove(tv[mini].votes.size()-1) );
				}
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						tv[i].tally = 0;
					}
				}
				Vector oldTied = tiedVotes;
				tiedVotes = new Vector();
				while ( oldTied.size() > 0 ) {
					bucketize( (NameVote[])oldTied.remove(oldTied.size()-1) );
				}
			}
		}
		// Find the winner
		for ( i = 0; i < tv.length; i++ ) {
			if ( tv[i].active ) {
				break;
			}
		}
		if ( i >= tv.length || tv[i] == null ) {
			// No winner. Bad source data?
			winners = new NameVote[0];
		} else {
			winners[0] = new NameVote( tv[i].name, (float)(tv[i].votes.size() + tv[i].tally) );
		}
		if ( explain != null && rounds != null ) {
			roundsToHTML( explain, rounds, winners );
		}
		return winners;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>Best IRV Count</th></tr>" );
		for ( int i = 0; i < t.length; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( t[i].name );
			sb.append( "</td><td>" );
			if ( Math.round( t[i].rating ) == t[i].rating ) {
				sb.append( (long)(t[i].rating) );
			} else {
				sb.append( t[i].rating );
			}
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	/** @return "Instant Runoff Voting" */
	public String name() {
		return "Instant Runoff Voting";
	}
	
	/**
	 @param sb where to print the table
	 @param rounds is Vector<TallyState[]> of intermediate state.
	 @return sb with stuff
	 */
	public static StringBuffer roundsToHTML( StringBuffer sb, Vector rounds, NameVote[] winners ) {
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
							sb.append( winners[i].name );
							sb.append( "</td><td>" );
							sb.append( tv[i].tally );
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

	public StringBuffer htmlExplain( StringBuffer sb ){
		winners = null;
		getWinners(sb);
		return htmlSummary( sb );
	}
	
	static {
		registerImpl( "IRV", NamedIRV.class );
	}
};
