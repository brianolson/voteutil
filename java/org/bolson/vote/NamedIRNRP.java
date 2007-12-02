package org.bolson.vote;

import java.util.Vector;
import java.util.HashMap;

/**
Instant Runoff Normalized Ratings with Proportional-Representation extension.
 @author Brian Olson
*/
public class NamedIRNRP extends NamedIRNR implements MultiSeatElectionMethod {
	protected int seats = 1;

	/**
	 How many winners will we target when running getWinners() ?
	 @param seats number of openings to fill from this set of choices and votes.
	 @see #getWinners()
	 */
	public void setNumSeats(int seats) {
		this.seats = seats;
	}

	/** deweight per contribution to currently active choices.
	 if false, deweight per contribution to all the choices. */
	protected boolean deweightPerCurrent = true;

	public int init( String[] argv ) {
		if ( argv != null ) {
			for ( int i = 0; i < argv.length && argv[i] != null; i++ ) {
				if ( argv[i].equals("seats") ) {
					argv[i] = null;
					i++;
					seats = Integer.parseInt( argv[i] );
					argv[i] = null;
				}
			}
		}
		return super.init( argv );
	}
	
	protected void deweight( float[] voterWeight, TallyState[] evict ) {
		for ( int v = 0; v < votes.size(); v++ ) {
			NameVote[] ot;
			double ts;
			boolean hasIt;
			if ( voterWeight[v] <= 0.0 ) {
				continue;
			}
			ts = 0.0;
			ot = (NameVote[])votes.elementAt( v );
			hasIt = false;
			for ( int c = 0; c < ot.length; c++ ) {
				TallyState tts;
				boolean active;
				tts = (TallyState)names.get( ot[c].name );
				active = tts.active;
				if ( ! active ) {
					for ( int i = 0; i < evict.length; i++ ) {
						if ( evict[i] == tts ) {
							active = true;
							hasIt = true;
							break;
						}
					}
				}
				if ( ! deweightPerCurrent ) {
					active = true; // deweight per contribution to original vote with all choices active
				}
				if ( active && ! Float.isNaN( ot[c].rating ) ) {
					if ( rmsnorm ) {
						ts += ot[c].rating * ot[c].rating;
					} else {
						ts += Math.abs(ot[c].rating);
					}
				}
			}
			if ( ! hasIt ) {
				// no vote on any winning choice, nothing to do to this vote
				continue;
			}
			if ( rmsnorm ) {
				ts = Math.sqrt( ts );
			}
			ts = 1.0 / ts;
			for ( int c = 0; c < ot.length; c++ ) {
				TallyState tts;
				tts = (TallyState)names.get( ot[c].name );
				if ( ! Float.isNaN( ot[c].rating ) && ot[c].rating > 0 ) {
					for ( int i = 0; i < evict.length; i++ ) {
						if ( evict[i] == tts ) {
							double tp;
							tp = ot[c].rating * ts;
							voterWeight[v] -= tp;
							break;
						}
					}
				}
			}
		}
	}

	public NameVote[] getWinners() {
		boolean debug = false;
		java.io.PrintWriter out = null;
		if ( winners != null ) {
			return winners;
		}
		TallyState[] namea;
		{
			Object[] nameoa = names.values().toArray();
			namea = new TallyState[nameoa.length];
			int i;
			for ( i = 0; i < nameoa.length; i++ ) {
				namea[i] = (TallyState)nameoa[i];
				namea[i].active = true;
			}
			nameoa = null;
		}
		int numc = namea.length;
		if ( numc == 0 ) {
			return winners = new NameVote[0];
		}
		//double max = Double.NEGATIVE_INFINITY;
		int numWinners = 0;
		int numActive = numc;
		double dt[] = new double[numc];
		float voterWeight[] = new float[votes.size()];
		double totalWeight = votes.size();
		double quota = totalWeight / ( seats + 1.0 );
		for ( int i = 0; i < voterWeight.length; i++ ) {
			voterWeight[i] = 1.0f;
		}
		winners = new NameVote[numc];
		while ( numWinners < seats && numActive > 1 ) {
			// per IR setup
			for ( int c = 0; c < numc; c++ ) {
				if ( namea[c].active ) {
					namea[c].tally = 0.0;
				}
			}
			// sum up into tally
			for ( int v = 0; v < votes.size(); v++ ) {
				NameVote[] ot;
				double ts;
				boolean hasAny;
				if ( voterWeight[v] <= 0.0 ) {
					continue;
				}
				ts = 0.0;
				ot = (NameVote[])votes.elementAt( v );
				hasAny = false;
				for ( int c = 0; c < ot.length; c++ ) {
					TallyState tts;
					tts = (TallyState)names.get( ot[c].name );
					if ( tts.active && ! Float.isNaN( ot[c].rating ) ) {
						hasAny = true;
						if ( rmsnorm ) {
							ts += ot[c].rating * ot[c].rating;
						} else {
							ts += Math.abs(ot[c].rating);
						}
					}
				}
				if ( hasAny ) {
					if ( rmsnorm ) {
						ts = Math.sqrt( ts );
					}
					ts = 1.0 / ts;
					for ( int c = 0; c < ot.length; c++ ) {
						TallyState tts;
						tts = (TallyState)names.get( ot[c].name );
						if ( tts.active && ! Float.isNaN( ot[c].rating ) ) {
							double tp;
							tp = ot[c].rating * ts;
							if ( ! Double.isNaN( tp ) ) {
								tts.tally += tp * voterWeight[v];
							}
						}
					}
				} else /*if ( voterWeight[v] > 0.0 )*/ {
					// vote exhausted, remove remaining weight from total weight and recalculate quota
					totalWeight -= voterWeight[v];
					voterWeight[v] = 0.0f;
					quota = totalWeight / ( seats + 1.0 );
				}
			}
			int mini = -1, maxi = -1;
			int maxtie = 0;
			int mintie = 0;
			double min = 0.0, max = 0.0;
			int i;
			for ( i = 0; i < namea.length; i++ ) {
				if ( namea[i].active ) {
					mini = maxi = i;
					min = max = namea[i].tally;
					break;
				}
			}
			for ( ; i < namea.length; i++ ) {
				if ( namea[i].active ) {
					if ( namea[i].tally < min ) {
						min = namea[i].tally;
						mini = i;
						mintie = 1;
					} else if ( namea[i].tally == min ) {
						mintie++;
					}
					if ( namea[i].tally > max ) {
						max = namea[i].tally;
						maxi = i;
						maxtie = 1;
					} else if ( namea[i].tally == max ) {
						maxtie++;
					}
				}
			}
			if ( max > quota ) {
				// elect if possible
				if ( maxtie == 1 ) {
					winners[numWinners] = new NameVote( namea[maxi].name, (float)namea[maxi].tally );
					namea[maxi].active = false;
					numActive--;
					numWinners++;
					deweight( voterWeight, new TallyState[]{namea[maxi]} );
				} else if ( maxtie > 1 ) {
					// elect winning tie
					TallyState[] evict = new TallyState[maxtie];
					int ei = 0;
					for ( i = 0; i < namea.length; i++ ) {
						if ( namea[i].tally == max ) {
							winners[numWinners] = new NameVote( namea[i].name, (float)namea[i].tally );
							numActive--;
							numWinners++;
							namea[i].active = false;
							evict[ei] = namea[i];
							ei++;
						}
					}
					deweight( voterWeight, evict );
				} else {
					System.err.println("maxtie " + maxtie + ", this should never happen");
				}
			} else {
				// deactivate losers
				if ( mintie == 1 ) {
					winners[numWinners+numActive] = new NameVote( namea[mini].name, (float)namea[mini].tally );
					namea[mini].active = false;
					numActive--;
				} else if ( mintie > 1 ) {
					for ( i = 0; i < namea.length; i++ ) {
						if ( namea[i].tally == min ) {
							winners[numWinners+numActive] = new NameVote( namea[i].name, (float)namea[i].tally );
							numActive--;
							namea[i].active = false;
						}
					}
				} else {
					System.err.println("mintie " + mintie + ", this should never happen");
				}
			}
		}
		while ( numActive > 0 ) {
			int mini = -1;
			double min = 0.0;
			int i;
			for ( i = 0; i < namea.length; i++ ) {
				if ( namea[i].active ) {
					if ( mini == -1 ) {
						mini = i;
						min = namea[i].tally;
					} else if ( min > namea[i].tally ) {
						min = namea[i].tally;
						mini = i;
					}
				}
			}
			winners[numWinners+numActive] = new NameVote( namea[mini].name, (float)namea[mini].tally );
			namea[mini].active = false;
			numActive--;
		}
		return winners;
	}

	public static java.text.DecimalFormat ratingFormat = new java.text.DecimalFormat( "0.00" );
	public StringBuffer htmlSummary( StringBuffer sb ) {
		if ( winners == null ) {
			getWinners();
		}
		sb.append( "<table border=\"1\"><tr><th>Name</th><th>IRNRP Rating</th></tr>" );
		for ( int i = 0; i < winners.length && winners[i] != null; i++ ) {
			sb.append( "<tr><td>" );
			sb.append( winners[i].name );
			sb.append( "</td><td>" );
			ratingFormat.format( winners[i].rating, sb, new java.text.FieldPosition( java.text.NumberFormat.INTEGER_FIELD ) );
			//sb.append( winners[i].rating );
			sb.append( "</td></tr>" );
		}
		sb.append( "</table>" );
		return sb;
	}
	public String name() {
		return "Instant Runoff Normalized Ratings Proportional";
	}
	
	static {
		registerImpl( "IRNRP", NamedIRNRP.class );
	}
};
