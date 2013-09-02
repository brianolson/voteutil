package voteutil

import "testing"

func TestVrrBasic(t *testing.T) {
	BasicEM(t, new(VRR))
}

func TestVrrTie(t *testing.T) {
	RankTie(t, new(VRR))
}

func TestVrr2(t *testing.T) {
	var x NameVote
	x = make(NameVote, 2)
	em := new(VRR)
	em.Vote(x)
	_, _ = em.GetResult()
}
