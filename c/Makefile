## set the include and lib directory for your PostgreSQL if you have it
#COMPILE_POSTGRES:=-I/Library/PostgreSQL/include -DHAVE_POSTGRES
#LINK_POSTGRES:=-L/Library/PostgreSQL/lib -lpq

UNAME=$(shell uname)

include make/${UNAME}

CFLAGS+=-g -Wall -ansi -pedantic -std=c99
#CFLAGS+=-O2 -Wall
LDFLAGS+=-g

OBJS := NamedVotingSystem.o IRNR.o RawRating.o VRR.o IRV.o Histogram.o STV.o

all:	libvote.a countvotes

libvote.a:	${OBJS}
	ar -c -r libvote.a ${OBJS}
	ranlib libvote.a

countvotes:	libvote.a countvotes.o
countvotes:	LDFLAGS+=${LINK_POSTGRES}

countvotes.o:	CFLAGS+=${COMPILE_POSTGRES}

## ALTERNATE BUILD LINES
#cc -g -Wall -DHAVE_POSTGRES countvotes.c -I/Library/PostgreSQL/include -L/Library/PostgreSQL/lib -lpq -L. -lvote
#cc -g -Wall countvotes.c -L. -lvote

## MY TEST RUN LINE
#time ./countvotes --pg "user=poll dbname=poll" --query "SELECT vote FROM votes WHERE pid = 10;" --dump

clean:
	rm -rf *.o libvote.a countvotes
