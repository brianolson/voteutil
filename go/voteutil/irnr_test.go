package voteutil

import (
	"log"
	"sort"
	"strings"
	"testing"
)

func TestIrnrBasic(t *testing.T) {
	BasicEM(t, NewIRNR())
}

func TestIrnrTie(t *testing.T) {
	RankTie(t, NewIRNR())
}

func winnerNameList(result *NameVote) []string {
	out := make([]string, len(*result))
	for oi, nv := range *result {
		out[oi] = nv.Name
	}
	//sort.Strings(out)
	return out
}

func stringSetEquiv(a, b []string) bool {
	if a == nil {
		if b != nil {
			return false
		}
		return true
	}
	if b == nil {
		return false
	}
	if len(a) != len(b) {
		return false
	}
	sort.Strings(a)
	sort.Strings(b)
	for ai, ax := range a {
		if b[ai] != ax {
			return false
		}
	}
	return true
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

const twoSeatOneVote = `a=9&b=8&c=7&d=6&e=5&f=4&g=3&h=2&i=1`

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

// Just like A except 0 -> nil
const threeSeatB = `a=9&c=8&d=2
a=9&c=8&d=2
a=9&c=8&d=2
a=9&c=8&d=2
a=9&c=8&d=2
a=9&c=8&d=2
a=9&c=8&d=2
b=9
b=9
b=9
b=9
b=9
d=2
d=2`

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

func ThreeSeatB() []NameVote {
	return stringToNameVotes(threeSeatB)
}

func TestIrnrTrivial2Seat(t *testing.T) {
	em := NewIRNR()
	multiSeatTrivial2Seat(t, em)
}

func multiSeatTrivial2Seat(t *testing.T, em MultiSeat) {
	multiSeat2SeatInner(t, em, twoSeatA)
}

func multiSeat2SeatOneVote(t *testing.T, em MultiSeat) {
	multiSeat2SeatInner(t, em, twoSeatOneVote)
}

func multiSeat2SeatInner(t *testing.T, em MultiSeat, votes string) {
	t.Parallel()
	em.SetSeats(2)

	va := stringToNameVotes(votes)
	for _, nv := range va {
		em.Vote(nv)
	}

	results, numWinners := em.GetResult()
	if results == nil {
		t.Fatal("nil results")
	}
	if len(*results) < 2 {
		t.Fatal("short results", results)
	}
	xr := []string{(*results)[0].Name, (*results)[1].Name}
	sort.Strings(xr)
	ExpectString(t, xr[0], "a")
	ExpectString(t, xr[1], "b")
	ExpectInt(t, numWinners, 2)
}

func TestIrnr3SeatA(t *testing.T) {
	em := NewIRNR()
	multiSeat3SeatA(t, em)
}

func TestIrnr3SeatB(t *testing.T) {
	em := NewIRNR()
	multiSeat3SeatB(t, em)
}

func multiSeat3SeatA(t *testing.T, em MultiSeat) {
	multiSeat3SeatInner(t, em, ThreeSeatA())
}

func multiSeat3SeatB(t *testing.T, em MultiSeat) {
	multiSeat3SeatInner(t, em, ThreeSeatB())
}

func multiSeat3SeatInner(t *testing.T, em MultiSeat, votes []NameVote) {
	t.Parallel()
	em.SetSeats(3)

	for _, nv := range votes {
		em.Vote(nv)
	}

	results, numWinners := em.GetResult()
	if results == nil || len(*results) < 3 {
		t.Fatal("wat")
		return
	}
	ExpectInt(t, numWinners, 3)
	winningNames := winnerNameList(results)
	goodset := []string{"a", "b", "c"}
	// set equivalency is good enough, don't have to return set in order
	if !stringSetEquiv(goodset, winningNames[:3]) {
		t.Errorf("bad results, wanted %v, got %v", goodset, results)
	}
}
