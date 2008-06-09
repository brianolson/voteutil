package org.bolson.vote;

import java.util.HashMap;
import java.util.ArrayList;

/**
 Utility class for converting NameVote[] to more compact representation.
 */
public class NameIndex {
	protected HashMap they = new HashMap(); // HashMap<String,Integer>
	protected ArrayList names = new ArrayList(); // ArrayList<String>
	
	public String name(int index) {
		return (String)names.get(index);
	}
	public int index(String name) {
		Object o = they.get(name);
		if ( o == null ) {
			int ni = they.size();
			they.put(name, new Integer(ni));
			names.add(name);
			return ni;
		}
		return ((Integer)o).intValue();
	}
	
	NameVotingSystem.IndexVoteSet indexify(NameVotingSystem.NameVote[] vote){
		NameVotingSystem.IndexVoteSet toret = new NameVotingSystem.IndexVoteSet(vote.length);
		for ( int i = 0; i < vote.length; i++ ) {
			toret.index[i] = index(vote[i].name);
			toret.rating[i] = vote[i].rating;
		}
		return toret;
	}
	NameVotingSystem.NameVote[] nameify(NameVotingSystem.IndexVoteSet iv) {
		NameVotingSystem.NameVote[] toret = new NameVotingSystem.NameVote[iv.rating.length];
		for ( int i = 0; i < iv.rating.length; i++ ) {
			toret[i] = new NameVotingSystem.NameVote( name(iv.index[i]), iv.rating[i] );
		}
		return toret;
	}
}