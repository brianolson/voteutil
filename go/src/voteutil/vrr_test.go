package voteutil

import "testing"

// votes := 

func TestVrrCompile(t *testing.T) {
	var em ElectionMethod
	em = new(VRR)
	em.Vote(nil)
}
