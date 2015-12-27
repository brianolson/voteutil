package voteutil

import (
	"log"
	"strings"
	"testing"
)

func TestIrnrBasic(t *testing.T) {
	BasicEM(t, NewIRNR())
}

func TestIrnrTie(t *testing.T) {
	RankTie(t, NewIRNR())
}

const twoSeatA = `a=9&b=0&c=0
a=9&b=0&c=0
a=9&b=0&c=0
a=9&b=0&c=0
a=0&b=9&c=0
a=0&b=9&c=0
a=0&b=9&c=0
a=0&b=0&c=2
a=0&b=0&c=2`

const threeSeatA = `a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=9&b=0&c=8&d=2
a=0&b=9&c=0&d=0
a=0&b=9&c=0&d=0
a=0&b=9&c=0&d=0
a=0&b=9&c=0&d=0
a=0&b=9&c=0&d=0
a=0&b=0&c=0&d=2
a=0&b=0&c=0&d=2`

func stringToNameVotes(text string) []NameVote {
	parts := strings.Split(text, "\n")
	out := make([]NameVote, len(parts))
	for pi, p := range parts {
		nv, err := UrlToNameVote(p)
		if err != nil {
			log.Print(err)
			return nil
		}
		out[pi] = *nv
	}
	return out
}

func TwoSeatA() []NameVote {
	return stringToNameVotes(twoSeatA)
}

func ThreeSeatA() []NameVote {
	return stringToNameVotes(threeSeatA)
}

/*
var IRNR2SeatTestVotes []string = []string{
	"a=9&b=0&c=0",
	"a=9&b=0&c=0",
	"a=9&b=0&c=0",
	"a=9&b=0&c=0",

	"a=0&b=9&c=0",
	"a=0&b=9&c=0",
	"a=0&b=9&c=0",

	"a=0&b=0&c=2",
	"a=0&b=0&c=2",
}
*/

func TestIrnrTrivial2Seat(t *testing.T) {
	em := NewIRNR()
	multiSeatTrivial2Seat(t, em)
}

func multiSeatTrivial2Seat(t *testing.T, em MultiSeat) {
	//ms := em.(MultiSeat)
	em.SetSeats(2)

	/*for _, vs := range IRNR2SeatTestVotes {
		nv, err := UrlToNameVote(vs)
		if err != nil {
			t.Fatal(err)
			return
		}
		em.Vote(*nv)
	}*/
	for _, nv := range TwoSeatA() {
		em.Vote(nv)
	}

	results, numWinners := em.GetResult()
	ExpectString(t, (*results)[0].Name, "a")
	ExpectString(t, (*results)[1].Name, "b")
	ExpectInt(t, numWinners, 2)
}

/*
var IRNR3SeatTestVotes []string = []string{
	// Largest faction prefers: a,c
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",
	"a=9&b=0&c=8&d=2",

	// Second faction wants: b
	"a=0&b=9&c=0&d=0",
	"a=0&b=9&c=0&d=0",
	"a=0&b=9&c=0&d=0",
	"a=0&b=9&c=0&d=0",
	"a=0&b=9&c=0&d=0",

	// minority faction wants d, sorry
	"a=0&b=0&c=0&d=2",
	"a=0&b=0&c=0&d=2",
}
*/

func TestIrnr3Seat(t *testing.T) {
	em := NewIRNR()
	multiSeat3Seat(t, em)
}

func multiSeat3Seat(t *testing.T, em MultiSeat) {
	em.SetSeats(3)

/*
	for _, vs := range IRNR3SeatTestVotes {
		nv, err := UrlToNameVote(vs)
		if err != nil {
			t.Fatal(err)
			return
		}
*/
	for _, nv := range ThreeSeatA() {
		em.Vote(nv)
	}

	results, numWinners := em.GetResult()
	if results == nil || len(*results) < 3 {
		t.Fatal("wat")
		return
	}
	ExpectString(t, (*results)[0].Name, "a")
	ExpectString(t, (*results)[1].Name, "b")
	ExpectString(t, (*results)[2].Name, "c")
	ExpectInt(t, numWinners, 3)
}
