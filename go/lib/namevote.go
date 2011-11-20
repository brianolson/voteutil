package voting

import "strconv"
import "os"
import "url"

type NameRating struct {
	Name string
	Rating float64
}

//type NameVote map[string] float64
//type IndexVote map[int] float64
type NameVote []NameRating
type IndexVote struct {
	Indexes []int
	Ratings []float64
}

func NewIndexVote(size int) *IndexVote {
	out := new(IndexVote)
	out.Indexes = make([]int, size)
	out.Ratings = make([]float64, size)
	return out
}

// Map from names to indexes.
// The reverse should be kept as an array of string.
type NameMap struct {
	Indexes map[string] int
	Names []string
}

func (nm *NameMap) NameToIndex(name string) int {
	if nm.Indexes == nil {
		nm.Indexes = make(map[string] int)
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

// Parse url-query formatted vote.
// choice+one=1&choice+2=2&...
func UrlToNameVote(vote string) (*NameVote, os.Error) {
	vals, err := url.ParseQuery(vote)
	if err != nil {
		return nil, err
	}
	out := new(NameVote)
	for name, slist := range vals {
		if len(slist) != 1 {
			return nil, os.NewError("Too many values for name: " + name)
		}
		rating, err := strconv.Atof64(slist[0])
		if err != nil {
			return nil, err
		}
		*out = append(*out, NameRating{name, rating})
	}
	return out, nil
}

type ElectionMethod interface {
	Vote(NameVote)
	VoteIndexes(IndexVote)
	GetWinners() *NameVote
	HtmlExlpaination() string
}

/*
function ends without a return statement
func ReturnValue(foo bool) int {
	if foo {
		return 1
	} else {
		return 0
	}
}

*/
