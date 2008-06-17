package org.bolson.vote;
import java.util.ArrayList;
import java.util.HashMap;

/**
Sum up all effective first place votes.
Elect top choice above quota.
Deweight voters to quota/vote.

Need deweight per voter per choice.

Truncated ballots should, if possible, fully deweight on any choices that do get elected. Let voters who cast fuller ballots transfer more to lower ranked choices.

@see <a href="http://stv.sourceforge.net/Details.html">STV in python</a>
@see <a href="http://en.wikipedia.org/wiki/Single_Transferable_Vote">STV on Wikipedia</a>
 @author Brian Olson
*/
public class NamedSTV extends NameVotingSystem implements MultiSeatElectionMethod {
	/** Number of choices who will count as elected. */
	protected int seats = 1;
	/** HashMap<String,TallyState> map from choice names to the TallyState about them. */
	protected HashMap they = new HashMap();
	/** ArrayList<TallyState> for lookup by index. */
	protected ArrayList indexTS = new ArrayList();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;
	/** ArrayList<WeightedVote> record of all votes passed in. */
	protected ArrayList votes = new ArrayList();

	/**
	 How many winners will we target when running getWinners() ?
	 @param seats number of openings to fill from this set of choices and votes.
	 @see #getWinners()
	 */
	public void setNumSeats(int seats) {
		this.seats = seats;
	}

	/**
	 Wrapper for a NameVote[] vote so that it can be deweighted during intermediate steps of STV.
	 */
	protected static class WeightedVote {
		public int[] index;
		public float[] rating;
		double weight = 1.0;
		public WeightedVote( int size ) {
			index = new int[size];
			rating = new float[size];
		}
		public void reset() {
			weight = 1.0;
		}

		/**
			@return dead vote amount, to be removed from total for quota calculation. This might always be 1.0 or 0.0 ?
		*/
		public double vote( ArrayList indexTS ) {
			TallyState ts;
			int i;
			double tw = 1.0;
			for ( i = 0; i < index.length; i++ ) {
				ts = (TallyState)indexTS.get( index[i] );
				if ( ! ts.active ) {
					// skip it, do nothing.
				} else if ( ts.weight >= tw ) {
					// consume vote.
					ts.tally += tw;
					tw = 0.0;
					return 0.0;
				} else {
					ts.tally += ts.weight;
					tw -= ts.weight;
				}
				if ( tw < 0.00001 ) {
					return 0.0;
				}
			}
			i = index.length - 1;
			ts = (TallyState)indexTS.get( index[i] );
			while ( ! ts.active ) {
				i--;
				if ( i < 0 ) {
					// no place left to dump this vote weight
					return tw;
				}
				ts = (TallyState)indexTS.get( index[i] );
			}
			ts.tally += tw;
			ts.deadEndTally += tw;
			return 0.0;
		}
		
		/**
		 Setup operation which makes ratings in sorted order for easier counting later.
		 Implemented as bubble-sort out of laziness and it's good enough because ballots are probably pretty short.
		 */
		public void sort() {
			boolean notdone = true;
			while ( notdone ) {
				notdone = false;
				for ( int i = 0; i < index.length - 1; ++i ) {
					if ( rating[i] < rating[i+1] ) {
						int ti = index[i];
						float tr = rating[i];
						index[i] = index[i+1];
						rating[i] = rating[i+1];
						index[i+1] = ti;
						rating[i+1] = tr;
						notdone = true;
					}
				}
			}
		}
	} /* class WeightedVote */
	
	public NamedSTV() {
	}

	/**
	 Holds the internal count state for one choice.
	 */
	protected static class TallyState {
		public String name;
		public int index;
		public double tally = 0.0;

		/** subset of votes that this is the last ranked choice for. */
		public double deadEndTally = 0.0;

		public double weight = 1.0;
		public boolean active = true;
		public boolean elected = false;
		public TallyState( String nin ) {
			name = nin;
		}
	}
	
	/**
	 Modify this STV method.
	 <p>"seats" <i>n</i> Sets the number of choices to elect.<br />
	 "droop" sets droop quota(default)<br />
	 "hare" sets hare quota<br />
	 "imperiali" sets imperiali quota</p>
	 @see #quota(int,double,int)
	 */
	public int init( String[] argv ) {
		if ( argv != null ) {
			for ( int i = 0; i < argv.length && argv[i] != null; i++ ) {
				if ( argv[i].equals("seats") ) {
					argv[i] = null;
					i++;
					seats = Integer.parseInt( argv[i] );
					argv[i] = null;
				} else if ( argv[i].equals("hare") ) {
					quotaStyle = HARE;
					argv[i] = null;
				} else if ( argv[i].equals("droop") ) {
					quotaStyle = DROOP;
					argv[i] = null;
				} else if ( argv[i].equals("imperiali") ) {
					quotaStyle = IMPERIALI;
					argv[i] = null;
				}
			}
		}
		return super.init( argv );
	}
	
	protected TallyState get( String name ) {
		TallyState v = (TallyState)they.get( name );
		if ( v == null ) {
			v = new TallyState( name );
			v.index = they.size();
			they.put( name, v );
			indexTS.add( v );
		}
		return v;
	}
	/**
	 Record vote. Keeps a reference to passed in vote array.
	 */
	public void voteRating( NameVote[] vote ) {
		if ( vote == null || vote.length == 0 ) {
			return;
		}
		WeightedVote wv = new WeightedVote( vote.length );
		for ( int i = 0; i < vote.length; i++ ) {
			// causes creation of TallyState for name if not already existing
			TallyState v = get( vote[i].name );
			wv.index[i] = v.index;
			wv.rating[i] = vote[i].rating;
		}
		wv.sort();
		votes.add( wv );
		winners = null;
	}
	/**
	 Allows use of java.util.Arrays.sort(Object[],java.util.Comparator) on arrays of TallyState.
	 (Maybe I should just make TallyState Comparable?)
	 */
	protected static class TallyStateComparator implements java.util.Comparator {
		public int compare( Object ai, Object bi ) {
			TallyState a = (TallyState)ai;
			TallyState b = (TallyState)bi;
			if ( a.active && ! b.active ) {
				return -1;
			}
			if ( b.active && ! a.active ) {
				return 1;
			}
			if ( a.tally < b.tally ) {
				return 1;
			} else if ( a.tally > b.tally ) {
				return -1;
			} else {
				return 0;
			}
		}
		public boolean equals( Object o ) {
			return o instanceof TallyStateComparator;
		}
	}
	
	/** Used in htmlSummary. Default "0.00" */
	public static java.text.DecimalFormat ratingFormat = new java.text.DecimalFormat( "0.##" );
	public static void p2f(double f, StringBuffer sb) {
		ratingFormat.format( f, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
	}
	protected static class RoundTemp {
		public String name;
		public double tally;
		public double deadEndTally;
		public double weight;
		//public boolean active;
		public RoundTemp(String n, double t, double det, double w) {
			name = n;
			tally = t;
			deadEndTally = det;
			weight = w;
		}
	}

	public StringBuffer makeRoundsHTML( StringBuffer explain, ArrayList rounds, NameVote[] winners ) {
		explain.append("<table border=\"1\"><tr><th rowspan=\"2\">Name</th>");
		for ( int i = 0; i < rounds.size(); ++i ) {
			explain.append("<th colspan=\"3\">Round ");
			explain.append(i+1);
			explain.append("</th>");
		}
		explain.append("</tr>\n<tr>");
		for ( int i = 0; i < rounds.size(); ++i ) {
			explain.append("<th>Tally</th><th>Dead</th><th>Weight</th>");
		}
		explain.append("</tr>\n");
		for ( int c = 0; c < winners.length; ++c ) {
			explain.append("<tr><td>");
			explain.append(winners[c].name);
			explain.append("</td>");
			for ( int r = 0; r < rounds.size(); ++r ) {
				RoundTemp[] round = (RoundTemp[])rounds.get(r);
				RoundTemp tr = null;
				for ( int i = 0; i < round.length; ++i ) {
					if ( winners[c].name.equals(round[i].name) ) {
						tr = round[i];
						break;
					}
				}
				if ( tr == null ) {
					explain.append("<td></td><td></td><td></td>");
				} else {
					explain.append("<td>");
					p2f( tr.tally, explain );
					explain.append("</td><td>");
					p2f( tr.deadEndTally, explain );
					explain.append("</td><td>");
					p2f( tr.weight, explain );
					explain.append("</td>");
				}
			}
			explain.append("</tr>\n");
		}
		explain.append("<tr><td></td>");
		for ( int r = 0; r < rounds.size(); ++r ) {
			RoundTemp[] round = (RoundTemp[])rounds.get(r);
			RoundTemp tr = null;
			for ( int i = 0; i < round.length; ++i ) {
				if ( round[i].name == null ) {
					tr = round[i];
					break;
				}
			}
			explain.append("<td colspan=\"3\">Total Vote: ");
			p2f( tr.tally, explain );
			explain.append("<br>Quota: ");
			p2f( tr.deadEndTally, explain );
			explain.append("</td>");
		}
		explain.append("</tr>\n");
		explain.append("</table>\n");
		return explain;
	}
	
	// no data, just function, only ever need one of these.
	protected static TallyStateComparator theTSC = new TallyStateComparator();
	/**
	 Run STV over accumulated votes and return results.
	 @return total ranking of all choices. [0..seats-1] are elected.
	 */
	public NameVote[] getWinners() {
		return getWinners( null );
	}
	/**
	 Run STV over accumulated votes and return results.
	 @param explain appends HTML explanation into this StringBuffer
	 @return total ranking of all choices. [0..seats-1] are elected.
	 */
	public NameVote[] getWinners( StringBuffer explain ) {
		if ( winners != null ) {
			return winners;
		}
		double totalweight = 0;
		TallyState[] tv = new TallyState[they.size()];
		int i = 0;
		java.util.Iterator ti = they.values().iterator();
		int winpos = 0;
		ArrayList rounds = null;
		if ( explain != null ) {
			rounds = new ArrayList();
		}
		while ( ti.hasNext() ) {
			TallyState ts = (TallyState)ti.next();
			ts.active = true;
			tv[i] = ts;
			tv[i].weight = 1.0;
			i++;
		}
		while ( true ) {
			double q;
			boolean newelected;
			int numelected;
			do {
				newelected = false;
				numelected = 0;
				int numActive = 0;
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						tv[i].elected = false;
						tv[i].tally = 0.0;
						tv[i].deadEndTally = 0.0;
						numActive++;
					}
				}
				java.util.Iterator vi = votes.iterator();
				totalweight = 0.0;
				while ( vi.hasNext() ) {
					WeightedVote twv = (WeightedVote)vi.next();
					twv.weight = 1.0;
					totalweight += 1.0 - twv.vote( indexTS );
				}
				q = quota( quotaStyle, totalweight, seats - winpos );
				java.util.Arrays.sort( tv, theTSC );
				RoundTemp tr[] = null;
				int tri = 0;
				if ( explain != null ) {
					tr = new RoundTemp[numActive+1];
					rounds.add( tr );
				}
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						if ( tv[i].tally > q ) {
							if ( ! tv[i].elected ) {
								newelected = true;
								tv[i].elected = true;
								numelected++;
							}
							// winner. deweight.
							// calculate weight fraction for all those not devoting their entire weight.
							tv[i].weight = q / (tv[i].tally - tv[i].deadEndTally);
						} else if ( tv[i].elected && tv[i].tally < q ) {
							System.err.println("choice \"" + tv[i].name + "\" was marked elected but now below quota " + q + " with tally " + tv[i].tally );
						}
						if ( explain != null ) {
							tr[tri] = new RoundTemp(tv[i].name, tv[i].tally, tv[i].deadEndTally, tv[i].weight);
							++tri;
						}
					}
				}
				if ( explain != null ) {
					// dummy entry to store global stats
					tr[tri] = new RoundTemp(null, totalweight, q, Double.NaN);
				}
			} while ( newelected && numelected < seats );
			if ( numelected == seats ) {
				break;
			}
			double min = -1;
			int mini = -1;
			for ( i = 0; i < tv.length; i++ ) {
				if ( tv[i].active ) {
					if ((mini == -1) || (tv[i].tally < min)) {
						min = tv[i].tally;
						mini = i;
					}
				}
			}
			if ( mini != -1 ) {
				tv[mini].active = false;
			} else {
				System.err.println("no min to disqualify!");
				for ( i = 0; i < tv.length; i++ ) {
					System.err.println(tv[i].name + "(" + (tv[i].active ? "active" : "      " ) + ")=" + tv[i].tally);
				}
				break;
			}
		}
		java.util.Arrays.sort( tv, theTSC );
		winners = new NameVote[tv.length];
		for ( i = 0; i < tv.length; i++ ) {
			winners[i] = new NameVote( tv[i].name, (float)(tv[i].tally) );
		}
		if ( explain != null ) {
			makeRoundsHTML( explain, rounds, winners );
		}
		return winners;
	}
	protected StringBuffer htmlResultTable( StringBuffer sb, NameVote[] t ) {
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>Best STV Count</th></tr>" );
		for ( int i = 0; i < t.length; i++ ) {
			if ( i < seats ) {
				sb.append("<tr bgcolor=\"#bbffbb\"><td>");
			} else {
				sb.append("<tr bgcolor=\"#ffbbbb\"><td>");
			}				
			sb.append( t[i].name );
			sb.append( "</td><td>" );
			sb.append( t[i].rating );
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		return htmlResultTable( sb, t );
	}
	public StringBuffer htmlExplain( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners( sb );
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		return htmlResultTable( sb, t );
	}
	public String name() {
		return "Single Transferrable Vote";
	}
	
	
	public static final int DROOP = 1;
	public static final int HARE = 2;
	public static final int IMPERIALI = 3;
	
	protected int quotaStyle = DROOP;
	
	/**
	 Three methods of calculating STV election threshold or 'quota'.
	 Droop (1, default): (votes / (seats+1.0)) + 1.0
	 Hare (2): votes / seats
	 Imperiali (3): votes / (seats + 2.0)
	 */
	public static double quota( int style, double votes, int seats ) {
		switch ( style ) {
			case HARE:
				return votes / seats;
			case IMPERIALI:
				return votes / (seats + 2.0);
			case DROOP:
			default:
				return (votes / (seats+1.0)) + 1.0;
		}
	}
	
	public double quota( double votes ) {
		return quota( quotaStyle, votes, seats );
	}
	
	public static void main( String[] argv ) {
		try {
			(new NamedSTV()).defaultMain( argv );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	
	static {
		registerImpl( "STV", NamedSTV.class );
	}
}
