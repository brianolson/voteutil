package org.bolson.vote.staticballot;
import java.io.*;
import java.util.Enumeration;
/**
 * Command line app for processing votes.
 @author Brian Olson
 @deprecated {@link org.bolson.vote.countvotes}
 @see org.bolson.vote.countvotes
 */
public class vote {
    boolean rating = true;
    boolean clist = false;
    boolean isFloat = false;
    boolean isBin = false;
    boolean scriptMode = false;
    boolean htmlOut = false;
    boolean printState = false;
    boolean quiet = false;
    boolean printVotes = false;
    boolean validateVotes = false;
    boolean historgam = false;
	boolean verbose = false;
    Class fac = null;
	String[] facArgv = new String[0];
    int numc = -1;
    boolean histogram = false;
    int histBuckets = -1;
    String names[] = null;
    int histyle = 2;
	VotingSystemFactory[] ftab = VotingSystemFactory.standardVotingSystemList;
    VotingSystem[] tab = null;
    Histogram hist = null;
    int votes = 0;
	
	static abstract class VoteSource {
		abstract int[] getIntVote( int[] pi ) throws java.io.IOException;
		abstract float[] getFloatVote( float[] pf ) throws java.io.IOException;
		abstract double[] getDoubleVote( double[] pf ) throws java.io.IOException;
		public int numc;
	}
	static class BinVoteSource extends VoteSource {
		public DataInputStream dis = null;
		public BinVoteSource( DataInputStream din ) {
			dis = din;
		}
		int[] getIntVote( int[] pi ) throws java.io.IOException {
			for ( int i = 0; i < numc; i++ ) {
				pi[i] = dis.readInt();
			}
			return pi;
		}
		public float[] getFloatVote( float[] pf ) throws java.io.IOException {
			for ( int i = 0; i < numc; i++ ) {
				pf[i] = dis.readFloat();
			}
			return pf;
		}
		public double[] getDoubleVote( double[] pf ) throws java.io.IOException {
			for ( int i = 0; i < numc; i++ ) {
				pf[i] = dis.readDouble();
			}
			return pf;
		}
	}
	static class TextVoteSource extends VoteSource {
		public StreamTokenizer st = null;
		public TextVoteSource( StreamTokenizer stin ) {
			st = stin;
		}
		public int[] getIntVote( int[] pf ) throws java.io.IOException {
			int tt;
			for ( int i = 0; i < numc; i++ ) {
				tt = st.nextToken();
				if ( tt == StreamTokenizer.TT_EOF ) {
					if ( i != 0 ) {
						System.err.println("\twrong number of votes on line at end of file "+st.lineno()+". eol at i="+i+" wanted "+numc );
						return null;
					}
				} else if ( tt == StreamTokenizer.TT_EOL ) {
					if ( i != 0 ) {
						System.err.println("\twrong number of votes on line "+st.lineno()+". eol at i="+i+" wanted "+numc );
						return null;
					} else {
						tt = st.nextToken();
					}
				} else if ( tt != StreamTokenizer.TT_NUMBER ) {
					System.err.println("token " + tt + " unexpected, wanted number ("+StreamTokenizer.TT_NUMBER+")" );
					return null;
				}
				pf[i] = (int)st.nval;
			}
			return pf;
		}
		public float[] getFloatVote( float[] pf ) throws java.io.IOException {
			int tt;
			for ( int i = 0; i < numc; i++ ) {
				tt = st.nextToken();
				if ( tt == StreamTokenizer.TT_EOL ) {
					if ( i != 0 ) {
						System.err.println("\twrong number of votes on line "+st.lineno()+". eol at i="+i+" wanted "+numc );
						return null;
					} else {
						tt = st.nextToken();
					}
				}
				if ( tt != StreamTokenizer.TT_NUMBER ) {
					return null;
				}
				pf[i] = (float)st.nval;
			}
			return pf;
		}
		public double[] getDoubleVote( double[] pf ) throws java.io.IOException {
			int tt;
			for ( int i = 0; i < numc; i++ ) {
				tt = st.nextToken();
				if ( tt == StreamTokenizer.TT_EOL ) {
					if ( i != 0 ) {
						System.err.println("\twrong number of votes on line "+st.lineno()+". eol at i="+i+" wanted "+numc );
						return null;
					} else {
						tt = st.nextToken();
					}
				}
				if ( tt != StreamTokenizer.TT_NUMBER ) {
					return null;
				}
				pf[i] = st.nval;
			}
			return pf;
		}
	}
	/** utility to StringToNumberArray and CListToNumberArray */
	public static String stripNonDig( String s ) {
		int b = -1, e = s.length(), i;
		for ( i = 0; i < e; i++ ) {
			char c;
			c = s.charAt(i);
			if ( (b == -1) && (Character.isDigit( c ) || (c == '-') || (c == '.')) ) {
				b = i;
			} else if ( (b != -1) && !((c == '.') || Character.isDigit( c )) ) {
				e = i;
			}
		}
		if ( (b == 0) && (e == s.length()) ) {
			return s;
		}
		return s.substring( b, e );
	}
	public static Object StringToNumberArray( String votes, int len ) {
		String[] vp = votes.split(",");
		if ( len == -1 ) {
			len = vp.length;
		} else if ( vp.length != len ) {
			System.err.println("wanted " + len + " parts but got " + vp.length + ", bad vote: \"" + votes + "\"" );
			return null;
		}
		if ( votes.indexOf( '.' ) != -1 ) {
			// there are decimal points, assume double
			double[] v;
			v = new double[len];
			for ( int vi = 0; vi < v.length; vi++ ) {
				v[vi] = Double.parseDouble( stripNonDig( vp[vi] ) );
			}
			return v;
		} else {
			// no decimals, parse ints
			int[] v;
			v = new int[len];
			for ( int vi = 0; vi < v.length; vi++ ) {
				v[vi] = Integer.parseInt( stripNonDig( vp[vi] ) );
			}
			return v;
		}
	}
	public static Object CListToNumberArray( String cd, int numc, java.util.Hashtable namehash ) throws Exception {
		int[] ranks = new int[numc];
		int cdlen;
		int rank, nextrank;
		int pp = 0, sp;
		cdlen = cd.length();
		rank = 1;
		nextrank = 2;
		for ( int ci = 0; ci < ranks.length; ci++ ) {
			ranks[ci] = VotingSystem.NO_VOTE;
		}
		for ( int ci = 0; ci < ranks.length; ci++ ) {
			int ep;
			String ciname;
			int nextpp;
			boolean iseq;
			while ( (pp < cdlen) && Character.isWhitespace( cd.charAt( pp ) ) ) {
				pp++;
			}
			if ( pp >= cdlen ) {
				break;
			}
			ep = cd.indexOf( '=', pp );
			sp = cd.indexOf( '>', pp );
			iseq = false;
			if ( ep == -1 ) {
				if ( sp == -1 ) {
					sp = cdlen;
				} else {
					// sp = sp
				}
			} else {
				if ( sp == -1 ) {
					sp = ep;
					iseq = true;
				} else if ( ep < sp ) {
					sp = ep;
					iseq = true;
				} // else { sp = sp; }
			}
			nextpp = sp + 1;
			while ( (sp > pp) && Character.isWhitespace( cd.charAt( sp - 1 ) ) ) {
				sp--;
			}
			if ( pp < sp ) {
				ciname = cd.substring( pp, sp );
				Object o;
				int r;
				o = namehash.get( ciname );
				if ( o == null ) {
					if ( namehash.size() >= numc ) {
						throw new Exception("name \""+ciname+"\" is a new name byeond the "+numc+" declared candidates");
					} else {
						r = namehash.size();
						namehash.put( ciname, new Integer( r ) );
					}
				} else {
					r = ((Integer)o).intValue();
				}
				ranks[r] = rank;
				if ( ! iseq ) {
					rank = nextrank;
				}
				nextrank++;
			}
			pp = nextpp;
			/*out.print('"');
			out.print(clp[ci]);
			out.print("\", ");*/
		}
		return ranks;
	}
	public static String[] namehashToNameArray( java.util.Hashtable namehash ) {
		return namehashToNameArray( namehash, namehash.size() );
	}
	public static String[] namehashToNameArray( java.util.Hashtable namehash, int numc ) {
		String[] cnames = new String[numc];
		for ( Enumeration namee = namehash.keys(); namee.hasMoreElements(); ) {
			String nei;
			nei = (String)namee.nextElement();
			Integer neir = (Integer)namehash.get( nei );
			cnames[neir.intValue()] = nei;
		}
		return cnames;
	}
	/** For a text file formatted "a>b>c" */
	static class CandidateListVoteSource extends VoteSource {
		public BufferedReader r = null;
		public java.util.Hashtable namehash = new java.util.Hashtable();
		public CandidateListVoteSource( Reader din ) {
			if ( r instanceof BufferedReader ) {
				r = (BufferedReader)din;
			} else {
				r = new BufferedReader( din );
			}
		}
		int[] getIntVote( int[] pi ) throws java.io.IOException {
			String line = r.readLine();
			if ( line != null ) {
				try {
					return (int[])CListToNumberArray( line, numc, namehash );
				} catch ( Exception e ) {
					throw new java.io.IOException( e.toString() );
				}
			}
			return null;
		}
		public float[] getFloatVote( float[] pf ) throws java.io.IOException {
			return null;
		}
		public double[] getDoubleVote( double[] pf ) throws java.io.IOException {
			return null;
		}
	}
    
	public void initHistogram() {
		int nbuck = histBuckets;
		double min, max;
		if ( nbuck == -1 ) {
			if ( rating ) {
				nbuck = 11;
			} else {
				nbuck = numc;
			}
		}
		if ( rating ) {
			if ( isFloat ) {
				min = -1.0;
				max = 1.0;
			} else {
				min = 0;
				max = 10;
			}
		} else {
			min = 1;
			max = numc;
		}
		hist = new Histogram( numc, nbuck, min, max );
    }
	public void voteStream( InputStream is ) {
		if ( ! isBin ) {
			voteReader( new InputStreamReader( is ) );
			return;
		}
		DataInputStream dis = new DataInputStream( is );
		voteStream( dis );
	}
	public void voteStream( DataInputStream dis ) {
		VoteSource vs = new BinVoteSource( dis );
		vs.numc = numc;
		voteFromVoteSourceWithInit( vs );
	}
	public void voteReader( Reader r ) {
		if ( isBin ) {
			System.err.println( "vote(Reader) only for text voting" );
			return;
		}
		StreamTokenizer st = new StreamTokenizer( r );
		VoteSource vs = new TextVoteSource( st );
		vs.numc = numc;
		voteFromVoteSourceWithInit( vs );
	}
public void voteFromTextFile( String filename ) throws Exception {
	BufferedReader fin = null;
    DataInputStream dis = null;
	VoteSource vs;

    if ( validateVotes && isFloat && ! rating ) {
	System.err.println("floating point rankings are nonsensical. (fractional placement?) aborting. not processing file \"" + filename + '"' );
	return;
    }
	if ( verbose ) {
		System.err.println("reading votes from \"" + filename + "\", processing with " + fac + ", " + numc + " choices, " + (rating ? "Rating " : "Ranking " ) + (isFloat ? "float " : "int ") + (isBin ? "bin" : "text") );
	}
    if ( isBin ) {
		dis = new DataInputStream( new FileInputStream( filename ));
		if ( numc == -1 ) {
			numc = dis.readInt();
		}
		vs = new BinVoteSource( dis );
    } else {
		fin = new BufferedReader(new FileReader( filename ));
		fin.mark(256);
		String firstLine = fin.readLine();
		if ( firstLine.charAt(0) == '!' ) {
			String[] argv = firstLine.substring(1).split(" ");
			readArgv( argv );
		} else {
			fin.reset();
		}
		if ( clist ) {
			vs = new CandidateListVoteSource( fin );
		} else {
			StreamTokenizer st = null;
			st = new StreamTokenizer( fin );
			st.eolIsSignificant( true );
			st.parseNumbers();
			if ( numc == -1 ) {
				if ( st.nextToken() != StreamTokenizer.TT_NUMBER ) {
					System.out.println( filename + ": numc undefined and first token of file not a number");
					return;
				}
				numc = (int)st.nval;
				if ( st.nextToken() != StreamTokenizer.TT_EOL ) {
					System.out.println( filename + ": numc undefined and first line not a number by itself");
				}
			}
			vs = new TextVoteSource( st );
		}
    }
	vs.numc = numc;
	voteFromVoteSourceWithInit( vs );
	if ( (vs instanceof CandidateListVoteSource) && (names == null) ) {
		CandidateListVoteSource clvs = (CandidateListVoteSource)vs;
		names = namehashToNameArray( clvs.namehash, numc );
	}
}
public void voteFromVoteSourceWithInit( VoteSource vs ) {
	if ( tab == null ) {
		try {
			/*tab = new VotingSystem[]{
				(VotingSystem)(fac.getConstructors())[0].newInstance( new Object[]{new Integer( numc )} )
			};
			tab[0].init( facArgv );*/
			tab = VotingSystemFactory.buildEnabled( ftab, numc );
		} catch ( Exception e ) {
			e.printStackTrace();
			return;
		}
	}
    if ( histogram && hist == null ) {
		initHistogram();
	}
	voteFromVoteSource( vs );
}
public static void printVote( float pf[] ) {
	for ( int i = 0; i < pf.length; i++ ) {
		if ( i != 0 ) {
			System.out.print( '\t' );
		}
		System.out.print( pf[i] );
	}
	System.out.println();
}
public static void printVote( int pf[] ) {
	for ( int i = 0; i < pf.length; i++ ) {
		if ( i != 0 ) {
			System.out.print( '\t' );
		}
		System.out.print( pf[i] );
	}
	System.out.println();
}
public void voteOne( float pf[] ) {
	if ( rating ) {
		for ( int i = 0; i < tab.length; i++ ) {
			tab[i].voteRating( pf );
		}
		if ( hist != null ) {
			hist.addRating( pf );
		}
	} else {
		/* this is gross, never do this */
		int pi[] = new int[numc];
		for ( int i = 0; i < numc; i++ ) {
			pi[i] = (int)pf[i];
		}
		for ( int i = 0; i < tab.length; i++ ) {
			tab[i].voteRanking( pi );
		}
		if ( hist != null ) {
			hist.addRanking( pi );
		}
	}
}
public void voteOne( int pi[] ) {
	if ( rating ) {
		for ( int i = 0; i < tab.length; i++ ) {
			tab[i].voteRating( pi );
		}
		if ( hist != null ) {
			hist.addRating( pi );
		}
	} else {
		for ( int i = 0; i < tab.length; i++ ) {
			tab[i].voteRanking( pi );
		}
		if ( hist != null ) {
			hist.addRanking( pi );
		}
	}
}
public void voteFloatFromVoteSource( VoteSource vs ) {
    float pf[] = null;
	pf = new float[numc];
    try { while ( true ) {
		//	System.out.print("vote:");
		pf = vs.getFloatVote( pf );
		if ( pf == null ) break;
	    if ( printVotes ) {
			printVote( pf );
		}
		voteOne( pf );
	    votes++;
    }} catch ( Exception e ) {
		e.printStackTrace();
    };
}
public void voteIntFromVoteSource( VoteSource vs ) {
    int pf[] = null;
	pf = new int[numc];
    try { while ( true ) {
		//	System.out.print("vote:");
		pf = vs.getIntVote( pf );
		if ( pf == null ) {
			if ( verbose ) {
				System.err.println("getIntVote returned null after "+votes+" votes");
			}
			break;
		}
	    if ( printVotes ) {
			printVote( pf );
		}
		voteOne( pf );
	    votes++;
    }} catch ( Exception e ) {
		e.printStackTrace();
    };
}
public void voteFromVoteSource( VoteSource vs ) {
    if ( isFloat ) {
		voteFloatFromVoteSource( vs );
    } else {
		voteIntFromVoteSource( vs );
    }
}
/*public void voteFromVoteSource( VoteSource vs ) {
    int pi[] = null;
    float pf[] = null;
    if ( isFloat ) {
		pf = new float[numc];
    } else {
		pi = new int[numc];
    }
    try { while ( true ) {
		//	System.out.print("vote:");
		if ( isFloat ) {
		    pf = vs.getFloatVote( pf );
			if ( pf == null ) break;
		} else {
		    pi = vs.getIntVote( pi );
			if ( pi == null ) break;
		}
	    if ( printVotes ) {
			for ( int i = 0; i < numc; i++ ) {
				if ( i != 0 ) {
					System.out.print( '\t' );
				}
				if ( isFloat ) {
					System.out.print( pf[i] );
				} else {
					System.out.print( pi[i] );
				}
			}
			System.out.println();
		}
	    if ( isFloat ) {
			vote( pf );
	    } else {
			vote( pi );
	    }
	    votes++;
    }} catch ( Exception e ) {
		e.printStackTrace();
    };
}*/
/**
Print summary table.
 prints a table just containing each VotingSystem name and its winner.
 @param out receives the html table
 @param vs systems that have been voted and can getWinners
 @param cnames choice names to match with winner indecies
 @param debug sets if getWinners is to collect debug info
 @param numSeats number of seats available in this election
 */
public static void winnerSummaryHTMLTable( java.io.PrintWriter out, VotingSystem[] vs, String[] cnames, boolean debug, int numSeats ) {
	if ( true ) {
		out.print("<TABLE BORDER=\"1\"><TR><TH>System</TH><TH>Winner(s)</TH></TR>");
	} else {
		out.print("<TABLE BORDER=\"1\"><TR><TH valign=\"top\" rowspan=\"");
		out.print( vs.length + 1 );
		out.println("\"><font size=\"+3\">Results Summary</fonts></TH><TH>System</TH><TH>Winner(s)</TH></TR>");
	}
	for ( int v = 0; v < vs.length; v++ ) {
		int[] wi;
		vs[v].debug = debug;
		if ( numSeats != 1 ) {
			wi = vs[v].getWinners( numSeats );
		} else {
			wi = vs[v].getWinners();
		}
		if ( wi != null ) {
			out.print("<TR><TD>");
			out.print( vs[v].name() );
			out.print("</TD><TD>");
			if ( wi.length == 1 ) {
				out.print( cnames[wi[0]] );
			} else {
				for ( int w = 0; w < wi.length; w++ ) {
					out.print( cnames[wi[w]] );
					if ( (w + 1) < wi.length ) {
						out.println( ", " );
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
 @param cnames choice names to match with winner indecies
 @param debug sets if getWinners is to collect debug info
 @param numSeats number of seats available in this election
 */
public static void resultsHTMLDisplay( java.io.PrintWriter out, VotingSystem[] vs, String[] cnames, boolean debug, int numSeats ) {
	for ( int v = 0; v < vs.length; v++ ) {
		int[] wi;
		vs[v].debug = debug;
		if ( numSeats != 1 ) {
			wi = vs[v].getWinners( numSeats );
		} else {
			wi = vs[v].getWinners();
		}
		if ( wi != null ) {
			if ( vs[v].debug ) {
				String dbh;
				dbh = vs[v].getDebugHTML( cnames );
				if ( dbh != null ) {
					out.print("<H2>DEBUG: ");
					out.print( vs[v].name() );
					out.println("</H2>");
					out.flush();
					out.print( dbh );
				}
			}
			out.print("<H2>");
			out.print( vs[v].name() );
			out.println("</H2>");
			out.println("<P>");
			out.flush();
			out.println( vs[v].htmlSummary( cnames ) );
			if ( wi.length == 1 ) {
				out.print("Winner: ");
				out.println( cnames[wi[0]] );
			} else {
				out.print("Winners: ");
				for ( int w = 0; w < wi.length; w++ ) {
					out.print( cnames[wi[w]] );
					if ( (w + 1) < wi.length ) {
						out.println( ", " );
					}
				}
			}
			out.println("</P>");
		} else if ( vs[v] instanceof Histogram ) {
			out.println( vs[v].htmlSummary( cnames ) );
			out.println("</P>");
		}
		out.flush();
	}
}
public void displayTab() {
	if ( names == null ) {
		names = new String[numc];
		for ( int i = 0; i < numc; i++ ) {
			names[i] = Integer.toString( i + 1 );
		}
	}
	if ( htmlOut ) {
		try {
			java.io.PrintWriter out = new java.io.PrintWriter( System.out );
			// now, display proper output
			// first, short summary table
			winnerSummaryHTMLTable( out, tab, names, verbose, 1 );
			if ( hist != null ) {
				out.println("<H1>Vote Histogram</H1>");
				out.println( hist.toString( 4, names ) );
			}
			out.println("<H1>Details</H1>");
			// full display
			resultsHTMLDisplay( out, tab, names, verbose, 1 );
			out.println("<HR>");
			out.flush();
		} catch ( Throwable e ) {
			e.printStackTrace();
		}
		return;
	}
    //System.out.println( votes + " votes counted" );
    int[] winners;
	for ( int tabi = 0; tabi < tab.length; tabi++ ) {
		if ( verbose ) {
			tab[tabi].debug = true;
		}
		winners = tab[tabi].getWinners();
		if ( verbose ) {
			if ( htmlOut ) {
				System.out.print( tab[tabi].getDebugHTML( names ) );
			} else {
				System.out.println( tab[tabi].getDebugText( names ) );
			}
		}
		if ( printState ) {
			if ( names != null ) {
				System.out.print( tab[tabi].toString( names ) );
			} else {
				System.out.print( tab[tabi].toString() );
			}
		}
		if ( htmlOut ) {
			System.out.print( tab[tabi].htmlSummary( names ) );
		}
		if ( scriptMode ) {
			for ( int i = 0; i < winners.length; i++ ) {
				System.out.println( winners[i] );
			}
		} else if ( winners.length == 0 ) {
			System.out.println("no winner found,"+tab[tabi]);
		} else if ( winners.length == 1 ) {
			if ( names != null ) {
				System.out.println( "Winner: " + names[winners[0]] );
			} else {
				System.out.println("winner, choice number " + (winners[0] + 1) + " (of 1.." + numc + ")" );
			}
		} else {
			if ( names != null ) {
				System.out.print( "Winners: " + names[winners[0]] );
				for ( int i = 1; i < winners.length; i++ ) {
					System.out.print(", " + names[winners[i]] );
				}
			} else {
				System.out.print( winners.length + " winners, numbers " + (winners[0] + 1) );
				for ( int i = 1; i < winners.length; i++ ) {
					System.out.print(", " + (winners[i] + 1) );
				}
			}
			System.out.println(" (of 1.." + numc + ")" );
		}
	}
    if ( hist != null ) {
		System.out.print( hist.toString( histyle, names ) );
    }
}

static final String optstring = "AbBcdDfhi:n:Rrw";
static final String usageText = "vote [-AbBcdfhRrw][-n number of choices][-i vote file][vote file ...]\n";
static final String helpText =
"\t-A\tIntant Runoff Normalized Ratings\t-b\tbinary\n\t-B\ttext (default)\n\t-c\tCondorcet\n\t-D\tBorda count (default)\n\t-d\tinteger (default)\n\t-f\tfloat (-1.0 .. 1.0)\n\t-h\tthis help\n\t-H\toutput html table\n\t-i f\timmediately load and run vote file f\n\t-I\tInstant Runoff Vote\n\t-n N\tnumber of choices\n\t-N f\tfile with names of choices\n\t-p\tprint full state, not just winner\n\t-q\tquiet, don't print winner\n\t-R\tnumbers are rankings (1 best) (default)\n\t-r\tnumbers are ratings (higher better)\n\t-s\tscript friendly output (zero indexed \\n separated list of winners)\n\t-S\tmake histogram of ranked or rated votes\n\t--system classname\tspecify an implementation of org.bolson.vote.VotingSystem\n\t-T #\thistogram style #\n\t-v\tverbose\n\t-w\tRaw Sum of ratings\n\t-Y\tverify that votes are valid\n";

public static void main( String argv[] ) {
    vote it = new vote();
    argv = it.readArgv( argv );
    if ( argv != null ) for ( int i = 0; i < argv.length; i++ ) {
	try {
	    it.voteFromTextFile( argv[i] );
		it.displayTab();
	} catch ( Exception e ) {
	    e.printStackTrace();
	}
    }
}
/**
 * @param argv like main()
 * @return unused arguments (filenames)
 */
public String[] readArgv( String argv[] ) {
    int i;
	java.util.Vector vFacArgv = new java.util.Vector();

    if ( fac == null ) {
		try {
			fac = Class.forName("org.bolson.vote.RawRating");
		} catch ( java.lang.ClassNotFoundException e ) {
			e.printStackTrace();
		}
	}
    for ( i = 0; i < argv.length; i++ ) {
	if ( argv[i].equals("-A") || argv[i].equals( "--IRNR" ) ) {
		try {
			fac = Class.forName("org.bolson.vote.IRNR");
		} catch ( java.lang.ClassNotFoundException e ) {
			e.printStackTrace();
		}
	} else if ( argv[i].equals("-B") || argv[i].equals( "--text" ) ) {
	    isBin = false;
	} else if ( argv[i].equals("-b") || argv[i].equals( "--binary" ) ) {
	    isBin = true;
	} else if ( argv[i].equals("-C") ) {
		clist = true;
	    rating = false;
	} else if ( argv[i].equals("-c") || argv[i].equals( "--condorcet" ) ) {
	    try {
		fac = Class.forName("org.bolson.vote.Condorcet");
	    } catch ( java.lang.ClassNotFoundException e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("-D") || argv[i].equals( "--borda" ) ) {
	    try {
		fac = Class.forName("org.bolson.vote.BordaVotingSystem");
	    } catch ( java.lang.ClassNotFoundException e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("-d") ) {
	    isFloat = false;
	} else if ( argv[i].equals("-f") ) {
	    isFloat = true;
	} else if ( argv[i].equals("-h") ) {	// help
	    System.out.println(usageText);
	    System.out.println(helpText);
	} else if ( argv[i].equals("-H") ) {
	    htmlOut = true;
	} else if ( argv[i].equals("-I") || argv[i].equals( "--IRV" ) ) {
	    try {
		fac = Class.forName("org.bolson.vote.IRV");
	    } catch ( java.lang.ClassNotFoundException e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("-i") ) {
	    i++;
	    try {
		voteFromTextFile( argv[i] );
	    } catch ( Exception e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("-M") ) {
		i++;
		vFacArgv.add( argv[i] );
	} else if ( argv[i].equals("-N") ) {
		String[] tnam;
	    i++;
	    tnam = readNames( argv[i] );
		if ( tnam != null ) {
			setNames( tnam );
		}
	} else if ( argv[i].equals("-n") ) {
	    i++;
	    numc = Integer.parseInt( argv[i] );
	} else if ( argv[i].equals("-p") ) {
	    printState = true;
	} else if ( argv[i].equals("-q") ) {
	    quiet = true;
	} else if ( argv[i].equals("-R") ) {
	    rating = false;
	} else if ( argv[i].equals("-r") ) {
	    rating = true;
	} else if ( argv[i].equals("-s") ) {
	    scriptMode = true;
	} else if ( argv[i].equals("-S") ) {
	    histogram = true;
	} else if ( argv[i].equals("-T") ) {
	    i++;
	    histyle = Integer.parseInt( argv[i] );
	    histogram = true;
	} else if ( argv[i].equals("-v") || argv[i].equals( "--verbose" )) {
		verbose = true;
	} else if ( argv[i].equals("-w") || argv[i].equals( "--raw" ) ) {
	    try {
		fac = Class.forName("org.bolson.vote.RawRating");
	    } catch ( java.lang.ClassNotFoundException e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("-W") || argv[i].equals( "--IRNR" )) {
	    try {
			fac = Class.forName("org.bolson.vote.IRNR");
	    } catch ( java.lang.ClassNotFoundException e ) {
			e.printStackTrace();
	    }
	} else if ( argv[i].equals("-Y") ) {
	    validateVotes = true;
	} else if ( argv[i].equals("--system") ) {
	    i++;
	    try {
		fac = Class.forName(argv[i]);
	    } catch ( java.lang.ClassNotFoundException e ) {
		e.printStackTrace();
	    }
	} else if ( argv[i].equals("--printVotes") ) {
	    printVotes = true;
	} else if ( argv[i].equals("--") ) {
	    break;
	} else if ( argv[i].startsWith("-") ) {
	    System.err.println("bogus flag: " + argv[i] );
	    return null;
	} else {
	    break;
	}
    }
	if ( vFacArgv.size() != 0 ) {
		facArgv = new String[vFacArgv.size()];
		for ( int j = 0; j < vFacArgv.size(); j++ ) {
			facArgv[j] = (String)vFacArgv.elementAt( j );
		}
	}
    String toret[] = new String[argv.length - i];
    for ( int j = 0; i + j < argv.length; j++ ) {
	toret[j] = argv[i+j];
    }
    return toret;
}

public void setNames( String[] namesi ) {
	names = namesi;
	if ( numc == -1 ) {
		numc = names.length;
	}	
}
/**
 * @param filename A file with the names of choices, one per line.
 * @return The names (lines) of the file.
 */
static String[] readNames( String filename ) {
    try {
	FileReader fr = new FileReader( filename );
	BufferedReader br = new BufferedReader( fr );
	java.util.Vector tn = new java.util.Vector();
	String s;
	while ( (s = br.readLine()) != null ) {
	    tn.addElement( s );
	}
	br.close();
	String[] toret = new String[tn.size()];
	for ( int i = 0; i < toret.length; i++ ) {
	    toret[i] = (String)tn.elementAt( i );
	}
	return toret;
    } catch ( Exception e ) {
	return null;
    }
}
}
