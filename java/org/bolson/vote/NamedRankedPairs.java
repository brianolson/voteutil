package org.bolson.vote;

/**
NamedVRR that defaults to Ranked-Pairs mode on.
*/
public class NamedRankedPairs extends NamedVRR {
	public NamedRankedPairs() {
		rankedPairsMode = true;
	}

	static {
		registerImpl( "RP", NamedRankedPairs.class );
	}
}
