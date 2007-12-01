package org.bolson.vote;
import java.util.Vector;
import java.util.HashMap;

public class NamedIRV extends NameVotingSystem {
	HashMap they = new HashMap();
	NameVote[] winners = null;
	Vector votes = new Vector();
	Vector deadVotes = new Vector();
	Vector tiedVotes = new Vector();

	public NamedIRV() {
	}

	protected static class TallyState {
		public String name;
		public double tally = 0.0;
		public boolean active = true;
		public Vector votes = new Vector();
		public TallyState( String nin ) {
			name = nin;
		}
	}
	
	/*public int init( String[] argv ) {}*/
	
	TallyState get( String name ) {
		TallyState v = (TallyState)they.get( name );
		if ( v == null ) {
			v = new TallyState( name );
			they.put( name, v );
		}
		return v;
	}
	public void voteRating( NameVote[] vote ) {
		if ( vote == null || vote.length == 0 ) {
			return;
		}
		winners = null;
		for ( int i = 0; i < vote.length; i++ ) {
			// causes creation of TallyState for name if not already existing
			TallyState v = get( vote[i].name );
		}
		votes.add( vote );
	}
	void bucketize( NameVote[] vote ) {
		float max = Float.NaN;
		int tied = 1;
		int i = 0;
		TallyState v = null, maxv = null;
		for ( ; i < vote.length; i++ ) {
			v = get( vote[i].name );
			if ( v.active ) {
				maxv = v;
				max = vote[i].rating;
				i++;
				break;
			}
		}
		if ( maxv == null ) {
			// none of the names in this vote are active
			deadVotes.add( vote );
			return;
		}
		for ( ; i < vote.length; i++ ) {
			v = get( vote[i].name );
			if ( ! v.active ) {
				// ignore
			} else if ( vote[i].rating > max ) {
				max = vote[i].rating;
				maxv = v;
				tied = 1;
			} else if ( vote[i].rating == max ) {
				tied++;
			}
		}
		if ( tied == 1 ) {
			maxv.votes.add( vote );
		} else {
			double fract = 1.0 / tied;
			for ( i = 0; i < vote.length; i++ ) {
				if ( vote[i].rating == max ) {
					v = get( vote[i].name );
					v.tally += fract;
				}
			}
			tiedVotes.add( vote );
		}
	}
	public NameVote[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		TallyState[] tv = new TallyState[they.size()];
		int i = 0;
		java.util.Iterator ti = they.values().iterator();
		votes.addAll( deadVotes );
		deadVotes.clear();
		int numActive = 0;
		while ( ti.hasNext() ) {
			TallyState ts = (TallyState)ti.next();
			votes.addAll( ts.votes );
			ts.votes.clear();
			ts.tally = 0;
			ts.active = true;
			numActive++;
			tv[i] = ts;
			i++;
		}
		while ( votes.size() > 0 ) {
		    bucketize( (NameVote[])votes.remove(votes.size()-1) );
		}
		winners = new NameVote[tv.length];
		// numActive == tv.length
		while ( numActive > 1 ) {
			double min = 0;
			int mini = 0;
			for ( i = 0; i < tv.length; i++ ) {
				if ( tv[i].active ) {
					min = tv[i].votes.size() + tv[i].tally;
					mini = i;
					break;
				}
			}
			for ( ; i < tv.length; i++ ) {
				double tm = tv[i].votes.size() + tv[i].tally;
				if ( tv[i].active && (tm < min) ) {
					min = tm;
					mini = i;
				}
			}
			tv[mini].active = false;
			numActive--;
			winners[numActive] = new NameVote( tv[mini].name, (float)min );
			if ( numActive > 1 ) {
				// redistribute disqualified votes
				while ( tv[mini].votes.size() > 0 ) {
					bucketize( (NameVote[])tv[mini].votes.remove(tv[mini].votes.size()-1) );
				}
				for ( i = 0; i < tv.length; i++ ) {
					if ( tv[i].active ) {
						tv[i].tally = 0;
					}
				}
				Vector oldTied = tiedVotes;
				tiedVotes = new Vector();
				while ( oldTied.size() > 0 ) {
					bucketize( (NameVote[])oldTied.remove(oldTied.size()-1) );
				}
			}
		}
		for ( i = 0; i < tv.length; i++ ) {
			if ( tv[i].active ) {
				break;
			}
		}
		if ( i >= tv.length || tv[i] == null ) {
			return new NameVote[0];
		}
		winners[0] = new NameVote( tv[i].name, (float)(tv[i].votes.size() + tv[i].tally) );
		return winners;
	}
	public StringBuffer htmlSummary( StringBuffer sb ) {
		NameVote[] t;
		t = getWinners();
		if ( t == null || t.length == 0 || t[0] == null ) {
			return sb;
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>Best IRV Count</th></tr>" );
		for ( int i = 0; i < t.length; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( t[i].name );
			sb.append( "</td><td>" );
			if ( Math.round( t[i].rating ) == t[i].rating ) {
				sb.append( (long)(t[i].rating) );
			} else {
				sb.append( t[i].rating );
			}
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Instant Runoff Voting";
	}
	
	static {
		registerImpl( "IRV", NamedIRV.class );
	}
};
