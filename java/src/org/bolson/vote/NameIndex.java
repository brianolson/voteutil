package org.bolson.vote;

import java.util.HashMap;
import java.util.ArrayList;

/**
 Utility class for converting NameVote[] to more compact representation.
 @author Brian Olson
 */
public class NameIndex {
	protected HashMap they = new HashMap(); // HashMap<String,Integer>
	protected ArrayList names = new ArrayList(); // ArrayList<String>
	
	/**
	 Return name for index.
	 Does not check that index is valid and may throw out-of-bounds exception.
	 */
	public String name(int index) {
		return (String)names.get(index);
	}
	
	/**
	 Return index of name. An index is assigned if name is being seen for first time.
	 */
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
	
	/**
	 Convert NameVote[] into IndexVoteSet.
	 Names being seen for first time get index allocated.
	 @see #index(String)
	 */
	public NameVotingSystem.IndexVoteSet indexify(NameVotingSystem.NameVote[] vote){
		NameVotingSystem.IndexVoteSet toret = new NameVotingSystem.IndexVoteSet(vote.length);
		for ( int i = 0; i < vote.length; i++ ) {
			toret.index[i] = index(vote[i].name);
			toret.rating[i] = vote[i].rating;
		}
		return toret;
	}
	public NameVotingSystem.NameVote[] nameify(NameVotingSystem.IndexVoteSet iv) {
		NameVotingSystem.NameVote[] toret = new NameVotingSystem.NameVote[iv.rating.length];
		for ( int i = 0; i < iv.rating.length; i++ ) {
			toret[i] = new NameVotingSystem.NameVote( name(iv.index[i]), iv.rating[i] );
		}
		return toret;
	}
}