package org.bolson.vote;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class NamedApproval extends NameVotingSystem {
	/* public int init( String[] argv ) {
	} */
	HashMap they = new HashMap();

	NameVote[] winners = null;
	
	public void voteRating( NameVote[] vote ) {
		winners = null;
		for ( int i = 0; i < vote.length; i++ ) {
			if ( ! Float.isNaN( vote[i].rating ) ) {
				int[] r = (int[])they.get( vote[i].name );
				if ( r == null ) {
					r = new int[1];
					they.put( vote[i].name, r );
				}
				if ( vote[i].rating > 0 ) {
					r[0]++;
				}
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
			int[] r;
			name = (String)e.getKey();
			r = (int[])e.getValue();
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
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>Approval Votes</th></tr>" );
		for ( int i = 0; i < t.length; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( t[i].name );
			sb.append( "</td><td>" );
			sb.append( t[i].rating );
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Approval";
	}
	
	static {
		registerImpl( "Approval", NamedApproval.class );
	}
};
