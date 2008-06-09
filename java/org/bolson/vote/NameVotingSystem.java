package org.bolson.vote;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;

/**
Taking a bunch of (name,rating) pairs this type of voting system allows for write-ins and perhaps a somewhat simpler usage.
 @author Brian Olson
*/
public abstract class NameVotingSystem {
	/** If true, save or spew extra info while running.
		Defaults to false, can be enabled by a "debug" option to init. */
	protected boolean debug = false;
	
	/** If debug, then log stuff here. */
	protected StringBuffer debugLog = null;
	
	/** If debug, return debugLog.toString(), else null. */
	public String getDebug() {
		if ( debug && debugLog != null ) {
			return debugLog.toString();
		} else {
			return null;
		}
	}

	/**
	Set options for voting system.
	Default implementation sets debug to true if it sees "debug".
	Skip past null entries, which may have been yanked by a subclass.
	Unless there's an error, subclass's last line should be "return super.init( argv );"
	Multi-seat capable implementations should accept an option pair  ("seats", &lt;int&gt;) to set the number of seats.
	@param argv array of options, just like main()
	@return 0 on success
	*/
	public int init( String[] argv ) {
		if ( argv == null ) {
			return 0;
		}
		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i] == null ) {
				// skip
			} else if ( argv[i].equals("debug") ) {
				debug = true;
				debugLog = new StringBuffer("debug enabled\n");
				argv[i] = null;
			}
		}
		if ( debug ) {
			// warn on unconsumed options
			for ( int i = 0; i < argv.length; i++ ) {
				if ( argv[i] != null ) {
					debugLog.append( "warning: unconsumed option argv[");
					debugLog.append( i );
					debugLog.append( "] = \"" );
					debugLog.append( argv[i] );
					debugLog.append( "\"\n" );
				}
			}
		}
		return 0;
	}
	
	/**
	Vote an ordering specified by a string.
	Names are separated by '>' or '='.
	Name at the left of the list are preferred to names further right in the less when separated by '>'
	and considered equivalend when separated by '='.
	White space surrounding the separator is discarded. White space internal to a name is preserved.
	"Ralph Nader > Al Gore > George Bush" might translate to 
	{ ("Ralph Nader",2.0), ("Al Gore",1.0), ("George Bush",0.0) }
	and then be passed to VoteRating(NameVote[])
	@see #voteRating(NameVote[])
	@param vote A String of names separated by '>' or '='.
	*/
	public void voteOrderSpec( String vote ) {
		voteRating( voteSpecToNameVoteArray( vote ) );
	}

	/**
	Vote a set of ratings.
	Keys of the map should be strings (choice names) and values should be a Number or a Float parseable string.
	The default implementation unpacks the map into a NameVote array and calls voteRating( NameVote[] )
	@see #voteRating(NameVote[])
	@param vote a set of (name,rating) pairs
	*/
	public void voteRating( Map vote ) {
		Set s = vote.entrySet();
		Object[] es = s.toArray();
		if ( es.length <= 0 ) {
			// throw an exception?
			return;
		}
		NameVote[] nv = new NameVote[es.length];
		for ( int i = 0; i < es.length; i++ ) {
			Map.Entry en;
			en = (Map.Entry)es[i];
			String name = (String)en.getKey();
			float rating;
			Object ratingO = en.getValue();
			if ( ratingO instanceof Number ) {
				rating = ((Number)ratingO).floatValue();
			} else if ( ratingO instanceof String ) {
				rating = Float.parseFloat((String)ratingO);
			} else {
				throw new ClassCastException("don't know how to handle rating of type: " + ratingO.getClass().getName());
			}
			nv[i] = new NameVote( name, rating );
		}
		es = null;
		s = null;
		voteRating( nv );
	}
	
	/**
	Vote a set of ratings.
	Keys of the map should be strings (choice names) and values should be of a numeric type.
	May keep a reference to the passed in vote (don't modify it after passing it in).
	This is faster than the version that takes a map because no conversion is done.
	@see #voteRating(Map)
	@param vote a set of (name,rating) pairs
	*/
	public abstract void voteRating( NameVote[] vote );
	
	/**
	@return A sorted array of (name,rating) pairs.
	 This is the total ordering of all choices, not just the elected winner(s).
	 'Total ordering' may be imprecise below top few choices.
	 For example, VRR/Cordorcet need not run tie-breaker methods on groups of tied choices that don't tie with the elected choice.
	*/
	public abstract NameVote[] getWinners();

	/**
	Get HTML summary of voting system state.
	@param sb a valid StringBuffer to which HTML summary will be appended.
	@return same StringBuffer passed in, with HTML summary of voting system state appended.
	*/
	public abstract StringBuffer htmlSummary( StringBuffer sb );
	
	/**
	@return name of this voting system (may be modified by settings from init)
	*/
	public abstract String name();
	
	/**
	Get HTML summary of voting system state.
	Returns toString() of a new StringBuffer passed to htmlSummary(StringBuffer)
	@see #htmlSummary(StringBuffer)
	@return HTML summary of voting system state.
	*/
	public String htmlSummary() {
		return (htmlSummary( new StringBuffer() )).toString();
	}

	/**
	Get HTML explaination of how the election worked.
	Typically show intermediate rounds or other counting state progress.
	Default implementation calls {@link #htmlSummary(StringBuffer)}.
	Can be optionally overrided for extra educational benefit.
	It may be really slow.
	It may be a complete re-implementation of the election method that emits stuff as it goes.
	@param sb a valid StringBuffer to which HTML explaination will be appended.
	@return same StringBuffer passed in, with HTML explaination of voting system process appended.
	*/
	public StringBuffer htmlExplain( StringBuffer sb ) {
		return htmlSummary( sb );
	}
	
	/**
	Get HTML explaination of how the election worked.
	Typically show intermediate rounds or other counting state progress.
	Default implementation calls {@link #htmlExplain(StringBuffer)}.
	@see #htmlExplain(StringBuffer)
	@return HTML explaination of voting system decision process.
	*/
	public String htmlExplain() {
		return (htmlExplain( new StringBuffer() )).toString();
	}
	
	public static class NameVote implements Comparable {
		public String name;
		public float rating;
		public NameVote( String nin, float rin ) {
			name = nin;
			rating = rin;
		}
		public int compareTo( Object o ) throws ClassCastException {
			NameVote b = (NameVote)o;
			if ( rating < b.rating ) {
				return 1;
			} else if ( rating > b.rating ) {
				return -1;
			}
			return name.compareTo( b.name );
		}
		public boolean equals( Object o ) {
			NameVote b = (NameVote)o;
			return rating == b.rating && ((name == null && b.name == null) || name.equals(b.name));
		}
	}
	public static class ReverseComparator implements java.util.Comparator {
		public int compare( Object a, Object b ) throws ClassCastException {
			return ((Comparable)b).compareTo( a );
		}
	}
	
	/**
	Convert a list of names separated by '>' or '='.
	e.g. "Name One>Name Two=Name Three>Name Four"
	@param cd names
	@return equivalent NameVote array or null on error
	*/
	public static NameVote[] voteSpecToNameVoteArray( String cd ) {
		if ( cd == null || cd.length() == 0 ) {
			return null;
		}
		char[] vsa = cd.toCharArray();
		int numc = 1;
		int i;
		for ( i = 0; i < vsa.length; i++ ) {
			if ( vsa[i] == '=' || vsa[i] == '>' ) {
				numc++;
			}
		}
		NameVote[] toret = new NameVote[numc];
		int namestart = 0;
		int nameend;
		int rp;
		float rating = (float)(numc);
		float nextrating = rating - 1.0f;
		for ( i = 0; i < numc; i++ ) {
			char splitter = '\0';
			while ( (namestart < vsa.length) && Character.isWhitespace( vsa[namestart] ) ) {
				namestart++;
			}
			if ( namestart >= vsa.length ) {
				break;
			}
			rp = namestart + 1;
			while ( rp < vsa.length ) {
				if (vsa[rp] == '=') {
					splitter = '=';
					break;
				} else if (vsa[rp] == '>') {
					splitter = '>';
					break;
				}
				rp++;
			}
			nameend = rp - 1;
			while ( (rp > namestart) && Character.isWhitespace( vsa[nameend] ) ) {
				nameend--;
			}
			toret[i] = new NameVote( new String( vsa, namestart, nameend - namestart + 1 ), rating );
			namestart = rp + 1;
			if ( splitter == '>' ) {
				rating = nextrating;
			}
			nextrating -= 1.0f;
		}
		return toret;
	}
	
	/**
	Convert Name=Rating pairs in a string to an array of NameVote.
	@param cd String like "Name One=9	Name Two=3	Name Three=-9"
	@param splitPattern pattern to break cd on
	@param trimWhitespace remove excess whitespace at start and end of names
	@return NameVote array from cd
	*/
	public static NameVote[] nameEqStrToVoteArray( String cd, String splitPattern, boolean trimWhitespace ) {
		String[] nvpairs = cd.split( splitPattern );
		NameVote[] toret = new NameVote[nvpairs.length];
		for ( int v = 0; v < nvpairs.length; v++ ) {
			String[] ab;
			ab = nvpairs[v].split("=");
			if ( ab == null || ab.length == 0 ) {
				return null;
			}
			if ( trimWhitespace ) {
				ab[0] = ab[0].trim(); 
			}
			if ( ab.length == 1 || ab[1] == null || ab[1].length() == 0 ) {
				toret[v] = new NameVote( ab[0], Float.NaN );
			} else {
				toret[v] = new NameVote( ab[0], Float.parseFloat( ab[1] ) );
			}
		}
		return toret;
	}
	/**
	Convert Name=Rating pairs in a string to an array of NameVote.
	Calls nameEqStrToVoteArray( cd, "\t", true ), splitting on tab and triming other whitespace.
	@param cd String like "Name One=9	Name Two=3	Name Three=-9"
	@return NameVote array from cd
	*/
	public static NameVote[] nameEqStrToVoteArray( String cd ) {
		return nameEqStrToVoteArray( cd, "\t", true );
	}
	/** Vote a tab-separated name=rating string. */
	public void voteNameEqStr( String cd ) {
		voteRating( nameEqStrToVoteArray( cd ));
	}
	public static void sort( NameVote[] they ) {
		java.util.Arrays.sort( they );
	}
	
	/** Reads tab separated name=rating vote per line. */
	public void readVotes( java.io.Reader r ) throws java.io.IOException {
		java.io.BufferedReader br;
		if ( r instanceof java.io.BufferedReader ) {
			br = (java.io.BufferedReader)r;
		} else {
			br = new java.io.BufferedReader( r );
		}
		String line;
		while ( (line = br.readLine()) != null ) {
			voteRating( nameEqStrToVoteArray( line ));
		}
	}
	
	/** Use this for making simple subclass main() to run a vote of that type.
		(new Sub()).defaultMain( argv[] );
	*/
	public void defaultMain( String argv[] ) {
		try {
			init( argv );
			readVotes( new java.io.InputStreamReader( System.in ) );
			java.io.PrintWriter out = new java.io.PrintWriter( new java.io.OutputStreamWriter( System.out ));
			out.print("<html><head><title>");
			out.print(name());
			out.print(" results</title></head><body><h1>");
			out.print(name());
			out.print(" results</h1>");
			out.flush();
			out.print(htmlSummary());
			out.print("</body></html>\n");
			out.flush();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	It's just like return s.replaceAll( "%", "%25" ).replaceAll( "&", "%26" ).replaceAll( "=", "%3d" ); but faster.
	@param s string to %hex escape
	@return string with no '&' or '=' chars, them having been %hex escaped
	*/
	public static String percentHexify( String s ) {
		//return s.replaceAll( "%", "%25" ).replaceAll( "&", "%26" ).replaceAll( "=", "%3d" );
		char[] src = s.toCharArray();
		int growth = 0;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			if ( src[i] == '%' ) {
				growth += 2;
			} else if ( src[i] == '&' ) {
				growth += 2;
			} else if ( src[i] == '=' ) {
				growth += 2;
			} 
		}
		if ( growth == 0 ) {
			return s;
		}
		char[] toret = new char[src.length+growth];
		int outpos = 0;
		for ( i = 0; i < src.length; i++ ) {
			if ( src[i] == '%' ) {
				toret[outpos] = '%'; outpos++;
				toret[outpos] = '2'; outpos++;
				toret[outpos] = '5';
			} else if ( src[i] == '&' ) {
				toret[outpos] = '%'; outpos++;
				toret[outpos] = '2'; outpos++;
				toret[outpos] = '6';
			} else if ( src[i] == '=' ) {
				toret[outpos] = '%'; outpos++;
				toret[outpos] = '3'; outpos++;
				toret[outpos] = 'd';
			} else {
				toret[outpos] = src[i];
			}
			outpos++;
		}
		return new String( toret );
	}

	public static int denibble( char c ) throws NumberFormatException {
		if ( c >= '0' && c <= '9' ) {
			return c - '0';
		} else if ( c >= 'a' && c <= 'f' ) {
			return c - 'a' + 0xA;
		} else if ( c >= 'A' && c <= 'F' ) {
			return c - 'A' + 0xA;
		} else {
			throw new NumberFormatException( "char '" + c + "' is not a hex char" );
		}
		//return -1;
	}
	/**
	un-escape ASCII string with %xx escapes in it to, converting such sequences to bytes.
	@param in string which may have %xx escapes in it
	@return unescaped string
	*/
	public static String depercentHexify( String in ) {
		char[] a = in.toCharArray();
		int torep = 0;
		for ( int i = 0; i < a.length; i++ ) {
			if ( a[i] == '%' ) {
				torep++;
			}
		}
		if ( torep == 0 ) {
			return in;
		}
		char[] ar = new char[a.length - (torep * 2)];
		int j = 0;
		for ( int i = 0; i < a.length; i++ ) {
			if ( a[i] == '%' ) {
				int c = 0;
				i++;
				c = denibble( a[i] ) << 4;
				i++;
				c |= denibble( a[i] );
				ar[j] = (char)c;
			} else {
				ar[j] = a[i];
			}
			j++;
		}
		return new String( ar );
	}
	/** Parse a name=value&amp;name=value url query type string into a NameVote[]. 
		@param s the url query type string with candidate names and ratings
		@return an array of NameVote with candidate names and ratings */
	public static NameVote[] fromUrlEncoded( String s ) {
		/* It's important to _not_ use java.util.regex split or match here
		because they are slow and String.indexOf and String.substring are all
		that is needed and work out to be much much faster. */
		if ( s.length() == 0 ) {
			return new NameVotingSystem.NameVote[0];
		}
		int numVotes = 1;
		int amp = s.indexOf( '&' );
		while ( amp != -1 ) {
			numVotes++;
			amp = s.indexOf( '&', amp + 1 );
		}
		NameVote[] toret = new NameVotingSystem.NameVote[numVotes];
		int pos = 0;
		amp = s.indexOf( '&' );
		int eq;
		for ( int i = 0; i < toret.length; i++ ) {
			eq = s.indexOf( '=', pos );
			String name;
			if ( ((amp != -1) && (eq > amp)) || eq == -1 ) {
				toret[i] = new NameVotingSystem.NameVote( "", Float.NaN );
			} else {
				float value;
				String prename;
				prename = s.substring( pos, eq );
				name = depercentHexify( prename );
				if ( eq + 1 == amp ) {
					value = Float.NaN;
				} else {
					String vstr;
					if ( amp == -1 ) {
						vstr = s.substring( eq + 1 );
					} else {
						vstr = s.substring( eq + 1, amp );
					}
					value = Float.parseFloat( vstr );
				}
				toret[i] = new NameVotingSystem.NameVote( name, value );
			}
			pos = amp + 1;
			amp = s.indexOf( '&', pos );
		}
		return toret;
	}
	/**
	url encode a vote
	@param they vote to encode
	@return "name=rating&name=rating"...
	*/
	public static String urlEncode( NameVote[] they ) {
		if ( they == null ) {
			return null;
		}
		if ( they.length == 0 ) {
			return "";
		}
		StringBuffer toret = new StringBuffer( percentHexify( they[0].name ) );
		toret.append( "=" );
		toret.append( they[0].rating );
		for ( int i = 1; i < they.length; i++ ) {
			toret.append( '&' );
			toret.append( percentHexify( they[i].name ) );
			toret.append( "=" );
			toret.append( they[i].rating );
		}
		return toret.toString();
	}
	/**
	Print summary table.
	prints a table just containing each VotingSystem name and its winner.
	@param out receives the html table
	@param vs systems that have been voted and can getWinners
	@param debug sets if getWinners is to collect debug info
	@param numSeats number of seats available in this election
	*/
	public static void winnerSummaryHTMLTable( java.io.PrintWriter out, NameVotingSystem[] vs, boolean debug, int numSeats ) {
		if ( true ) {
			out.print("<TABLE BORDER=\"1\"><TR><TH>System</TH><TH>Winner(s)</TH></TR>");
		} else {
			out.print("<TABLE BORDER=\"1\"><TR><TH valign=\"top\" rowspan=\"");
			out.print( vs.length + 1 );
			out.println("\"><font size=\"+3\">Results Summary</fonts></TH><TH>System</TH><TH>Winner(s)</TH></TR>");
		}
		for ( int v = 0; v < vs.length; v++ ) {
			NameVote[] wi;
			//vs[v].debug = debug;
			wi = vs[v].getWinners();
			if ( wi != null && wi.length > 0 ) {
				out.print("<TR><TD>");
				out.print( vs[v].name() );
				out.print("</TD><TD>");
				if ( wi[0] == null ) {
					out.print("null NameVote winner");
				} else {
					out.print( wi[0].name );
					for ( int w = 1; (w < wi.length) && (wi[w] != null) && (wi[w].rating == wi[0].rating); w++ ) {
						out.print( ", " );
						if ( wi[0] == null ) {
							out.print("null NameVote winner");
						} else {
							out.print( wi[w].name );
						}
					}
				}
				out.println("</TD></TR>");
			}
		}
		out.println("</TABLE>");
	}
	
	/**
	Print full results.
	Prints each VotingSystem's html summary.
	@param out receives the html results
	@param vs systems that have been voted and can getWinners
	@param debug sets if getWinners is to collect debug info
	@param numSeats number of seats available in this election
	*/
	public static void resultsHTMLDisplay( java.io.PrintWriter out, NameVotingSystem[] vs, boolean debug, int numSeats ) {
		for ( int v = 0; v < vs.length; v++ ) {
			NameVote[] wi;
			wi = vs[v].getWinners();
			out.print("<H2>");
			out.print( vs[v].name() );
			out.println("</H2>");
			out.flush();
			out.println( vs[v].htmlSummary() );
			if ( wi == null || wi.length <= 0 || wi[0] == null ) {
			} else {
				out.println("<P>");
				if ( (wi.length == 1) || (wi[0].rating != wi[1].rating) ) {
					out.print("Winner: ");
					out.println( wi[0].name );
				} else {
					out.print("Winners: ");
					out.print( wi[0].name );
					for ( int w = 1; w < wi.length && wi[w].rating == wi[0].rating; w++ ) {
						out.print( ", " );
						out.print( wi[w].name );
					}
				}
				out.println("</P>");
			}
			out.flush();
		}
	}
	
	/* HashMap<String,Class> mapping short election method implementation names to their Class objects. */
	static private final HashMap implRegistry = new HashMap();
	
	/**
	 Register an implementatino of NameVotingSystem so that it can be found from getImplForName and getImplNames.
	 Use a short name with no spaces suitable for being a command-line argument.
	 @param name short name for this implementation
	 @param c MyVotingSystem.class
	 */
	protected static void registerImpl( String name, Class c ) {
		implRegistry.put( name, c );
	}
	public static Class getIpmlForName( String name ) {
		return (Class)implRegistry.get( name );
	}
	/**
	 @return Iterator of String short names for implemented and registered election methods.
	 */
	public static Iterator getImplNames() {
		return implRegistry.keySet().iterator();
	}
};
