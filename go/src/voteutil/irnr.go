package voteutil

import (
	"log"
	"math"
)

type InstantRunoffNormalizedRatings struct {
	Names *NameMap

	votes []IndexVote
	maxNameIndex int
	seats int
}

func NewInstantRunoffNormalizedRatings() ElectionMethod {
	return new(InstantRunoffNormalizedRatings)
}

func NewIRNR() ElectionMethod {
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
	for _, ni := range(vote.Indexes) {
		if ni > it.maxNameIndex {
			it.maxNameIndex = ni
		}
	}
	it.votes = append(it.votes, vote)
}

// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) GetResult() (*NameVote, int) {
	if it.maxNameIndex > 100000 {
		// really want to throw an exception here
		// TODO: log error message
		log.Print("too many names ", it.maxNameIndex)
		return nil, -10001
	}
	out := new(NameVote)
	if it.seats == 0 {
		// switch from initialization default to something reasonable
		it.seats = 1
	}
	enabled := make([]bool, it.maxNameIndex + 1)
	candidateSums := make([]float64, it.maxNameIndex + 1)
	for i, _ := range(enabled) {
		enabled[i] = true
	}
	numEnabled := len(enabled)
	// do many rounds, successively disqualify loosers
	for numEnabled > it.seats {
		// init sums for this round
		for i := 0; i < len(candidateSums); i++ {
			candidateSums[i] = 0.0
		}
		// count all votes
		for _, vote := range(it.votes) {
			votesum := 0.0
			// gather magnitude of this vote so we can normalize it
			for vii, ni := range(vote.Indexes) {
				if enabled[ni] {
					tf := vote.Ratings[vii]
					votesum += tf * tf
				}
			}
			if votesum <= 0.0 {
				// nothing left of this vote
				// TODO: count exhausted votes for reporting
				continue
			}
			votesum = math.Sqrt(votesum)
			for vii, ni := range(vote.Indexes) {
				if enabled[ni] {
					candidateSums[ni] += vote.Ratings[vii] / votesum
				}
			}
		}
		minvalue := 0.0
		mincount := 0
		for i, csum := range(candidateSums) {
			if ! enabled[i] {
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
			// TODO: log error message
			return nil, -10002
		}
		if (numEnabled - mincount) >= it.seats {
			// disable loser(s)
			for i, csum := range(candidateSums) {
				if ! enabled[i] {
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
			for i, csum := range(candidateSums) {
				if ! enabled[i] {
					continue
				}
				*out = append(*out, NameRating{it.Names.IndexToName(i),csum})
			}
			out.Sort()
			return out, numEnabled
		}
	}
	return nil, -1
}


// Return HTML explaining the result.
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) HtmlExplaination() string {
	return "<p>TODO: IRNR explanation</p>"
}

// Set shared NameMap
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) SetSharedNameMap(names *NameMap) {
	it.Names = names
}

// simple tag, lower case, no spaces
// ElectionMethod interface
func (it *InstantRunoffNormalizedRatings) ShortName() string {
	return "Instant Runoff Normalized Ratings"
}

// Set the number of desired winners
// MultiSeat interface
func (it *InstantRunoffNormalizedRatings) SetSeats(seats int) {
}
