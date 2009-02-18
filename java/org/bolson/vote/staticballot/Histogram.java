package org.bolson.vote.staticballot;

/**
 Collects votes and presents HTML histograms.
 @author Brian Olson
 */
public class Histogram extends VotingSystem {
	//    boolean isRanking = true;
	int outerTableCols = 4;
	
	/** number of candidates */
	//    int numc;
	
	/** histogram buckets b[numc][blen] */
	int[] b;
	/** bucket length per choice */
	int blen;
	/** value of low bucket */
	double l;
	/** value of high bucket */
	double h;
	/** sum of b[numc][blen] -> totals[numc] */
	int[] totals;
	
	int maxbucket;
	int maxtotal;
	int numvotes;
	
	/** URL to insert in generated Histogram HTML. Defaults to relative URL "b.png" . */
	public String barImgUrl = "b.png";
	public boolean useSpan = true;
	
	int bucketfy( int rate ) {
		if ( rate < l )
			return 0;
		if ( rate > h )
			return blen-1;
		return (int)((
			((rate - l) * (blen-1))
			/ (h - l))
			+ 0.5);
	}
	int bucketfy( float rate ) {
		if ( rate < l )
			return 0;
		if ( rate > h )
			return blen-1;
		return (int)((
			((rate - l) * (blen-1))
			/ (h - l))
			+ 0.5);
	}
	int bucketfy( double rate ) {
		if ( rate < l )
			return 0;
		if ( rate > h )
			return blen-1;
		return (int)((
			((rate - l) * (blen-1))
			/ (h - l))
			+ 0.5);
	}
	
	public Histogram( int numcIn, int nbuck, double min, double max ) {
		super( numcIn );
		blen = nbuck;
		l = min;
		h = max;
		b = new int[numc*blen];
		totals = new int[numc];
		clear();
	}
	public Histogram( int numc ) {
		super( numc );
		blen = 0;
		h = l = Double.NaN;
		b = null;
		totals = null;
	}
	public VotingSystem init( String argv[] ) {
		int i, j = 0;
		for ( i = 0; i < argv.length; i++ ) {
			if ( argv[i] == null ) {
			} else if ( argv[i].equals( "buckets" ) ) {
				i++;
				blen = Integer.parseInt( argv[i] );
			} else if ( argv[i].equals( "min" ) ) {
				i++;
				l = Double.parseDouble( argv[i] );
			} else if ( argv[i].equals( "max" ) ) {
				i++;
				h = Double.parseDouble( argv[i] );
			} else {
				argv[j] = argv[i];
				j++;
			}
		}
		if ( j > 0 ) {
			if ( j < argv.length ) {
				String[] nargv = new String[j];
				for ( i = 0; i < j; i++ ) {
					nargv[i] = argv[i];
				}
				return super.init( nargv );
			} else {
				return super.init( argv );
			}
		}
		return this;
	}
	public void clear() {
		for ( int i = 0; i < numc*blen; i++ ) {
			b[i] = 0;
		}
		for ( int i = 0; i < numc; i++ ) {
			totals[i] = 0;
		}
		maxbucket = 0;
		maxtotal = 0;
		numvotes = 0;
	}
	
	public int voteRanking( int[] rankings ) {
		addRanking( rankings );
		return 0;
	}
	public void addRanking( int[] rankings ) {
		int i;
		boolean anyvote = false;
		for ( i = 0; i < numc; i++ ) if ( rankings[i] != VotingSystem.NO_VOTE  ) {
			int bu, bv;
			bu = bucketfy( rankings[i] );
			b[i*blen + bu] = bv = b[i*blen + bu] + 1;
			anyvote = true;
			if ( bv > maxbucket ) {
				maxbucket = bv;
			}
			totals[i]++;
			if ( totals[i] > maxtotal ) {
				maxtotal = totals[i];
			}
		}
		if ( anyvote ) {
			numvotes++;
		}
	}
	public int voteRating( int[] ratings ) {
		addRating( ratings );
		return 0;
	}
	public void addRating( int[] ratings ) {
		int i;
		boolean anyvote = false;
		for ( i = 0; i < numc; i++ ) if ( ratings[i] != VotingSystem.NO_VOTE  ) {
			int bu, bv;
			bu = bucketfy( ratings[i] );
			b[i*blen + bu] = bv = b[i*blen + bu] + 1;
			anyvote = true;
			if ( bv > maxbucket ) {
				maxbucket = bv;
			}
			totals[i]++;
			if ( totals[i] > maxtotal ) {
				maxtotal = totals[i];
			}
		}
		if ( anyvote ) {
			numvotes++;
		}
	}
	public int voteRating( float[] ratings ) {
		addRating( ratings );
		return 0;
	}
	public void addRating( float[] ratings ) {
		int i;
		boolean anyvote = false;
		for ( i = 0; i < numc; i++ ) if ( ratings[i] != VotingSystem.NO_VOTE  ) {
			int bu, bv;
			bu = bucketfy( ratings[i] );
			b[i*blen + bu] = bv = b[i*blen + bu] + 1;
			anyvote = true;
			if ( bv > maxbucket ) {
				maxbucket = bv;
			}
			totals[i]++;
			if ( totals[i] > maxtotal ) {
				maxtotal = totals[i];
			}
		}
		if ( anyvote ) {
			numvotes++;
		}
	}
	public int voteRating( double[] ratings ) {
		addRating( ratings );
		return 0;
	}
	public void addRating( double[] ratings ) {
		int i;
		boolean anyvote = false;
		for ( i = 0; i < numc; i++ ) if ( ratings[i] != VotingSystem.NO_VOTE  ) {
			int bu, bv;
			bu = bucketfy( ratings[i] );
			b[i*blen + bu] = bv = b[i*blen + bu] + 1;
			anyvote = true;
			if ( bv > maxbucket ) {
				maxbucket = bv;
			}
			totals[i]++;
			if ( totals[i] > maxtotal ) {
				maxtotal = totals[i];
			}
		}
		if ( anyvote ) {
			numvotes++;
		}
	}
	
	public String toString() {
		return toString( 1, null );
	}
	public static String defaultValueTitle = "Ranking";
	public String valueTitle = defaultValueTitle;
	
	int MAX_WIDTH = 100;
	boolean MAXIMIZE_WIDTH = true;
	
	public String toString( int style, String names[] ) {
		StringBuffer sb;
		double bstep = (h - l) / (blen - 1);
		double base = l - bstep/2.0;
		double scale = 1.0;
		if ( MAXIMIZE_WIDTH || maxbucket > MAX_WIDTH ) {
			scale = ((double)MAX_WIDTH) / ((double)maxbucket);
		}
		if ( style == 2 ) {
			/* gnuplot style */
			sb = new StringBuffer( "set data style histeps\nset terminal png color\nset xlabel \""+valueTitle+"\"\nset ylabel \"Votes\"\nset nolabel\n" );
			for ( int c = 0; c < numc; c++ ) {
				if ( names != null ) {
					sb.append( "set title \'" ).append( names[c] ).append("\'\n");
					sb.append( "set output \"" ).append( names[c] ).append( ".png\"\n" );
				} else {
					sb.append( "set output \"").append( c ).append( ".png\"\n" );
				}
				sb.append( "plot \"-\"\n" );
				for ( int i = 0; i < blen; i++ ) {
					int hval;
					hval = b[c*blen + i];
					sb.append( '\t' ).append( l + i*bstep ).append( '\t' ).append( hval ).append( '\n' );
				}
				sb.append( "e\n" );
			}
			return sb.toString();
		}
		int oTC = 0;
		sb = new StringBuffer();
		if ( outerTableCols > 1 ) {
			sb.append( "<table border=\"1\">" );
		}
		for ( int c = 0; c < numc; c++ ) {
			//int ctot;
			//ctot = 0;
			if ( outerTableCols > 1 ) {
				if ( oTC == 0 ) {
					sb.append( "<tr>" );
				}
				sb.append( "<td width=\"" ).append( 100 / outerTableCols ).append( "%\">" );
			}
			if ( style == 3 || style == 4 ) {
				sb.append( "<table><tr><th colspan=\"2\">" );
			}
			if ( names != null ) {
				sb.append( names[c] ).append( '\n' );
			} else {
				sb.append( "choice " ).append( c + 1 ).append( '\n' );
			}
			if ( style == 3 || style == 4 ) {
				sb.append( "</th></tr><tr><th>"+valueTitle+"</th><th>Votes</th></tr>\n" );
			}
			for ( int i = blen-1; i >= 0; i-- ) {
				int hval;
				hval = b[c*blen + i];
				//ctot += hval;
				if ( style == 1 ) {
					sb.append( '\t' ).append( base + i*bstep ).append( ".." ).append( base + (i+1)*bstep ).append( ":\t" ).append( hval ).append( '\n' );
				} else if ( style == 3 ) {
					sb.append( "<tr><td>" ).append( l + i*bstep ).append( "</td><td>" ).append( hval ).append( "</td></tr>\n" );
				} else if ( style == 4 ) {
					sb.append( "<tr><td>" ).append( l + i*bstep );
					if ( useSpan ) {
						sb.append( "</td><td><div style=\"background-color: #bb99ff; width: " ).append( (int)(hval * scale) ).append( "\">" ).append( hval ).append( "</div></td></tr>\n" );
					} else {
						sb.append( "</td><td><img src=\"").append( barImgUrl ).append("\" height=\"10\" width=\"" ).append( (int)(hval * scale) ).append( "\"> " ).append( hval ).append( "</td></tr>\n" );
					}
				} else {
					sb.append( '\t' ).append( l + i*bstep ).append( '\t' ).append( hval ).append( '\n' );
				}
			}
			if ( style == 3 || style == 4 ) {
				sb.append( "<tr><td>total</td><td>" ).append( totals[c] ).append( "</td></tr></table>\n" );
			} else {
				sb.append( "\ttotal\t" ).append( totals[c] ).append( '\n' );
			}
			if ( outerTableCols > 1 ) {
				sb.append( "</td>" );
				oTC = ( oTC + 1 ) % outerTableCols;
				if ( oTC == 0 ) {
					sb.append( "</tr>\n" );
				}
			}
		}
		if ( outerTableCols > 1 ) {
			if ( oTC != 0 ) {
				sb.append( "</tr>" );
			}
			sb.append( "</table>\n" );
		}
		return sb.toString();
	}
	public String htmlSummary( String[] names ) {
		return toString( 4, names );
	}
	public String toString( String[] names ) {
		return toString( 1, names );
	}
	public int[] getWinners() {
		return null;
	}
	public int[] getWinners( int numSeats ) {
		return null;
	}
}
