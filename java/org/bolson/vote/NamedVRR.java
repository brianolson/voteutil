package org.bolson.vote;

import java.util.HashMap;

/**
Virtual Round Robin election (Condorcet).
 @see <a href="http://en.wikipedia.org/wiki/Condorcet's_Method">Condorcet's Method (Wikipedia)</a>
 @author Brian Olson
 */
public class NamedVRR extends NameVotingSystem {
	/** "DUMMY_CHOICE_NAME" is standin for choices not voted on yet so that write-ins count correctly. */
	protected static final String dummyname = "DUMMY_CHOICE_NAME";
	/** HashMap<String,Count> maps choice names to Count.
	 @see Count */
	protected HashMap counts = new HashMap();
	/** Cache of winners. Set by getWinners. Cleared by voteRating. */
	protected NameVote[] winners = null;
	/** intermediate count calculated during getWinners */
	protected int defeatCount[] = null;

	/**
	 Checks arguments to modify this VRR.
	 "winningVotes" and "margins" modify cycle resolution.
	 @see #winningVotes
	 @see #margins
	 */
	public int init( String argv[] ) {
		if ( argv == null ) {
			return 0;
		}
		for ( int i = 0; i < argv.length; ++i ) {
			if ( argv[i] == null ) {
				// skip
			} else if ( argv[i].equals("winningVotes") ) {
				winningVotes = true;
				margins = false;
				argv[i] = null;
			} else if ( argv[i].equals("margins") ) {
				winningVotes = false;
				margins = true;
				argv[i] = null;
			}
		}
		return super.init( argv );
	}
	
	public NamedVRR() {
		counts.put( dummyname, new Count( dummyname, 0 ) );
	}
	
	/**
	 Record a vote.
	 Keeps only a summation of the votes, not individual vote data.
	*/
	public void voteRating( NameVote[] vote ) {
		Count[] cs = new Count[vote.length];
		// minimize hash table lookup, and force fork if any new names. need to fork before modifying any data.
		for ( int i = 0; i < vote.length; i++ ) {
			cs[i] = getCount( vote[i].name );
		}
		winners = null;
		for ( int i = 0; i < vote.length; i++ ) {
			Count a;
			a = cs[i];
			for ( int j = i + 1; j < vote.length; j++ ) {
				Count b;
				b = cs[j];
				if ( a.index == b.index ) {
					// ignore duplicate names on vote
					continue;
				}
				try {
				if ( vote[i].rating > vote[j].rating ) {
					if ( a.index > b.index ) {
						a.counts[b.index]++; // a wins
					} else {
						b.counts[b.index + a.index]++; // b looses
					}
				} else if ( vote[j].rating > vote[i].rating ) {
					if ( a.index > b.index ) {
						a.counts[a.index + b.index]++; // a looses
					} else {
						b.counts[a.index]++; // b wins
					}
				}
				} catch ( ArrayIndexOutOfBoundsException ae ) {
					System.out.println("<p>vote["+i+"].name="+vote[i].name+", rating="+vote[i].rating+", index="+a.index+"<br>vote["+j+"].name="+vote[j].name+", rating="+vote[j].rating+", index="+b.index+"</p>");
				}
			}
		}
		// All names not voted assumed to rate -0, lose to names with >= 0 ratings and beat < 0 ratings.
		for ( java.util.Iterator ci = counts.entrySet().iterator(); ci.hasNext(); ) {
			java.util.Map.Entry e = (java.util.Map.Entry)ci.next();
			String name = (String)e.getKey();
			Count d = (Count)e.getValue();
			boolean isvoted = false;
			for ( int i = 0; i < vote.length; i++ ) {
				if ( d == cs[i] || name.equals( vote[i].name ) ) {
					isvoted = true;
					break;
				}
			}
			if ( isvoted ) {
				continue;
			}
			// name wasn't voted on.
			for ( int i = 0; i < vote.length; i++ ) {
				Count a;
				a = cs[i];
				if ( vote[i].rating >= 0 ) {
					if ( a.index > d.index ) {
						a.counts[d.index]++; // a wins
					} else {
						d.counts[d.index + a.index]++; // d looses
					}
				} else if ( 0 > vote[i].rating ) {
					if ( a.index > d.index ) {
						a.counts[a.index + d.index]++; // a looses
					} else {
						d.counts[a.index]++; // d wins
					}
				}
			}
		}
	}
	
	protected static NameVote[] makeWinners( Count[] they, int[] defeatCount ) {
		NameVote[] winners = new NameVote[they.length];
		int i;
		for ( i = 0; i < they.length; i++ ) {
			winners[i] = new NameVote( they[i].name, (float)(defeatCount[i] * -1.0) );
		}
		sort( winners );
		return winners;
	}
	/**
		find Condorcet winner with CSSD cycle resolution
	 */
	public NameVote[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		Count[] they = getIndexedCounts( false );
		int numc = they.length;
		if ( numc == 0 ) {
			return new NameVote[0];
		}
		int[] tally = getTallyArray( they );
		defeatCount = new int[numc];

		Condorcet.countDefeats( numc, tally, defeatCount );
		for ( int i = 0; i < numc; i++ ) {
			if ( defeatCount[i] == 0 ) {
				// we have a winner
				winners = makeWinners( they, defeatCount );
				return winners;
			}
		}
		return getWinnersCSSD( they, tally );
	}
	
	/** Extract counts in order of name index from counts mapping. 
	 @see #counts
	 */
	protected Count[] getIndexedCounts( boolean ldebug ) {
		int numi = counts.size();
		if ( ! ldebug ) {
			numi--;
		}
		Count[] they = new Count[numi];
		java.util.Iterator ti = counts.values().iterator();
		int i = 0, j;
		while ( ti.hasNext() ) {
			Count tc;
			tc = (Count)ti.next();
			if ( (! ldebug) && tc.name.equals( dummyname ) ) {
				// skip
			} else {
				they[i] = tc;
				i++;
			}
		}
		// sort on index
		for ( i = 0; i < they.length; i++ ) {
			for ( j = i + 1; j < they.length; j++ ) {
				if ( they[i].index > they[j].index ) {
					Count tc = they[i];
					they[i] = they[j];
					they[j] = tc;
				}
			}
		}
		return they;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		Count[] they = getIndexedCounts( debug );
		if ( winners == null ) {
			getWinners();
			if ( winners == null ) {
				return sb;
			}
		}
		/*if ( debugLog != null ) {
			sb.append( "<pre>debug:\n" );
			sb.append( debugLog );
			sb.append( "</pre>" );
		}*/
		sb.append( "<table border=\"1\"><tr><td></td>" );
		for ( int i = 0; i < winners.length; i++ ) {
			sb.append( "<td>(" );
			sb.append( i );
			sb.append( ")</td>" );
		}
		sb.append( "</tr>" );
		int[] indextr = new int[winners.length];
		for ( int xi = 0; xi < winners.length; xi++ ) {
			int i;
			for ( i = 0; i < they.length; i++ ) {
				if ( winners[xi].name.equals( they[i].name ) ) {
					indextr[xi] = i;
				}
			}
		}
		for ( int xi = 0; xi < indextr.length; xi++ ) {
			int i;
			i = indextr[xi];
			sb.append( "<tr><td>(" );
			sb.append( xi );
			sb.append( ") " );
			sb.append( they[i].name );
			sb.append( "</td>" );
			for ( int xj = 0; xj < indextr.length; xj++ ) {
				int j;
				j = indextr[xj];
				if ( i == j ) {
					sb.append( "<td></td>" );
				} else {
					int thisw, otherw;
					if ( i > j ) {
						thisw = they[i].counts[j];
						otherw = they[i].counts[j + i];
					} else /*if ( i < j )*/ {
						thisw = they[j].counts[j + i];
						otherw = they[j].counts[i];
					}
					if ( thisw > otherw ) {
						sb.append("<td bgcolor=\"#bbffbb\">");
					} else if ( thisw < otherw ) {
						sb.append("<td bgcolor=\"#ffbbbb\">");
					} else {
						sb.append("<td>");
					}				
					sb.append( thisw );
					sb.append( "</td>" );
				}
			}
			sb.append( "</tr>" );
		}
		sb.append( "</table>" );
		sb.append( "<table border=\"1\">" );
		for ( int i = 0; i < winners.length; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( winners[i].name );
			sb.append( "</td><td>" );
			sb.append( (int)(winners[i].rating) );
			sb.append( "</tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Virtual Round Robin";
	}

	/**
	 Holds a portion of the VRR table.
	 Holds beats and defeats for this.name vs all others voted on before it.
	 */
	protected static class Count {
		String name;
		int index;
/**
counts laid out in concentric rings.
x, y and z indicate higher indexed counts.
A 0 x y z
B x 0 y z
C y y 0 z
D z z z 0
*/
		public int[] counts;
		Count( String nin, int ix ) {
			name = nin;
			index = ix;
			counts = new int[index * 2];
		}
	};
	protected Count getCount( String name ) {
		Count c = (Count)counts.get( name );
		if ( c == null ) {
			Count d;
			d = (Count)counts.get( dummyname );
			c = new Count( name, counts.size() );
			for ( int j = 0; j < d.index; j++ ) {
				c.counts[j] = d.counts[j];
				c.counts[j + c.index] = d.counts[j + d.index];
			}
			// swap index and counts so dummy is always outtermost
			int ti = c.index;
			c.index = d.index;
			d.index = ti;
			int[] tc = c.counts;
			c.counts = d.counts;
			d.counts = tc;
			counts.put( c.name, c );
		}
		return c;
	}
	
	/** build a tally array like Condorcet class uses, easier to use that code with */
	protected static int[] getTallyArray( Count[] they ) {
		int numc = they.length;
		int[] tally = new int[numc*numc];
		for ( int i = 0; i < they.length; i++ ) {
			for ( int j = 0; j < they.length; j++ ) {
				if ( i == j ) {
					tally[i*numc + j] = 0;
				} else if ( i > j ) {
					tally[i*numc + j] = they[i].counts[j];
				} else /*if ( i < j )*/ {
					tally[i*numc + j] = they[j].counts[j + i];
				}
			}
		}
		return tally;
	}
	
	/** Break cycles based on which defeat A>B in the cycle has the fewest votes for A>B.
	 One of winningVotes and margins should be true.
	 @see #margins
	 */
	public boolean winningVotes = true;
	/** Break cycles based on which defeat A>B in the cycle has smallest difference between votes for A>B and votes for B>A.
	 One of winningVotes and margins should be true.
	 @see #winningVotes
	 */
	public boolean margins = false;

	/** Cycle resolution. */
	public NameVote[] getWinnersCSSD( Count[] they, int[] tally ) {
		// cloneproof schwartz set dropping
		// which ought to be the same as above "beatpath" method, but a new implementation
		int j,k;
		int numc = they.length;
		int[] ss = Condorcet.getSchwartzSet( numc, tally, defeatCount, debugLog );
		int mindj, mindk, mind; // minimum defeat, index and strength
		
		boolean notdone = true;
		int tie = 0;
		
		while ( notdone ) {
			notdone = false;
			mind = Integer.MAX_VALUE;
			mindj = -1;
			mindk = -1;
			tie = 0;
			// find weakest defeat between members of schwartz set
			for ( int ji = 0; ji < ss.length - 1; ji++ ) {
				j = ss[ji];
				for ( int ki = ji + 1; ki < ss.length; ki++ ) {
					k = ss[ki];
					int vj, vk;
					vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
					vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
					if ( vj > vk ) {
						if ( winningVotes ) {
							if ( vj < mind ) {
								mind = vj;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( vj == mind ) {
								tie++;
							}
						} else if ( margins ) {
							int m = vj - vk;
							if (m < mind) {
								mind = m;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( m == mind ) {
								tie++;
							}
						}
					} else if ( vk > vj ) {
						if ( winningVotes ) {
							if ( vk < mind ) {
								mind = vk;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( vk == mind ) {
								tie++;
							}
						} else if ( margins ) {
							int m = vk - vj;
							if (m < mind) {
								mind = m;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( m == mind ) {
								tie++;
							}
						}
					}
				}
			}
			if ( tie == 0 ) {
				if ( debugLog != null ) {
					debugLog.append("tie = 0, no weakest defeat found to cancel\n");
				}
				return winners = makeWinners( they, defeatCount );
			}
			// all are tied
			if ( tie == ss.length) {
				if ( debugLog != null ) {
					debugLog.append("tie==ss.length, mind=").append(mind).append(", mindj=").append(mindj).append(", mindk=").append(mindk).append('\n');
				}
				return winners = makeWinners( they, defeatCount );
			}
			tally[mindk*numc + mindj] = 0;
			tally[mindj*numc + mindk] = 0;
			if ( debugLog != null ) {
				debugLog.append( mindk ).append( '/' ).append( mindj ).append(" = 0\n");
				//htmlTable( debugLog, numc, tally, "intermediate", null );
			}
			ss = Condorcet.getSchwartzSet( numc, tally, defeatCount, debugLog );
			if ( ss.length == 1 ) {
				return winners = makeWinners( they, defeatCount );
			}
			if ( debugLog != null ) {
				debugLog.append("ss={ ");
				debugLog.append( ss[0] );
				for ( j = 1; j < ss.length; j++ ) {
					debugLog.append(", ");
					debugLog.append( ss[j] );
				}
				debugLog.append(" }\n");
			}
			notdone = true;
		}
		return winners = makeWinners( they, defeatCount );
	}
	
	static {
		registerImpl( "VRR", NamedVRR.class );
	}
};
