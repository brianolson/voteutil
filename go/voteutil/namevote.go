package voteutil

import (
	"encoding/json"
	"fmt"
	"io"
	"net/url"
	"sort"
	"strconv"
	"strings"
)

type NameRating struct {
	Name   string
	Rating float64
}

type NameVote []NameRating

// IndexVote is perhaps excessively optimized to pack nicely.
//
// You might think that a slice of struct of {int,float} would be nicer, and
// you'd be right, but (int32,float64) will leave 4 bytes of dead space when you
// make an array of them. int[],float64[] will pack more efficiently.
type IndexVote struct {
	Indexes []int
	Ratings []float64
}

// sort.Interface
func (it *IndexVote) Len() int {
	return len(it.Indexes)
}

// sort.Interface
// Is 'reversed' causing highest rating first.
func (it *IndexVote) Less(i, j int) bool {
	return it.Ratings[j] < it.Ratings[i]
}

// sort.Interface
func (it *IndexVote) Swap(i, j int) {
	ti := it.Indexes[i]
	tr := it.Ratings[i]
	it.Indexes[i] = it.Indexes[j]
	it.Ratings[i] = it.Ratings[j]
	it.Indexes[j] = ti
	it.Ratings[j] = tr
}

func (it *IndexVote) Sort() {
	sort.Sort(it)
}

func NewIndexVote(size int) *IndexVote {
	out := new(IndexVote)
	out.Indexes = make([]int, size)
	out.Ratings = make([]float64, size)
	return out
}

// In-place sort
func (it *NameVote) Sort() {
	sort.Sort(it)
}

// sort.Interface
func (it *NameVote) Len() int {
	return len(*it)
}
func (it *NameVote) Less(i, j int) bool {
	a := (*it)[i].Rating > (*it)[j].Rating
	if a {
		return true
	}
	if (*it)[i].Rating == (*it)[j].Rating {
		c := strings.Compare((*it)[i].Name, (*it)[j].Name)
		return c < 0
	}
	return false
}
func (it *NameVote) Swap(i, j int) {
	t := (*it)[i]
	(*it)[i] = (*it)[j]
	(*it)[j] = t
}

func (it *NameVote) PrintHtml(out io.Writer) {
	fmt.Fprint(out, "<table class=\"namevote\">")
	for _, nr := range *it {
		fmt.Fprintf(out, "<tr><td class=\"name\">%s</td><td class=\"rate\">%0.2f</td></tr>", nr.Name, nr.Rating)
	}
	fmt.Fprint(out, "</table>")
}

func (it *NameVote) Append(name string, rating float64) *NameVote {
	out := append(*it, NameRating{name, rating})
	return &out
}

func (it *NameVote) Prepend(name string, rating float64) *NameVote {
	if len(*it) == 0 {
		x := []NameRating{{name, rating}}
		tx := NameVote(x)
		return &tx
	}
	out := make([]NameRating, len(*it)+1)
	copy(out[1:], *it)
	out[0] = NameRating{name, rating}
	tout := NameVote(out)
	return &tout
}

// Changes ratings in place
func (it *NameVote) ConvertRankingsToRatings() {
	maxRank := (*it)[0].Rating
	for _, nr := range *it {
		if nr.Rating > maxRank {
			maxRank = nr.Rating
		}
	}
	offset := maxRank + 1
	for i, nr := range *it {
		(*it)[i].Rating = offset - nr.Rating
	}
}

func (it *NameVote) String() string {
	m := make(map[string]float64, len(*it))
	for _, nr := range *it {
		m[nr.Name] = nr.Rating
	}
	mb, err := json.Marshal(m)
	if err != nil {
		return err.Error()
	}
	return string(mb)
}

// Map from names to indexes.
// The reverse should be kept as an array of string.
type NameMap struct {
	Indexes map[string]int
	Names   []string
}

func (nm *NameMap) NameToIndex(name string) int {
	if nm.Indexes == nil {
		nm.Indexes = make(map[string]int)
	}
	x, ok := nm.Indexes[name]
	if ok {
		return x
	}
	x = len(nm.Names)
	nm.Indexes[name] = x
	nm.Names = append(nm.Names, name)
	return x
}

func (nm *NameMap) IndexToName(x int) string {
	return nm.Names[x]
}

func (nm *NameMap) NameVoteToIndexVote(it NameVote) *IndexVote {
	//out := new(IndexVote)
	out := NewIndexVote(len(it))
	i := 0
	for _, nv := range it {
		out.Indexes[i] = nm.NameToIndex(nv.Name)
		out.Ratings[i] = nv.Rating
		i++
	}
	return out
}

// Return pairs [name1, value1, name2, value2, ...]
func urlParseQuery(q string) ([]string, error) {
	keystart := 0
	valstart := -1
	var key string
	var val string
	var err error = nil
	partcount := 1
	for _, ch := range q {
		if ch == '&' {
			partcount += 1
		}
	}
	out := make([]string, 0, partcount)
	for bytei, ch := range q {
		switch ch {
		case '=':
			if keystart >= 0 {
				key = q[keystart:bytei]
				keystart = -1
				valstart = bytei + 1
			}
		case '&', ';':
			if valstart >= 0 {
				val = q[valstart:bytei]
				key, err = url.QueryUnescape(key)
				if err != nil {
					return nil, err
				}
				val, err = url.QueryUnescape(val)
				if err != nil {
					return nil, err
				}
				out = append(out, key, val)
				valstart = -1
				keystart = bytei + 1
			}
		default:
		}
	}
	if valstart >= 0 {
		val = q[valstart:]
		key, err = url.QueryUnescape(key)
		if err != nil {
			return nil, err
		}
		val, err = url.QueryUnescape(val)
		if err != nil {
			return nil, err
		}
		out = append(out, key, val)
	}
	return out, err
}

// Parse url-query formatted vote.
// choice+one=1&choice+2=2&...
func UrlToNameVote(vote string) (*NameVote, error) {
	vals, err := urlParseQuery(vote)
	if err != nil {
		return nil, err
	}
	out := new(NameVote)
	for i := 0; i < len(vals); i += 2 {
		name := vals[i]
		value := vals[i+1]
		rating, err := strconv.ParseFloat(value, 64)
		if err != nil {
			return nil, err
		}
		*out = append(*out, NameRating{name, rating})
	}
	return out, nil
}

func NameVoteToUrl(vote NameVote) string {
	parts := make([]string, len(vote))
	for i, nv := range vote {
		parts[i] = fmt.Sprintf("%s=%g", url.QueryEscape(nv.Name), nv.Rating)
	}
	return strings.Join(parts, "&")
}

type ElectionMethod interface {
	// Add a vote to this instance.
	Vote(NameVote)

	// Add a vote to this instance.
	VoteIndexes(IndexVote)

	// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
	GetResult() (scores *NameVote, numWinners int)

	// Return HTML explaining the result.
	HtmlExplaination() (scores *NameVote, numWinners int, html string)

	// Set shared NameMap
	SetSharedNameMap(names *NameMap)

	// simple tag, lower case, no spaces
	ShortName() string

	// Longer descriptive name, may include options specific to this instance
	Name() string
}

type MultiSeat interface {
	ElectionMethod

	// Set the number of desired winners
	SetSeats(seats int)
}
