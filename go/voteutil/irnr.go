package voteutil

import (
	"bytes"
	"fmt"
	"io"
	"log"
	"math"
)

type InstantRunoffNormalizedRatings struct {
	Names *NameMap

	votes        []IndexVote
	maxNameIndex int
	seats        int
}

func NewInstantRunoffNormalizedRatings() *InstantRunoffNormalizedRatings {
	return new(InstantRunoffNormalizedRatings)
}

func NewIRNR() *InstantRunoffNormalizedRatings {
	return new(InstantRunoffNormalizedRatings)
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) Vote(vote NameVote) {
	if it.Names == nil {
		it.Names = new(NameMap)
	}
	iv := it.Names.NameVoteToIndexVote(vote)
	it.VoteIndexes(*iv)
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) VoteIndexes(vote IndexVote) {
	for _, ni := range vote.Indexes {
		if ni > it.maxNameIndex {
			it.maxNameIndex = ni
		}
	}
	it.votes = append(it.votes, vote)
}

// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) GetResult() (*NameVote, int) {
	return it.GetResultExplain(nil)
}

func printExplainTd(explain io.Writer, f float64) {
	if math.IsNaN(f) {
		fmt.Fprint(explain, "<td class=\"x\"></td>")
	} else {
		fmt.Fprintf(explain, "<td>%0.2f</td>", f)
	}
}

func (it *InstantRunoffNormalizedRatings) GetResultExplain(explain io.Writer) (*NameVote, int) {
	if it.maxNameIndex > 100000 {
		// really want to throw an exception here
		log.Print("too many names ", it.maxNameIndex)
		if explain != nil {
			fmt.Fprintf(explain, "<p>too many names: %d</p>", it.maxNameIndex)
		}
		return nil, -10001
	}
	if explain != nil {
		fmt.Fprintf(explain, "<p>%d votes</p>", len(it.votes))
	}
	out := new(NameVote)
	if it.seats == 0 {
		// switch from initialization default to something reasonable
		it.seats = 1
	}
	enabled := make([]bool, it.maxNameIndex+1)
	candidateSums := make([]float64, it.maxNameIndex+1)
	for i, _ := range enabled {
		enabled[i] = true
	}
	numEnabled := len(enabled)
	var exhaustedBallots int
	var oldSums [][]float64 = nil
	var exhaustHistory []int = nil
	if explain != nil {
		oldSums = make([][]float64, 0)
		exhaustHistory = make([]int, 0)
	}
	// do many rounds, successively disqualify loosers
	for numEnabled > it.seats {
		// init sums for this round
		for i := 0; i < len(candidateSums); i++ {
			if enabled[i] {
				candidateSums[i] = 0.0
			} else {
				candidateSums[i] = math.NaN()
			}
		}
		exhaustedBallots = 0
		// count all votes
		for _, vote := range it.votes {
			votesum := 0.0
			// gather magnitude of this vote so we can normalize it
			for vii, ni := range vote.Indexes {
				if enabled[ni] {
					tf := vote.Ratings[vii]
					votesum += tf * tf
				}
			}
			if votesum <= 0.0 {
				// nothing left of this vote
				exhaustedBallots++
				continue
			}
			votesum = math.Sqrt(votesum)
			for vii, ni := range vote.Indexes {
				if enabled[ni] {
					candidateSums[ni] += vote.Ratings[vii] / votesum
				}
			}
		}
		minvalue := 0.0
		mincount := 0
		for i, csum := range candidateSums {
			if !enabled[i] {
				continue
			}
			if (mincount == 0) || (csum < minvalue) {
				minvalue = csum
				mincount = 1
			} else if csum == minvalue {
				mincount++
			}
		}

		if mincount == 0 {
			log.Print("no active candidate has any votes!")
			if explain != nil {
				fmt.Fprintf(explain, "<p>no active candidate has any votes!</p>")
			}
			return nil, -10002
		}
		if (numEnabled - mincount) >= it.seats {
			// disable loser(s)
			for i, csum := range candidateSums {
				if !enabled[i] {
					continue
				}
				if csum == minvalue {
					*out = append(*out, NameRating{it.Names.IndexToName(i), csum})
					enabled[i] = false
					numEnabled--
					mincount--
				}
			}
		}
		// This works if the above block runs because things
		// were disabled, or if the above block did _not_ run
		// because it would have left fewer winners than
		// seats. Thus, the below block may return more than
		// $seats winners if there is a tie for last place.
		if (numEnabled - mincount) <= it.seats {
			// build and return output
			for i, csum := range candidateSums {
				if !enabled[i] {
					continue
				}
				*out = append(*out, NameRating{it.Names.IndexToName(i), csum})
			}
			out.Sort()
			if explain != nil {
				fmt.Fprintf(explain, "<table class=\"irnrExplain\"><tr class=\"ha\"><td></td><th colspan=\"%d\">Rounds</th></tr><tr class=\"hb\"><th>Name</th>", len(oldSums)+1)
				for c := 0; c < len(oldSums)+1; c++ {
					fmt.Fprintf(explain, "<td class=\"rn\">%d</td>", c+1)
				}
				fmt.Fprint(explain, "</tr>")
				for _, nr := range *out {
					ci := it.Names.NameToIndex(nr.Name)
					csum := candidateSums[ci]
					fmt.Fprintf(explain, "<tr class=\"cs\"><td class=\"name\">%s</td>", it.Names.IndexToName(ci))
					for _, roundSums := range oldSums {
						printExplainTd(explain, roundSums[ci])
					}
					printExplainTd(explain, csum)
					fmt.Fprint(explain, "</tr>\n")
				}
				if exhaustedBallots > 0 {
					fmt.Fprint(explain, "<tr class=\"exhausted\"><td>Exhausted Ballots</td>")
					for _, eb := range exhaustHistory {
						fmt.Fprintf(explain, "<td>%d</td>", eb)
					}
					fmt.Fprintf(explain, "<td>%d</td></tr></table>\n", exhaustedBallots)
				} else {
					fmt.Fprint(explain, "</table>\n")
				}
			}
			return out, numEnabled
		}

		if explain != nil {
			oldSums = append(oldSums, candidateSums)
			exhaustHistory = append(exhaustHistory, exhaustedBallots)
			candidateSums = make([]float64, it.maxNameIndex+1)
		}
	}

	if explain != nil {
		fmt.Fprintf(explain, "<p>internal error in irnr.go</p>")
	}
	return nil, -1
}

// Return HTML explaining the result.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) HtmlExplaination() (scores *NameVote, numWinners int, html string) {
	var buf bytes.Buffer
	result, numWinners := it.GetResultExplain(&buf)
	//winners, _ := it.GetResultExplain(&buf)
	// fmt.Fprint(&buf, "<p>final:</p>")
	// winners.PrintHtml(&buf)
	return result, numWinners, string(buf.Bytes())
}

// Set shared NameMap
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) SetSharedNameMap(names *NameMap) {
	it.Names = names
}

// simple tag, lower case, no spaces
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) ShortName() string {
	return "irnr"
}

// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) Name() string {
	return "Instant Runoff Normalized Ratings"
}

// Set the number of desired winners
// MultiSeat interface
func (it *InstantRunoffNormalizedRatings) SetSeats(seats int) {
	it.seats = seats
}
