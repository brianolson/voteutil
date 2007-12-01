package org.bolson.vote;
import java.lang.reflect.Constructor;
import java.util.regex.Pattern;

/** VoteSouce with Options
Store configuration for a VoteSource ready to produce one when numc is known. */
public class VotingSystemFactory {
	public String name;
	public Constructor f;
	public String[] argv;
	public String shortOpt;
	public boolean onDef = true;
	
	public String toString() {
		Pattern quote = Pattern.compile("\"");
		Pattern bs = Pattern.compile("\\\\");
		StringBuffer toret = new StringBuffer( "name=\"" );
		toret.append(quote.matcher(bs.matcher(name).replaceAll("\\")).replaceAll("\\\""));
		toret.append( "\", constructor=\"" );
		toret.append(quote.matcher(bs.matcher(f.toString()).replaceAll("\\")).replaceAll("\\\""));
		toret.append( "\"" );
		if ( argv != null && argv.length > 0 ) {
			toret.append( ", argv={ \"" );
			// escape all backslashes, then escaple all quotes
			toret.append(quote.matcher(bs.matcher(argv[0]).replaceAll("\\")).replaceAll("\\\""));
			for ( int i = 1; i < argv.length; i++ ) {
				toret.append( "\", \"" );
				// escape all backslashes, then escaple all quotes
				toret.append(quote.matcher(bs.matcher(argv[i]).replaceAll("\\")).replaceAll("\\\""));
			}
			toret.append( "\" }" );
		}
		toret.append( ", shortOpt=\"" );
		toret.append(quote.matcher(bs.matcher(shortOpt).replaceAll("\\")).replaceAll("\\\""));
		toret.append( "\", " );
		toret.append( onDef );
		return toret.toString();
	}
	public String desc() {
		Pattern quote = Pattern.compile("\"");
		Pattern bs = Pattern.compile("\\\\");
		StringBuffer toret = new StringBuffer( "class=\"" );
		toret.append(quote.matcher(bs.matcher(f.getDeclaringClass().toString()).replaceAll("\\")).replaceAll("\\\""));
		toret.append( "\"" );
		if ( argv != null && argv.length > 0 ) {
			toret.append( ", argv={ \"" );
			// escape all backslashes, then escaple all quotes
			toret.append(quote.matcher(bs.matcher(argv[0]).replaceAll("\\")).replaceAll("\\\""));
			for ( int i = 1; i < argv.length; i++ ) {
				toret.append( "\", \"" );
				// escape all backslashes, then escaple all quotes
				toret.append(quote.matcher(bs.matcher(argv[i]).replaceAll("\\")).replaceAll("\\\""));
			}
			toret.append( "\" }" );
		}
		toret.append( ", shortOpt=\"" );
		toret.append(quote.matcher(bs.matcher(shortOpt).replaceAll("\\")).replaceAll("\\\""));
		toret.append( "\", " );
		toret.append( onDef );
		return toret.toString();
	}
	public static VotingSystemFactory fromDesc( String d ) {
		// FIXME WRITEME 
		try {
			int di = 0, ni;
			di = d.indexOf( "class=\"" );
			if ( di < 0 ) {
				return null;
			}
			di = d.indexOf( '"', di );
			di++;
			ni = d.indexOf( '"', di );
			// annoying. un backslashify.
			while ( d.charAt( ni - 1 ) == '\\' ) {
				
			}
			Class c = Class.forName( d.substring( di,ni ) );
			if ( c == null ) {
				return null;
			}
			di = d.indexOf( "argv=" );
			if ( di >= 0 ) {
				di = d.indexOf( '{', di );
				di++;
				int oqi, cqi;
				oqi = d.indexOf( '"', di );
				oqi++;
				
			}
		} catch ( Exception e ) {
		}
		return null;
	}
	public VotingSystemFactory( Class vsClass, String soIn ) throws NoSuchMethodException {
		name = null;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = null;
		shortOpt = soIn;
		build(3);
	}
	public VotingSystemFactory( Class vsClass, String soIn, boolean onDefIn ) throws NoSuchMethodException {
		name = null;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = null;
		shortOpt = soIn;
		onDef = onDefIn;
		build(3);
	}
	public VotingSystemFactory( Class vsClass, String nameIn, String soIn ) throws NoSuchMethodException {
		name = nameIn;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = null;
		shortOpt = soIn;
		if ( nameIn == null ) {
			build(3);
		}
	}
	public VotingSystemFactory( Class vsClass, String nameIn, String soIn, String[] argvIn ) throws NoSuchMethodException {
		name = nameIn;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
		if ( nameIn == null ) {
			build(3);
		}
	}
	public VotingSystemFactory( Class vsClass, String soIn, String[] argvIn ) throws NoSuchMethodException {
		name = null;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
		build(3);
	}
	public VotingSystemFactory( Class vsClass, String nameIn, String soIn, String[] argvIn, boolean onDefIn ) throws NoSuchMethodException {
		name = nameIn;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
		onDef = onDefIn;
		if ( nameIn == null ) {
			build(3);
		}
	}
	public VotingSystemFactory( Class vsClass, String soIn, String[] argvIn, boolean onDefIn ) throws NoSuchMethodException {
		name = null;
		f = vsClass.getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
		onDef = onDefIn;
		build(3);
	}
	public VotingSystemFactory( VotingSystem vsIn, String soIn ) throws NoSuchMethodException {
		name = vsIn.name();
		f = vsIn.getClass().getConstructor( new Class[]{ int.class } );
		argv = null;
		shortOpt = soIn;
	}
	public VotingSystemFactory( VotingSystem vsIn, String[] argvIn, String soIn ) throws NoSuchMethodException {
		if ( argvIn != null ) {
			vsIn.init( argvIn );
		}
		name = vsIn.name();
		f = vsIn.getClass().getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
	}
	public VotingSystemFactory( VotingSystem vsIn, String[] argvIn, String soIn, boolean on ) throws NoSuchMethodException {
		if ( argvIn != null ) {
			vsIn.init( argvIn );
		}
		name = vsIn.name();
		f = vsIn.getClass().getConstructor( new Class[]{ int.class } );
		argv = argvIn;
		shortOpt = soIn;
		onDef = on;
	}
	public VotingSystem build( int numc ) {
		try {
			VotingSystem toret = (VotingSystem)(f.newInstance( new Object[]{ new Integer(numc) } ));
			if ( argv != null ) {
				toret.init( argv );
			}
			if ( name == null ) {
				name = toret.name();
			}
			return toret;
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return null;
	}
	
	/** convenience function */
	public static VotingSystem[] buildAll( VotingSystemFactory[] vsl, int numc ) {
		VotingSystem[] toret = new VotingSystem[vsl.length];
		for ( int i = 0; i < vsl.length; i++ ) {
			toret[i] = vsl[i].build( numc );
		}
		return toret;
	}
	/** return array of VotingSystem for which vsl[].onDef is true */
	public static VotingSystem[] buildEnabled( VotingSystemFactory[] vsl, int numc ) {
		int numsys = 0;
		for ( int i = 0; i < vsl.length; i++ ) {
			if ( vsl[i].onDef ) {
				numsys++;
			}
		}
		VotingSystem[] toret = new VotingSystem[numsys];
		int tp = 0;
		for ( int i = 0; i < vsl.length; i++ ) {
			if ( vsl[i].onDef ) {
				toret[tp] = vsl[i].build( numc );
				tp++;
			}
		}
		return toret;
	}
	
	/** Suggestion: take a .clone() of this array and set the onDef flags */
	public static final VotingSystemFactory[] standardVotingSystemList;
	static {
		VotingSystemFactory[] tsvsl = null;
		try {
		tsvsl = new VotingSystemFactory[]{
			new VotingSystemFactory( RawRating.class, "rr" ),
			new VotingSystemFactory( IRNR.class, "irnr" ),
			new VotingSystemFactory( INR.class, "inr", false ),
			new VotingSystemFactory( Condorcet.class, "c", new String[]{ "-minpref","-wv" } ),
			new VotingSystemFactory( Condorcet.class, "cm", new String[]{ "-minpref","-margins" } ),
			new VotingSystemFactory( Condorcet.class, "cb", new String[]{ "-minpref","-wv","-both" } ),
			new VotingSystemFactory( Condorcet.class, "cmb", new String[]{ "-minpref","-margins","-both" } ),
			new VotingSystemFactory( RankedPairs.class, "rp", new String[]{ "-margins" } ),
			new VotingSystemFactory( RankedPairs.class, "rp" ),
			new VotingSystemFactory( CondorcetRTB.class, "crtb" ),
			new VotingSystemFactory( PairwiseRatingDifferentialSummation.class, "prads" ),
			new VotingSystemFactory( Approval.class, "a" ),
			new VotingSystemFactory( IRV.class, "irv" ),
			new VotingSystemFactory( IRV.class, "irvd", new String[]{ "-dup" } ),
			new VotingSystemFactory( IRV.class, "irvx", new String[]{ "-dq" } ),
			new VotingSystemFactory( BordaVotingSystem.class, "b", false ),
			new VotingSystemFactory( Bucklin.class, "bu", false ),
			new VotingSystemFactory( Bucklin.class, "but", new String[]{ "-trunc" }, false ),
			new VotingSystemFactory( Coombs.class, "coo", false ),
			new VotingSystemFactory( STV.class, "Single Transferrable Vote", "stv" ),
			new VotingSystemFactory( IRNRP.class, "irnrp" ),
			new VotingSystemFactory( IRNRP.class, "irnrp", new String[]{ "bolson" } ),
		};
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		standardVotingSystemList = tsvsl;
	}

	public static void main( String[] argv ) {
		for ( int i = 0; i < standardVotingSystemList.length; i++ ) {
			System.out.println( standardVotingSystemList[i].desc() );
		}
	}
}
