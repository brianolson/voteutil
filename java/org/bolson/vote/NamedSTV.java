package org.bolson.vote;
import java.util.ArrayList;
import java.util.HashMap;

/**
Sum up all effective first place votes.
Elect top choice above quota.
Deweight voters to quota/vote.

Need deweight per voter per choice.

Truncated ballots should, if possible, fully deweight on any choices that do get elected. Let voters who cast fuller ballots transfer more to lower ranked choices.

@href http://stv.sourceforge.net/Details.html
@href http://en.wikipedia.org/wiki/Single_Transferable_Vote
*/
public class NamedSTV extends NameVotingSystem {
	int seats = 1;
	HashMap they = new HashMap();
	NameVote[] winners = null;
	ArrayList votes = new ArrayList();
	
	protected static class WeightedVote {
		NameVote[] vote;
		double weight = 1.0;
		int end;
		public WeightedVote( NameVote[] v ) {
			vote = v;
			java.util.Arrays.sort( vote );
			end = vote.length - 1;
		}
		public void reset() {
			weight = 1.0;
			end = vote.length - 1;
		}

		/**
			@return dead vote amount, to be removed from total for quota calculation. This might always be 1.0 or 0.0 ?
		*/
		public double vote( HashMap they ) {
			TallyState ts;
			int i;
			i = end - 1;
			while ( i >= 0 ) {
				ts = (TallyState)they.get( vote[i].name );
				if ( ts.active ) break;
				// found a new end.
				end = i;
				i--;
			}
			if ( end == 0 ) {
				// all votes here are inactive.
				return 1.0;
			}
			double tw = 1.0;
			for ( i = 0; i < end; i++ ) {
				ts = (TallyState)they.get( vote[i].name );
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
			i = end;
			ts = (TallyState)they.get( vote[i].name );
			while ( ! ts.active ) {
				i--;
				if ( i < 0 ) {
					// no place left to dump this vote weight
					return tw;
				}
				ts = (TallyState)they.get( vote[i].name );
			}
			ts.tally += tw;
			ts.deadEndTally += tw;
			return 0.0;
		}
	} /* class WeightedVote */
	
	public NamedSTV() {
	}

	protected static class TallyState {
		public String name;
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
	
	TallyState get( String name ) {
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
		votes.add( new WeightedVote( vote ) );
	}
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
	// no data, just function, only ever need one of these.
	protected static TallyStateComparator theTSC = new TallyStateComparator();
	public NameVote[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		double totalweight = 0;
		TallyState[] tv = new TallyState[they.size()];
		int i = 0;
		java.util.Iterator ti = they.values().iterator();
		int numActive = 0;
		int winpos = 0;
		while ( ti.hasNext() ) {
			TallyState ts = (TallyState)ti.next();
			ts.active = true;
			numActive++;
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
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						tv[i].elected = false;
						tv[i].tally = 0.0;
						tv[i].deadEndTally = 0.0;
					}
				}
				java.util.Iterator vi = votes.iterator();
				totalweight = 0.0;
				while ( vi.hasNext() ) {
					WeightedVote twv = (WeightedVote)vi.next();
					twv.weight = 1.0;
					totalweight += 1.0 - twv.vote( they );
				}
				q = quota( quotaStyle, totalweight, seats - winpos );
				java.util.Arrays.sort( tv, theTSC );
				if ( debug ) {
					debugLog.append("<table border=\"1\"><tr><th>Name</th><th>Tally</th><th>Dead End Tally</th><th>Weight</th></tr>");
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
						if ( debug ) {
							debugLog.append("<tr><td>");
							debugLog.append(tv[i].name);
							debugLog.append("</td><td>");
							debugLog.append(tv[i].tally);
							debugLog.append("</td><td>");
							debugLog.append(tv[i].deadEndTally);
							debugLog.append("</td><td>");
							debugLog.append(tv[i].weight);
							debugLog.append("</td></tr>\n");
						}
					}
				}
				if ( debug ) {
					debugLog.append("<tr><td colspan=\"4\" align=\"center\">total vote: ");
					debugLog.append(totalweight);
					debugLog.append(", quota: ");
					debugLog.append(q);
					debugLog.append("</td></tr></table>\n");
				}
				/*
				// recount having deweighted winners
				totalweight = 0.0;
				vi = votes.iterator();
				while ( vi.hasNext() ) {
					WeightedVote twv = (WeightedVote)vi.next();
					twv.weight = 1.0;
					totalweight += 1.0 - twv.vote( they );
				}
				*/
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
				return null;
			}
		}
		java.util.Arrays.sort( tv, theTSC );
		winners = new NameVote[tv.length];
		for ( i = 0; i < tv.length; i++ ) {
			winners[i] = new NameVote( tv[i].name, (float)(tv[i].tally) );
		}
		return winners;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
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
	public String name() {
		return "Single Transferrable Vote";
	}
	
	
	static final int DROOP = 1;
	static final int HARE = 2;
	static final int IMPERIALI = 3;
	
	int quotaStyle = DROOP;
	
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
	
	double quota( double votes ) {
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
