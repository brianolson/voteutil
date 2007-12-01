CPPOBJS := NameVotingSystem.o IRNR.o

OG:=-g
#OG:=-O2
CXXFLAGS:=-Wall -ansi -pedantic -MMD ${OG}

all:	countvotes_dynamic

countvotes_dynamic:	libvote_dynamic.a countvotes_named.o
	${CXX} countvotes_named.o libvote_dynamic.a -o countvotes_dynamic

libvote_dynamic.a:	${CPPOBJS}
	ar -c -r libvote_dynamic.a ${CPPOBJS}
	ranlib libvote_dynamic.a

clean:
	rm -rf *.o *.d libvote_dynamic.a countvotes_dynamic

-include *.d

