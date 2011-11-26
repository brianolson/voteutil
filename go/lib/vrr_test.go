package voting

import "testing"

func TestVrrBasic(t *testing.T) {
	BasicEM(t, new(VRR))
}

func TestVrrTie(t *testing.T) {
	RankTie(t, new(VRR))
}
