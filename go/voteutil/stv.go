package voteutil

import (
	"bytes"
	"fmt"
	"io"
	"log"
	//"math"
	"math/rand"
)

type SingleTransferrableVote struct {
	Names *NameMap

	votes        []IndexVote
	maxNameIndex int
	seats        int
}

func NewSTV() *SingleTransferrableVote {
	return new(SingleTransferrableVote)
}

func NewSingleTransferrableVote() *SingleTransferrableVote {
	return new(SingleTransferrableVote)
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *SingleTransferrableVote) Vote(vote NameVote) {
	if it.Names == nil {
		it.Names = new(NameMap)
	}
	iv := it.Names.NameVoteToIndexVote(vote)
	it.VoteIndexes(*iv)
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *SingleTransferrableVote) VoteIndexes(vote IndexVote) {
	for _, ni := range vote.Indexes {
		if ni > it.maxNameIndex {
			it.maxNameIndex = ni
		}
	}
	vote.Sort()
	it.votes = append(it.votes, vote)
}

// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
// ElectionMethod interface
func (it *SingleTransferrableVote) GetResult() (*NameVote, int) {
	return it.GetResultExplain(nil)
}

// Return HTML explaining the result.
// ElectionMethod interface
func (it *SingleTransferrableVote) HtmlExplaination() (scores *NameVote, numWinners int, html string) {
	var buf bytes.Buffer
	result, numWinners := it.GetResultExplain(&buf)
	return result, numWinners, string(buf.Bytes())
}

func Quota(votes uint64, seats int) uint64 {
	quota := votes / uint64(seats+1)
	for (quota * uint64(seats+1)) < votes {
		quota++
	}
	return quota
}

const initialVotePower = 1000000

// context too messy to just live in function stack
type stvCountState struct {
	explain          io.Writer
	enabled          []bool
	candidateSums    []uint64
	deweight         []float64
	numEnabled       int
	exhaustedBallots uint64
	oldSums          [][]uint64
	exhaustHistory   []int
	winners          []int
	numWinners       int // winners[0:numWinners] have been elected
	numLosers        int // winners[len-numLosers:len] have been disqualified
	it               *SingleTransferrableVote
}

func (x *stvCountState) init(it *SingleTransferrableVote, explain io.Writer) {
	x.it = it
	x.explain = explain

	x.enabled = make([]bool, it.maxNameIndex+1)
	x.candidateSums = make([]uint64, it.maxNameIndex+1)
	x.deweight = make([]float64, it.maxNameIndex+1)
	x.winners = make([]int, it.maxNameIndex+1)
	for i := range x.enabled {
		x.enabled[i] = true
		x.deweight[i] = 1.0
	}
	x.numEnabled = len(x.enabled)
	x.exhaustedBallots = 0
	if explain != nil {
		x.oldSums = make([][]uint64, 0)
		x.exhaustHistory = make([]int, 0)
	}
	x.numWinners = 0
	x.numLosers = 0
}

func (x *stvCountState) alreadyWinning(ci int) bool {
	i := 0
	for i < x.numWinners {
		if x.winners[i] == ci {
			return true
		}
		i++
	}
	return false
}

func (x *stvCountState) result() (*NameVote, int) {
	out := make([]NameRating, len(x.candidateSums))
	// TODO: sort on most-winning
	for oi := range out {
		out[oi].Name = x.it.Names.IndexToName(oi)
		out[oi].Rating = float64(x.candidateSums[oi]) / initialVotePower
	}
	nv := NameVote(out)
	nv.Sort()
	if x.explain != nil {
		nv.PrintHtml(x.explain)
	}
	return &nv, x.it.seats
}

func (x *stvCountState) doIt() {
	for x.numEnabled > x.it.seats {
		numWinners := x.pass()

		if numWinners >= x.it.seats {
			// Done!
			if numWinners > x.it.seats {
				log.Printf("Weird! got %d winners for %d seats", numWinners, x.it.seats)
				if x.explain != nil {
					fmt.Fprintf(x.explain, "<p class=\"err\">Got %d winners for %d seats</p>\n", numWinners, x.it.seats)
				}
			}
			return
		}
		if x.explain != nil {
			fmt.Fprintf(x.explain, "<p>%d winners</p>", numWinners)
		}

		// else, disable a loser
		x.disable()
	}
}

func (x *stvCountState) disable() {
	mini := -1
	var minsum uint64
	ties := 0
	// find loser(s)
	for ci, csum := range x.candidateSums {
		if !x.enabled[ci] {
			continue
		}
		if (mini == -1) || (csum < minsum) {
			mini = ci
			minsum = csum
			ties = 1
		} else if csum == minsum {
			ties++
		}
	}

	if ties == 0 {
		msg := "STV internal error, nothing to disable"
		log.Print(msg)
		if x.explain != nil {
			fmt.Fprintf(x.explain, "<p class=\"err\">%s</p>", msg)
		}
	} else if ties == 1 {
		// one loser
		x.enabled[mini] = false
		x.winners[len(x.winners)-x.numLosers-1] = mini
		x.numLosers++
		x.numEnabled--
	} else {
		// ties > 1; several losers
		if (x.numEnabled - ties) < x.it.seats {
			// too many tied for last place
			log.Printf("%d/%d tie for last place, picking random last place winner", ties, x.numEnabled)
			if x.explain != nil {
				fmt.Fprintf(x.explain, "<p class=\"warn\">%d/%d tie for last place, picking random last place winner</p>", ties, x.numEnabled)
			}
			for x.numEnabled > x.it.seats {
				loseri := rand.Int31n(int32(ties))
				for ci, csum := range x.candidateSums {
					if !x.enabled[ci] {
						continue
					}
					if csum == minsum {
						if loseri == 0 {
							if x.explain != nil {
								fmt.Fprintf(x.explain, "<p>disable %s</p>", x.it.Names.IndexToName(ci))
							}
							x.enabled[ci] = false
							x.winners[len(x.winners)-x.numLosers-1] = ci
							x.numLosers++
							x.numEnabled--
							ties--
						} else {
							loseri--
						}
					}
				}
			}
			return
		}
		// tie for last place all fail out at once
		for ci, csum := range x.candidateSums {
			if !x.enabled[ci] {
				continue
			}
			if csum == minsum {
				x.enabled[ci] = false
				x.winners[len(x.winners)-x.numLosers-1] = ci
				x.numLosers++
				x.numEnabled--
			}
		}
	}
}

// run a pass, resulting in a disqualification or a solution
func (x *stvCountState) pass() int {
	for eni, en := range x.enabled {
		if !en {
			x.deweight[eni] = 0.0
		}
	}

	deweightChanged := true
	deweightCycleLimit := 10
	var numWinners int

	for deweightChanged && (deweightCycleLimit > 0) {
		deweightChanged = false
		// reset, recount
		x.exhaustedBallots = 0
		for i := range x.candidateSums {
			x.candidateSums[i] = 0
		}
		// count all votes
		for _, vote := range x.it.votes {
			x.vote(vote)
		}

		quota := Quota((uint64(len(x.it.votes))*initialVotePower)-x.exhaustedBallots, x.it.seats)

		if x.explain != nil {
			fmt.Fprintf(x.explain, "<p>early sums:")
			var vsum uint64 = 0
			for ci, csum := range x.candidateSums {
				fmt.Fprintf(x.explain, " %s:%d", x.it.Names.IndexToName(ci), csum)
				vsum += csum
			}
			fmt.Fprintf(x.explain, ", vsum=%d, exhausted=%d, quota=%d</p>\n", vsum, x.exhaustedBallots, quota)
		}

		numWinners = 0
		for ci, csum := range x.candidateSums {
			if csum > quota {
				newDeweight := x.deweight[ci] * float64(quota) / float64(csum-1)
				if x.explain != nil {
					fmt.Fprintf(x.explain, "<p>%s deweight %f -> %f</p>\n", x.it.Names.IndexToName(ci), x.deweight[ci], newDeweight)
				}
				if newDeweight != x.deweight[ci] {
					deweightChanged = true
				}
				x.deweight[ci] = newDeweight
				numWinners++
			}
		}
		if numWinners >= x.it.seats {
			if x.explain != nil {
				fmt.Fprintf(x.explain, "<p>found %d winners, done</p>\n", numWinners)
			}
			return numWinners
		}
		deweightCycleLimit--
	}
	if x.explain != nil {
		fmt.Fprintf(x.explain, "<p>enabled: %#v</p>\n", x.enabled)
		fmt.Fprintf(x.explain, "<p>sums: %#v</p>\n", x.candidateSums)
	}
	return numWinners
}

// count a vote
func (x *stvCountState) vote(vote IndexVote) {
	var votePower uint64 = initialVotePower

	votei := 0 // position within the sorted vote

	firstEnabled := -1

	for (votei < len(vote.Indexes)) && (votePower > 0) {
		ci := vote.Indexes[votei]

		// A disabled candidate, or a rating less than zero, is not a vote for any choice
		for (!x.enabled[ci]) || (vote.Ratings[votei] <= 0) {
			votei++
			if votei >= len(vote.Indexes) {
				if firstEnabled >= 0 {
					x.candidateSums[firstEnabled] += votePower
				} else {
					x.exhaustedBallots += votePower
				}
				return
			}
			ci = vote.Indexes[votei]
		}

		if firstEnabled < 0 {
			firstEnabled = ci
		}

		ties := uint64(1)
		ti := votei + 1
		for (ti < len(vote.Ratings)) && (vote.Ratings[votei] == vote.Ratings[ti]) {
			ties++
			ti++
		}

		if ties == 1 {
			// easy
			appliedVotePower := uint64(initialVotePower * x.deweight[ci])
			if votePower < appliedVotePower {
				appliedVotePower = votePower
			}
			x.candidateSums[ci] += appliedVotePower
			votePower -= appliedVotePower
		} else {
			votePower = x.tieDistribute(votei, ti, vote, votePower)
		}

		votei++
	}

	x.exhaustedBallots += votePower
}

func (x *stvCountState) tieDistribute(votei, ti int, vote IndexVote, votePower uint64) uint64 {
	// sum up votePower that _could_ be assigned to the ties based on their deweight
	sumAVP := uint64(0)
	for xi := votei; xi < ti; xi++ {
		appliedVotePower := uint64(initialVotePower * x.deweight[vote.Indexes[xi]])
		sumAVP += appliedVotePower
	}

	if votePower >= sumAVP {
		// use appliedVotePower as calcualted
		votePowerDebit := uint64(0)
		for xi := votei; xi < ti; xi++ {
			ci := vote.Indexes[xi]
			appliedVotePower := uint64(initialVotePower * x.deweight[ci])
			votePowerDebit += appliedVotePower
			x.candidateSums[ci] += appliedVotePower
		}
		// assert(votePower > votePowerDebit)
		votePower -= votePowerDebit
	} else {
		// scale appliedVotePower, use up votePower exactly
		votePowerDebit := uint64(0)
		for xi := votei; xi < ti; xi++ {
			ci := vote.Indexes[xi]
			appliedVotePower := uint64(float64(votePower) * initialVotePower * x.deweight[ci] / float64(sumAVP))
			votePowerDebit += appliedVotePower
			x.candidateSums[ci] += appliedVotePower
		}
		for votePower > votePowerDebit {
			// use up any truncation shortage
			for xi := votei; xi < ti; xi++ {
				ci := vote.Indexes[xi]
				x.candidateSums[ci] += 1
				votePowerDebit += 1
				if votePower == votePowerDebit {
					break
				}
			}
		}
		for votePower < votePowerDebit {
			// oops, overdid it back off a little
			for xi := votei; xi < ti; xi++ {
				ci := vote.Indexes[xi]
				x.candidateSums[ci] -= 1
				votePowerDebit -= 1
				if votePower == votePowerDebit {
					break
				}
			}
		}
		// assert(votePower > votePowerDebit)
		votePower -= votePowerDebit
	}
	return votePower
}

func (it *SingleTransferrableVote) GetResultExplain(explain io.Writer) (*NameVote, int) {
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
	//out := new(NameVote)
	if it.seats == 0 {
		// switch from initialization default to something reasonable
		it.seats = 1
	}
	if it.seats == 1 {
		log.Print("WARNING: STV electing 1 seat is a bad election method. Considered Harmful. Do not use.")
		if explain != nil {
			fmt.Fprintf(explain, "<p style=\"color:#f22;font-size:150%%\">WARNING: STV electing 1 seat is a bad election method. Considered Harmful. Do not use.</p>")
		}
	}

	var state stvCountState
	state.init(it, explain)
	state.doIt()
	return state.result()
}

// Set shared NameMap
// ElectionMethod interface
func (it *SingleTransferrableVote) SetSharedNameMap(names *NameMap) {
	it.Names = names
}

// simple tag, lower case, no spaces
// ElectionMethod interface
func (it *SingleTransferrableVote) ShortName() string {
	return "stv"
}

// Full proper name. May be extended to describe options.
// e.g. "Single Transferrable Vote (fractional transfer)"
// ElectionMethod interface
func (it *SingleTransferrableVote) Name() string {
	return "Single Transferrable Vote"
}

// Set the number of desired winners
// MultiSeat interface
func (it *SingleTransferrableVote) SetSeats(seats int) {
	it.seats = seats
	if it.seats == 1 {
		log.Print("WARNING: STV electing 1 seat is a bad election method. Considered Harmful. Do not use.")
	}
}
