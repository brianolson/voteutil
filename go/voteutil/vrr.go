package voteutil

import (
	"bytes"
	"fmt"
	"io"
	"sort"
	"strings"
)

// Virtual Round Robin election, aka Condorcet's method.
type VRR struct {
	Names *NameMap

	WinningVotesMode bool
	MarginsMode      bool
	// counts[3] points to an array of length 6: [3 beats 0, 3 beats 1, 3 beats 2, 0 beats 3, 1 beats 3, 2 beats 3]
	counts [][]int
	total  int
}

func NewVRR() ElectionMethod {
	x := new(VRR)
	x.WinningVotesMode = true
	return x
}

func (it *VRR) increment(winner, loser int) {
	if winner > loser {
		//fmt.Printf("counts[%d][%d]++\n", winner, loser)
		it.counts[winner][loser] += 1
	} else {
		//fmt.Printf("counts[%d][%d]++\n", loser, loser+winner)
		it.counts[loser][loser+winner] += 1
	}
}

// Return the number of rankings winner>loser
func (it *VRR) get(winner, loser int) int {
	var out int
	if winner > loser {
		out = it.counts[winner][loser]
	} else {
		out = it.counts[loser][loser+winner]
	}
	return out
}

func (it *VRR) ensure(maxindex int) {
	// maxindex := len(it.Names.Names) - 1
	for len(it.counts) <= maxindex {
		//fmt.Printf("new vrr shell counts[%d] = [%d]int\n", len(it.counts), len(it.counts) * 2)
		it.counts = append(it.counts, make([]int, len(it.counts)*2))
	}
}

type nvi struct {
	Name   string
	Index  int
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
	//out := new(NameVote)
	out := make([]NameRating, len(defeats))
	for i, dcount := range defeats {
		out[i].Name = it.Names.IndexToName(i)
		out[i].Rating = float64(len(defeats) - dcount)
	}
	onv := NameVote(out)
	onv.Sort()
	tieCount := 1
	for i := 1; i < len(out); i++ {
		if out[i].Rating == out[0].Rating {
			tieCount++
		} else {
			break
		}
	}
	return &onv, tieCount
}

func ain(x int, they []int) bool {
	for _, v := range they {
		if v == x {
			return true
		}
	}
	return false
}

func defIsBlocked(hi, lo int, bd []int) bool {
	for i := 0; i < len(bd); i += 2 {
		if bd[i] == hi && bd[i+1] == lo {
			return true
		}
	}
	return false
}

// ElectionMethod interface
// return sorted result, num 'winners'
func (it *VRR) GetResult() (*NameVote, int) {
	return it.GetResultExplain(nil)
}

type DefeatSorter struct {
	it *VRR
	defeats []int
	blockedDefeats []int

	// sorted candidate indexes
	sortOrder []int
}
func newDefeatSorter(it *VRR, defeats []int, blockedDefeats []int) *DefeatSorter {
	out := &DefeatSorter{
		it,
		defeats,
		blockedDefeats,
		nil,
	}
	out.sortOrder = make([]int, len(defeats))
	for i := range out.sortOrder {
		out.sortOrder[i] = i
	}
	return out
}
func (ds *DefeatSorter) Len() int {
	return len(ds.defeats)
}
func (ds *DefeatSorter) Less(si, sj int) bool {
	i := ds.sortOrder[si]
	j := ds.sortOrder[sj]
	if ds.defeats[i] < ds.defeats[j] {
		return true
	} else if ds.defeats[i] == ds.defeats[j] {
		if defIsBlocked(i, j, ds.blockedDefeats) {
			// [i] does not count as defeating [j]
			return false
		}
		ivj := ds.it.get(i, j)
		jvi := ds.it.get(j, i)
		if ivj > jvi {
			// [i] defeats [j], is less defeated than [j]
			return true
		}
		return false
	} else {
		return false
	}
}
func (ds *DefeatSorter) Swap(si, sj int) {
	t := ds.sortOrder[si]
	ds.sortOrder[si] = ds.sortOrder[sj]
	ds.sortOrder[sj] = t
}
func (ds *DefeatSorter) WriteExplain(explain io.Writer) {
	if explain == nil {
		return
	}
	fmt.Fprint(explain, "<p>effective defeat counts:")
	for _, candi := range ds.sortOrder {
		dn := ds.it.Names.IndexToName(candi)
		fmt.Fprintf(explain, " (%s %d)", dn, ds.defeats[candi])
	}
	fmt.Fprint(explain, "</p>\n")
}
func (ds *DefeatSorter) makeWinners() (*NameVote, int) {
	out := make([]NameRating, len(ds.defeats))
	for sorti, candi := range ds.sortOrder {
		out[sorti].Name = ds.it.Names.IndexToName(candi)
		out[sorti].Rating = float64(len(ds.defeats) - ds.defeats[candi])
	}
	tieCount := 1
	for i := 1; i < len(out); i++ {
		if out[i].Rating == out[0].Rating {
			tieCount++
		} else {
			break
		}
	}
	ouv := NameVote(out)
	return &ouv, tieCount
}

// return sorted result, num 'winners'
func (it *VRR) GetResultExplain(explain io.Writer) (*NameVote, int) {
	if len(it.counts) == 0 {
		return &NameVote{}, 0
	}

	// plain Condorcet.
	// for each choice, count how many times it is defeated by another.
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

	mindefeat := len(it.counts)
	mini := len(it.counts)
	for i, def := range defeats {
		if def == 0 {
			// we have a winner
			return it.makeWinners(defeats)
		}
		if def < mindefeat {
			mindefeat = def
			mini = i
		}
	}

	blockedDefeats := []int{}
	// find the active set of anything that defeats the thing with the least defeats

	for true {
		activeset := []int{mini}
		setgrows := true
		for setgrows {
			setgrows = false
			for _, j := range activeset {
				for i := 0; i < len(it.counts); i++ {
					if ain(i, activeset) {
						continue
					}
					if defIsBlocked(i, j, blockedDefeats) {
						continue
					}
					ivj := it.get(i, j)
					jvi := it.get(j, i)
					if ivj > jvi {
						activeset = append(activeset, i)
						setgrows = true
					}
				}
			}
		}
		if len(activeset) == 1 {
			// winner
			defeats[mini] = 0
			//return it.makeWinners(defeats)
			break
		}
/*
		activeNames := make([]string, len(activeset))
		for ai, an := range activeset {
			activeNames[ai] = it.Names.IndexToName(an)
		}
		fmt.Printf("active set %#v\n", activeNames)
*/
		if explain != nil {
			fmt.Fprintf(explain, "<p>Top choices: %s", it.Names.IndexToName(activeset[0]))
			for i := 1; i < len(activeset); i++ {
				an := activeset[i]
				fmt.Fprintf(explain, ", %s", it.Names.IndexToName(an))
			}
			fmt.Fprint(explain, "</p>")
		}

		// find weakest defeat between active winners
		minstrength := it.total
		mins := []int{}
		for i, a := range activeset {
			for _, b := range activeset[i+1:] {
				avb := it.get(a, b)
				bva := it.get(b, a)

				var vhi int
				var vlo int
				var hi int
				var lo int
				var strength int
				if avb > bva {
					hi = a
					lo = b
					vhi = avb
					vlo = bva
				} else {
					hi = b
					lo = a
					vhi = bva
					vlo = avb
				}
				if defIsBlocked(hi, lo, blockedDefeats) {
					continue
				}
				if it.WinningVotesMode {
					strength = vhi
				} else if it.MarginsMode {
					strength = vhi - vlo
				} else {
					panic("VRR needs Mode")
				}
/*
				fmt.Printf(
					"%s>%s %d>%d %d\n",
					it.Names.IndexToName(hi),
					it.Names.IndexToName(lo),
					vhi, vlo, strength)
*/
				if strength < minstrength {
					minstrength = strength
					mins = []int{hi, lo}
				} else if strength == minstrength {
					mins = append(mins, hi, lo)
				}
			}
		}

		if (len(mins) / 2) == len(activeset) {
			// N way tie. give up.
			//return it.makeWinners(defeats)
			break
		}

		for mi := 0; mi < len(mins); mi += 2 {
			hi := mins[mi]
			lo := mins[mi+1]
			//fmt.Printf("drop defeat %s>%s (%d>%d)\n", it.Names.IndexToName(hi), it.Names.IndexToName(lo), it.get(hi, lo), it.get(lo, hi))
			if explain != nil {
				fmt.Fprintf(
					explain,
					"<p>Weakest defeat is %s (%d) v %s (%d). %s has one fewer defeat.</p>\n",
					it.Names.IndexToName(hi), it.get(hi, lo),
					it.Names.IndexToName(lo), it.get(lo, hi),
					it.Names.IndexToName(lo))
			}
			blockedDefeats = append(blockedDefeats, hi, lo)
			defeats[lo] -= 1
			mini = lo
		}
	}

	//fmt.Printf("total=%d\n", it.total)
	// TODO: drop weakest defeat
	//out := new(NameVote)
	//return out, -1
	//return it.makeWinners(defeats)
	ds := newDefeatSorter(it, defeats, blockedDefeats)
	sort.Sort(ds)

	if explain != nil {
		ds.WriteExplain(explain)
	}

	return ds.makeWinners()
}

// ElectionMethod interface
func (it *VRR) HtmlExplaination() string {
	resultExplain := new(bytes.Buffer)
	results, _ := it.GetResultExplain(resultExplain)
	//results, _ := it.GetResult() // _ = numWinners
	parts := []string{resultExplain.String(), "<table border=\"1\"><tr><td colspan=\"2\"></td>"}
	for y, _ := range *results {
		parts = append(parts, fmt.Sprintf("<th>%d</th>", y+1))
	}
	parts = append(parts, "</tr>")
	for y, nv := range *results {
		yni := it.Names.NameToIndex(nv.Name)
		parts = append(parts, fmt.Sprintf("<tr><th>%d</th><td>%s</td>", y+1, nv.Name))
		for x, nv := range *results {
			if x == y {
				parts = append(parts, "<td></td>")
			} else {
				xni := it.Names.NameToIndex(nv.Name)
				count := it.get(yni, xni)
				revcount := it.get(xni, yni)
				bg := ""
				if count > revcount {
					bg = " bgcolor=\"#bfb\""
				} else if revcount > count {
					bg = " bgcolor=\"#fbb\""
				}
				parts = append(parts, fmt.Sprintf("<td%s>%d</td>", bg, count))
			}
		}
		parts = append(parts, "</tr>")
	}
	parts = append(parts, "</table>")
	return strings.Join(parts, "")
}

// ElectionMethod interface
func (it *VRR) SetSharedNameMap(names *NameMap) {
	it.Names = names
}

// ElectionMethod interface
func (it *VRR) ShortName() string {
	return "Virtual Round Robin"
}
