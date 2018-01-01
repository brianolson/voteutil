package org.bolson.vote;

/**
VRR that defaults to Ranked-Pairs mode on.
*/
public class RankedPairs extends VRR {
	public RankedPairs() {
		rankedPairsMode = true;
	}

	static {
		registerImpl( "RP", RankedPairs.class );
	}
}
