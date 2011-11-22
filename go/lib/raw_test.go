package voting

import "testing"

func TestRawCompile(t *testing.T) {
	var em ElectionMethod
	em = new(RawSummation)
	em.Vote(nil)
}
