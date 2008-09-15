package org.bolson.vote;
/**
 * Top level abstract class for ranked or rated voting systems.
 * Most implementations will subclass either RankedVotingSystem or
 * RatedVotingSystem.
 * @see RankedVotingSystem
 * @see RatedVotingSystem
 @author Brian Olson
 */

public abstract class VotingSystem {
    /** number of candidates */
    protected int numc;

    /** In addition to interfaced methods, there shall be a constructor taking 
     * the integer number N of choices to be voted on. */
    public VotingSystem( int numCandidates ) {
	numc = numCandidates;
    };
	
	
	/** Debug info collecting flag.
		If true, then an implementation should collect data that can be retreived by getDebugText() and getDebugHTML()
		@see #getDebugText(String[])
		@see #getDebugHTML(String[])
		*/
	public boolean debug;
	
	/** Return textual representation of debug data.
		Default implementation returns null.
		@see #debug
		@param names Symbolic names for the choices makes human debugging easier. May be null.
		@return debug text (default=null) */
	public String getDebugText( String[] names ) {
		return null;
	}
	
	/** Return HTML representation of debug data.
		Default implementation is: "&lt;PRE&gt;"+getDebugText()+"&lt;/PRE&gt;"
		(except that this method returns null or the empty string if getDebugText() returns null or the empty string).
		@see #debug
		@param names Symbolic names for the choices makes human debugging easier. May be null.
		@return debug HTML */
	public String getDebugHTML( String[] names ) {
		String dbt = getDebugText( names );
		if ( dbt == null ) {
			return null;
		}
		if ( dbt.equals( "" ) ) {
			return dbt;
		}
		return "<PRE>"+dbt+"</PRE>";
	}

	/**
	 * Arguments to modify behavior of VotingSystem
	 * @return this so that you can do
	 <pre>vs = (new VS(numc)).init( new String[]{"blah", "grunt"} );</pre>
	 */
	public VotingSystem init( String argv[] ) {
		// default impl, do nothing
		return this;
	}
	
	/** accessor to read only variable
	 * @return numc
	 */
	public int getNumberOfCandidates() {
		return numc;
	}

    /**
     * @param rating An array int[numc].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public abstract int voteRating( int[] rating );
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public abstract int voteRating( float[] rating );
    /**
     * @param rating An array int[N].
     *	rating[i] is the rating of choice i.
     *  A rating can be any int value. Higher means more preferred.
     *  NO_VOTE for unrated.
     * @return 0 on success
     */
    public abstract int voteRating( double[] rating );
	/** 
		@param rating An array of int, float or double
		@return 0 on success
		@see #voteRating(int[])
		@see #voteRating(float[])
		@see #voteRating(double[])
		*/
	public int voteRating( Object rating ) throws ClassCastException {
		if ( rating instanceof int[] ) {
			return voteRating( (int[])rating );
		} else if ( rating instanceof float[] ) {
			return voteRating( (float[])rating );
		} else if ( rating instanceof double[] ) {
			return voteRating( (double[])rating );
		}
		throw new ClassCastException("rating not int, float or double array");
	}
    /**
     * @param ranking An array int[N].
     *	ranking[i] is the ranking of choice i.
     *  rankings are 1 (most preferred) through N (least).
     *  INT_MAX, N, or NO_VOTE for unspecified.
     * @return 0 on success
     */
    public abstract int voteRanking( int ranking[] );

    /**
     * squash (sum of absolute value of vote) to (max - min)
     * @param rating vote to check
     * @param min minimum (inclusive) value allowed for rating
     * @param max maximum (inclusive) value allowed for rating
     * @return true if rating.length == numc AND all values in rating are >= min and <= max
     */
    public boolean checkRatedVote( int rating[], int min, int max ) {
	if ( rating.length != numc ) return false;
	for ( int i = 0; i < rating.length; i++ ) {
	    if ( rating[i] == NO_VOTE ) {
		// OK
	    } else if ( rating[i] < min ) {
		return false;
	    } else if ( rating[i] > max ) {
		return false;
	    } // else OK
	}
	// didn't find anything bad, must be good
	return true;
    }
	
	/** Divide values by their average
		@param ratings take average, then devide each element by the average */
	public static void normalize( double[] ratings ) {
		normalize( ratings, ratings );
	}
	/** Divide values by their average
		@param src values to normalize
		@param dest receives normalized version of src */
	public static void normalize( double[] dest, double[] src ) {
		double sum = 0.0;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			sum += src[i];
		}
		sum = 1.0 / sum;
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
	/** Divide values by their average
		@param ratings take average, then devide each element by the average */
	public static void normalize( float[] ratings ) {
		normalize( ratings, ratings );
	}
	public static void normalize( float[] dest, float[] src ) {
		float sum = 0.0f;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			sum += src[i];
		}
		sum = 1.0f / sum;
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
	/** Divide values by their average
		@param src values to normalize
		@param dest receives normalized version of src */
	public static void normalize( double[] dest, int[] src ) {
		double sum = 0.0;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			sum += src[i];
		}
		sum = 1.0 / sum;
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
	/** Calculate "L2" norm. Divide values by sqrt( sum of squares ).
		@param ratings take sqrt of the sum of squares, then devide each element by the that */
	public static void normalizeL2( double[] ratings ) {
		normalizeL2( ratings, ratings );
	}
	/** Calculate "L2" norm. Divide values by sqrt( sum of squares ).
		@param src values to normalize
		@param dest receives normalized version of src */
	public static void normalizeL2( double[] dest, double[] src ) {
		double sum = 0.0;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			double ts;
			ts = src[i];
			sum += ts * ts;
		}
		sum = 1.0 / Math.sqrt( sum );
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
	/** Calculate "L2" norm. Divide values by sqrt( sum of squares ).
		@param ratings take sqrt of the sum of squares, then devide each element by the that */
	public static void normalizeL2( float[] ratings ) {
		normalizeL2( ratings, ratings );
	}
	/** Calculate "L2" norm. Divide values by sqrt( sum of squares ).
		@param src values to normalize
		@param dest receives normalized version of src */
	public static void normalizeL2( float[] dest, float[] src ) {
		float sum = 0.0f;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			float ts;
			ts = src[i];
			sum += ts * ts;
		}
		sum = (float)(1.0 / Math.sqrt( sum ));
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
	/** Calculate "L2" norm. Divide values by sqrt( sum of squares ).
		@param src values to normalize
		@param dest receives normalized version of src */
	public static void normalizeL2( double[] dest, int[] src ) {
		double sum = 0.0;
		int i;
		for ( i = 0; i < src.length; i++ ) {
			double ts;
			ts = src[i];
			sum += ts * ts;
		}
		sum = 1.0 / Math.sqrt( sum );
		for ( i = 0; i < src.length; i++ ) {
			dest[i] = src[i] * sum;
		}
	}
    /**
     * Checks that rating is valid.
     * @param rating vote to check
     * @param min minimum (inclusive) value allowed for rating
     * @param max maximum (inclusive) value allowed for rating
     * @return true if rating.length == numc AND all values in rating are >= min and <= max
     */
    public boolean isValidRatedVote( int rating[], int min, int max ) {
	if ( rating.length != numc ) return false;
	for ( int i = 0; i < rating.length; i++ ) {
	    if ( rating[i] == NO_VOTE ) {
		// OK
	    } else if ( rating[i] < min ) {
		return false;
	    } else if ( rating[i] > max ) {
		return false;
	    } // else OK
	}
	// didn't find anything bad, must be good
	return true;
    }
    /**
     * Checks that ranking is valid.
     * @param ranking vote to check
     * @return true if rating.length == numc AND all values in rating are >= 1 and <= numc
     */
    public boolean isValidRankedVote( int ranking[] ) {
	if ( ranking.length != numc ) return false;
	for ( int i = 0; i < ranking.length; i++ ) {
	    if ( ranking[i] == NO_VOTE ) {
		// OK
	    } else if ( ranking[i] < 1 ) {
		return false;
	    } else if ( ranking[i] > numc ) {
		return false;
	    } // else OK
	}
	// didn't find anything bad, must be good
	return true;
    }

	/** 
		In place threshold rating at 0. where ( rating[i] >= 0 ) rating = 1; else rating = 0;
		@param rating values to squash, modified in place.
		*/
    void squashToAcceptance( int rating[] ) {
		for ( int i = 0; i < numc; i++ ) {
			if ( rating[i] >= 0 ) {
				rating[i] = 1;
			} else {
				rating[i] = 0;
			}
		}
    }
	/**
		Maximize a vote between bounds.
		Multiply and shift so that the lowest rating becomes equal to tmin and the highest rating becomes equal to tmax.
	 @param vote ratings
	 @param tmin target minimum value
	 @param tmax target maximum value
	 */
	public static void stretch( double[] vote, float tmin, float tmax ) {
		double min = Double.MAX_VALUE;
		double max = -1.0f * Double.MAX_VALUE;
		for ( int i = 0; i < vote.length; i++ ) {
			if ( vote[i] < min ) {
				min = vote[i];
			}
			if ( vote[i] > max ) {
				max = vote[i];
			}
		}
		double scale = max - min;
		scale = (tmax - tmin) / scale;
		double shift = (max + min) / 2.0;
		shift *= scale;
		shift = ((tmax + tmin)/2.0) - shift;
		for ( int i = 0; i < vote.length; i++ ) {
			vote[i] = (double)((vote[i] * scale) + shift);
		}
	}
    
    /**
		Do processing if necessary, return winners.
	 <p>It is recommended to cache the return value in such a way that calling this function again does not do complex recalculation.
	 That cache should be cleared if voteRating or voteRanking is called.
     * @return indecies of winners (hopefully 1 of them)
     */
    public abstract int[] getWinners();

    /**
		Print verbose explaination of internal process as it happens.
	 By default, do no printing and return result of getWinners() .
	 * @param out where to print
	 * @return indecies of winners, hopefully 1 of them but in case of ties there may be more. Check .length to be sure.
	 @deprecated use getWinners(), debug, and getDebugText() or getDebugHTML()
	 @see #getWinners()
	 @see #debug
	 @see #getDebugText(String[])
	 @see #getDebugHTML(String[])
     */
    public int[] getWinners( java.io.PrintWriter out ) {
		int[] toret;
		toret = getWinners();
		if ( debug ) {
			out.println( getDebugText( null ) );
		}
		return toret;
	}
	
    /**
		Do processing if necessary, return winners.
	 Default implementation returns null so that not all systems have to be multi-seat systems.
	 * @param out where to print news and diagnostics
	 * @param numSeats the number of seats available. 
     * @return indecies of winners, hopefully numSeats of them but in case of ties there may be more and in case of some internal schism there may be fewer. Check .length to be sure.
	 @deprecated use getWinners(int), debug, and getDebugText() or getDebugHTML()
	 @see #getWinners(int)
	 @see #debug
	 @see #getDebugText(String[])
	 @see #getDebugHTML(String[])
     */
    public int[] getWinners( java.io.PrintWriter out, int numSeats ) {
		int[] toret;
		toret = getWinners( numSeats );
		if ( toret == null ) {
			return toret;
		}
		if ( debug ) {
			out.println( getDebugText( null ) );
		}
		return toret;
	}
	
    /**
		Do processing if necessary, return winners.
	 Default implementation returns null so that not all systems have to be multi-seat systems.
	 <p>It is recommended to cache the return value in such a way that calling this function again does not do complex recalculation.
	 That cache should be cleared if voteRating or voteRanking is called.
	 * @param numSeats the number of seats available. 
     * @return indecies of winners, hopefully numSeats of them but in case of ties there may be more and in case of some internal schism there may be fewer. Check .length to be sure.
     */
    public int[] getWinners( int numSeats ) {
		return null;
	}
	
    /**
     * A more interesting representation.
     * Include the names of the choices voted upon in the representation.
     * @param names The names of the choices.
     * @return state, with names!
     */
    public abstract String toString( String names[] );

    /**
     * A fancy html representation.
     * Include the names of the choices voted upon in the representation.
     * @param names The names of the choices. May be null.
     * @return state, with names!
     */
    public abstract String htmlSummary( String names[] );

	/** Return name of this voting system.
		This is a function so that it can potentially construct a detailed name based on options set in init().
		@return a descriptive name
		*/
	public String name() {
		return toString();
	}
    /**
     * This value represents that the voter made no action on some choice.
     * This is the default value. On a YES/NO or HOW MUCH the answer was no answer.
	 * For float and double votes NaN is the NO_VOTE value.
     */
    public static final int NO_VOTE = 0x80000000;
}
