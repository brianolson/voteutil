// Find and display a Condorcet cycle (and its resolution)

package main

import (
	"flag"
	"fmt"
	"math/rand"

	"github.com/brianolson/voteutil/go/voteutil"
)

func fillRandFloat(v []float64) {
	for i := range v {
		v[i] = rand.Float64()
	}
}

func makeVotes(v []float64, votes []voteutil.NameVote, choiceNames []string) {
	//numVoters := len(votes)
	numChoices := len(choiceNames)
	for i, vote := range votes {
		if len(vote) != numChoices {
			vote = make([]voteutil.NameRating, numChoices)
			votes[i] = vote
		}
		for c, name := range choiceNames {
			vote[c].Name = name
			vote[c].Rating = v[(i*numChoices)+c]
		}
	}
}

func main() {
	var numVoters int = 1000
	var numChoices int = 5

	flag.IntVar(&numVoters, "n", 1000, "number of voters to model")
	flag.IntVar(&numChoices, "c", 5, "number of choices to model")

	flag.Parse()

	voters := make([]float64, numVoters*numChoices)

	votes := make([]voteutil.NameVote, numVoters)

	choiceNames := make([]string, numChoices)
	for i := range choiceNames {
		choiceNames[i] = fmt.Sprintf("%c", 'A'+i)
	}

	for g := 0; g < 1000; g++ {
		fillRandFloat(voters)
		makeVotes(voters, votes, choiceNames)

		vrr := voteutil.NewVRR()
		for _, vote := range votes {
			vrr.Vote(vote)
		}
		vrr.GetResult()
		if vrr.CycleDetected {
			// winners, nwin
			_, _, explain := vrr.HtmlExplaination()
			fmt.Print(explain)
			if vrr.CycleDetected {
				return
			}
		}
	}
	fmt.Print("no cycle found in 10000 tries\n")
}
