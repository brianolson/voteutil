package voting

type NameVote map[string] float64
type IndexVote map[int] float64

// Map from names to indexes.
// The reverse should be kept as an array of string.
type NameMap struct {
	Indexes map[string] int
	Names []string
}

func (nm *NameMap) NameToIndex(name string) int {
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
	out := new(IndexVote)
	for name, value := range it {
		x := nm.NameToIndex(name)
		(*out)[x] = value
	}
	return out
}

type ElectionMethod interface {
	Vote(NameVote)
	VoteIndexes(IndexVote)
	GetWinners() *NameVote
	HtmlExlpaination() string
}
