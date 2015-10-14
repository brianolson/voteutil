package main

import "bufio"
//import "flag" // can't use, doesn't allow repeated options (--enable vrr --enable irnr)
import "fmt"
//import "log"
import "io"
import "os"
import "strings"
import "voteutil"


// from countvotes.c
/*
countvotes [--dump][--debug][--preindexed]\n"
"\t[--full-html|--no-full-html|--no-html-head]\n"
"\t[--disable-all][--enable-all][--seats n]\n"
"\t[--rankings][--help|-h|--list][--explain]\n"
"\t[-o filename|--out filenam]\n"
"\t[--enable|--disable hist|irnr|vrr|raw|irv|stv]\n"
"\t[input file name|-i votesfile|-igz gzipped-votesfile]\n"
"\t[--test]
*/

type ElectionMethodConstructor func() voteutil.ElectionMethod;

var classByShortname map[string]ElectionMethodConstructor
func init() {
	classByShortname = map[string]ElectionMethodConstructor {
		"vrr": voteutil.NewVRR,
		"raw": voteutil.NewRawSummation,
		"irnr": voteutil.NewIRNR,
	}
}


// Map String to List of Strings - Get
// If the key isn't in the map, put an empty slice there and return that.
func msls_get(it map[string] []string, key string) []string {
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
func ParseArgs(args []string, argnums map[string]int) (map[string] []string, error) {
	out := make(map[string] []string)
	pos := 0
	for pos < len(args) {
		ostr := args[pos]
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
		pos++;
	}

	return out, nil
}

// Get args by multiple names.
// e.g. ["i", "input"] gets things passed to -i or --input (or --i or -input)
func GetArgs(args map[string] []string, names []string) []string {
	out := []string{}
	for _, name := range(names) {
		values, ok := args[name]
		if ok {
			out = append(out, values...)
		}
	}
	return out
}


func ReadLine(f *bufio.Reader) (string, error) {
	pdata, isPrefix, err := f.ReadLine()
	if ! isPrefix {
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
	rawReader io.Reader
	bReader *bufio.Reader
}

func OpenFileVoteSource(path string) (*FileVoteSource, error) {
	rawin, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	return &FileVoteSource{rawin, bufio.NewReader(rawin)}, nil
}

func (it *FileVoteSource) GetVote() (*voteutil.NameVote, error) {
	// TODO: check EOF and return (nil, nil)
	line, err := ReadLine(it.bReader)
	if err != nil {
		return nil, err
	}
	return voteutil.UrlToNameVote(line)
}

/*
 test output format:
{{system name}}: {{winner name}}[, {{winner name}}]\n
*/

type Election struct {
	methods []voteutil.ElectionMethod
}

func (it *Election) Vote(vote *voteutil.NameVote) {
	for _, m := range(it.methods) {
		m.Vote(*vote)
	}
}

func (it *Election) VoteAll(source VoteSource) error {
	for true {
		vote, err := source.GetVote()
		if err != nil {
			return err
		}
		if vote == nil {
			return nil
		}
		it.Vote(vote)
	}
	return nil
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
		"o": 1,
		"out": 1,
		"test": 0,
		"i": 1,
		"enable": 1,
		"disable": 1,
		"explain": 0,
		"enable-all": 0,
		"disable-all": 0,
/*
TODO: implement
		"full-html": 0,
		"no-full-html": 0,
		"no-html-head": 0,
		"dump": 0,
		"debug": 0,
*/
	}

	methodEnabled := map[string]bool {
		"vrr": true,
		"irnr": true,
		"raw": true,
	}

	args, err := ParseArgs(os.Args[1:], argnums)
	if err != nil {
		fmt.Fprint(os.Stderr, err)
		os.Exit(1)
		return
	}

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
		for enkey, _ := range(methodEnabled) {
			methodEnabled[enkey] = true
		}
	}

	_, disableAll := args["disable-all"]
	if disableAll {
		for enkey, _ := range(methodEnabled) {
			methodEnabled[enkey] = false
		}
	}

	enableStrs, hasEnables := args["enable"]
	if hasEnables {
		for _, enstr := range(enableStrs) {
			doenable(methodEnabled, enstr, true)
		}
	}

	disableStrs, hasDisables := args["disable"]
	if hasDisables {
		for _, enstr := range(disableStrs) {
			doenable(methodEnabled, enstr, false)
		}
	}

	_, testMode := args["test"]
	_, showExplain := args["explain"]

	methods := make([]voteutil.ElectionMethod, 0)
	for methodShort, isEnabled := range(methodEnabled) {
		if isEnabled {
			methods = append(methods, classByShortname[methodShort]())
		}
	}

	election := Election{methods}

	if len(inNames) == 0 {
		vs := &FileVoteSource{
			os.Stdin,
			bufio.NewReader(os.Stdin),
		}
		election.VoteAll(vs)
	}
	for _, path := range(inNames) {
		vs, err := OpenFileVoteSource(path)
		if err != nil {
			fmt.Fprintf(os.Stderr, "%s: %s\n", path, err)
			break
		}
		election.VoteAll(vs)
	}
	
	for _, em := range methods {
		result, winners := em.GetResult()
		if testMode {
			fmt.Fprintf(outw, "%s: ", em.ShortName())
			if result != nil {
				for i, res := range(*result) {
					if i > 0 {
						fmt.Fprint(outw, ", ")
					}
					fmt.Fprint(outw, res.Name)
				}
			}
			fmt.Fprint(outw, "\n")
		} else if showExplain {
			fmt.Fprint(outw, em.HtmlExplaination())
		} else {
			fmt.Fprintf(outw, "winners:\n")
			fmt.Fprint(outw, result, winners)
		}
	}
}

