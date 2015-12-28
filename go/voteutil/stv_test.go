package voteutil

import "testing"

func TestSTVTrivial2Seat(t *testing.T) {
	em := NewSTV()
	multiSeatTrivial2Seat(t, em)
}

func TestSTV3SeatA(t *testing.T) {
	em := NewSTV()
	multiSeat3SeatA(t, em)
}

func TestSTV3SeatB(t *testing.T) {
	em := NewSTV()
	multiSeat3SeatB(t, em)
}
