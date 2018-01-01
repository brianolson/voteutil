package org.bolson.vote.staticballot;
/**
 * Implements the Condorcet method of ranked voting tabulation
 * and beatpath tie resolution based on Ratings, as suggested by James Green-Armytage
 @author Brian Olson
 */
public class CondorcetRTB extends Condorcet {
    protected int talley[];
	protected double dtalley[];
	protected double bpm[];
	String message = null;
	int[] winners = null;
    public CondorcetRTB( int numCandidates ) {
    	super( numCandidates );
		talley = new int[numc*numc];
		dtalley = new double[numc*numc];
		for ( int i = 0; i < numc*numc; i++ ) {
			talley[i] = 0;
			dtalley[i] = 0.0;
		}
    }
	public String name() {
		return "Condorcet + Rated Beatpath Cycle Resolution";
	}
	
    public int voteRating( int rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				int pk, pj;
				pk = rating[k];
				pj = rating[j];
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
					dtalley[k*numc + j] += pk - pj;
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
					dtalley[j*numc + k] += pj - pk;
				}
			}
		}
		return 0;
    }
    public int voteRating( float rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				float pk, pj;
				pk = rating[k];
				pj = rating[j];
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
					dtalley[k*numc + j] += pk - pj;
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
					dtalley[j*numc + k] += pj - pk;
				}
			}
		}
		return 0;
    }
    public int voteRating( double rating[] ) {
		int j, k;
		winners = null;
		// foreach pair of candidates, vote 1-on-1
		for ( j = 0; j < numc; j++ ) {
			for ( k = j + 1; k < numc; k++ ) {
				double pk, pj;
				pk = rating[k];
				pj = rating[j];
				if ( pk > pj ) {
					talley[k*numc + j]++;	// k beats j
					dtalley[k*numc + j] += pk - pj;
				} else if ( pj > pk ) {
					talley[j*numc + k]++;	// j beats k
					dtalley[j*numc + k] += pj - pk;
				}
			}
		}
		return 0;
    }
    
    public int[] getWinners() {
		if ( winners != null ) {
			return winners;
		}
	int defeatCount[];
	int j,k;
	int numWinners;
	int choiceIndecies[] = new int[numc];
	message = null;
    
	defeatCount = new int[numc];
	for ( j = 0; j < numc; j++ ) {
	    defeatCount[j] = 0;
	}
	for ( j = 0; j < numc; j++ ) {
	    for ( k = j + 1; k < numc; k++ ) {
		int vk, vj;
		vk = talley[k*numc + j];	// k beat j vk times
		vj = talley[j*numc + k];	// j beat k vj times
		if ( vj > vk ) {
		    defeatCount[k]++;
		} else if ( vj < vk ) {
		    defeatCount[j]++;
		}
	    }
	}
	numWinners = 0;
	for ( j = 0; j < numc; j++ ) {
	    if ( defeatCount[j] == 0 ) {
		choiceIndecies[numWinners] = j;
		numWinners++;
	    }
	}
	if ( numWinners > 1 ) {
	} else if ( numWinners == 0 ) {
		StringBuffer msg = new StringBuffer("no clear winner, defeats={ ");
		for ( j = 0; j < numc; j++ ) {
			msg.append(defeatCount[j]);
			if ( j < (numc - 1) ) {
				msg.append(", ");
			}
		}
		msg.append(" }, running preference-differential beatpath");
		message = msg.toString();
	    double winsize = -1.0 * Double.MAX_VALUE;
	    bpm = new double[numc*numc];
	    runBeatpath( talley, dtalley, bpm, numc, 0 );
	    for ( j = 0; j < numc; j++ ) {
			double winsizet;
			winsizet = -1.0 * Double.MAX_VALUE;
			defeatCount[j] = 0;
			for ( k = 0; k < numc; k++ ) if ( k != j ) {
				double bpmt = bpm[j*numc + k];
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
	}
/*    if ( 0 ) {
	if ( numWinners == 0 ) {
	    static int cErrCount = 0;
	    static char cErrFileName[32];
	    snprintf( cErrFileName, sizeof(cErrFileName), "condorcetError%.10d", cErrCount );
	    voterBinDump( cErrFileName, they, numv, numc );
	}
    }*/
/*    if ( 0 ) {
	if ( winner == -1 ) {
	    printf("no Condorcet-beatpath winner, bpm array:\n");
	    for ( j = 0; j < numc; j++ ) {
		printf("dc(%d)\t", defeatCount[j] );
		for ( k = 0; k < numc; k++ ) {
		    printf("%d\t", bpm[j*numc + k] );
		}
		printf("\n");
	    }
	    printf("\ntalley array:\n");
	    for ( j = 0; j < numc; j++ ) {
		for ( k = 0; k < numc; k++ ) {
		    printf("%d\t", talley[j*numc + k] );
		}
		printf("\n");
	    }
	    printf("\n");
	}
    }*/
	winners = new int[numWinners];
	for ( int i = 0; i < numWinners; i++ ) {
	    winners[i] = choiceIndecies[i];
	}
	return winners;
    }

static int umin( int a, int b ) {
    if ( a < b ) {
	return a;
    } else {
	return b;
    }
}
static double umin( double a, double b ) {
    if ( a < b ) {
		return a;
    } else {
		return b;
    }
}
/**
 * bpm Beat Path Matrix, filled in as we go.
 */
static void runBeatpath( int talley[], double dtalley[], double bpm[], int numc, int depth ) {
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
				bpm[k*numc + j] = dtalley[k*numc + j];
				bpm[j*numc + k] = 0;
			} else if ( vj > vk ) {
				bpm[k*numc + j] = 0;
				bpm[j*numc + k] = dtalley[j*numc + k];
			} else /* vj == vk */ {
				bpm[k*numc + j] = 0;
				bpm[j*numc + k] = 0;
			}
		}
    }

    while ( notDone ) {
		notDone = false;
		/*if ( 0 ) {
		for ( j = 0; j < numc; j++ ) {
			for ( k = 0; k < numc; k++ ) {
				printf("%d\t", bpm[j*numc + k] );
			}
			printf("\n");
		}
		printf("\n");
		}*/
		for ( j = 0; j < numc; j++ ) {
			for ( k = 0; k < numc; k++ ) if ( k != j ) {
				double vk;
				vk = bpm[k*numc + j];	// candidate k > j
				if ( vk != 0 ) {
					// sucessful beat, see if it can be used to get a better beat over another
					for ( int l = 0; l < numc; l++ ) if ( l != j && l != k ) { // don't care about self (k) or same (j)
						double vl;
						vl = umin( bpm[j*numc + l], vk );	// j > l
						if ( /*vl != 0 &&*/ vl > bpm[k*numc + l] ) {
							// better beat path found
							//			    printf("set bpm[%d * %d + %d] = %d\n", k, numc, l, vl);
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
			double vj, vk;
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
/*if ( 0 ) {
    for ( j = 0; j < numc; j++ ) {
	for ( k = 0; k < numc; k++ ) {
	    printf("%d\t", bpm[j*numc + k] );
	}
	printf("\n");
    }
}*/
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

public static StringBuffer htmlTable( StringBuffer sb, int numc, double[] arr, String title, String names[] ) {
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
	if ( message != null ) {
		sb.append(message);
		htmlTable( sb, numc, dtalley, "Rating Differential Array", names );
		htmlTable( sb, numc, bpm, "Beat-Path Results Array", names );
	}
    return sb.toString();
}
};
