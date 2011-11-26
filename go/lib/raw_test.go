package voting

import "testing"

func ExpectString(t *testing.T, actual string, expected string) {
	if actual != expected {
		t.Errorf("expected %v\n, got %v\n", expected, actual)
	}
}

func ExpectInt(t *testing.T, actual int, expected int) {
	if actual != expected {
		t.Errorf("expected %v\n, got %v\n", expected, actual)
	}
}

func BasicEM(t *testing.T, em ElectionMethod) {
	em.Vote(NameVote{{"foo", 1.0},{"bar",0.5}})
	result, winners := em.GetResult()
	ExpectInt(t, winners, 1)
	ExpectString(t, (*result)[0].Name, "foo")
	em.Vote(NameVote{{"foo", 0.5},{"bar",1.0}})
	em.Vote(NameVote{{"foo", 0.5},{"bar",1.0}})
	result, winners = em.GetResult()
	ExpectInt(t, winners, 1)
	ExpectString(t, (*result)[0].Name, "bar")
}

func RankTie(t *testing.T, em ElectionMethod) {
	em.Vote(NameVote{{"foo", 1.0},{"bar",0.5},{"baz",0.1}})
	em.Vote(NameVote{{"foo", 0.5},{"bar",1.0},{"baz",0.1}})
	em.Vote(NameVote{{"foo", 0.5},{"bar",1.0},{"baz",0.1}})
	em.Vote(NameVote{{"foo", 1.0},{"bar",0.5},{"baz",0.1}})
	result, winners := em.GetResult()
	ExpectInt(t, winners, 2)
	ExpectString(t, (*result)[2].Name, "baz")
}

func TestRawBasic(t *testing.T) {
	BasicEM(t, new(RawSummation))
}
