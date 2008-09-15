package org.bolson.vote;

import java.util.Arrays;
import java.util.ArrayList;
import java.io.StreamTokenizer;
import java.io.IOException;

/**
A proxy is an integer triplet: ( voter uid, proxy uid, priority )
 @author Brian Olson
 */
public class InstantProxy {
	public static int proxyIndex( Card cur, int[] uids ) {
		int[] px = cur.proxies;
		for ( int i = 0; i < px.length; i++ ) {
			int x;
			x = Arrays.binarySearch( uids, px[i] );
			if ( x >= 0 ) {
				return x;
			}
		}
		return -1;
	}
	
	/** recursive version, for breadth first search, stops when none active */
	public static int proxyIndex( Card cur, int[] uids, Card[] proxies, boolean[] active ) {
		int[] px = cur.proxies;
		for ( int i = 0; i < px.length; i++ ) {
			int x;
			x = Arrays.binarySearch( uids, px[i] );
			if ( x >= 0 ) {
				return x;
			}
		}
		return -1;
	}
	/**
	Determine how the votes cast get multiplied by receiving proxy votes.
	 @param proxies proxies to process. must be sorted on uid.
	 @param uids user ids of those who cast votes. Must be sorted to allow binary search.
	 */
	public static int[] getInstantProxyMultipliers( Card[] proxies, int[] uids ) {
		int[] toret = new int[uids.length];
		boolean[] active = null;
		for ( int i = 0; i < proxies.length; i++ ) {
			int x;
			Card cur;
			cur = proxies[i];
			x = Arrays.binarySearch( uids, cur.uid );
			if ( x < 0 ) {
				// didn't vote
				x = proxyIndex( cur, uids );
				if ( x < 0 ) {
					// no 1st level proxies voted, recurse, or discard
					// fixme, write recursion
					if ( active == null ) {
						active = new boolean[uids.length];
					}
					for ( int j = 0; j < active.length; j++ ) {
						active[j] = true;
					}
				}
				if ( x < 0 ) {
					// no proxies voted, discard
				} else {
					toret[x]++;
				}
			} else {
				// voted, they get 1 multiplier for self
				toret[x]++;
			}
		}
		return toret;
	}
	
	public static String explainVote() {
		return null;
	}
	
	public static int[] readIntFile( java.io.Reader r ) {
		StreamTokenizer st = new StreamTokenizer( r );
		st.parseNumbers();
		st.whitespaceChars(0,32);
		int[] ti = new int[16];
		int til = 0;
		int t;
		try {
			while ( (t = st.nextToken()) != StreamTokenizer.TT_EOF ) {
				if ( t == StreamTokenizer.TT_NUMBER ) {
					if ( til == ti.length ) {
						int[] nti = new int[ti.length*2];
						System.arraycopy( ti, 0, nti, 0, ti.length );
						ti = nti;
					}
					ti[til] = (int)(st.nval);
					til++;
				} else {
					System.err.println("bogus token type " + t );
				}
			}
		} catch ( IOException e ) {
			System.err.println("read " + til + " number entries before exception:" );
			e.printStackTrace();
		}
		if ( til == ti.length ) {
			return ti;
		}
		int[] toret = new int[til];
		System.arraycopy( ti, 0, toret, 0, til );
		return toret;
	}
	
	public static void main( String[] argv ) {
		String proxiesInName = argv[0];
		String voterUidsName = argv[1];
		try {
			System.out.println("reading " + proxiesInName );
			Card[] ipc = Card.readProxies( new java.io.FileReader( proxiesInName ) );
			System.out.println("read " + ipc.length );
			Arrays.sort( ipc );
			for ( int i = 0; i < ipc.length; i++ ) {
				System.out.println( ipc[i].toString() );
			}
			int[] vids = readIntFile( new java.io.FileReader( voterUidsName ));
			System.out.print("voted: " + vids[0]);
			for ( int i = 1; i < vids.length; i++ ) {
				System.out.print(", " + vids[i]);
			}
			System.out.println();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	public static class Card implements Comparable {
		/** this voter's user id */
		public int uid;
		/** proxies sorted by priority, vote goes to first which voted */
		public int[] proxies;
		
		public int compareTo( Object o ) {
			if ( o instanceof Card ) {
				Card c = (Card)o;
				return c.uid - uid;
			}
			if ( o instanceof Integer ) {
				return ((Integer)o).intValue() - uid;
			}
			throw new ClassCastException();
		}
		
		public Card( int uidIn, int[] proxiesIn ) {
			uid = uidIn;
			proxies = proxiesIn;
		}
		
		public String toString() {
			StringBuffer toret = new StringBuffer();
			toret.append( uid );
			if ( proxies == null || proxies.length == 0 ) {
				toret.append( " has no delegates" );
				return toret.toString();
			}
			toret.append( "'s delegates: " );
			toret.append( proxies[0] );
			for ( int i = 1; i < proxies.length; i++ ) {
				toret.append( ", " );
				toret.append( proxies[i] );
			}
			return toret.toString();
		}
		
		/** Read proxy declarations from a text file.
			Each line should be contain a list of decimal integers.
			The first number is the uid of the voter for which the rest of the numbers are prioritized uid numbers of proxy recipients.
			e.g.<br>
			<my uid> <my first proxy> <my second proxy> <my third proxy>
			*/
		public static Card[] readProxies( java.io.Reader inr ) {
			StreamTokenizer st = new StreamTokenizer( inr );
			st.parseNumbers();
			st.eolIsSignificant(true);
			st.whitespaceChars(0,32);
			ArrayList cards = new ArrayList();
			int[] ti = new int[16];
			int til = 0;
			int t;
			try {
				while ( (t = st.nextToken()) != StreamTokenizer.TT_EOF ) {
					//System.out.println( st.toString() );
					switch ( t ) {
						case StreamTokenizer.TT_EOL:
							if ( til > 1 ) {
								int[] pout = new int[til-1];
								System.arraycopy( ti, 1, pout, 0, til-1 );
								cards.add( new Card( ti[0], pout ));
							} else if ( til == 1 ) {
								cards.add( new Card( ti[0], new int[0] ));
							}
							til = 0;
							break;
						case StreamTokenizer.TT_NUMBER:
							if ( til == ti.length ) {
								int[] nti = new int[ti.length*2];
								System.arraycopy( ti, 0, nti, 0, ti.length );
								ti = nti;
							}
							ti[til] = (int)(st.nval);
							til++;
							break;
						default:
							System.err.println("bogus token type " + t );
							break;
					}
				}
			} catch ( IOException e ) {
				System.err.println("got " + cards.size() + " proxy entries before exception:" );
				e.printStackTrace();
			}
			Card[] toret = new Card[cards.size()];
			for ( int i = 0; i < toret.length; i++ ) {
				toret[i] = (Card)cards.get(i);
			}
			return toret;
		}
	}
}
