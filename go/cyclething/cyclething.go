// Find and display a Condorcet cycle (and its resolution)

package main

import (
	"flag"
	"fmt"
	"math/rand"

	"github.com/brianolson/voteutil/go/voteutil"
)

func fillUniformFloat(v []float64) {
	for i := range v {
		v[i] = rand.Float64()
	}
}

func fillNormalFloat(v []float64) {
	for i := range v {
		v[i] = rand.NormFloat64()
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

func makeDimensionalVotes(v []float64, cpos []float64, dimensions int, choiceNames []string, votes []voteutil.NameVote) {
	numChoices := len(choiceNames)
	for i, vote := range votes {
		if len(vote) != numChoices {
			vote = make([]voteutil.NameRating, numChoices)
			votes[i] = vote
		}
		for c, name := range choiceNames {
			coff := c * dimensions
			cc := cpos[coff : coff+dimensions]
			voff := i * dimensions
			vc := v[voff : voff+dimensions]
			rr := float64(0)
			for j := 0; j < dimensions; j++ {
				dj := cc[j] - vc[j]
				rr += dj * dj
			}
			vote[c].Name = name
			// don't need to sqrt to get a proper distance because they'll sort the same
			vote[c].Rating = 1 / rr
		}
	}
}

func main() {
	var numVoters int = 1000
	var numChoices int = 5
	var numTries int = 1000
	var dimensions = 0

	flag.IntVar(&numVoters, "v", 1000, "number of voters to model")
	flag.IntVar(&numChoices, "c", 5, "number of choices to model")
	flag.IntVar(&numTries, "n", 1000, "number of elections to try")

	flag.IntVar(&dimensions, "d", 0, "number of opinion space dimensions to simulate")

	flag.Parse()

	var voters []float64
	var cpos []float64
	if dimensions != 0 {
		voters = make([]float64, numVoters*dimensions)
		cpos = make([]float64, numChoices*dimensions)
	} else {
		voters = make([]float64, numVoters*numChoices)
	}

	votes := make([]voteutil.NameVote, numVoters)

	choiceNames := make([]string, numChoices)
	for i := range choiceNames {
		choiceNames[i] = fmt.Sprintf("%c", 'A'+i)
	}

	count := 0

	for g := 0; g < numTries; g++ {
		if dimensions != 0 {
			fillNormalFloat(cpos)
			fillNormalFloat(voters)
			makeDimensionalVotes(voters, cpos, dimensions, choiceNames, votes)
		} else {
			fillUniformFloat(voters)
			makeVotes(voters, votes, choiceNames)
		}

		vrr := voteutil.NewVRR()
		for _, vote := range votes {
			vrr.Vote(vote)
		}
		vrr.GetResult()
		if vrr.CycleDetected {
			// winners, nwin
			if count == 0 {
				_, _, explain := vrr.HtmlExplaination()
				fmt.Print(explain)
			}
			count++
		}
	}
	fmt.Printf("<p>%d cycles found in %d tries</p>\n", count, numTries)
}
