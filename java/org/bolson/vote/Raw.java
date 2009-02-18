package org.bolson.vote;

import java.util.Map;
import java.util.TreeMap;
import java.util.Iterator;

/**
 Raw sum of ratings, highest wins.
 @author Brian Olson
 */
public class Raw extends NameVotingSystem implements SummableVotingSystem, java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private void writeObject(java.io.ObjectOutputStream oos) throws java.io.IOException {
		oos.writeLong(serialVersionUID);
		oos.writeObject(they);
	}
	private void readObject(java.io.ObjectInputStream ois) throws java.io.IOException, ClassNotFoundException {
		ois.readLong();
		they = (TreeMap)ois.readObject();
	}

	TreeMap they = new TreeMap();
	NameVote[] winners = null;

	/** Add a partial tally into this instance.
	 @param other another Raw.
	 @throws ClassCastExepcion if other isn't a Raw.
	 */
	public void accumulateSubVote( SummableVotingSystem other ) throws ClassCastException {
		if ( other == null ) return;
		if ( ! (other instanceof Raw) ) {
			throw new ClassCastException("don't know how to add "+other.getClass().getName()+" into "+this.getClass().getName() );
		}
		Raw it = (Raw)other;
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

	/**
	 Adds up ratings. Does not keep a copy of the vote.
	 @param vote ballot data.
	 */
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
		java.util.Arrays.sort(toret);
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
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Raw Rating Summation";
	}
	
	static {
		registerImpl( "Raw", Raw.class );
	}
};
