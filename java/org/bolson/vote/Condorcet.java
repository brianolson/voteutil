package org.bolson.vote;
/**
 * Implements the Condorcet method of ranked voting tabulation
 * and beatpath tie resolution. See <a href="http://www.electionmethods.org/">electionmethods.org</a>.
 * Several variations are contained in this based on options submitted to init().
 * @see CondorcetRTB
 @author Brian Olson
 */
public class Condorcet extends RatedVotingSystem {
	protected int talley[];
	boolean usedBPM = false;
	int bpm[];
	int[] winners = null;
	int defeatCount[];
	StringBuffer debugsb = null;
	int[] defeatCountDebugClone = null;
	
	boolean noVoteIsMinPref = true;
	boolean noVoteIsAvgPref = false;
	boolean noVoteIsNoPref = false;
	boolean winningVotes = true;
	boolean margins = false;
	boolean addToBothOnTie = false;
	
	public Condorcet( int numCandidates ) {
		super( numCandidates );
		talley = new int[numc*numc];
		for ( int i = 0; i < numc*numc; i++ ) {
			talley[i] = 0;
		}
	}
	
	public String getDebugText( String[] names ) {
		if ( debugsb == null ) {
			return null;
		}
		if ( defeatCountDebugClone != null ) {
			debugsb.append("defeats={ ");
			for ( int j = 0; j < numc; j++ ) {
				if ( names != null ) {
					debugsb.append( names[j] );
					debugsb.append(" = ");
				} else if ( numc > 6 ) {
					debugsb.append( j );
					debugsb.append(" = ");
				}
				debugsb.append(defeatCountDebugClone[j]);
				if ( j < (numc - 1) ) {
					debugsb.append(", ");
				}
			}
			debugsb.append(" }\n");
		}
		return debugsb.toString();
	}
	
	/**
	 @param argv -minpref|-avgpref|-margins|-wv|-both
	 -minpref   "no vote" indicates minimal preference
	 -avgpref   "no vote" assigned average preference not otherwise assigned
	 -margins   tie breakers consider marginal votes (A_Over_B - B_Over_A)
	 -wv	tie breakers consider winning votes (A_Over_B)
	 -both  add to both on tie (otherwise add to neither on tie). can change outcome with -wv "winning votes".
	 @return this
	 */
	public VotingSystem init( String[] argv ) {
		for ( int i = 0; i < argv.length; i++ ) {
			if ( argv[i].equals("-minpref") ) {
				noVoteIsMinPref = true;
				noVoteIsAvgPref = false;
			} else if ( argv[i].equals("-avgpref") ) {
				noVoteIsMinPref = false;
				noVoteIsAvgPref = true;
			} else if ( argv[i].equals("-margins") ) {
				winningVotes = false;
				margins = true;
			} else if ( argv[i].equals("-wv") ) {
				winningVotes = true;
				margins = false;
			} else if ( argv[i].equals("-both") ) {
				addToBothOnTie = true;
			} else {
				System.err.println("Condorcet.init bogus arg: "+argv[i]);
			}
		}
		return this;
	}
	
	public String name() {
		StringBuffer toret = new StringBuffer("Condorcet");
		if ( noVoteIsMinPref ) {
			// shh, default
			//toret.append(", no-vote is a minimal rating");
		} else if ( noVoteIsAvgPref ) {
			toret.append(", no-vote is considered to be average");
		}
		if ( winningVotes ) {
			toret.append(", winning votes beatpath");
		} else if ( margins ) {
			toret.append(", marginal votes beatpath");
		}
		if ( addToBothOnTie ) {
			toret.append(", add to both on equal ranking");
		}
		return toret.toString();
	}
	
	public int voteRating( int rating[] ) {
		int j, k;
		int noVoteVal = 0;
		if ( noVoteIsMinPref ) {
			noVoteVal = Integer.MIN_VALUE;
		} else if ( noVoteIsAvgPref ) {
			for ( j = 0; j < numc; j++ ) {
				if ( rating[j] != NO_VOTE ) {
					noVoteVal += rating[j];
				}
			}
			noVoteVal /= numc;
		}
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			int pj;
			pj = rating[j];
			if ( pj == NO_VOTE ) {
				pj = noVoteVal;
			}
			for ( k = j + 1; k < numc; k++ ) {
				int pk;
				pk = rating[k];
				if ( pk == NO_VOTE ) {
					pk = noVoteVal;
				}
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
				} else if ( addToBothOnTie && ! ((rating[j] == NO_VOTE) && (rating[k] == NO_VOTE)) ) {
					talley[k*numc + j]++;
					talley[j*numc + k]++;
				}
			}
		}
		return 0;
	}
	public int voteRating( float rating[] ) {
		int j, k;
		float noVoteVal = 0.0f;
		if ( noVoteIsMinPref ) {
			noVoteVal = Float.MAX_VALUE * -1.0f;
		} else if ( noVoteIsAvgPref ) {
			for ( j = 0; j < numc; j++ ) {
				if ( ! Float.isNaN( rating[j] ) ) {
					noVoteVal += rating[j];
				}
			}
			noVoteVal /= numc;
		}
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			float pj;
			pj = rating[j];
			if ( Float.isNaN( pj ) ) {
				pj = noVoteVal;
			}
			for ( k = j + 1; k < numc; k++ ) {
				float pk;
				pk = rating[k];
				if ( Float.isNaN( pk ) ) {
					pk = noVoteVal;
				}
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
				} else if ( addToBothOnTie && ! (Float.isNaN(rating[j]) && Float.isNaN(rating[k])) ) {
					talley[k*numc + j]++;
					talley[j*numc + k]++;
				}
			}
		}
		return 0;
	}
	public int voteRating( double rating[] ) {
		int j, k;
		double noVoteVal = 0.0;
		if ( noVoteIsMinPref ) {
			noVoteVal = Double.MAX_VALUE * -1.0;
		} else if ( noVoteIsAvgPref ) {
			for ( j = 0; j < numc; j++ ) {
				if ( ! Double.isNaN( rating[j] ) ) {
					noVoteVal += rating[j];
				}
			}
			noVoteVal /= numc;
		}
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			double pj;
			pj = rating[j];
			if ( Double.isNaN( pj ) ) {
				pj = noVoteVal;
			}
			for ( k = j + 1; k < numc; k++ ) {
				double pk;
				pk = rating[k];
				if ( Double.isNaN( pk ) ) {
					pk = noVoteVal;
				}
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
				} else if ( addToBothOnTie && ! (Double.isNaN(rating[j]) && Double.isNaN(rating[k])) ) {
					talley[k*numc + j]++;
					talley[j*numc + k]++;
				}
			}
		}
		return 0;
	}
	
	/** return the set of choices that are only beaten by each other.
		@return the "Schwartz Set" */
	public int[] getSchwartzSet() {
		// pass in throw-away defeatCount
		return getSchwartzSet( new int[numc] );
	}
	/** return the set of choices that are only beaten by each other.
		@param defeatCount must be int[numc]
		by the reference property of java arrays, the defeatCount will be returned in this property. 
		@return the "Schwartz Set" */
	public int[] getSchwartzSet( int[] defeatCount ) {
		return getSchwartzSet( numc, talley, defeatCount, debugsb );
	}
	public static void countDefeats( int numc, int[] tally, int[] defeatCount ) {
		/* ndefeats is "numc choose 2" or ((numc !)/((2 !)*((numc - 2)!))) */
		int j,k;
		
		for ( j = 0; j < numc; j++ ) {
			defeatCount[j] = 0;
		}
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				int vk, vj;
				vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
				vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
				if ( vj > vk ) {
					defeatCount[k]++;
				} else if ( vj < vk ) {
					defeatCount[j]++;
				}
			}
		}
	}
	public static int[] getSchwartzSet( int numc, int[] tally, int[] defeatCount ) {
		return getSchwartzSet( numc, tally, defeatCount, null );
	}
	public static int[] getSchwartzSet( int numc, int[] tally, int[] defeatCount, StringBuffer debugsb ) {
		countDefeats( numc, tally, defeatCount );
		// start out set with first choice (probabbly replace it)
		int j,k;
		int mindefeats = defeatCount[0];
		int numWinners = 1;
		int choiceIndecies[] = new int[numc];
		choiceIndecies[0] = 0;
		for ( j = 1; j < numc; j++ ) {
			if ( defeatCount[j] < mindefeats ) {
				choiceIndecies[0] = j;
				numWinners = 1;
				mindefeats = defeatCount[j];
			} else if ( defeatCount[j] == mindefeats ) {
				choiceIndecies[numWinners] = j;
				numWinners++;
			}
		}
		if ( mindefeats != 0 ) {
			// the best there is was defeated by some choice, make sure that is added to the set
			for ( int i = 0; i < numWinners; i++ ) {
				// foreach k in set of least defeated ...
				k = choiceIndecies[i];
				for ( j = 0; j < numc; j++ ) if ( k != j ) {
					int vk, vj;
					vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
					vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
					if ( vj > vk ) {
						// j defeats k, j must be in the set
						boolean gotj = false;
						for ( int si = 0; si < numWinners; si++ ) {
							if ( choiceIndecies[si] == j ) {
								gotj = true;
								break;
							}
						}
						if ( ! gotj ) {
							choiceIndecies[numWinners] = j;
							numWinners++;
						}
					}
				}
			}
		}
		int[] sset = new int[numWinners];
		for ( j = 0; j < numWinners; j++ ) {
			sset[j] = choiceIndecies[j];
		}
		if ( ! verifySchwartzSet( numc, tally, sset, debugsb ) ) {
			//System.err.println("getSchwartzSet is returning an invalid Schwartz set!");
			if ( debugsb != null ) {
				htmlTable( debugsb, numc, tally, "tally not met by schwartz set", null );
				debugsb.append( "bad sset: " );
				debugsb.append( sset[0] );
				for ( j = 1; j < sset.length; j++ ) {
					debugsb.append(", ");
					debugsb.append(sset[j]);
				}
			}
		}
		return sset;
	}
	/** Verify set to have Schwartz Set properties.
		<ol><li>every member of the set beats every choice not in the set</li>
		<li>no member of the set is beaten by every other member of the set</li></ol>
		@param ss a candidate Schwartz Set
		@return true if ss is a Schwartz Set */
	public boolean verifySchwartzSet( int[] ss ) {
		return verifySchwartzSet( numc, talley, ss, debugsb );
	}
	public static boolean verifySchwartzSet( int numc, int[] tally, int[] ss ) {
		return verifySchwartzSet( numc, tally, ss, null );
	}
	public static boolean verifySchwartzSet( int numc, int[] tally, int[] ss, StringBuffer debugsb ) {
		for ( int i = 0; i < ss.length; i++ ) {
			int m;
			m = ss[i];
			// check for defeats by choices outside the set
			for ( int j = 0; j < numc; j++ ) {
				boolean notinset;
				notinset = true;
				for ( int k = 0; k < ss.length; k++ ) {
					if ( ss[k] == j ) {
						notinset = false;
						break;
					}
				}
				if ( notinset ) {
					int vm, vj;
					vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
					vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
					if ( vj > vm ) {
						if ( debugsb != null ) {
							debugsb.append("choice ");
							debugsb.append(m);
							debugsb.append(" in bad schwartz set defeated by ");
							debugsb.append(j);
							debugsb.append(" not in set\n");
						}
						return false;
					}
				}
			}
			// check if defated by all choices inside the set
			int innerDefeats = 0;
			for ( int k = 0; k < ss.length; k++ ) {
				int j;
				j = ss[k];
				if ( m != j ) {
					int vm, vj;
					vm = tally[m*numc + j];	// m beat j vm times // OR m prefered to j vm times
					vj = tally[j*numc + m];	// j beat m vj times // OR j prefered to m vj times
					if ( vj > vm ) {
						innerDefeats++;
					}
				}
			}
			if ( (innerDefeats > 0) && (innerDefeats == ss.length - 1) ) {
				if ( debugsb != null ) {
					debugsb.append("choice ");
					debugsb.append(m);
					debugsb.append(" in bad schwartz is defeated by all in set.\n");
				}
				return false;
			}
		}
		// not disproven by exhaustive test, thus it's good
		return true;
	}
	public int[] getWinnersSchwartzEqOne() {
		// schwartz based simple condorcet single winner detection
		winners = getSchwartzSet( defeatCount );
		if ( winners.length == 1 ) {
			return winners;
		}
		return null;
	}
	public int[] getWinnersSimpleIfOne() {
		// non-schwartz based simple condorcet single winner detection
		int numWinners = 0;
		int wi = -1;
		int j;
		
		countDefeats( numc, talley, defeatCount );
		
		for ( j = 0; j < numc; j++ ) {
			if ( defeatCount[j] == 0 ) {
				wi = j;
				numWinners++;
			}
		}
		if ( numWinners == 1 ) {
			return winners = new int[]{ wi };
		}
		return null;
	}
	public int[] getWinnersOldBPM() {
		// old BPM implementation, possibly buggy.
		int j,k;
		int winsize = Integer.MIN_VALUE;
		bpm = new int[numc*numc];
		runBeatpath( talley, bpm, numc, 0 );
		usedBPM = true;
		int choiceIndecies[] = new int[numc];
		int numWinners = 0;
		for ( j = 0; j < numc; j++ ) {
			int winsizet;
			winsizet = Integer.MIN_VALUE;
			defeatCount[j] = 0;
			for ( k = 0; k < numc; k++ ) if ( k != j ) {
				int bpmt = bpm[j*numc + k];
				if ( bpmt == 0 ) {
					defeatCount[j]++;
				} else if ( bpmt > winsizet ) {
					winsizet = bpmt;
				}
			}
			if ( defeatCount[j] == 0 ) {
				if ( winsizet > winsize ) {
					choiceIndecies[0] = j;
					numWinners = 1;
					winsize = winsizet;
				} else if ( winsizet == winsize ) {
					choiceIndecies[numWinners] = j;
					numWinners++;
					//winsize = winsizet;
				}
			}
		}
		winners = new int[numWinners];
		for ( int i = 0; i < numWinners; i++ ) {
			winners[i] = choiceIndecies[i];
		}
		return winners;
	}
	public int[] getWinnersCSSD() {
		// cloneproof schwartz set dropping
		// which ought to be the same as above "beatpath" method, but a new implementation
		int j,k;
		/*int[] defeatCount = new int[numc];*/
		int[] tally = (int[])talley.clone();
		int[] ss = getSchwartzSet( numc, tally, defeatCount, debugsb );
		int mindj, mindk, mind; // minimum defeat, index and strength
		
		boolean notdone = true;
		int tie = 0;
		
		while ( notdone ) {
			notdone = false;
			mind = Integer.MAX_VALUE;
			mindj = -1;
			mindk = -1;
			tie = 0;
			// find weakest defeat between members of schwartz set
			for ( int ji = 0; ji < ss.length - 1; ji++ ) {
				j = ss[ji];
				for ( int ki = ji + 1; ki < ss.length; ki++ ) {
					k = ss[ki];
					int vj, vk;
					vk = tally[k*numc + j];	// k beat j vk times // OR k prefered to j vk times
					vj = tally[j*numc + k];	// j beat k vj times // OR j prefered to k vj times
					if ( vj > vk ) {
						if ( winningVotes ) {
							if ( vj < mind ) {
								mind = vj;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( vj == mind ) {
								tie++;
							}
						} else if ( margins ) {
							int m = vj - vk;
							if (m < mind) {
								mind = m;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( m == mind ) {
								tie++;
							}
						}
					} else if ( vk > vj ) {
						if ( winningVotes ) {
							if ( vk < mind ) {
								mind = vk;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( vk == mind ) {
								tie++;
							}
						} else if ( margins ) {
							int m = vk - vj;
							if (m < mind) {
								mind = m;
								mindj = j;
								mindk = k;
								tie = 1;
							} else if ( m == mind ) {
								tie++;
							}
						}
					}
				}
			}
			if ( tie == 0 ) {
				if ( debug ) {
					debugsb.append("tie = 0, no weakest defeat found to cancel\n");
				}
				return ss;
			}
			// all are tied
			if ( tie == ss.length) {
				if ( debug ) {
					debugsb.append("tie==ss.length, mind=").append(mind).append(", mindj=").append(mindj).append(", mindk=").append(mindk).append('\n');
				}
				return ss;
			}
			tally[mindk*numc + mindj] = 0;
			tally[mindj*numc + mindk] = 0;
			if ( debug ) {
				debugsb.append( mindk ).append( '/' ).append( mindj ).append(" = 0\n");
				htmlTable( debugsb, numc, tally, "intermediate", null );
			}
			ss = getSchwartzSet( numc, tally, defeatCount, debugsb );
			if ( ss.length == 1 ) {
				return ss;
			}
			if ( debug ) {
				debugsb.append("ss={ ");
				debugsb.append( ss[0] );
				for ( j = 1; j < ss.length; j++ ) {
					debugsb.append(", ");
					debugsb.append( ss[j] );
				}
				debugsb.append(" }\n");
			}
			notdone = true;
		}
		return ss;
	}
	public int[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
		if ( defeatCount == null ) {
			defeatCount = new int[numc];
		}
		if ( debug ) {
			debugsb = new StringBuffer();
		} else {
			debugsb = null;
		}
		if ( false ) {
			// new, schwartz based
			winners = getWinnersSchwartzEqOne();
			if ( winners != null ) {
				return winners;
			}
		} else {
			// old non-schwartz based method
			winners = getWinnersSimpleIfOne();
			if ( winners != null ) {
				return winners;
			}
		}
		if ( debug ) {
			defeatCountDebugClone = (int[])defeatCount.clone();
		}
		//		return getWinnersOldBPM();
		return getWinnersCSSD();
	}
	
	static int umin( int a, int b ) {
		if ( a < b ) {
			return a;
		} else {
			return b;
		}
	}
	/**
	 * bpm Beat Path Matrix, filled in as we go.
	 */
	void runBeatpath( int talley[], int bpm[], int numc, int depth ) {
		int j, k;
		// foreach pair of candidates
		boolean notDone = true;
		for ( j = 0; j < numc; j++ ) {
			bpm[j*numc + j] = 0;
			for ( k = j + 1; k < numc; k++ ) {
				int vj, vk;
				vk = talley[k*numc + j];	// k beat j vk times
				vj = talley[j*numc + k];	// j beat k vj times
				if ( vk > vj ) {
					if ( winningVotes ) {
						bpm[k*numc + j] = vk;
						bpm[j*numc + k] = 0;
					} else { // margins
						bpm[k*numc + j] = vk - vj;
						bpm[j*numc + k] = 0;
					}
				} else if ( vj > vk ) {
					if ( winningVotes ) {
						bpm[k*numc + j] = 0;
						bpm[j*numc + k] = vj;
					} else {
						bpm[k*numc + j] = 0;
						bpm[j*numc + k] = vj - vk;
					}
				} else /* vj == vk */ {
					if ( winningVotes ) {
						bpm[k*numc + j] = vk;
						bpm[j*numc + k] = vj;
					} else {
						bpm[k*numc + j] = 0;
						bpm[j*numc + k] = 0;
					}
				}
			}
		}
		
		while ( notDone ) {
			notDone = false;
			for ( j = 0; j < numc; j++ ) {
				for ( k = 0; k < numc; k++ ) if ( k != j ) {
					int vk;
					vk = bpm[k*numc + j];	// candidate k > j
					if ( vk != 0 ) {
						// sucessful beat, see if it can be used to get a better beat over another
						for ( int l = 0; l < numc; l++ ) if ( l != j && l != k ) { // don't care about self (k) or same (j)
							int vl;
							vl = umin( bpm[j*numc + l], vk );	// j > l
							if ( /*vl != 0 &&*/ vl > bpm[k*numc + l] ) {
								// better beat path found
								//			    printf("set bpm[%d * %d + %d] = %d\n", k, numc, l, vl);
								if ( debug ) {
									debugsb.append("set bpm[");
									debugsb.append(k);
									debugsb.append('*');
									debugsb.append(numc);
									debugsb.append(" + ");
									debugsb.append(l);
									debugsb.append("] = ");
									debugsb.append(vl);
									debugsb.append('\n');
								}
								bpm[k*numc + l] = vl;
								notDone = true;
							}
						}
					}
				}
			}
		}
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				int vj, vk;
				vk = bpm[k*numc + j];
				vj = bpm[j*numc + k];
				if ( vk > vj ) {
					//bpm[k*numc + j] = vk;
					bpm[j*numc + k] = 0;
				} else if ( vj > vk ) {
					bpm[k*numc + j] = 0;
					//bpm[j*numc + k] = vj;
				}
			}
		}
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			for ( int ii = 0; ii < numc; ii++ ) {
				sb.append( talley[i*numc + ii] );
				sb.append( '\t' );
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}    
	public String toString( String names[] ) {
		if ( names == null ) return toString();
		StringBuffer sb = new StringBuffer();
		for ( int i = 0; i < numc; i++ ) {
			sb.append( names[i] ).append( '\t' );
			for ( int ii = 0; ii < numc; ii++ ) {
				sb.append( talley[i*numc + ii] );
				sb.append( '\t' );
			}
			sb.append( '\n' );
		}
		return sb.toString();
	}
	
	public static StringBuffer htmlTable( StringBuffer sb, int numc, int[] arr, String title, String names[] ) {
		if ( names != null ) {
			sb.append( "<table border=\"1\"><tr><th></th><th colspan=\"" );
		} else {
			sb.append( "<table border=\"1\"><tr><th>Choice Index</th><th colspan=\"");
		}
		sb.append( numc );
		sb.append("\">");
		sb.append( title );
		sb.append("</th></tr>\n" );
		for ( int i = 0; i < numc; i++ ) {
			sb.append("<tr><td>");
			if ( names != null ) {
				sb.append( names[i] );
			} else {
				sb.append( i );
			}
			sb.append("</td>");
			for ( int j = 0; j < numc; j++ ) {
				if ( (i == j) && (arr[i*numc + j] == 0) ) {
					sb.append("<td bgcolor=\"#ffffff\"></td>");
				} else {
					if ( arr[i*numc + j] > arr[j*numc + i] ) {
						sb.append("<td bgcolor=\"#bbffbb\">");
					} else if ( arr[i*numc + j] < arr[j*numc + i] ) {
						sb.append("<td bgcolor=\"#ffbbbb\">");
					} else {
						sb.append("<td>");
					}
					sb.append(arr[i*numc + j]);
					sb.append("</td>");
				}
			}
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");
		return sb;
	}
	public String htmlSummary( String names[] ) {
		StringBuffer sb;
		sb = new StringBuffer(1024);
		htmlTable( sb, numc, talley, "Condorcet Vote Array", names );
		if ( usedBPM ) {
			int[] schwartzSet = getSchwartzSet();
			sb.append("no clear winner, schwartz set={ ");
			for ( int j = 0; j < schwartzSet.length; j++ ) {
				if ( names != null ) {
					sb.append( names[schwartzSet[j]] );
				} else {
					sb.append( schwartzSet[j] );
				}
				if ( j < (schwartzSet.length - 1) ) {
					sb.append(", ");
				}
			}
			sb.append(" }, running beatpath");
			htmlTable( sb, numc, bpm, "Beat-Path Results Array", names );
		}
		return sb.toString();
	}
};
