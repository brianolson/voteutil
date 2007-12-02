package org.bolson.vote;

import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;

/**
 Raw sum of ratings, highest wins.
 @author Brian Olson
 */
public class NamedRaw extends NameVotingSystem implements SummableVotingSystem, java.io.Serializable {
	/* public int init( String[] argv ) {
	} */
	TreeMap they = new TreeMap();
	NameVote[] winners = null;

	public void accumulateSubVote( SummableVotingSystem other ) throws ClassCastException {
		if ( other == null ) return;
		if ( ! (other instanceof NamedRaw) ) {
			throw new ClassCastException("don't know how to add "+other.getClass().getName()+" into "+this.getClass().getName() );
		}
		NamedRaw it = (NamedRaw)other;
		winners = null;
		Iterator ti = it.they.entrySet().iterator();
		while ( ti.hasNext() ) {
			Map.Entry e;
			e = (Map.Entry)ti.next();
			String name;
			double[] ri;
			name = (String)e.getKey();
			ri = (double[])e.getValue();
			if ( ! Double.isNaN( ri[0] ) ) {
				double[] r = (double[])they.get( name );
				if ( r == null ) {
					r = new double[1];
					r[0] = 0.0;
					they.put( name, r );
				}
				r[0] += ri[0];
			}
		}
	}

	public void voteRating( NameVote[] vote ) {
		winners = null;
		for ( int i = 0; i < vote.length; i++ ) {
			if ( ! Double.isNaN( vote[i].rating ) ) {
				double[] r = (double[])they.get( vote[i].name );
				if ( r == null ) {
					r = new double[1];
					r[0] = 0.0;
					they.put( vote[i].name, r );
				}
				r[0] += vote[i].rating;
			}
		}
	}
	public NameVote[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		NameVote[] toret = new NameVote[they.size()];
		Iterator ti = they.entrySet().iterator();
		int i = 0;
		while ( ti.hasNext() ) {
			Map.Entry e;
			e = (Map.Entry)ti.next();
			String name;
			double[] r;
			name = (String)e.getKey();
			r = (double[])e.getValue();
			toret[i] = new NameVote( name, (float)(r[0]) );
			i++;
		}
		// selection sort
		for ( i = 0; i < toret.length; i++ ) {
			for ( int j = i + 1; j < toret.length; j++ ) {
				if ( toret[i].rating < toret[j].rating ) {
					NameVote t = toret[i];
					toret[i] = toret[j];
					toret[j] = t;
				}
			}
		}
		winners = toret;
		return toret;
	}
	public static java.text.DecimalFormat ratingFormat = new java.text.DecimalFormat( "0.00" );
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>Raw Rating Summation</th></tr>" );
		for ( int i = 0; i < t.length; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( t[i].name );
			sb.append( "</td><td>" );
			ratingFormat.format( t[i].rating, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
			//sb.append( t[i].rating );
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Raw Rating Summation";
	}
	
	static {
		registerImpl( "Raw", NamedRaw.class );
	}
};
