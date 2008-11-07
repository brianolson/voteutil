## set the include and lib directory for your PostgreSQL if you have it
#COMPILE_POSTGRES:=-I/Library/PostgreSQL/include -DHAVE_POSTGRES
#LINK_POSTGRES:=-L/Library/PostgreSQL/lib -lpq

UNAME=$(shell uname)

-include make/${UNAME}

#GOPT=-g
GOPT=-O2
#GOPT=-g -pg

CFLAGS+=${GOPT} -Wall -ansi -pedantic -std=c99
#-MMD is incompatible with make/Darwin mode of building for multiple archs.
#Instead it can be enabled in other make/* targets.
#CFLAGS+=-MMD
LDFLAGS+=${GOPT} -lz

OBJS := NamedVotingSystem.o IRNR.o RawRating.o VRR.o IRV.o Histogram.o STV.o
OBJS += NChooseKSlates.o RoundScore.o

all:	libvote.a countvotes permuteutil

libvote.a:	${OBJS}
	ar -c -r libvote.a ${OBJS}
	ranlib libvote.a

countvotes:	libvote.a countvotes.o
countvotes:	LDFLAGS+=${LINK_POSTGRES}

countvotes.o:	CFLAGS+=${COMPILE_POSTGRES}

permuteutil:	libvote.a permuteutil.o

opt:
	${MAKE} GOPT=-O2

clean:
	rm -rf *.o libvote.a countvotes *.d permuteutil

-include *.d
