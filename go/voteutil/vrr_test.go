package voteutil

import (
	"strings"
	"testing"
)

func TestVrrBasic(t *testing.T) {
	BasicEM(t, NewVRR())
}

func TestVrrTie(t *testing.T) {
	RankTie(t, NewVRR())
}

func TestVrr2(t *testing.T) {
	t.Parallel()
	var x NameVote
	x = make(NameVote, 2)
	em := NewVRR()
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
	em := NewVRR()
	voteString(t, em, "a=9")
	voteString(t, em, "b=9")
	vrrInnerCheck(t, em, 0, 1, 1)
	vrrInnerCheck(t, em, 1, 0, 1)
}
func TestVRRABC(t *testing.T) {
	t.Parallel()
	em := NewVRR()
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
	em := NewVRR()
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
	em := NewVRR()
	voteString(t, em, "a=9&b=8")
	vrrIdCheck(t, em, "a", 0)
	vrrIdCheck(t, em, "b", 1)
	voteString(t, em, "b=9&a=8")
	voteString(t, em, "c=9")
	vrrIdCheck(t, em, "c", 2)
	voteString(t, em, "d=9")
	vrrIdCheck(t, em, "d", 3)
	voteString(t, em, "a=1")
	vrrInnerCheck(t, em, 0, 1, 2) // a > b
	vrrInnerCheck(t, em, 1, 0, 1) // b > a
	vrrInnerCheck(t, em, 2, 0, 1) // c > a
	vrrInnerCheck(t, em, 0, 2, 3) // a > c
	vrrInnerCheck(t, em, 1, 2, 2) // b > c
	vrrInnerCheck(t, em, 0, 3, 3) // a > d
}

func testResultBefore(t *testing.T, a, b string, result *NameVote) {
	ai := -1
	bi := -1
	for i, v := range *result {
		if v.Name == a {
			ai = i
		}
		if v.Name == b {
			bi = i
		}
	}
	if ai == -1 {
		t.Errorf("did not find %v in result", a)
		return
	}
	if bi == -1 {
		t.Errorf("did not find %v in result", b)
		return
	}
	if bi < ai {
		t.Errorf("out of order %v[%v] @ %d not before %v[%v] @ %d", a, (*result)[ai].Rating, ai, b, (*result)[bi].Rating, bi)
	}
}

func TestVRROrderedResult(t *testing.T) {
	t.Parallel()
	em := NewVRR()
	voteString(t, em, "a=1&b=1&c=0&d=0&e=0")

	result, _ := em.GetResult()
	testResultBefore(t, "a", "c", result)
	testResultBefore(t, "a", "d", result)
	testResultBefore(t, "a", "e", result)
	testResultBefore(t, "b", "c", result)
	testResultBefore(t, "b", "d", result)
	testResultBefore(t, "b", "e", result)
}

func TestVRROrderedResultSharedNameMapFull(t *testing.T) {
	t.Parallel()
	em := NewVRR()
	nm := NameMap{}
	nm.Names = []string{"a", "b", "c", "d", "e"}
	nm.Indexes = make(map[string]int, len(nm.Names))
	for xi, xname := range nm.Names {
		nm.Indexes[xname] = xi
	}
	em.SetSharedNameMap(&nm)
	xv := IndexVote{Indexes: []int{0, 1, 2, 3, 4}, Ratings: []float64{1, 1, 0, 0, 0}}
	em.VoteIndexes(xv)

	result, _ := em.GetResult()
	testResultBefore(t, "a", "c", result)
	testResultBefore(t, "a", "d", result)
	testResultBefore(t, "a", "e", result)
	testResultBefore(t, "b", "c", result)
	testResultBefore(t, "b", "d", result)
	testResultBefore(t, "b", "e", result)
}

func testExplainIndexBefore(t *testing.T, namea, nameb, explain string) {
	aindex := strings.Index(explain, namea)
	bindex := strings.Index(explain, nameb)
	ok := true
	if aindex < 0 {
		t.Errorf("%v not found in %v", namea, explain)
		ok = false
	}
	if bindex < 0 {
		t.Errorf("%v not found in %v", nameb, explain)
		ok = false
	}
	if ok && bindex < aindex {
		t.Errorf("wanted %v before %v in %v", namea, nameb, explain)
	}
}

func TestVRROrderedResultSharedNameMapPartial(t *testing.T) {
	t.Skip("doesn't quite pass yet")
	t.Parallel()
	em := NewVRR()
	em.debug = true
	nm := NameMap{}
	nm.Names = []string{"aCNAME", "bCNAME", "cCNAME", "dCNAME", "eCNAME", "fCNAME"}
	nm.Indexes = make(map[string]int, len(nm.Names))
	for xi, xname := range nm.Names {
		nm.Indexes[xname] = xi
	}
	em.SetSharedNameMap(&nm)
	xv := IndexVote{Indexes: []int{2, 4}, Ratings: []float64{1, 2}}
	t.Logf("vote %#v", xv)
	em.VoteIndexes(xv)

	result, _, explain := em.HtmlExplaination()
	testExplainIndexBefore(t, "cCNAME", "aCNAME", explain)
	testExplainIndexBefore(t, "cCNAME", "bCNAME", explain)
	testExplainIndexBefore(t, "cCNAME", "dCNAME", explain)
	testExplainIndexBefore(t, "cCNAME", "fCNAME", explain)
	testExplainIndexBefore(t, "eCNAME", "aCNAME", explain)
	testExplainIndexBefore(t, "eCNAME", "bCNAME", explain)
	testExplainIndexBefore(t, "eCNAME", "cCNAME", explain)
	testExplainIndexBefore(t, "eCNAME", "dCNAME", explain)
	testExplainIndexBefore(t, "eCNAME", "fCNAME", explain)
	//result, _ := em.GetResult()
	testResultBefore(t, "cCNAME", "aCNAME", result)
	testResultBefore(t, "cCNAME", "bCNAME", result)
	testResultBefore(t, "cCNAME", "dCNAME", result)
	testResultBefore(t, "cCNAME", "fCNAME", result)
	testResultBefore(t, "eCNAME", "aCNAME", result)
	testResultBefore(t, "eCNAME", "bCNAME", result)
	testResultBefore(t, "eCNAME", "cCNAME", result)
	testResultBefore(t, "eCNAME", "dCNAME", result)
	testResultBefore(t, "eCNAME", "fCNAME", result)
}
