#include "NameVotingSystem.h"
#include "IRNR.h"
#include <string.h>
#include <errno.h>
#include <assert.h>

#if HAVE_POSTGRES
#include "libpq-fe.h"
#endif

const char* helptext = "countvotes [--dump][--debug][--preindexed]\n"
"\t[--full-html|--no-full-html|--no-html-head]\n"
"\t[--disable-all][--enable-all]\n"
"\t[--enable|--disable hist|irnr|vrr|raw|irv|stv]\n"
"\tinput-file-name|\n"
#if HAVE_POSTGRES
"\t--pg \"PostgreSQL connect string\" --query \"SQL;\"\n"
#endif
;


struct vvsf {
	NameVotingSystem* (*make)(void);
	const char* name;
	const char* enableName;
	int enabled;
};

struct vvsf votestFatoryTable[] = {
	{
		makeIRNR, "Instant Runoff Normalized Ratings", "irnr", 1
	},{
		NULL, NULL, NULL, 0
	}
};

void voteLine( char* line, NameIndex* ni );
void doFile( const char* finame, NameIndex* ni );

#if HAVE_POSTGRES
void doPG( const char* finame, NameIndex* ni );
char* dbspec = NULL;// = "user=poll dbname=poll";
const char* dbquery = NULL;
#endif

int preindexed = 0;
NameVotingSystem** systems = NULL;
const char** systemNames = NULL;
int numSystems = 0;
int redumpVotes = 0;
int fullHtml = 1;
int testOutput = 0;
bool debug = false;

int main( int argc, char** argv ) {
	NameIndex ni;
	int i;
	const char* finame = NULL;

#if HAVE_POSTGRES
	dbspec = getenv("PGPOLLDB");
#endif

	for ( i = 1; i < argc; i++ ) {
		if ( !strcmp( argv[i], "--dump" )) {
			redumpVotes = 1;
		} else if ( !strcmp( argv[i], "--debug" )) {
			debug = true;
		} else if ( !strcmp( argv[i], "--preindexed" )) {
			preindexed = 1;
		} else if ( !strcmp( argv[i], "--full-html" )) {
			fullHtml = 1;
		} else if ( !strcmp( argv[i], "--no-full-html" )) {
			fullHtml = 0;
		} else if ( !strcmp( argv[i], "--no-html-head" )) {
			fullHtml = 0;
		} else if ( !strcmp( argv[i], "--test" )) {
			fullHtml = 0;
			testOutput = 1;
		} else if ( !strcmp( argv[i], "--enable" )) {
			int j;
			int any = 0;
			i++;
			for ( j = 0; votestFatoryTable[j].enableName != NULL; j++ ) {
				if ( !strcmp(votestFatoryTable[j].enableName, argv[i]) ) {
					votestFatoryTable[j].enabled = 1;
					any = 1;
				}
			}
			if ( ! any ) {
				printf("unknown --enable method \"%s\"\n\toptions are: %s", argv[i], votestFatoryTable[0].enableName );
				for ( j = 1; votestFatoryTable[j].enableName != NULL; j++ ) {
					printf(", %s", votestFatoryTable[j].enableName );
				}
				printf("\n");
				exit(1);
			}
		} else if ( !strcmp( argv[i], "--disable" )) {
			int j;
			int any = 0;
			i++;
			for ( j = 0; votestFatoryTable[j].enableName != NULL; j++ ) {
				if ( !strcmp(votestFatoryTable[j].enableName, argv[i]) ) {
					votestFatoryTable[j].enabled = 0;
					any = 1;
				}
			}
			if ( ! any ) {
				printf("unknown --disable method \"%s\"\n", argv[i] );
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
					(!strcmp( argv[i], "-h" )) ) {
			int j;
			puts(helptext);
			printf("known methods (short name for enable/disable, long name, enabled by default):\n");
			for ( j = 0; votestFatoryTable[j].enableName != NULL; j++ ) {
				printf("\t%s \"%s\" (%s)\n", votestFatoryTable[j].enableName, votestFatoryTable[j].name, votestFatoryTable[j].enabled ? "enabled" : "disabled" );
			}
			exit(0);
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

	if ( fullHtml ) {
		printf("<html><head><title>vote results</title></head><body bgcolor=\"#ffffff\" text=\"#000000\">\n");
	}
	if ( debug ) {
		printf("<pre>debug:\n");
	}

	i = 0;
	while ( votestFatoryTable[i].name != NULL ) {
		//printf("found system %d: %s\n", numSystems, votestFatoryTable[numSystems].name );
		if ( votestFatoryTable[i].enabled ) {
			numSystems++;
		}
		i++;
	}

	systems = (NameVotingSystem**)malloc( sizeof(NameVotingSystem*)*numSystems );
	systemNames = (const char**)malloc( sizeof(const char*)*numSystems );
	numSystems = 0;
	i = 0;
	while ( votestFatoryTable[i].name != NULL ) {
		//printf("found system %d: %s\n", numSystems, votestFatoryTable[numSystems].name );
		if ( votestFatoryTable[i].enabled ) {
			systems[numSystems] = votestFatoryTable[i].make();
			systemNames[numSystems] = votestFatoryTable[i].name;
			systems[numSystems]->setSharedNameIndex( &ni );
			//		printf("system[%d] \"%s\" initted\n", i, votestFatoryTable[i].name );
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
		printf("</pre>\n");
	}

	// print results
	NameVote* winners = NULL;
	for ( i = 0; i < numSystems; i++ ) {
		winners = NULL;
		int numWinners;
		numWinners = systems[i]->getWinners( 0, &winners );
		if ( testOutput ) {
			printf( "%s: ", systemNames[i] );
			if ( numWinners > 0 ) {
				int w;
				printf( "%s", winners[0].name );
				for ( w = 1; w < numWinners; w++ ) {
					printf( ", %s", winners[w].name );
				}
			}
			printf( "\n" );
		} else if ( 1 ) {
			printf( "<h2>%s</h2>", systemNames[i] );
			systems[i]->htmlSummary( stdout );
		} else {
			printf("system[%d]: \"%s\"\n", i, votestFatoryTable[i].name );
			int j;
			for ( j = 0; j < numWinners; j++ ) {
				printf( "\"%s\"\t%.9g\n", winners[j].name, winners[j].rating );
			}
			printf( "\n" );
		}
	}
	//printf( "done\n" );
	if ( fullHtml ) {
		printf("</body></html>\n");
	}
	return 0;
}


void voteLine( char* line, NameIndex* ni ) {
	StoredIndexVoteNode* x;
	int i;
	if ( preindexed ) {
		x = StoredIndexVoteNode::fromIndexURL(line);
		for ( i = 0; i < x->numVotes; i++ ) {
			while ( x->vote[i].index >= ni->nextIndex ) {
				char* name = (char*)malloc(12);
				snprintf(name,12,"%d",ni->nextIndex);
				// fetch index just to ensure name is in there
				ni->getIndex( name );
			}
		}
	} else {
		x = StoredIndexVoteNode::fromIndexURL(line);
	}
	if ( x == NULL ) {
		printf("error parsing vote line:\n\t%s\n", line );
		return;
	}
	//printf("read vote of %d name-rating pairs\n", x->numVotes );
	if ( redumpVotes ) {
		char* reconst;
		reconst = x->toURLVoteString(ni);
		printf("%s\n", reconst );
		free(reconst);
	}
	for ( i = 0; i < numSystems; i++ ) {
		StoredIndexVoteNode* use;
		if ( i + 1 < numSystems ) {
			use = x->clone();
		} else {
			// consume x on last system
			use = x;
		}
		systems[i]->voteStoredIndexVoteNode( use );
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
#define MAX_LINE_LEN 4096
#endif

void doFile( const char* finame, NameIndex* ni ) {
	FILE* fin;
	char* line;
	line = (char*)malloc( MAX_LINE_LEN+1 );
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

