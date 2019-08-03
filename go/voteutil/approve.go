package voteutil

import (
	"fmt"
	"strings"
)

type ApprovalVote struct {
	Names  *NameMap
	counts map[int]int
}

func NewApprovalVote() *ApprovalVote {
	return &ApprovalVote{Names: nil, counts: make(map[int]int)}
}

// ElectionMethod interface
func (av *ApprovalVote) Vote(vote NameVote) {
	if av.Names == nil {
		av.Names = &NameMap{}
	}
	iv := av.Names.NameVoteToIndexVote(vote)
	av.VoteIndexes(*iv)
}

// ElectionMethod interface
func (av *ApprovalVote) VoteIndexes(vote IndexVote) {
	for i, ni := range vote.Indexes {
		if vote.Ratings[i] > 0.0 {
			av.counts[ni] = av.counts[ni] + 1
		}
	}
}

// ElectionMethod interface
func (av *ApprovalVote) GetResult() (scores *NameVote, numWinners int) {
	if av.Names == nil {
		av.Names = &NameMap{}
	}
	out := make([]NameRating, len(av.counts))
	opos := 0
	for index, sum := range av.counts {
		out[opos].Name = av.Names.IndexToName(index)
		out[opos].Rating = float64(sum)
		opos++
	}
	nv := NameVote(out)
	scores = &nv
	scores.Sort()
	maxv := (*scores)[0].Rating
	numWinners = 1
	for i := 1; i < len(*scores); i++ {
		if (*scores)[i].Rating == maxv {
			numWinners++
		} else {
			break
		}
	}
	return
}

// ElectionMethod interface
func (av *ApprovalVote) HtmlExplaination() (scores *NameVote, numWinners int, html string) {
	scores, numWinners = av.GetResult()
	exout := strings.Builder{}
	exout.WriteString("<table><tr><th>Name</th><th>Votes</th></tr>")
	for _, nv := range *scores {
		fmt.Fprintf(&exout, "<tr><td>%s</td><td>%.0f</td></tr>", nv.Name, nv.Rating)
	}
	for _, name := range av.Names.Names {
		found := false
		for _, nv := range *scores {
			if nv.Name == name {
				found = true
				break
			}
		}
		if found {
			continue
		}
		fmt.Fprintf(&exout, "<tr><td>%s</td><td>0</td></tr>", name)
	}
	exout.WriteString("</table>")
	html = exout.String()
	return
}

// ElectionMethod interface
func (av *ApprovalVote) SetSharedNameMap(names *NameMap) {
	av.Names = names
}

// ElectionMethod interface
func (av *ApprovalVote) ShortName() string {
	return "approval"
}

// ElectionMethod interface
func (av *ApprovalVote) Name() string {
	return "Approval"
}
