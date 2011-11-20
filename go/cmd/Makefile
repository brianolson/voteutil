include $(GOROOT)/src/Make.inc

GCIMPORTS=-I ../lib/_obj
LDIMPORTS=-L../lib/_obj

TARG=countvotes
GOFILES=\
	countvotes.go\


include $(GOROOT)/src/Make.cmd

_go_.$O:	../lib/_obj/voting.a

../lib/_obj/voting.a:
	$(MAKE) -C ../lib
