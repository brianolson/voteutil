/* This file is placed in the public domain without warrantee by it's creator, Brian Olson. */
#include <stdlib.h>
#include <stdio.h>
#include <limits.h>
#include <time.h>
#include <unistd.h>
#include <math.h>
#include <string.h>
#include <errno.h>
#include <assert.h>

#include "Condorcet.h"
#include "BordaVotingSystem.h"
#include "RawRating.h"
#include "IRV.h"
#include "IRNR.h"
#include "Histogram.h"

char** readNames( char* filename );
#if __ghs
extern "C" char* strdup( const char* );
#endif

class runParams {
public:
	bool rating;
	bool isFloat;
	bool isBin;
	bool scriptMode;
	bool htmlOut;
	bool testMode;
	bool printState;
	bool quiet;
	VotingSystem* (*fac)(int);
	char* inputname;
	FILE* fin;
	int numc;
	int numWinners;
	int* p;
	bool histogram;
	int histBuckets;
	char** names;
	int histyle;

	int iMin, iMax;
	double dMin, dMax, dScale;
	bool iMinClamp, iMaxClamp, dMinClamp, dMaxClamp;

	const char* histBarUrl;

	runParams();

	void getenv();

	void getArgsFromString( char* line );

	int getArgs( int argc, char** argv );
};

runParams::runParams()
	: rating( false ), isFloat( false ), isBin( false ), scriptMode( false ),
	htmlOut( false ), testMode( false ),
	printState( false ), quiet( false ),
	fac( BordaVotingSystem::newBordaVotingSystem ),
	inputname( NULL ), fin( NULL ), numc( -1 ),
	numWinners( -1 ), p( NULL ), histogram( false ), histBuckets( -1 ),
	names( NULL ), histyle( 2 ),
	iMin( 0 ), iMax( 10 ), dMin( -1.0 ), dMax( 1.0 ), dScale( 1000.0 ),
	iMinClamp( false ), iMaxClamp( false ), dMinClamp( false ), dMaxClamp( false ),
	histBarUrl( 0 )
{}

void runParams::getenv() {
	const char* ev;
	ev = ::getenv( "HIST_BAR_URL" );
	if ( ev ) {
		histBarUrl = ev;
	}
}

// break line into argv[], pass to getArgs(,)
void runParams::getArgsFromString( char* line ) {
	int argc = 2; // program name = "getArgsFromString", plus the tail arg
	char quote = 0;
	char** argv;
	int i;
	char* pos;
	char* spos;
	char c;

	//fprintf(stderr,"line \"%s\"\n", line );
	pos = line;
	while ( (c = *pos) ) {
		if ( c == quote ) {
			quote = 0;
		} else if ( (!quote) && (c == '\'') ) {
			quote = '\'';
		} else if ( (!quote) && (c == '"') ) {
			quote = '"';
		} else if ( (!quote) && (c == '\n') ) {
			break;
		} else if ( (quote != '\'') && (c == '\\') ) {
			pos++;
			if ( ! *pos ) break;
		} else if ( (!quote) && ((c == ' ') || (c == '\t')) ) {
			//fprintf(stderr,"split at '%c', pos %d\n", c, pos - line );
			argc++;
		}
		pos++;
	}

	argv = (char**)malloc( (argc+1)*sizeof(char*) );
	assert(argv);
	argv[0] = "getArgsFromString";
	i = 1;
	pos = line;
	spos = pos;
	quote = 0;
	while ( (c = *pos) ) {
		if ( c == quote ) {
			quote = 0;
		} else if ( (!quote) && (c == '\'') ) {
			quote = '\'';
		} else if ( (!quote) && (c == '"') ) {
			quote = '"';
		} else if ( (quote != '\'') && (c == '\\') ) {
			pos++;
			if ( ! *pos ) break;
		} else if ( (!quote) && ((c == ' ') || (c == '\t')) ) {
			argv[i] = spos;
			i++;
			*pos = '\0';
			spos = pos + 1;
		}
		pos++;
	}
	argv[i] = spos;
	argv[argc] = NULL;
	for ( i = 0; i < argc; i++ ) {
		//fprintf(stderr,"argv[%d] = \"%s\"\n", i, argv[i] );
	}
	getArgs( argc, argv );
	free(argv);
}

int voteFromTextFile( runParams* r ) {
	VotingSystem* tab;
	Histogram* hist = NULL;
	char* line = NULL;
	int* p;
	int numc = r->numc;
	char linebuf[1024];
	if ( !r->fin ) return -1;
	if ( r->isBin ) {
		if ( numc == -1 ) fread( &numc, sizeof(numc), 1, r->fin );
	} else {
		line = fgets( linebuf, sizeof(linebuf), r->fin );
		if ( line && (line[0] == '!') ) {
			r->getArgsFromString( line + 1 );
			line = NULL;
			numc = r->numc;
		}
		//if ( numc == -1 ) fscanf( r->fin, "%d\n", &numc );
	}
	tab = r->fac( numc );
	if ( r->histogram ) {
		int nbuck = r->histBuckets;
		double min, max;
		if ( nbuck == -1 ) {
			if ( r->rating ) {
				nbuck = 11;
			} else {
				nbuck = numc;
			}
		}
		if ( r->rating ) {
			if ( r->isFloat ) {
				min = r->dMin * r->dScale;
				max = r->dMax * r->dScale;
			} else {
				min = r->iMin;
				max = r->iMax;
			}
		} else {
			min = 1;
			max = numc;
		}
		hist = new Histogram( numc, nbuck, min, max );
		if ( r->histBarUrl ) {
			hist->barImageUrl = r->histBarUrl;
		}
	}
	p = new int[numc];
	while ( 1 ) {
		if ( r->isBin ) {
			int err;
			for ( int i = 0; i < numc; i++ ) {
				if ( r->isFloat ) {
					float tf;
					err = fread( &tf, sizeof(float), 1, r->fin );
					p[i] = (int)(tf * r->dScale);
				} else {
					err = fread( &(p[i]), sizeof(int), 1, r->fin );
				}
				if ( err <= 0 ) goto noMoreVotes;
			}
		} else {
			char* pos;
			char* npos;
			size_t llen;
			if ( line == NULL ) {
				line = fgets( linebuf, sizeof(linebuf), r->fin );
			}
			if ( line == NULL ) {
				goto noMoreVotes;
			}
			llen = strlen( line );
			pos = line;
			for ( int i = 0; i < numc; i++ ) {
				if ( *pos == '\t' || *pos == '\n' ) {
					p[i] = NO_VOTE;
					pos++;
				} else if ( r->isFloat ) {
					double td;
					td = strtod( pos, &npos );
					if ( pos == npos || errno == ERANGE ) {
						goto noMoreVotes;
					}
					pos = npos;
					if ( *pos == '\t' ) pos++;
					p[i] = (int)(td * r->dScale);
				} else {
					long tl;
					tl = strtol( pos, &npos, 10 );
					if ( pos == npos || errno == ERANGE ) {
						goto noMoreVotes;
					}
					pos = npos;
					if ( *pos == '\t' ) pos++;
					p[i] = tl;
				}
				if ( (size_t)(pos - line) > llen ) {
					if ( i != numc - 1 ) {
						fprintf( stderr, "warning, short line\n" );
					}
					break;
				}
			}
			line = NULL;
		}
#if 0
		fprintf( stderr, "voting %s\t", r->rating ? "rating " : "ranking" );
		for ( int i = 0; i < numc; i++ ) {
			if ( p[i] == NO_VOTE ) {
				fprintf( stderr, "\t");
			} else {
				fprintf( stderr, "%d\t", p[i] );
			}
		}
		fprintf( stderr, "\n");
#endif
		if ( r->rating ) {
			tab->voteRating( p );
		} else {
			tab->voteRanking( p );
		}
		if ( hist ) {
			if ( r->rating ) {
				hist->addRating( p );
			} else {
				hist->addRanking( p );
			}
		}
	}
noMoreVotes:
	fclose( r->fin );
	r->fin = NULL;
	r->numWinners = tab->getWinners( p );
	r->p = p;
	if ( r->printState ) {
		tab->print( stdout, r->names );
	}
	if ( r->htmlOut ) {
		tab->htmlSummary( stdout, r->names );
	}
	if ( hist ) {
		hist->print( stdout, r->histyle, r->names );
	}
	delete tab;
	return 0;
}

void printWinners( runParams* r ) {
	if ( r->scriptMode ) {
		for ( int i = 0; i < r->numWinners; i++ ) {
			printf("%d\n", r->p[i] );
		}
	} else if ( r->numWinners == 1 ) {
		if ( r->names ) {
			printf("Winner: %s\n", r->names[r->p[0]] );
		} else {
			printf("winner, choice number %d (of 1..%d)\n", r->p[0] + 1, r->numc );
		}
	} else {
		if ( r->names ) {
			printf("Winners: %s", r->names[r->p[0]] );
			for ( int i = 1; i < r->numWinners; i++ ) {
				printf(", %s", r->names[r->p[0]] );
			}
		} else {
			printf("%d winners, numbers %d", r->numWinners, r->p[0] + 1 );
			for ( int i = 1; i < r->numWinners; i++ ) {
				printf(", %d", r->p[i] + 1 );
			}
		}
		printf(" (of 1..%d)\n", r->numc );
	}
	delete [] r->p;
	r->p = NULL;
}

char* optstring = "AbBcdDfhHi:In:N:pqRrsST:u:w";
const char* const usageText = "vote [-bBcdfhHpqRrw][-n number of choices][-i vote file][vote file ...]\n";
const char* const helpText =
"\t-A\tInstant Runoff Normalized Ratings\n"
"\t-b\tbinary\n"
"\t-B\ttext (default)\n"
"\t-c\tCondorcet\n"
"\t-D\tBorda count (default)\n"
"\t-d\tinteger (default)\n"
"\t-f\tfloat (-1.0 .. 1.0)\n"
"\t-h\tthis help\n"
"\t-H\toutput html table\n"
"\t-i f\timmediately load and run vote file f\n"
"\t-I\tInstant Runoff Vote\n"
"\t-n N\tnumber of choices\n"
"\t-N f\tfile with names of choices\n"
"\t-p\tprint full state, not just winner\n"
"\t-q\tquiet, don't print winner\n"
"\t-R\tnumbers are rankings (1 best) (default)\n"
"\t-r\tnumbers are ratings (higher better)\n"
"\t-s\tscript friendly output (zero indexed \\n separated list of winners)\n"
"\t-S\tmake histogram of ranked or rated votes\n"
"\t-u\thistogram bar image url (default \"b.png\")\n"
"\t-T #\thistogram style #\n"
"\t-w\tRaw Sum of ratings\n"
;

FILE* myOpen( const char* optarg ) {
	if ( !strcmp(optarg,"-") ) {
		return stdin;
	} else {
		FILE* fin = fopen( optarg, "r" );
		if ( !fin ) {
			perror( optarg );
		}
		return fin;
	}
}


int main( int argc, char** argv ) {
	extern char* optarg;
	extern int optind;

	runParams r;

	r.getenv();
	optind = r.getArgs( argc, argv );
	if ( optind < 0 ) return 1;

	for ( ; optind < argc; optind++ ) {
		optarg = argv[optind];
		r.fin = myOpen( optarg );
		voteFromTextFile( &r );
		if ( ! r.quiet ) {
			printWinners( &r );
		}
	}

	return 0;
}

char** readNames( char* filename ) {
	char** toret;
	int namecap = 100;
	int nami = 0;
	char* cur;
	size_t curLen;
	FILE* fin;
	static char* linebuf = (char*)malloc(4096);
	void* otoret;

	fin = fopen( filename, "r" );
	if ( fin == NULL ) {
		perror( filename );
		return NULL;
	}

	toret = (char**)malloc( sizeof(char*) * namecap );
	assert(toret);
	while ( (cur = fgets( linebuf, 4096, fin )) != NULL ) {
		curLen = strlen( cur );
		if ( cur[curLen-1] == '\n' ) {
			curLen--;
		}
		toret[nami] = (char*)malloc( curLen + 1 );
		assert(toret[nami]);
		memcpy( toret[nami], cur, curLen );
		toret[nami][curLen] = '\0';
		nami++;
		if ( nami == namecap ) {
			otoret = toret;
			namecap *= 2;
			toret = (char**)realloc( toret, sizeof(char*) * namecap );
			if ( toret == NULL ) {
				perror("realloc");
				free( otoret );
				return NULL;
			}
		}
	}
	fclose( fin );
	otoret = toret;
	toret = (char**)realloc( toret, sizeof(char*) * nami );
	if ( toret == NULL ) {
		free( otoret );
	}
	return toret;
}


int runParams::getArgs( int argc, char** argv ) {
	int i;
	for ( i = 1; i < argc; i++ ) {
		if ( argv[i][0] != '-' ) {
			break;
		}
		switch ( argv[i][1] ) {
			case 'A':
				fac = IRNR::newIRNR;
				break;
			case 'B':
				isBin = false;
				break;
			case 'b':
				isBin = true;
				break;
			case 'c':
				fac = Condorcet::newCondorcet;
				break;
			case 'D':
				fac = BordaVotingSystem::newBordaVotingSystem;
				break;
			case 'd':
				isFloat = false;
				break;
			case 'f':
				isFloat = true;
				break;
			case 'h':	// help
				printf(usageText);
				printf(helpText);
				break;
			case 'H':
				htmlOut = true;
				break;
			case 'i':
				i++;
				inputname = argv[i];
#if 0
				fin = myOpen( argv[i] );
				voteFromTextFile( this );
				if ( ! quiet )
					printWinners( this );
#endif
				break;
			case 'I':
				fac = IRV::newIRV;
				break;
			case 'n':
				i++;
				numc = atoi( argv[i] );
				break;
			case 'N':
				// read names
				i++;
				names = readNames( argv[i] );
				break;
			case 'p':
				printState = true;
				break;
			case 'q':
				quiet = true;
				break;
			case 'R':
				rating = false;
				break;
			case 'r':
				rating = true;
				break;
			case 's':
				scriptMode = true;
				break;
			case 'S':
				histogram = true;
				break;
			case 'T':
				i++;
				histyle = atoi( argv[i] );
				histogram = true;
				break;
			case 'u':
				i++;
				histBarUrl = strdup( argv[i] );
				break;
			case 'w':
				fac = RawRating::newRawRating;
				break;
			case '-': // long option, or just "--"
				if ( argv[i][2] == '\0' ) {
					break;
				}
				if ( !strcmp(argv[i], "--test") ) {
					testMode = 1;
					break;
				}
			default:
				printf("unknown option \'%s\'\n", argv[i] );
				return -1;
		}
	}
	return i;
}
