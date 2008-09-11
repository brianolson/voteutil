package org.bolson.vote;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;

/**
 * command line utility for counting votes.
 @author Brian Olson
 */
public class countvotes {
	// TODO WRITEME comma-separated-value input mode
	// TODO WRITEME interpret number values as 1,2,3... rankings
	public static final String[] usage = {
		"countvotes [--urlencoded|--whitespace|--votespec][--histMax n][--histMin n][-m method.class.name]",
		"\t[-i votedata|-igz gzipped-votedata]",
		"\t[--dump][--disable-all][--enable-all][--seats n]",
		"\t[--disable method][--enable method][--explain][--full-html|--no-full-html|--text|--short]",
		"\t[--histMax n][--histMin n]",
		"\t[-- args to methods [debug]] < votedata"
	};
	public static void printUsage() {
		for ( int i = 0; i < usage.length; i++ ) {
			System.err.println(usage[i]);
		}
	}
	public static class ShortNameClassName {
		public String shortName;
		public String className;
		public boolean enabled;
		public boolean testable;
		/**
		@param short_name
		@param class_name
		*/
		public ShortNameClassName(String short_name, String class_name) {
			shortName = short_name;
			className = class_name;
			enabled = false;
			testable = true;
		}
		/**
		@param short_name
		@param class_name
		@param default_enabled Set whether method will be in the default enabled set of methods before --enable --disable command line options.
		@param test_enabled this method will be included in --test output
		*/
		public ShortNameClassName(String short_name, String class_name,
				boolean default_enabled, boolean test_enabled) {
			shortName = short_name;
			className = class_name;
			enabled = default_enabled;
			testable = test_enabled;
		}
	}
	public static final String[] enableNames = {
		"hist","irnr","vrr","rp","raw","irv","stv"
	};
	public static final String[] enableClassNames = {
		"org.bolson.vote.NamedHistogram", "org.bolson.vote.NamedIRNR", "org.bolson.vote.NamedVRR", "org.bolson.vote.NamedRankedPairs", "org.bolson.vote.NamedRaw", "org.bolson.vote.NamedIRV", "org.bolson.vote.NamedSTV"
	};
	public static final ShortNameClassName[] methodNames = {
		new ShortNameClassName("hist", "org.bolson.vote.NamedHistogram", true, true),
		new ShortNameClassName("irnr", "org.bolson.vote.NamedIRNR", true, true),
		new ShortNameClassName("vrr", "org.bolson.vote.NamedVRR", true, true),
		new ShortNameClassName("rp", "org.bolson.vote.NamedRankedPairs", false, true),
		new ShortNameClassName("raw", "org.bolson.vote.NamedRaw", true, true),
		new ShortNameClassName("irv", "org.bolson.vote.NamedIRV", true, true),
		new ShortNameClassName("stv", "org.bolson.vote.NamedSTV", false, false),
	};

	public static final int MODE_URL = 1;
	public static final int MODE_WS_NAMEQ = 2;
	public static final int MODE_GTEQ_SPEC = 3;
	public static final int MODE_CSV = 4;
	
	public static final int OUT_FULL_HTML = 1;
	public static final int OUT_PART_HTML = 2;
	public static final int OUT_TEXT = 3;
	public static final int OUT_SHORT = 4;
	public static final int OUT_TEST = 5;
	
	public static final boolean isHtml(int outmode) {
		return (outmode == OUT_FULL_HTML) || (outmode == OUT_PART_HTML);
	}

	public static void main( String[] argv ) {
		boolean debug = false;
		InputStream fin = System.in;
		String[] methodArgs = null;
		int histMin = -10, histMax = 10;
		Vector countClasses = new Vector();
		int mode = MODE_URL; // 1 = url encoded, 2 = whitespace separated
		boolean redumpVotes = false;
		int outmode = OUT_FULL_HTML;
		boolean explain = false;
		PrintWriter out = null;
		int seats = 1;
		
		NamedHistogram histInstance = null;
		NameVotingSystem firstWinner = null;

		//boolean[] enabled = new boolean[]{ true,true,true,true,true,false };
		/*countClasses.add( enableClassNames[0] );
		countClasses.add( enableClassNames[1] );
		countClasses.add( enableClassNames[2] );
		countClasses.add( enableClassNames[3] );
		countClasses.add( enableClassNames[4] );*/
		for ( int i = 0; i < methodNames.length; i++ ) {
			if ( methodNames[i].enabled ) {
				countClasses.add( methodNames[i].className );
			}
		}

		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i].equals("-i") ) {
				i++;
				try {
					fin = new java.io.FileInputStream(argv[i]);
				} catch ( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			} else if ( argv[i].equals("-igz") ) {
				i++;
				try {
					fin = new java.util.zip.GZIPInputStream(new java.io.FileInputStream(argv[i]));
				} catch ( Exception e ) {
					e.printStackTrace();
					System.exit(1);
				}
			} else if ( argv[i].equals("--disable-all") ) {
				countClasses = new Vector();
			} else if ( argv[i].equals("--enable-all") ) {
				for ( int j = 0; j < methodNames.length; j++ ) {
					countClasses.add( methodNames[j].className );
				}
			} else if ( argv[i].equals("--disable") ) {
				boolean any = false;
				i++;
				for ( int j = 0; j < methodNames.length; j++ ) {
					if ( methodNames[j].shortName.equals( argv[i] ) ) {
						countClasses.remove( methodNames[j].className );
						any = true;
					}
				}
				if ( ! any ) {
					System.err.println("bogus --disable: " + argv[i] );
					System.exit(1);
				}
			} else if ( argv[i].equals("--enable") ) {
				boolean any = false;
				i++;
				for ( int j = 0; j < methodNames.length; j++ ) {
					if ( methodNames[j].shortName.equals( argv[i] ) ) {
						countClasses.add( methodNames[j].className );
						any = true;
					}
				}
				if ( ! any ) {
					System.err.println("bogus --enable: " + argv[i] );
					System.exit(1);
				}
			} else if ( argv[i].equals("--seats") ) {
				i++;
				seats = Integer.parseInt(argv[i]);
				countClasses = new Vector();
				countClasses.add( "org.bolson.vote.NamedSTV" );
				countClasses.add( "org.bolson.vote.NamedHistogram" );
			} else if ( argv[i].equals("--urlencoded") ) {
				mode = MODE_URL;
			} else if ( argv[i].equals("--whitespace") ) {
				mode = MODE_WS_NAMEQ;
			} else if ( argv[i].equals("--votespec") ) {
				mode = MODE_GTEQ_SPEC;
			} else if ( argv[i].equals("--explain") ) {
				explain = true;
			} else if ( argv[i].equals("--full-html") ) {
				outmode = OUT_FULL_HTML;
			} else if ( argv[i].equals("--no-full-html") ) {
				outmode = OUT_PART_HTML;
			} else if ( argv[i].equals("--test") ) {
				outmode = OUT_TEST;
				countClasses = new Vector();
				// start at 1, skip histogram
				for ( int j = 1; j < methodNames.length; j++ ) {
					if ( methodNames[j].testable ) {
						countClasses.add( methodNames[j].className );
					}
				}
			} else if ( argv[i].equals("--text") ) {
				outmode = OUT_TEXT;
			} else if ( argv[i].equals("--short") ) {
				outmode = OUT_SHORT;
			} else if ( argv[i].equals("--dump") ) {
				redumpVotes = true;
			} else if ( argv[i].equals("--help") || argv[i].equals("-help") || argv[i].equals("-h") ) {
				printUsage();
				System.out.println("available election methods:");
				for ( int j = 0; j < methodNames.length; j++ ) {
					System.out.print( methodNames[j].shortName );
					if ( (j+1) < methodNames.length ) {
						System.out.print( ", " );
					}
				}
				System.out.println();
				return;
			} else if ( argv[i].equals("--histMax") ) {
				i++;
				histMax = Integer.parseInt( argv[i] );
			} else if ( argv[i].equals("--histMin") ) {
				i++;
				histMin = Integer.parseInt( argv[i] );
			} else if ( argv[i].equals("--out") ||
					    argv[i].equals("-o") ) {
				i++;
				try {
					out = new PrintWriter(new java.io.FileWriter( argv[i] ));
				} catch (java.io.IOException e) {
					e.printStackTrace();
					return;
				}
			} else if ( argv[i].equals("-m") ) {
				i++;
				countClasses.add( argv[i] );
			} else if ( argv[i].equals("--") ) {
				i++;
				int j = 0;
				methodArgs = new String[argv.length - i];
				while ( i < argv.length ) {
					if ( argv[i].equals("debug") ) {
						debug = true;
					}
					methodArgs[j] = argv[i];
					i++; j++;
				}
				break;
			} else {
				System.err.println("bogus arg \"" + argv[i] + "\"" );
				System.exit(1);
			}
		}
		if ( out == null ) {
			out = new PrintWriter(new java.io.OutputStreamWriter(System.out));
		}
		if ( outmode == OUT_FULL_HTML ) {
			out.println("<html><head><title>vote results</title></head><body bgcolor=\"#ffffff\" text=\"#000000\">");
		}
		// setup methods to count into
		if ( debug ) {
			if ( isHtml(outmode) ) {
				out.print("<pre>");
			}
			out.println("debug:");
		}
		NameVotingSystem[] vs;
		String initArgs[] = null;
		if ( seats != 1 ) {
			initArgs = new String[2];
			initArgs[0] = "seats";
			initArgs[1] = Integer.toString( seats );
		}
		if ( countClasses.size() > 0 ) {
			vs = new NameVotingSystem[countClasses.size()];
			for ( int i = 0; i < vs.length; i++ ) {
				String cn;
				cn = (String)countClasses.get(i);
				if ( cn.equals( "Histogram" ) || cn.equals("NamedHistogram") || cn.equals("org.bolson.vote.NamedHistogram") ) {
					histInstance = new NamedHistogram( histMin, histMax );
					vs[i] = histInstance;
				} else {
					vs[i] = getMethodInstance( cn );
					if ( firstWinner == null ) {
						firstWinner = vs[i];
					}
					if ( vs[i] == null ) {
						System.err.println( cn + " failed to instantiate");
						return;
					}
					if ( initArgs != null ) {
						vs[i].init( (String[])initArgs.clone() );
					}
				}
			}
		} else {
			histInstance = new NamedHistogram( histMin, histMax );
			firstWinner = new NamedIRNR();
			vs = new NameVotingSystem[] {
				histInstance,
				firstWinner,
				new NamedVRR(),
				new NamedRaw(),
				new NamedIRV(),
			};
		}
		for ( int vi = 0; vi < vs.length; vi++ ) {
			if ( methodArgs == null ) {
				vs[vi].init( null );
			} else {
				vs[vi].init( (String[])methodArgs.clone() );
			}
		}
		// done setting up, read input and count votes
		try {
			String line;
			BufferedReader r = new BufferedReader( new InputStreamReader( fin ) );
			while ( (line = r.readLine()) != null ) {
				NameVotingSystem.NameVote[] nv;
				//nv = NameVotingSystem.voteSpecToNameVoteArray( line );
				if ( mode == MODE_URL ) {
					nv = NameVotingSystem.fromUrlEncoded( line );
				} else if ( mode == MODE_WS_NAMEQ ) {
					nv = NameVotingSystem.nameEqStrToVoteArray( line );
				} else if ( mode == MODE_GTEQ_SPEC ) {
					nv = NameVotingSystem.voteSpecToNameVoteArray( line );
				} else {
					System.err.println("ICE");
					return;
				}
				if ( redumpVotes ) {
					out.print( NameVotingSystem.urlEncode(nv) );
					out.println();
				}
				for ( int vi = 0; vi < vs.length; vi++ ) {
					vs[vi].voteRating( nv );
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		if ( debug && isHtml(outmode) ) {
			out.println( "</pre>" );
		}
		// display results, various modes
		if ( outmode == OUT_TEST ) {
			// minimal and easily regular output so that a good test result has no diff to standard output
			for ( int vi = 0; vi < vs.length; vi++ ) {
				out.print( vs[vi].name() + ": " );
				try {
					NameVotingSystem.NameVote[] winners = vs[vi].getWinners();
					if ( winners == null || winners.length == 0 ) {
						out.println();
						continue;
					}
					out.print( winners[0].name );
					for ( int i = 1; i < winners.length; i++ ) {
						out.print( ", " );
						out.print( winners[i].name );
					}
				} catch (Exception e) {
					e.printStackTrace();
					out.print( "fail" );
				}
				out.println();
			}
			out.flush();
			return;
		}
		if ( outmode == OUT_TEXT ) {
			// simple plain text output
			for ( int vi = 0; vi < vs.length; vi++ ) {
				out.println( vs[vi].name() );
			}
			out.flush();
			return;
		}
		if ( outmode == OUT_SHORT ) {
		}
		// only HTML outputting modes remain below here
		for ( int vi = 0; vi < vs.length; vi++ ) {
			out.print( "<h2>" );
			out.print( vs[vi].name() );
			out.print( "</h2>" );
			String hs = null;
			if ( vs[vi] == histInstance && firstWinner != null ) {
				hs = histInstance.htmlSummary( new StringBuffer(), firstWinner.getWinners() ).toString();
			} else if ( explain ) {
				hs = vs[vi].htmlExplain();
			} else {
				hs = vs[vi].htmlSummary();
			}
			if ( debug ) {
				String dbs = vs[vi].getDebug();
				if ( dbs != null && dbs.length() > 0 ) {
					out.println( "<h3>debug:</h3><pre>" );
					out.println( dbs );
					out.println( "</pre>" );
				}
			}
			out.println( hs );
		}
		if ( outmode == OUT_FULL_HTML ) {
			out.println("</body></html>");
		}
		out.flush();
	}
	
	protected static final String[] gmiPrefixes = {
		"", "Named", "org.bolson.vote.", "org.bolson.vote.Named"
	};
	public static NameVotingSystem getMethodInstance(String name) {
		Class c;
		StringBuffer errors = new StringBuffer();
		for ( int i = 0; i < gmiPrefixes.length; i++ ) {
			try {
				String cn = gmiPrefixes[i] + name;
				c = Class.forName( cn );
				// can this return null or does it just throw?
				if ( c != null ) {
					if ( NameVotingSystem.class.isAssignableFrom(c) ) {
						try {
							return (NameVotingSystem)c.newInstance();
						} catch ( Exception e ) {
							e.printStackTrace();
						}
						return null;
					} else {
						errors.append("found class ").append(cn).append(" but it is not a NameVotingSystem").append('\n');
					}
				}
			} catch ( ClassNotFoundException e ) {
				// don't care. try next prefix
			}
		}
		System.err.println(errors.toString());
		return null;
	}
}
