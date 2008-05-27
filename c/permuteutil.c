#include "NChooseKSlates.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

const char usage[] = "usage:\n"
"\tpermuteutil <choose> \"choice 1\" \"choice 2\" ...\n"
"\tpermuteutil calc n k\n";

int main(int argc, const char** argv) {
	NameIndex natural;
	NameIndex permed;
	unsigned long k;
	int i;
	
	if ( !strcmp(argv[1], "calc")) {
		unsigned long n;
		n = strtoul(argv[2], NULL, 10);
		k = strtoul(argv[3], NULL, 10);
		printf("%lu choose %lu = %lu\n", n, k, NChooseK(n, k));
		return 0;
	}
	{
		char* endp;
		k = strtoul(argv[1], &endp, 10);
		if (endp == argv[1]) {
			fprintf(stderr, "could not understand choice count \"%s\"\n", argv[1]);
			fputs(usage, stderr);
			exit(1);
		}
	}

	initNameIndex( &natural );
	initNameIndex( &permed );

	for (i = 2; i < argc; ++i) {
		nameIndex( &natural, argv[i] );
	}

	enumerateNChoseKNameIndex( &natural, natural.nextIndex, k, &permed );
	
	for (i = 0; i < permed.nextIndex; ++i) {
		printf("%s\n", permed.names[i]);
	}
	fflush(stdout);
	fprintf(stderr, "(%d == %d) permutations\n", i, permed.nextIndex);
	return 0;
}
