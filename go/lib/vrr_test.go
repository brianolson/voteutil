package voting

import "testing"

// votes := 

func TestEM(t *testing.T) {
	var em ElectionMethod
	em = new(VRR)
	em.Vote(nil)
}
