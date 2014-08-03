package voteutil

import "fmt"

// Virtual Round Robin election, aka Condorcet's method.
type VRR struct {
	Names *NameMap

	// counts[3] points to an array of length 6: [3 beats 0, 3 beats 1, 3 beats 2, 0 beats 3, 1 beats 3, 2 beats 3]
	counts [][]int
	total int
}


func (it *VRR) increment(winner, loser int) {
	if winner > loser {
		//fmt.Printf("counts[%d][%d]++\n", winner, loser)
		it.counts[winner][loser] += 1
	} else {
		//fmt.Printf("counts[%d][%d]++\n", loser, loser+winner)
		it.counts[loser][loser + winner] += 1
	}
}

// Return the number of rankings winner>loser
func (it *VRR) get(winner, loser int) int {
	var out int
	if winner > loser {
		out = it.counts[winner][loser]
	} else {
		out = it.counts[loser][loser + winner]
	}
	return out
}

func (it *VRR) ensure(maxindex int) {
	// maxindex := len(it.Names.Names) - 1
	for len(it.counts) <= maxindex {
		//fmt.Printf("new vrr shell counts[%d] = [%d]int\n", len(it.counts), len(it.counts) * 2)
		it.counts = append(it.counts, make([]int, len(it.counts) * 2))
	}
}

type nvi struct {
	Name string
	Index int
	Rating float64
}

// ElectionMethod interface
func (it *VRR) Vote(vote NameVote) {
	maxi := 0
	voti := make([]nvi, len(vote))
	i := 0
	if it.Names == nil {
		it.Names = new(NameMap)
	}
	for _, nv := range vote {
		index := it.Names.NameToIndex(nv.Name)
		voti[i].Name = nv.Name
		voti[i].Rating = nv.Rating
		voti[i].Index = index
		if index > maxi {
			maxi = index
		}
		i++
	}
	it.ensure(maxi)
	for i, va := range voti {
		for j := i + 1; j < len(voti); j++ {
			vb := voti[j]
			if va.Rating > vb.Rating {
				it.increment(va.Index, vb.Index)
			} else if vb.Rating > va.Rating {
				it.increment(vb.Index, va.Index)
			}
		}
	}
	it.total++
}

// ElectionMethod interface
func (it *VRR) VoteIndexes(vote IndexVote) {
	if len(vote.Indexes) <= 0 {
		return
	}
	maxi := vote.Indexes[0]
	for i := 1; i < len(vote.Indexes); i++ {
		if vote.Indexes[i] > maxi {
			maxi = vote.Indexes[i]
		}
	}
	it.ensure(maxi)
	for i, index := range vote.Indexes {
		ri := vote.Ratings[i]
		for j := i + 1; j < len(vote.Indexes); j++ {
			rj := vote.Ratings[j]
			if ri > rj {
				it.increment(index, vote.Indexes[j])
			} else if rj > ri {
				it.increment(vote.Indexes[j], index)
			}
		}
	}
	it.total++
}

func (it *VRR) makeWinners(defeats []int) (*NameVote, int) {
	out := new(NameVote)
	maxd := 0
	notDone := true
	// insertion sort
	for notDone {
		notDone = false
		for i, dcount := range defeats {
			if dcount == maxd {
				*out = append(*out, NameRating{
					it.Names.IndexToName(i),
					float64(0 - dcount)})
			} else if dcount > maxd {
				notDone = true
			}
		}
		maxd++
	}
	tieCount := 1
	for i := 1; i < len(*out); i++ {
		if (*out)[i].Rating == (*out)[0].Rating {
			tieCount++
		} else {
			break
		}
	}
	return out, tieCount
}

// ElectionMethod interface
func (it *VRR) GetResult() (*NameVote, int) {
	defeats := make([]int, len(it.counts))
	for i := 0; i < len(it.counts); i++ {
		for j := i + 1; j < len(it.counts); j++ {
			ivj := it.get(i, j)
			jvi := it.get(j, i)
			if ivj > jvi {
				defeats[j]++
			} else if jvi > ivj {
				defeats[i]++
			} else {
				// how to count ties?
			}
		}
	}
	
	for _, def := range defeats {
		if def == 0 {
			return it.makeWinners(defeats)
		}
	}
	fmt.Printf("total=%d\n", it.total)
	// TODO: drop weakest defeat
	out := new(NameVote)
	return out, -1
}

// ElectionMethod interface
func (it *VRR) HtmlExlpaination() string {
	return ""
}

// ElectionMethod interface
func (it *VRR) SetSharedNameMap(names *NameMap) {
	it.Names = names
}

// ElectionMethod interface
func (it *VRR) ShortName() string {
	return "Virtual Round Robin"
}
