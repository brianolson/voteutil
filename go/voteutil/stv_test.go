package voteutil

import "testing"

func TestSTVTrivial2Seat(t *testing.T) {
	em := NewSTV()
	multiSeatTrivial2Seat(t, em)
}

func TestSTV3Seat(t *testing.T) {
	em := NewSTV()
	multiSeat3Seat(t, em)
}
