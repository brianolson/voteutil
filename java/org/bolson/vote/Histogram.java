package org.bolson.vote;

import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

/**
	The normal vote interface in this case accumulates the ratings into histograms per name.
 @author Brian Olson
 */
public class Histogram extends NameVotingSystem {
	protected float min, max, step;
	protected int buckets;
	protected HashMap they = new HashMap();
	protected int maxbucket = 0;
	public boolean intmode = false;
	/** lowfirst indicates 1st,2nd,3rd,... rankings input */
	public boolean lowfirst = false;
	/** bucketscale used at display time on bucket labels */
	public float bucketscale = 1.0f;
	/** offset used at display time on bucket labels */
	public float offset = 0.0f;
	/** doscaleoffset used at display time on bucket labels */
	public boolean doscaleoffset = false;

	/** make getWinners return null */
	public boolean disableWinners = true;

	public String labelname = "Rating";
	
	double minRecorded = Double.MAX_VALUE;
	double maxRecorded = Double.MAX_VALUE * -1.0;
	double sum;
	protected int votes;

	protected class Choice implements Comparable {
		public int[] counts;
		public int votes = 0;
		public double sum = 0.0;
		public String name;
		
		public Choice( String nameIn, int buckets ) {
			counts = new int[buckets];
			name = nameIn;
		}
		public void vote( float rating ) {
			if ( Float.isNaN( rating ) ) {
				return;
			}
			sum += rating;
			votes++;
			if ( rating < min ) {
				counts[0]++;
				if ( counts[0] > maxbucket ) { maxbucket = counts[0]; }
			} else if ( rating >= max ) {
				counts[buckets-1]++;
				if ( counts[buckets-1] > maxbucket ) { maxbucket = counts[buckets-1]; }
			} else {
				/*
				 0    1    2    3    4
				 |----|----|----|----|----|
				 min                      max
				 5 buckets
				 */
				int x;
				x = (int)Math.floor( step * (rating - min) );
				counts[x]++;
				if ( counts[x] > maxbucket ) { maxbucket = counts[x]; }
			}
		}
		public int compareTo( Object bi ) throws ClassCastException {
			Choice b = (Choice)bi;
			double avea = sum/votes;
			double aveb = b.sum/b.votes;
			if ( avea < aveb ) {
				return -1;
			} else if ( avea > aveb ) {
				return 1;
			}
			return 0;
		}
	}
	/**
		equivalent to Histogram( -10, 10 );
	 */
	public Histogram() {
		buckets = 21;
		min = -10.5f;
		max = 10.5f;
		step = 1.0f;
		intmode = true;
	}
	
	public int init( String[] argv ) {
		if ( argv != null ) {
			for ( int i = 0; i < argv.length; i++ ) {
				if ( argv[i] == null ) {
				} else if ( argv[i].equals( "hist-lowfirst" )) {
					lowfirst = true;
				}
			}
		}
		return super.init( argv );
	}
	/**
		Constructor creates a histogram with some number of buckets evenly distributed with the min rating and max rating at the limits.
<pre>
   0    1    2    3    4
 |----|----|----|----|----|
min                      max
5 buckets each with a width of (buckets/(max-min))
</pre>
	 Values less than min are accumulated in the min bucket and values greater than max are accumulated in the max bucket.
	 @param numBuckets number of buctets to accumulate histogram data in
	 @param minRating value of the lowest edge of the lowest bucket
	 @param maxRating value of the highest edge of the highest bucket
	 */
	 public Histogram( int numBuckets, float minRating, float maxRating ) {
		buckets = numBuckets;
		min = minRating;
		max = maxRating;
		step = buckets/(max-min);
		/*System.out.print("step="+step+", "+min);
		for ( int i = 1; i <= buckets; i++ ) {
			System.out.print(" .. ");
			System.out.print( (min + i/step) );
		}
		System.out.println();*/
	}
	 /**
		 Constructor creates a histogram with some number of buckets to contain the possible integer values including minint through maxint.
<pre>
   0    1    2    3    4
 |----|----|----|----|----|
min                      max
5 buckets
minint 0, maxint 4
min = -0.5, max = 4.5
</pre>
	  @param minint value of lowest bucket
	  @param maxint value of highest bucket
	  */
	 public Histogram( int minint, int maxint ) {
		/*
		   0    1    2    3    4
		 |----|----|----|----|----|
		min                      max
		 5 buckets
		 minint 0, maxint 4
		 min = -0.5, max = 4.5
		 */
		buckets = maxint - minint + 1;
		step = 1.0f;
		min = (float)(minint - 0.5);
		max = (float)(maxint + 0.5);
		intmode = true;
		/*
		System.out.print("step="+step+", "+min);
		for ( int i = 1; i <= buckets; i++ ) {
			System.out.print(" .. ");
			System.out.print( (min + i/step) );
		}
		System.out.println("\nintmode true");
		 */
	}
	 /**
		 The normal vote interface in this case accumulates the ratings into histograms per name.
		 */
	public void voteRating( NameVote[] vote ) {
		for ( int i = 0; i < vote.length; i++ ) {
			if ( ! Float.isNaN( vote[i].rating ) ) {
				sum += vote[i].rating; votes++;
				if ( vote[i].rating < minRecorded ) {
					minRecorded = vote[i].rating;
				}
				if ( vote[i].rating > maxRecorded ) {
					maxRecorded = vote[i].rating;
				}
				Choice c = (Choice)they.get( vote[i].name );
				if ( c == null ) {
					c = new Choice( vote[i].name, buckets );
					they.put( vote[i].name, c );
				}
				c.vote( vote[i].rating );
			}
		}
	}
	protected String[] names() {
		String[] names = new String[they.size()];
		Iterator ni = they.keySet().iterator();
		int i = 0;
		while ( ni.hasNext() ) {
			names[i] = (String)ni.next();
			i++;
		}
		if ( i != names.length ) {
			System.err.println( "they.size() lied and returned " + names.length + " but we only got " + (i-1) + " names" );
			return null;
		}
		return names;
	}
	protected Choice[] getChoices() {
		Choice[] toret = new Choice[they.size()];
		Iterator ni = they.values().iterator();
		int i = 0;
		while ( ni.hasNext() ) {
			toret[i] = (Choice)ni.next();
			i++;
		}
		return toret;
	}
	protected int[][] counts( String[] names ) {
		int[][] toret = new int[names.length][];
		for ( int i = 0; i < names.length; i++ ) {
			Choice c = (Choice)they.get( names[i] );
			if ( c != null ) {
				toret[i] = c.counts;
			}
		}
		return toret;
	}
	 /**
	  @return average ratings for each name
	  */
	public NameVote[] getWinners() {
		if ( disableWinners ) {
			return null; 
		}
		NameVote[] toret = new NameVote[they.size()];
		Iterator ni = they.values().iterator();
		int i = 0;
		while ( ni.hasNext() ) {
			Choice c = (Choice)ni.next();
			toret[i] = new NameVote( c.name, (float)(c.sum / c.votes) );
			i++;
		}
		java.util.Arrays.sort( toret, new NameVotingSystem.ReverseComparator() );
		return toret;
	}

	int outerTableCols = 4;
	public String barImgUrl = "b.png";
	public boolean useSpan = true;
	public boolean printPercents = false;
    int MAX_WIDTH = 100;
    boolean MAXIMIZE_WIDTH = true;
	
	public static final String HIST_ROW_VALUES_LABEL = "%valueslabel";
	public static final String HIST_ROW_SCALED_HVAL = "%scaledhval";
	public static final String HIST_ROW_HVAL = "%hval";
	public static final String[] HIST_ROW_keys = { HIST_ROW_VALUES_LABEL, HIST_ROW_SCALED_HVAL, HIST_ROW_HVAL };
	protected static String[] globalHistRowFormat = {
		"<tr><td>",
		HIST_ROW_VALUES_LABEL,
		"</td><td><div style=\"background-color: #bb99ff; width: ",
		HIST_ROW_SCALED_HVAL,
		"\">",
		HIST_ROW_HVAL,
		"</div></td></tr>\n"
	};
/**
	Set default format for histogram rows.
<p>row template:<br>
%valueslabel	n,>=n,<n,[i..j)<br>
%hval<br>
%scaledhval</p>

<p>default is:<br>
<tt>&lt;tr&gt;&lt;td&gt;%valuesLabel&lt;/td&gt;&lt;td&gt;&lt;div style="background-color: #bb99ff; width: %scaledhval"&gt;%hval&lt;/div&gt;&lt;/td&gt;&lt;/tr&gt;\n</tt></p>
@param format string which will be parsed with above %keys to set histogram row format
*/
/*
default is: (without html mangling)
<tr><td>%valuesLabel</td><td><div style="background-color: #bb99ff; width: %scaledhval">%hval</div></td></tr>\n
*/
	public static void setGlobalHistRowFormat( String format ) {
		globalHistRowFormat = parseFormat( format, HIST_ROW_keys );
	}
	protected static String[] parseFormat( String format, String[] keys ) {
		ArrayList tf = new ArrayList();
		int pos = 0;
		int[] keypos = new int[keys.length];
		while ( pos < format.length() ) {
			int mkpos = -1;
			int mki = -1;
			for ( int i = 0; i < keys.length; i++ ) {
				keypos[i] = format.indexOf( keys[i], pos );
				if ( keypos[i] != -1 ) {
					if ( mki == -1 ) {
						mki = i;
						mkpos = keypos[i];
					} else if ( keypos[i] < mkpos ) {
						mki = i;
						mkpos = keypos[i];
					}
				}
			}
			if ( mki == -1 ) {
				// no keys found, consume the rest of the format as string constant
				tf.add( format.substring( pos ) );
				break;
			}
			tf.add( format.substring( pos, mkpos ) );
			tf.add( keys[mki] );
			pos = mkpos + keys[mki].length();
		}
		String[] nf = new String[tf.size()];
		for ( int i = 0; i < tf.size(); i++ ) {
			nf[i] = (String)tf.get( i );
		}
		return nf;
	}
	protected void formatHistRow( StringBuffer sb, int i, int hval, double scale ) {
		formatHistRow( sb, i, hval, scale, globalHistRowFormat );
	}
	protected void formatHistRow( StringBuffer sb, int i, int hval, double scale, String[] histRowFormat ) {
		String valueLabel = null;
		float lmin = min;
		float lmax = max;
		float lstep = step;
		if ( doscaleoffset ) {
			lmin = lmin * bucketscale + offset;
			lmax = lmax * bucketscale + offset;
			lstep = lstep * bucketscale;
		}
		if ( intmode ) {
			valueLabel = Integer.toString( (int)Math.rint(i*lstep + lmin + 0.5) );
		} else if ( i == buckets - 1 ) {
			valueLabel = ">= " + (lmax - (1.0/lstep));
		} else if ( i == 0 ) {
			valueLabel = "< " + (lmin + (1.0/lstep));
		} else {
			valueLabel = "[" + (lmin + (i/lstep)) + " .. " + (lmin + ((i+1)/lstep)) + ")";
		}
		for ( int j = 0; j < histRowFormat.length; j++ ) {
			String fe = histRowFormat[j];
			if ( fe == HIST_ROW_VALUES_LABEL ) {
				sb.append( valueLabel );
			} else if ( fe == HIST_ROW_SCALED_HVAL ) {
				sb.append( (int)(hval * scale) );
			} else if ( fe == HIST_ROW_HVAL ) {
				sb.append( hval );
			} else {
				sb.append( fe );
			}
		}
	}
	protected void histRow( StringBuffer sb, int i, int hval, double scale ) {
		String valueLabel = null;
		float lmin = min;
		float lmax = max;
		float lstep = step;
		if ( doscaleoffset ) {
			lmin = lmin * bucketscale + offset;
			lmax = lmax * bucketscale + offset;
			lstep = lstep * bucketscale;
		}
		if ( intmode ) {
			valueLabel = Integer.toString( (int)Math.rint(i*lstep + lmin + 0.5) );
		} else if ( i == buckets - 1 ) {
			valueLabel = ">= " + (lmax - (1.0/lstep));
		} else if ( i == 0 ) {
			valueLabel = "< " + (lmin + (1.0/lstep));
		} else {
			valueLabel = "[" + (lmin + (i/lstep)) + " .. " + (lmin + ((i+1)/lstep)) + ")";
		}
		if ( printPercents ) {
			sb.append( "<tr><td>" ).append( valueLabel ).append( "</td><td>" ).append( hval ).append( "</td></tr>\n" );
		} else {
			sb.append( "<tr><td>" ).append( valueLabel );
			if ( useSpan ) {
				sb.append( "</td><td><div style=\"background-color: #bb99ff; width: " ).append( (int)(hval * scale) ).append( "\">" ).append( hval ).append( "</div></td></tr>\n" );
			} else {
				sb.append( "</td><td><img src=\"").append( barImgUrl ).append("\" height=\"10\" width=\"" ).append( (int)(hval * scale) ).append( "\"> " ).append( hval ).append( "</td></tr>\n" );
			}
		}
	}

	public static final String HIST_CHOICE_NAME = "%name";
	public static final String HIST_CHOICE_LABEL = "%label";
	public static final String HIST_CHOICE_ROWS = "%rows";
	public static final String HIST_CHOICE_SUM = "%sum";
	public static final String HIST_CHOICE_VOTES = "%votes";
	public static final String HIST_CHOICE_AVERAGE = "%average";
	public static final String HIST_CHOICE_keys[] = {
		HIST_CHOICE_NAME, HIST_CHOICE_LABEL, HIST_CHOICE_ROWS,
		HIST_CHOICE_SUM, HIST_CHOICE_VOTES, HIST_CHOICE_AVERAGE
	};
	protected static String[] globalHistChoiceFormat = {
		"<table border=\"0\"><tr><th colspan=\"2\">",
		HIST_CHOICE_NAME,
		"</th></tr><tr><th>",
		HIST_CHOICE_LABEL,
		"</th><th>Votes</th></tr>",
		HIST_CHOICE_ROWS,
		"<tr><td>votes</td><td>",
		HIST_CHOICE_VOTES,
		"</td></tr><tr><td>average</td><td>",
		HIST_CHOICE_AVERAGE,
		"</td></tr></table>"
	};
	public StringBuffer formatChoice( StringBuffer sb, Choice c, int minBucket, int maxBucket, double scale, String[] format ) {
		for ( int i = 0; i < format.length; i++ ) {
			if ( format[i] == HIST_CHOICE_NAME ) {
				sb.append( c.name );
			} else if ( format[i] == HIST_CHOICE_LABEL ) {
				sb.append( labelname );
			} else if ( format[i] == HIST_CHOICE_ROWS ) {
				int[] cc = c.counts;
				if ( lowfirst ) {
					for ( int j = minBucket; j <= maxBucket; j++ ) {
						int hval;
						hval = cc[j];
						formatHistRow( sb, j, hval, scale );
					}
				} else {
					for ( int j = maxBucket; j >= minBucket; j-- ) {
						int hval;
						hval = cc[j];
						formatHistRow( sb, j, hval, scale );
					}
				}
			} else if ( format[i] == HIST_CHOICE_VOTES ) {
				sb.append( c.votes );
			} else if ( format[i] == HIST_CHOICE_AVERAGE ) {
				sb.append( c.sum / c.votes );
			} else if ( format[i] == HIST_CHOICE_SUM ) {
				sb.append( c.sum );
			} else {
				sb.append( format[i] );
			}
		}
		return sb;
	}

/*
choice template:
%name
%label	Rankings / Ratings
%rows
%sum
%votes
%average

default is:
<table border="0"><tr><th colspan="2">%name</th></tr><tr><th>%label</th><th>Votes</th></tr>
%rows
<tr><td>votes</td><td>%votes</td></tr><tr><td>average</td><td>%average</td></tr></table>
*/
	public StringBuffer renderChoice( StringBuffer sb, Choice c, int minBucket, int maxBucket, double scale ) {
		sb.append( "<table><tr><th colspan=\"2\">" );
		sb.append( c.name ).append( '\n' );
		sb.append( "</th></tr><tr><th>" );
		sb.append( labelname );
		sb.append( "</th><th>Votes</th></tr>\n" );
		int[] cc = c.counts;
		int total = 0;
		if ( lowfirst ) {
			for ( int i = minBucket; i <= maxBucket; i++ ) {
				int hval;
				hval = cc[i];
				total += hval;
				histRow( sb, i, hval, scale );
			}
		} else {
			for ( int i = maxBucket; i >= minBucket; i-- ) {
				int hval;
				hval = cc[i];
				total += hval;
				histRow( sb, i, hval, scale );
			}
		}
		sb.append( "<tr><td>votes</td><td>" ).append( total );
		sb.append( "</td></tr><tr><td>average</td><td>" );
		sb.append( c.sum / c.votes ).append( "</td></tr></table>\n" );
		return sb;
	}

	/**
		Makes HTML tables and CSS span bars to show the histogram.
		@param sb gets HTML summary appended to it.
	 @return sb with HTML summary appended to it.
		*/
	public StringBuffer htmlSummary( StringBuffer sb ) {
		return htmlSummary( sb, (String[])null );
	}
	/**
		Makes HTML tables and CSS span bars to show the histogram.
	 @param sb gets HTML summary appended to it.
	 @param winners Histogram will present the choices in the order they are in this array of winners (typically sorted winningest first).
	 @return sb with HTML summary appended to it.
	 */
	public StringBuffer htmlSummary( StringBuffer sb, NameVote[] winners ) {
		if ( winners == null ) {
			return htmlSummary( sb, (String[])null );
		}
		String[] names = new String[winners.length];
		for ( int i = 0; i < winners.length; i++ ) {
			names[i] = winners[i].name;
		}
		return htmlSummary( sb, names );
	}
	/**
		Makes HTML tables and CSS span bars to show the histogram.
	 @param sb gets HTML summary appended to it.
	 @param names which names to get histogram for, in that order.
	 @return sb with HTML summary appended to it.
	 */
	public StringBuffer htmlSummary( StringBuffer sb, String[] names ) {
		Choice[] choices;
		if ( names == null ) {
			choices = getChoices();
			java.util.Arrays.sort( choices, new NameVotingSystem.ReverseComparator() );
		} else {
			choices = new Choice[names.length];
			for ( int i = 0; i < names.length; i++ ) {
				choices[i] = (Choice)they.get(names[i]);
			}
		}
		double scale = 1.0;
		if ( MAXIMIZE_WIDTH || maxbucket > MAX_WIDTH ) {
			scale = ((double)MAX_WIDTH) / ((double)maxbucket);
		}
		int oTC = 0;
		if ( outerTableCols > 1 ) {
			sb.append( "<table border=\"1\">" );
		}
		int maxBucket = (int)Math.floor( step * (maxRecorded - min) );
		if ( maxBucket > buckets - 1 ) {
			maxBucket = buckets - 1;
		}
		int minBucket = (int)Math.floor( step * (minRecorded - min) );
		if ( minBucket < 0 ) {
			minBucket = 0;
		}
		for ( int c = 0; c < choices.length; c++ ) {
			if ( choices[c] == null ) {
				// there could be a name passed in not histogrammed
				continue;
			}
			if ( outerTableCols > 1 ) {
				if ( oTC == 0 ) {
					sb.append( "<tr>" );
				}
				sb.append( "<td width=\"" ).append( 100 / outerTableCols ).append( "%\">" );
			}
			renderChoice( sb, choices[c], minBucket, maxBucket, scale );
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
			sb.append( "</table>" );
		}
		return sb;
	}
	public String name() {
		return "Histogram";
	}
	public static void main( String[] argv ) {
		(new Histogram()).defaultMain( argv );
	}
	
	static {
		registerImpl( "Histogram", Histogram.class );
	}
};
