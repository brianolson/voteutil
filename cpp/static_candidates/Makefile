CPPOBJS := BordaVotingSystem.o Condorcet.o Histogram.o IRNR.o IRV.o
CPPOBJS += RankedVotingSystem.o RatedVotingSystem.o RawRating.o
CPPOBJS += VotingSystem.o

CXXFLAGS:=-Wall -ansi -pedantic -MMD

all:	countvotes_static

countvotes_static:	libvote_static.a vote.o
	${CXX} vote.o libvote_static.a -o countvotes_static

libvote_static.a:	${CPPOBJS}
	ar -c -r libvote_static.a ${CPPOBJS}
	ranlib libvote_static.a

clean:
	rm -rf *.o *.d libvote_static.a countvotes_static

-include *.d

