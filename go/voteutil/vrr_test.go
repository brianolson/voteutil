package voteutil

import "testing"

func TestVrrBasic(t *testing.T) {
	BasicEM(t, new(VRR))
}

func TestVrrTie(t *testing.T) {
	RankTie(t, new(VRR))
}

func TestVrr2(t *testing.T) {
	t.Parallel()
	var x NameVote
	x = make(NameVote, 2)
	em := new(VRR)
	em.Vote(x)
	_, _ = em.GetResult()
}

func voteString(t *testing.T, em ElectionMethod, vote string) {
	nv, err := UrlToNameVote(vote)
	if err != nil {
		t.Errorf("bad vote string in test, %#v: %s", vote, err)
	}
	em.Vote(*nv)
}

func vrrInnerCheck(t *testing.T, em *VRR, a, b, expected int) {
	v := em.get(a, b)
	if expected != v {
		t.Errorf("vrr[%d > %d] = %d but expected %d", a, b, v, expected)
	}
}

func vrrIdCheck(t *testing.T, em *VRR, name string, id int) {
	actual, ok := em.Names.Indexes[name]
	if !ok {
		t.Errorf("vrr[%#v] unknown choice!", name)
	}
	if actual != id {
		t.Errorf("vrr[%#v] id=%d but wanted %d", name, actual, id)
	}
}

func TestVRRAB(t *testing.T) {
	t.Parallel()
	em := new(VRR)
	voteString(t, em, "a=9")
	voteString(t, em, "b=9")
	vrrInnerCheck(t, em, 0, 1, 1)
	vrrInnerCheck(t, em, 1, 0, 1)
}
func TestVRRABC(t *testing.T) {
	t.Parallel()
	em := new(VRR)
	voteString(t, em, "a=9")
	voteString(t, em, "b=9")
	voteString(t, em, "c=9&a=8")
	vrrInnerCheck(t, em, 0, 1, 2)
	vrrInnerCheck(t, em, 1, 0, 1)
	vrrInnerCheck(t, em, 2, 0, 1)
	vrrInnerCheck(t, em, 2, 1, 1)
}

func TestVRRABC2(t *testing.T) {
	t.Parallel()
	em := new(VRR)
	voteString(t, em, "a=9&b=8")
	voteString(t, em, "b=9&a=8")
	voteString(t, em, "c=9")
	vrrInnerCheck(t, em, 0, 1, 1) // a > b
	vrrInnerCheck(t, em, 1, 0, 1) // b > a
	vrrInnerCheck(t, em, 2, 0, 1) // c > a
	vrrInnerCheck(t, em, 0, 2, 2) // a > c
	vrrInnerCheck(t, em, 1, 2, 2) // b > c
}

func TestVRRABCD(t *testing.T) {
	t.Parallel()
	em := new(VRR)
	voteString(t, em, "a=9&b=8")
	vrrIdCheck(t, em, "a", 0)
	vrrIdCheck(t, em, "b", 1)
	voteString(t, em, "b=9&a=8")
	voteString(t, em, "c=9")
	vrrIdCheck(t, em, "c", 2)
	voteString(t, em, "d=9")
	vrrIdCheck(t, em, "d", 3)
	voteString(t, em, "a=9")
	vrrInnerCheck(t, em, 0, 1, 2) // a > b
	vrrInnerCheck(t, em, 1, 0, 1) // b > a
	vrrInnerCheck(t, em, 2, 0, 1) // c > a
	vrrInnerCheck(t, em, 0, 2, 3) // a > c
	vrrInnerCheck(t, em, 1, 2, 2) // b > c
	vrrInnerCheck(t, em, 0, 3, 3) // a > d
}
