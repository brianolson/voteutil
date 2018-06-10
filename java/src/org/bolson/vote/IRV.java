package org.bolson.vote;
import java.util.ArrayList;
import java.util.HashMap;

/**
 Instant Runoff Voting.
 Do not use, IRV is bad, VRR is better, IRNR is my favorite.
 
 @see VRR
 @see IRNR
 @author Brian Olson
 */
public class IRV extends NameVotingSystem implements IndexVotable {
	/** Map names to TallyState instance. Could be HashMap<String,TallyState> */
	protected HashMap they = new HashMap();
	/** ArrayList<String> for lookup of name from index. */
	protected ArrayList indexNames = new ArrayList();
	/** ArrayList<TallyState> for lookup by index. */
	protected ArrayList indexTS = new ArrayList();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;
	/** Holds each passed in vote.
	 This would be ArrayList<IndexVoteSet> if I broke Java 1.4 compatibility. */
	protected ArrayList votes = new ArrayList();
	/** ArrayList<IndexVoteSet> */
	protected ArrayList tiedVotes = new ArrayList();

	public IRV() {
	}

	/**
	 Holds the internal count state for one choice.
	 */
	protected static class TallyState implements Comparable {
		public String name;
		public int index;
		
		/** The sum of partial votes from cast ties.
			Total count is (votes.size() + fractions) */
		public double fractions = 0.0;
		
		public boolean active = true;

		/** ArrayList<IndexVoteSet> */
		public ArrayList votes = new ArrayList();

		public TallyState( String nin ) {
			name = nin;
		}
		
		/**
		 Copy state for later verbose round descriptions.
		 fractions usage in copy is full count, not just fractions.
		 */
		public TallyState stateCopy() {
			TallyState x = new TallyState(name);
			x.fractions = fractions + votes.size();
			x.active = active;
			return x;
		}
		
		/**
		Sorts on name by default String comparator.
		*/
		public int compareTo(Object o) throws ClassCastException {
			if ( o instanceof String ) {
				return name.compareTo((String)o);
			}
			if ( o instanceof TallyState ) {
				return name.compareTo( ((TallyState)o).name );
			}
			throw new ClassCastException("not a TallyState");
		}
	}
	
	protected TallyState get( String name ) {
		TallyState v = (TallyState)they.get( name );
		if ( v == null ) {
			v = new TallyState( name );
			v.index = they.size();
			indexNames.add( name );
			indexTS.add( v );
			they.put( name, v );
		}
		return v;
	}
	public void voteRating( NameVote[] vote ) {
		if ( vote == null || vote.length == 0 ) {
			return;
		}
		IndexVoteSet iv = new IndexVoteSet(vote.length);
		for ( int i = 0; i < vote.length; i++ ) {
			// causes creation of TallyState for name if not already existing
			TallyState v = get( vote[i].name );
			iv.index[i] = v.index;
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
	protected void bucketize( IndexVoteSet vote ) {
		float max = Float.NaN;
		int tied = 1;
		int i = 0;
		TallyState v = null;
		TallyState maxv = null;
		// find an active one and initialize max
		while ( i < vote.index.length ) {
			v = (TallyState)indexTS.get(vote.index[i]);
			if ( v.active ) {
				maxv = v;
				max = vote.rating[i];
				i++;
				break;
			}
			i++;
		}
		if ( maxv == null ) {
			// none of the names in this vote are active
			return;
		}
		for ( ; i < vote.index.length; i++ ) {
			v = (TallyState)indexTS.get(vote.index[i]);
			if ( ! v.active ) {
				// ignore
			} else if ( vote.rating[i] > max ) {
				max = vote.rating[i];
				maxv = v;
				tied = 1;
			} else if ( vote.rating[i] == max ) {
				tied++;
			}
		}
		if ( tied == 1 ) {
			maxv.votes.add( vote );
		} else {
			double fract = 1.0 / tied;
			for ( i = 0; i < vote.index.length; i++ ) {
				if ( vote.rating[i] == max ) {
					v = (TallyState)indexTS.get(vote.index[i]);
					if ( v.active ) {
						v.fractions += fract;
					}
				}
			}
			tiedVotes.add( vote );
		}
	}
	
	public NameVote[] getWinners() {
		return getWinners(null);
	}
	public NameVote[] getWinners(StringBuffer explain) {
		if ( explain != null ) {
			winners = null;
		}
		if ( winners != null ) {
			return winners;
		}
		TallyState[] tv = new TallyState[they.size()];
		int i = 0;
		java.util.Iterator ti = they.values().iterator();
		int numActive = 0;
		while ( ti.hasNext() ) {
			TallyState ts = (TallyState)ti.next();
			ts.votes.clear();
			ts.fractions = 0;
			ts.active = true;
			numActive++;
			tv[i] = ts;
			i++;
		}
		java.util.Arrays.sort(tv);
		java.util.Iterator vi = votes.iterator();
		while ( vi.hasNext() ) {
		    bucketize( (IndexVoteSet)vi.next() );
		}
		winners = new NameVote[tv.length];
		ArrayList rounds = null;  // ArrayList<TallyState[]>
		if (explain != null) {
			rounds = new ArrayList();
		}
		// numActive == tv.length
		while ( numActive > 1 ) {
			double min = 0;
			int mini = 0;
			// find an active one, initialize min
			i = 0;
			while ( i < tv.length ) {
				if ( tv[i].active ) {
					min = tv[i].votes.size() + tv[i].fractions;
					mini = i;
					i++;
					break;
				}
				i++;
			}
			// find min
			int ties = 1;
			while ( i < tv.length ) {
				if ( tv[i].active ) {
					double tm = tv[i].votes.size() + tv[i].fractions;
					if ( tm < min ) {
						min = tm;
						mini = i;
						ties = 1;
					} else if ( tm == min ) {
						ties++;
					} else if ( debugLog != null ) {
						if ( Math.abs(tm - min) < 0.001 ) {
							debugLog.append("very small diff between mins: ").append(Math.abs(tm - min)).append("\n");
						}
					}
				}
				i++;
			}
			if (rounds != null) {
				TallyState[] ntv = new TallyState[tv.length];
				for ( i = 0; i < tv.length; i++ ) {
					ntv[i] = tv[i].stateCopy();
				}
				rounds.add(ntv);
			}
			// ArrayList<ArrayList<IndexVoteSet> >
			ArrayList rebuck = new ArrayList();
			if (ties == numActive) {
				break;
			} else if (ties == 1) {
				if ( debugLog != null ) { debugLog.append("ties=1, dq \"").append(tv[mini].name).append("\"\n"); }
				tv[mini].active = false;
				tv[mini].fractions = min;  // archive best count
				rebuck.add( tv[mini].votes );
				numActive--;
				winners[numActive] = new NameVote( tv[mini].name, (float)min );
			} else {
				if ( debugLog != null ) { debugLog.append("ties=").append(ties).append(" dq:\n"); }
				for ( i = tv.length - 1; i >= 0; i-- ) {
					if ( tv[i].active &&
							(tv[i].votes.size() + tv[i].fractions == min) ) {
						if ( debugLog != null ) { debugLog.append("\t\"").append(tv[i].name).append("\"\n"); }
						tv[i].active = false;
						tv[i].fractions = min;
						rebuck.add( tv[i].votes );
						numActive--;
						winners[numActive] = new NameVote( tv[i].name, (float)min );
					}
				}
			}
			if ( numActive > 1 ) {
				// redistribute disqualified votes
				while ( rebuck.size() > 0 ) {
					ArrayList votes = (ArrayList)rebuck.remove(rebuck.size()-1);
					while ( votes.size() > 0 ) {
						bucketize( (IndexVoteSet)votes.remove(votes.size()-1) );
					}
				}
				rebuck = null;
				// reset fractions before re-count of tied votes.
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						tv[i].fractions = 0;
					}
				}
				ArrayList oldTied = tiedVotes;
				tiedVotes = new ArrayList();
				while ( oldTied.size() > 0 ) {
					bucketize( (IndexVoteSet)oldTied.remove(oldTied.size()-1) );
				}
			}
		}
		// Find the winner
		int outi = 0;
		for ( i = 0; i < tv.length; i++ ) {
			if ( tv[i].active ) {
				// assert(tv[i] == null);
				winners[outi] = new NameVote( tv[i].name, (float)(tv[i].votes.size() + tv[i].fractions) );
				outi++;
			}
		}
		// check that winners[...] contains no nulls?
		for ( i = 0; i < winners.length; i++ ) {
			if ( winners[i] == null ) {
				winners[i] = new NameVote("IRV IMPLEMENTATION ERROR", 0.0f);
			}
		}
		/*if ( i >= tv.length || tv[i] == null ) {
			// No winner. Bad source data?
			winners = new NameVote[0];
		} else {
			winners[0] = new NameVote( tv[i].name, (float)(tv[i].votes.size() + tv[i].fractions) );
		}*/
		if ( explain != null && rounds != null ) {
			roundsToHTML( explain, rounds, winners );
		}
		return winners;
	}

	/** Used in htmlSummary. Default "0.00" */
	public static java.text.DecimalFormat ratingFormat = new java.text.DecimalFormat( "0.##" );

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
			ratingFormat.format( t[i].rating, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
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
							sb.append( "</td><td>" );
							ratingFormat.format( tv[i].fractions, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
							sb.append( "</td>" );
						}
					}
					if ( ! found ) {
						System.err.println( "round(" + r + "): could not find winners[" + c + "] \"" + winners[c].name + "\" in tv:" );
						for ( int i = 0; i < tv.length; i++ ) {
							System.err.println( tv[i].name + " = " + tv[i].fractions );
						}
					}
				}
				sb.append( "</tr>\n" );
			}
			StringBuffer exaustedVoteRow = new StringBuffer("<tr>");
			sb.append("<tr>");
			double maxActiveVote = 0.0;
			for ( int r = 0; r < rounds.size(); r++ ) {
				TallyState[] tv = (TallyState[])rounds.get(r);
				double av = 0.0;
				for (TallyState ts : tv) {
					if (ts.active) {
						av += ts.fractions;
					}
				}
				if (av > maxActiveVote) {
					maxActiveVote = av;
				}
				sb.append("<td>active votes:</td><td>");
				ratingFormat.format( av, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
				sb.append("</td>");
				exaustedVoteRow.append("<td>exausted votes:</td><td>");
				ratingFormat.format( maxActiveVote - av, exaustedVoteRow, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
				exaustedVoteRow.append("</td>");
			}
			sb.append("</tr>");
			exaustedVoteRow.append("</tr>");
			sb.append(exaustedVoteRow);
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
		registerImpl( "IRV", IRV.class );
	}
};
