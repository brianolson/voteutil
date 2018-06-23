package voteutil

import "testing"

func nvEq(t *testing.T, actualp *NameVote, expected NameVote) {
	if actualp == nil {
		t.Error("actual NameVote is nil but expected value")
		return
	}
	actual := *actualp
	if len(actual) != len(expected) {
		t.Errorf("expected NameVote of length %d but got %d", len(expected), len(actual))
		return
	}
	for i, av := range actual {
		ev := expected[i]
		if av.Name != ev.Name {
			t.Errorf("expected NameVote[%d].Name %#v but got %#v", i, ev.Name, av.Name)
		}
		if av.Rating != ev.Rating {
			t.Errorf("expected NameVote[%d].Rating %f but got %f", i, ev.Rating, av.Rating)
		}
	}
}

func tUrlToNameVote(t *testing.T, v string) *NameVote {
	nv, err := UrlToNameVote(v)
	if err != nil {
		t.Errorf("UrlToNameVote err=%s", err)
		return nil
	}
	return nv
}

func TestUrlToNameVote(t *testing.T) {
	t.Parallel()
	nvEq(t, tUrlToNameVote(t, "a=1"), NameVote([]NameRating{{"a", 1}}))
	nvEq(t, tUrlToNameVote(t, "LONDON+BREED=1&JANE+KIM=2&MARK+LENO=3"), NameVote([]NameRating{{"LONDON BREED", 1}, {"JANE KIM", 2}, {"MARK LENO", 3}}))
}

func assertFloat64(t *testing.T, actual, expected float64) bool {
	if actual != expected {
		t.Errorf("expected %f got %f", expected, actual)
		return false
	}
	return true
}

func TestConvertRankingsToRatings(t *testing.T) {
	t.Parallel()
	nv := tUrlToNameVote(t, "LONDON+BREED=1&JANE+KIM=2&MARK+LENO=3")
	nv.ConvertRankingsToRatings()
	assertFloat64(t, (*nv)[0].Rating, 3)
	assertFloat64(t, (*nv)[1].Rating, 2)
	assertFloat64(t, (*nv)[2].Rating, 1)
}

func tUrlNameVoteRoundTrip(t *testing.T, v string) {
	nv, err := UrlToNameVote(v)
	if err != nil {
		t.Error(err)
		return
	}
	if nv == nil {
		t.Error("nil NameVote")
		return
	}
	xv := NameVoteToUrl(*nv)
	if v != xv {
		t.Errorf("cgi encoded vote changed on round trip %#v -> %#v", v, xv)
	}
}

func TestUrlNameVoteRoundTrip(t *testing.T) {
	t.Parallel()
	tUrlNameVoteRoundTrip(t, "a=1")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=1&JANE+KIM=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=1&JANE+KIM=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=1&LONDON+BREED=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=1&LONDON+BREED=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=3&MARK+LENO=2")
	tUrlNameVoteRoundTrip(t, "ANGELA+ALIOTO=3&RICHIE+GREENBERG=2")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&ANGELA+ALIOTO=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&ANGELA+ALIOTO=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&LONDON+BREED=2&AMY+FARAH+WEISS=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&LONDON+BREED=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&MARK+LENO=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&MARK+LENO=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&MICHELLE+BRAVO=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&RICHIE+GREENBERG=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=1&RICHIE+GREENBERG=2&MICHELLE+BRAVO=3")
	tUrlNameVoteRoundTrip(t, "ELLEN+LEE+ZHOU=3&RICHIE+GREENBERG=2")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&ANGELA+ALIOTO=2&ELLEN+LEE+ZHOU=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&ANGELA+ALIOTO=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&ELLEN+LEE+ZHOU=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&ELLEN+LEE+ZHOU=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&LONDON+BREED=2&AMY+FARAH+WEISS=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&LONDON+BREED=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&LONDON+BREED=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&LONDON+BREED=2&None=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&MARK+LENO=2&AMY+FARAH+WEISS=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&MARK+LENO=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&MARK+LENO=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=1&MARK+LENO=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "JANE+KIM=3&MARK+LENO=2")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&ANGELA+ALIOTO=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&ANGELA+ALIOTO=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&ELLEN+LEE+ZHOU=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&ELLEN+LEE+ZHOU=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&JANE+KIM=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&JANE+KIM=2&MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&MARK+LENO=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&MARK+LENO=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&MARK+LENO=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=1&None=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=2&None=3")
	tUrlNameVoteRoundTrip(t, "LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&ANGELA+ALIOTO=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&ANGELA+ALIOTO=2&MICHELLE+BRAVO=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&JANE+KIM=2&ELLEN+LEE+ZHOU=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&JANE+KIM=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&JANE+KIM=2&RICHIE+GREENBERG=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&LONDON+BREED=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&LONDON+BREED=2&JANE+KIM=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&LONDON+BREED=2&MICHELLE+BRAVO=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&MICHELLE+BRAVO=2&AMY+FARAH+WEISS=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=1&RICHIE+GREENBERG=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=2&ANGELA+ALIOTO=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=3")
	tUrlNameVoteRoundTrip(t, "MARK+LENO=3&JANE+KIM=2")
	tUrlNameVoteRoundTrip(t, "None=1&JANE+KIM=2&LONDON+BREED=3")
	tUrlNameVoteRoundTrip(t, "RICHIE+GREENBERG=1&AMY+FARAH+WEISS=2&ELLEN+LEE+ZHOU=3")
	tUrlNameVoteRoundTrip(t, "RICHIE+GREENBERG=1&ANGELA+ALIOTO=2&ELLEN+LEE+ZHOU=3")
}
