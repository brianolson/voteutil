#include "NamedVotingSystem.h"
#include "IRNR.h"
#include <string.h>
#include <errno.h>
#include <assert.h>

const char* helptext = "countvotes [--dump][--debug][--preindexed]\n"
"\t[--full-html|--no-full-html|--no-html-head]\n"
"\t[--disable-all][--enable-all]\n"
"\t[--rankings][--help|-h|--list][--explain]\n"
"\t[-o filename|--out filenam]\n"
"\t[--enable|--disable hist|irnr|vrr|raw|irv|stv]\n"
"\t[input file name|-i filename]\n"
#if HAVE_POSTGRES
"\t--pg \"PostgreSQL connect string\" --query \"SQL;\"\n"
#endif
;

VirtualVotingSystem* newVirtualRawRating();
VirtualVotingSystem* newVirtualIRNR();
VirtualVotingSystem* newVirtualHistogram();
VirtualVotingSystem* newVirtualIRV();
VirtualVotingSystem* newVirtualVRR();
VirtualVotingSystem* newVirtualRankedPairs();
VirtualVotingSystem* newVirtualSTV();

struct vvsf {
	VirtualVotingSystem* (*make)(void);
	const char* name;
	const char* enableName;
	int enabled;
};

struct vvsf votestFatoryTable[] = {
	{
		newVirtualHistogram, "Histogram", "hist", 1
	},{
		newVirtualIRNR, "Instant Runoff Normalized Ratings", "irnr", 1
	},{
		newVirtualVRR, "Virtual Round Robin", "vrr", 1
	},{
		newVirtualRankedPairs, "Virtual Round Robin, Ranked Pairs Resolution", "rp", 0
	},{
		newVirtualRawRating, "Raw Rating Summation", "raw", 1
	},{
		newVirtualIRV, "Instant Runoff Voting", "irv", 1
	},{
		newVirtualSTV, "Single Transferrable Vote", "stv", 0
	},{
		NULL, NULL, NULL, 0
	}
};

int setMethodEnabled(const char* shortName, int enable) {
	int j;
	int enableCount = 0;
	for ( j = 0; votestFatoryTable[j].enableName != NULL; j++ ) {
		if ( !strcmp(votestFatoryTable[j].enableName, shortName) ) {
			votestFatoryTable[j].enabled = enable;
			enableCount++;
		}
	}
	return enableCount;
}

void listMethods(FILE* out) {
	int j;
	fprintf(out, "known methods (short name for enable/disable, long name, enabled by default):\n");
	for ( j = 0; votestFatoryTable[j].enableName != NULL; j++ ) {
		fprintf(out, "\t%s \"%s\" (%s)\n", votestFatoryTable[j].enableName, votestFatoryTable[j].name, votestFatoryTable[j].enabled ? "enabled" : "disabled" );
	}
}

void voteLine( char* line, NameIndex* ni );
void doFile( const char* finame, NameIndex* ni );

#if HAVE_POSTGRES
#include "libpq-fe.h"

void doPG( const char* finame, NameIndex* ni );
char* dbspec = NULL;// = "user=poll dbname=poll";
const char* dbquery = NULL;
#endif

int preindexed = 0;
VirtualVotingSystem** systems = NULL;
const char** systemNames = NULL;
int numSystems = 0;
int redumpVotes = 0;
int fullHtml = 1;
int testOutput = 0;
int inputIsRankings = 0;
int seats = 1;
int explain = 0;

int main( int argc, char** argv ) {
	NameIndex ni;
	int i;
	const char* finame = NULL;
	const char* foname = NULL;
	FILE* out = stdout;

#if HAVE_POSTGRES
	dbspec = getenv("PGPOLLDB");
#endif

	for ( i = 1; i < argc; i++ ) {
		if ( !strcmp( argv[i], "--dump" )) {
			redumpVotes = 1;
		} else if ( !strcmp( argv[i], "--debug" )) {
			debug = 1;
		} else if ( !strcmp( argv[i], "--preindexed" )) {
			preindexed = 1;
		} else if ( !strcmp( argv[i], "--rankings" )) {
			inputIsRankings = 1;
		} else if ( !strcmp( argv[i], "--explain" )) {
			explain = 1;
		} else if ( !strcmp( argv[i], "--full-html" )) {
			fullHtml = 1;
		} else if ( !strcmp( argv[i], "--no-full-html" )) {
			fullHtml = 0;
		} else if ( !strcmp( argv[i], "--no-html-head" )) {
			fullHtml = 0;
		} else if ( !strcmp( argv[i], "--seats" )) {
			char* endp;
			i++;
			seats = strtol(argv[i], &endp, 10);
			if ((endp == argv[i]) || (errno == ERANGE) || (errno == EINVAL) || (seats <= 0)) {
				fprintf(stderr, "can't understand --seats \"%s\"\n", argv[i]);
				exit(1);
			}
			if (seats > 1) {
				// disable all, enable multi-seat capable.
				int j;
				for ( j = 0; votestFatoryTable[j].name != NULL; j++ ) {
					votestFatoryTable[j].enabled = 0;
				}
				assert(setMethodEnabled("hist", 1));
				assert(setMethodEnabled("stv", 1));
			}
		} else if ( !strcmp( argv[i], "--test" )) {
			int j;
			fullHtml = 0;
			testOutput = 1;
			for ( j = 0; votestFatoryTable[j].name != NULL; j++ ) {
				votestFatoryTable[j].enabled = 1;
			}
			assert(setMethodEnabled("hist", 0));
		} else if ( !strcmp( argv[i], "--enable" )) {
			int any = 0;
			i++;
			any = setMethodEnabled(argv[i], 1);
			if ( ! any ) {
				fprintf(stderr, "unknown --enable method \"%s\"\n", argv[i] );
				listMethods(stderr);
				exit(1);
			}
		} else if ( !strcmp( argv[i], "--disable" )) {
			int any = 0;
			i++;
			any = setMethodEnabled(argv[i], 0);
			if ( ! any ) {
				fprintf(stderr, "unknown --disable method \"%s\"\n", argv[i] );
				listMethods(stderr);
				exit(1);
			}
		} else if ( !strcmp( argv[i], "--disable-all" )) {
			int j;
			for ( j = 0; votestFatoryTable[j].name != NULL; j++ ) {
				votestFatoryTable[j].enabled = 0;
			}
		} else if ( !strcmp( argv[i], "--enable-all" )) {
			int j;
			for ( j = 0; votestFatoryTable[j].name != NULL; j++ ) {
				votestFatoryTable[j].enabled = 1;
			}
#if HAVE_POSTGRES
		} else if ( !strcmp( argv[i], "--pg" )) {
			i++;
			dbspec = argv[i];
		} else if ( !strcmp( argv[i], "--query" )) {
			i++;
			dbquery = argv[i];
#endif
		} else if ( (!strcmp( argv[i], "--help" )) ||
				    (!strcmp( argv[i], "-h" )) ||
				    (!strcmp( argv[i], "--list" )) ) {
			puts(helptext);
			listMethods(stdout);
			exit(0);
		} else if ( (!strcmp( argv[i], "-o" )) ||
				    (!strcmp( argv[i], "--out")) ) {
			i++;
			foname = argv[i];
			out = fopen(foname, "w");
			if (out == NULL) {
				perror(foname);
				exit(1);
			}
		} else if ( !strcmp( argv[i], "-i" )) {
			i++;
			finame = argv[i];
		} else if ( finame == NULL ) {
			finame = argv[i];
		} else {
			fprintf(stderr,"bogus arg \"%s\"\n", argv[i] );
			exit(1);
		}
	}

	initNameIndex( &ni );

	if ( fullHtml ) {
		fprintf(out, "<html><head><title>vote results</title></head><body bgcolor=\"#ffffff\" text=\"#000000\">\n");
	}
	if ( debug ) {
		fprintf(out, "<pre>debug:\n");
	}

	i = 0;
	while ( votestFatoryTable[i].name != NULL ) {
		//printf("found system %d: %s\n", numSystems, votestFatoryTable[numSystems].name );
		if ( votestFatoryTable[i].enabled ) {
			numSystems++;
		}
		i++;
	}

	systems = (VirtualVotingSystem**)malloc( sizeof(VirtualVotingSystem*)*numSystems );
	systemNames = (const char**)malloc( sizeof(const char*)*numSystems );
	numSystems = 0;
	i = 0;
	while ( votestFatoryTable[i].name != NULL ) {
		//printf("found system %d: %s\n", numSystems, votestFatoryTable[numSystems].name );
		if ( votestFatoryTable[i].enabled ) {
			systems[numSystems] = votestFatoryTable[i].make();
			if ( testOutput && 0 ) {
				systemNames[numSystems] = votestFatoryTable[i].enableName;
			} else {
				systemNames[numSystems] = votestFatoryTable[i].name;
			}
			systems[numSystems]->setSharedNameIndex( systems[numSystems]->it, &ni );
			//		printf("system[%d] \"%s\" initted\n", i, votestFatoryTable[i].name );
			if ( seats != 1 ) {
				if ( systems[numSystems]->setSeats == NULL ) {
					fprintf(stderr, "electing %d seats but system \"%s\" is not multi-seat capable\n", seats, systemNames[numSystems] );
					exit(1);
				}
				systems[numSystems]->setSeats(systems[numSystems]->it, seats);
			}
			numSystems++;
		}
		i++;
	}

	// done with setup, read and count votes

#if HAVE_POSTGRES
	if ( dbspec != NULL ) {
		doPG( finame, &ni );
	} else
#endif
	{
		doFile( finame, &ni );
	}

	if ( debug ) {
		fprintf(out, "</pre>\n");
	}

	// print results
	NameVote* winners = NULL;
	for ( i = 0; i < numSystems; i++ ) {
		winners = NULL;
		int numWinners;
		if ( testOutput ) {
			numWinners = systems[i]->getWinners( systems[i]->it, 0, &winners );
			fprintf(out, "%s: ", systemNames[i] );
			if ( numWinners > 0 ) {
				int w;
				fprintf(out, "%s", winners[0].name );
				for ( w = 1; w < numWinners; w++ ) {
					fprintf(out, ", %s", winners[w].name );
				}
			}
			fprintf(out, "\n" );
		} else if ( 1 ) {
			fprintf(out, "<h2>%s</h2>", systemNames[i] );
			if ( explain ) {
				systems[i]->htmlExplain( systems[i]->it, out );
			} else {
//				numWinners = systems[i]->getWinners( systems[i]->it, 0, &winners );
				systems[i]->htmlSummary( systems[i]->it, out );
			}
		} else {
			fprintf(out, "system[%d]: \"%s\"\n", i, votestFatoryTable[i].name );
			int j;
			for ( j = 0; j < numWinners; j++ ) {
				fprintf(out, "\"%s\"\t%.9g\n", winners[j].name, winners[j].rating );
			}
			fprintf(out, "\n" );
		}
	}
	//printf( "done\n" );
	if ( fullHtml ) {
		fprintf(out, "</body></html>\n");
	}
	return 0;
}


void voteLine( char* line, NameIndex* ni ) {
	StoredIndexVoteNode* x;
	int i;
	if ( preindexed ) {
		x = newStoredIndexVoteNodeFromIndexURL(line);
		for ( i = 0; i < x->numVotes; i++ ) {
			while ( x->vote[i].index >= ni->nextIndex ) {
				char* name = (char*)malloc(12);
				snprintf(name,12,"%d",ni->nextIndex);
				nameIndex( ni, name );
			}
		}
	} else {
		x = newStoredIndexVoteNodeFromURL(ni,line);
	}
	if ( x == NULL ) {
		fprintf(stderr, "error parsing vote line:\n\t%s\n", line );
		return;
	}
	if ( inputIsRankings ) {
		convertRankingsAndRatings(x);
	}
	//printf("read vote of %d name-rating pairs\n", x->numVotes );
	if ( redumpVotes ) {
		char* reconst;
		reconst = storedIndexVoteToURL( ni, x );
		printf("%s\n", reconst );
		free(reconst);
	}
	for ( i = 0; i < numSystems; i++ ) {
		StoredIndexVoteNode* use;
		if ( i + 1 < numSystems ) {
			use = dupStoredIndexVoteNode( x );
		} else {
			// consume x on last system
			use = x;
		}
		systems[i]->voteStoredIndexVoteNode( systems[i]->it, use );
	}
}

#if HAVE_POSTGRES
void doPG( const char* finame, NameIndex* ni ) {
	PGconn* conn;
	PGresult* rs;
	int row, nrows;
	char* line;

	conn = PQconnectdb( dbspec );
	if ( conn == NULL ) {
		fprintf(stderr,"failed to connect with dbspec: %s\n", dbspec);
		exit(1);
	}
	if ( dbquery == NULL ) {
		dbquery = finame;
	}
	if ( dbquery == NULL ) {
		fprintf(stderr,"database specified but no query\n");
		exit(1);
	}
	rs = PQexec( conn, dbquery );
	if ( rs == NULL ) {
		fprintf(stderr,"query \"%s\" failed: %s\n", dbquery, PQerrorMessage( conn ) );
		PQfinish(conn);
		exit(1);
	}

	nrows = PQntuples( rs );
	for ( row = 0; row < nrows; row++ ) {
		line = PQgetvalue( rs, row, 0 );
		voteLine( line, ni );
	}
	PQfinish( conn );
}
#endif

#ifndef MAX_LINE_LEN
#define MAX_LINE_LEN 0xffff
#endif

void doFile( const char* finame, NameIndex* ni ) {
	FILE* fin;
	char* line;
	line = malloc( MAX_LINE_LEN+1 );
	assert(line != NULL);

	if ( finame == NULL ) {
		fin = stdin;
	} else {
		//printf("opening \"%s\"\n", finame );
		fin = fopen( finame, "r" );
		if ( fin == NULL ) {
			perror(finame);
			exit(1);
		}
	}

	while ( fgets( line, MAX_LINE_LEN, fin ) != NULL ) {
		voteLine( line, ni );
	}
	fclose( fin );

	free( line );
}

