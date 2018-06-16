package main

import "bufio"

//import "flag" // can't use, doesn't allow repeated options (--enable vrr --enable irnr)
import "fmt"
import "log"
import "io"
import "os"
import "runtime/pprof"
import "strings"
import "strconv"
import "github.com/brianolson/voteutil/go/voteutil"

// from countvotes.c
const usage = `
countvotes [--dump][--debug][--preindexed]
	[--full-html|--no-full-html|--no-html-head]
	[--disable-all][--enable-all][--seats n]
	[--rankings][--help|-h|--list][--explain]
	[-o filename|--out filenam]
	[--enable|--disable hist|irnr|vrr|raw|irv|stv]
	[input file name|-i votesfile|-igz gzipped-votesfile]
	[--test]
`

type ElectionMethodConstructor func() voteutil.ElectionMethod

var classByShortname map[string]ElectionMethodConstructor

func init() {
	classByShortname = map[string]ElectionMethodConstructor{
		"vrr":  func() voteutil.ElectionMethod { return voteutil.NewVRR() },
		"raw":  func() voteutil.ElectionMethod { return voteutil.NewRawSummation() },
		"irnr": func() voteutil.ElectionMethod { return voteutil.NewIRNR() },
		"stv":  func() voteutil.ElectionMethod { return voteutil.NewSTV() },
	}
}

// Map String to List of Strings - Get
// If the key isn't in the map, put an empty slice there and return that.
func msls_get(it map[string][]string, key string) []string {
	sl, ok := it[key]
	if ok {
		return sl
	}
	sl = make([]string, 0, 1)
	it[key] = sl
	return sl
}

// Probably takes os.Args[1:]
// argnums is map from --?(option name) [some number of arguments for option, probably 0 or 1]
// --?(option name)=(.*) is always interpreted as one argument
// "--" stops parsing and all further args go to the list of non-option program arguments at out[""]
func ParseArgs(args []string, argnums map[string]int) (map[string][]string, error) {
	out := make(map[string][]string)
	pos := 0
	for pos < len(args) {
		ostr := args[pos]
		// "--" by itself, stop parsing args and pass the rest through as strings
		if ostr == "--" {
			sl := msls_get(out, "")
			sl = append(sl, args[pos+1:]...)
			out[""] = sl
			break
		}
		if ostr[0] == '-' {
			var argname string
			var eqpart string
			eqpos := strings.IndexRune(ostr, '=')
			if eqpos < 0 {
				eqpos = len(ostr)
			} else {
				eqpart = ostr[eqpos+1:]
			}
			if ostr[1] == '-' {
				argname = ostr[2:eqpos]
			} else {
				argname = ostr[1:eqpos]
			}
			if argname == "help" {
				os.Stdout.Write([]byte(usage))
				os.Exit(1)
			}
			argnum, ok := argnums[argname]
			if !ok {
				return nil, fmt.Errorf("unknown arg: %#v", ostr)
			}
			sl := msls_get(out, argname)
			if eqpos < len(ostr) {
				if argnum != 1 {
					return nil, fmt.Errorf("got =part which is only valid for args which take one value, $#v takes %d values", argname, argnum)
				}
				sl = append(sl, eqpart)
			} else {
				for argnum > 0 {
					pos++
					argnum--
					if pos >= len(args) {
						return nil, fmt.Errorf("missing argument to %#v", ostr)
					}
					sl = append(sl, args[pos])
				}
			}
			out[argname] = sl
		} else {
			sl := msls_get(out, "")
			sl = append(sl, ostr)
			out[""] = sl
		}
		pos++
	}

	return out, nil
}

// Get args by multiple names.
// e.g. ["i", "input"] gets things passed to -i or --input (or --i or -input)
func GetArgs(args map[string][]string, names []string) []string {
	out := []string{}
	for _, name := range names {
		values, ok := args[name]
		if ok {
			out = append(out, values...)
		}
	}
	return out
}

func ReadLine(f *bufio.Reader) (string, error) {
	pdata, isPrefix, err := f.ReadLine()
	if !isPrefix {
		return string(pdata), err
	}
	data := make([]byte, len(pdata))
	copy(data, pdata)
	for isPrefix {
		pdata, isPrefix, err = f.ReadLine()
		data = append(data, pdata...)
	}
	return string(data), err
}

type VoteSource interface {
	// Return (vote, nil) normally, (nil, nil) on EOF.
	GetVote() (*voteutil.NameVote, error)
}

type FileVoteSource struct {
	rawReader      io.Reader
	bReader        *bufio.Reader
	commentHandler func(line string)
	rankings       bool
}

func OpenFileVoteSource(path string) (*FileVoteSource, error) {
	rawin, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	return &FileVoteSource{rawin, bufio.NewReader(rawin), nil, false}, nil
}

func (it *FileVoteSource) GetVote() (*voteutil.NameVote, error) {
	if it.bReader == nil {
		return nil, nil
	}
	for true {
		line, err := ReadLine(it.bReader)
		if err == io.EOF {
			it.bReader = nil
			if len(line) == 0 {
				return nil, nil
			}
		} else if err != nil {
			log.Print("got err reading from vote file ", err)
			return nil, err
		}
		if len(line) == 0 {
			continue
		}
		if line[0] == '#' {
			if it.commentHandler != nil {
				it.commentHandler(line)
			}
		}
		if line[0] == '\r' || line[0] == '\n' {
			continue
		}
		vote, err := voteutil.UrlToNameVote(line)
		if err != nil {
			return vote, err
		}
		if it.rankings {
			vote.ConvertRankingsToRatings()
		}
		return vote, nil
	}
	return nil, nil
}

/*
 test output format:
{{system name}}: {{winner name}}[, {{winner name}}]\n
*/

type Election struct {
	methods   []voteutil.ElectionMethod
	VoteCount int
}

func (it *Election) Vote(vote *voteutil.NameVote) {
	it.VoteCount++
	for _, m := range it.methods {
		m.Vote(*vote)
	}
}

func (it *Election) VoteAll(source VoteSource) (int, error) {
	numVotes := 0
	for true {
		vote, err := source.GetVote()
		if err != nil {
			return numVotes, err
		}
		if vote == nil {
			return numVotes, nil
		}
		it.Vote(vote)
		numVotes++
	}
	return numVotes, nil
}

func doenable(methodEnabled map[string]bool, enstr string, endis bool) {
	comma := strings.IndexRune(enstr, ',')
	if comma != -1 {
		doenable(methodEnabled, enstr[:comma], endis)
		doenable(methodEnabled, enstr[comma+1:], endis)
	} else {
		_, ok := classByShortname[enstr]
		if !ok {
			var verb string
			if endis {
				verb = "enable"
			} else {
				verb = "disable"
			}
			fmt.Fprintf(os.Stderr, "cannot %s unknown method %#v", verb, enstr)
			os.Exit(1)
			return
		}
		methodEnabled[enstr] = endis
	}
}

func main() {
	//flag.Parse()
	//var rawin io.Reader
	var err error

	argnums := map[string]int{
		"o":           1,
		"out":         1,
		"test":        0,
		"i":           1,
		"enable":      1,
		"disable":     1,
		"explain":     0,
		"enable-all":  0,
		"disable-all": 0,
		"cpuprofile":  1,
		"seats":       1,
		"verbose":     0,
		"rankings":    0,
		"full-html":   0, // TODO: unused, implement
		/*
			   TODO: implement
			"no-full-html": 0,
			"no-html-head": 0,
			"dump":         0,
		*/
	}

	methodEnabled := map[string]bool{
		"vrr":  true,
		"irnr": true,
		"raw":  true,
		"stv":  false,
	}

	args, err := ParseArgs(os.Args[1:], argnums)
	if err != nil {
		fmt.Fprint(os.Stderr, err)
		os.Exit(1)
		return
	}

	_, verbose := args["verbose"]
	_, rankings := args["rankings"]

	outNames := GetArgs(args, []string{"o", "out"})

	if len(outNames) > 1 {
		fmt.Fprintf(os.Stderr, "error: can accept at most one output file name, got %#v", outNames)
		os.Exit(1)
		return
	}
	outw := os.Stdout
	if len(outNames) == 1 {
		path := outNames[0]
		if path == "-" {
			// already writing to stdout
		} else {
			outw, err = os.Create(path)
			if err != nil {
				fmt.Fprintf(os.Stderr, "%#v: cannot be opened for output\n", path)
				os.Exit(1)
				return
			}
		}
	}

	inNames := GetArgs(args, []string{"i", ""})

	_, enableAll := args["enable-all"]
	if enableAll {
		for enkey, _ := range methodEnabled {
			methodEnabled[enkey] = true
		}
	}

	_, disableAll := args["disable-all"]
	if disableAll {
		for enkey, _ := range methodEnabled {
			methodEnabled[enkey] = false
		}
	}

	enableStrs, hasEnables := args["enable"]
	if hasEnables {
		for _, enstr := range enableStrs {
			doenable(methodEnabled, enstr, true)
		}
	}

	disableStrs, hasDisables := args["disable"]
	if hasDisables {
		for _, enstr := range disableStrs {
			doenable(methodEnabled, enstr, false)
		}
	}

	cpuprofilePath, hasCpuprofile := args["cpuprofile"]
	if hasCpuprofile {
		cpuproff, err := os.Create(cpuprofilePath[0])
		if err != nil {
			log.Fatal("could not open cpuprofile file ", err)
		}
		pprof.StartCPUProfile(cpuproff)
		defer pprof.StopCPUProfile()
	}

	var seats int = 1
	seatsStrs, hasSeats := args["seats"]
	if hasSeats {
		ts, err := strconv.Atoi(seatsStrs[0])
		if err != nil {
			log.Fatal("bad arg for seats: ", seatsStrs[0])
		}
		seats = ts
	}

	_, testMode := args["test"]
	_, showExplain := args["explain"]

	methods := make([]voteutil.ElectionMethod, 0, len(methodEnabled))
	for methodShort, isEnabled := range methodEnabled {
		if isEnabled {
			nm := classByShortname[methodShort]()
			var seatable bool
			if nms, seatable := nm.(voteutil.MultiSeat); seatable {
				nms.SetSeats(seats)
			}
			if verbose {
				if seatable && (seats > 1) {
					fmt.Fprintf(os.Stderr, "enabled \"%s\", %d seats\n", methodShort, seats)
				} else {
					fmt.Fprintf(os.Stderr, "enabled \"%s\"\n", methodShort)
				}
			}
			methods = append(methods, nm)
		}
	}

	election := Election{methods, 0}

	if len(inNames) == 0 {
		vs := &FileVoteSource{
			os.Stdin,
			bufio.NewReader(os.Stdin),
			nil,
			rankings,
		}
		numVotes, err := election.VoteAll(vs)
		if err != nil {
			fmt.Fprintf(os.Stderr, "stdin: error reading votes: %s\n", err)
			os.Exit(1)
			return
		}
		if verbose {
			fmt.Fprintf(os.Stderr, "stdin: %d votes\n", numVotes)
		}
	}
	for _, path := range inNames {
		vs, err := OpenFileVoteSource(path)
		if err != nil {
			fmt.Fprintf(os.Stderr, "%s: %s\n", path, err)
			break
		}
		vs.rankings = rankings
		numVotes, err := election.VoteAll(vs)
		if err != nil {
			fmt.Fprintf(os.Stderr, "%s: error reading votes: %s\n", path, err)
			os.Exit(1)
			return
		}
		if verbose {
			fmt.Fprintf(os.Stderr, "%s: %d votes\n", path, numVotes)
		}
	}
	//log.Print("counted votes: ", election.VoteCount)

	for _, em := range methods {
		var result *voteutil.NameVote
		var winners int
		var html string
		if showExplain {
			result, winners, html = em.HtmlExplaination()
		} else {
			result, winners = em.GetResult()
		}
		if testMode {
			fmt.Fprintf(outw, "%s: ", em.ShortName())
			if result != nil {
				for i, res := range *result {
					if i > 0 {
						fmt.Fprint(outw, ", ")
					}
					fmt.Fprint(outw, res.Name)
				}
			}
			fmt.Fprint(outw, "\n")
		} else if showExplain {
			if len(methods) > 1 {
				fmt.Fprintf(outw, "<h2>%s</h2>", em.Name())
			}
			fmt.Fprint(outw, html)
		} else {
			if len(methods) > 1 {
				fmt.Fprint(outw, em.Name(), " ")
			}
			fmt.Fprint(outw, "winners:\n")
			//fmt.Fprint(outw, result, winners)
			for i, r := range *result {
				win := ""
				if i < winners {
					win = "* "
				}
				fmt.Fprintf(outw, "%s%s\t%0.2f\n", win, r.Name, r.Rating)
			}
		}
	}
}
