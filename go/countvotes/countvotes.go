package main

import "bufio"
import "flag"
import "fmt"
import "io"
import "os"
import "voteutil"


var filename = flag.String("in", "", "file name to read (default stdin")
var testMode = flag.Bool("test", false, "do test-mode output")

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

/*
 test output format:
{{system name}}: {{winner name}}[, {{winner name}}]\n
*/

func main() {
	flag.Parse()
	var rawin io.Reader
	var err error
	//var methods []*voteutil.ElectionMethod
	if len(*filename) > 0 {
		rawin, err = os.Open(*filename)
		if err != nil {
			return
		}
	} else {
		rawin = os.Stdin
	}
	in := bufio.NewReader(rawin)
	methods := make([]voteutil.ElectionMethod, 1)
	methods[0] = new(voteutil.VRR)
	//vrr := new(voteutil.VRR)
	for true {
		line, err := ReadLine(in)
		if err != nil {
			break
		}
		vote, err := voteutil.UrlToNameVote(line)
		for _, em := range methods {
			em.Vote(*vote)
		}
		//vrr.Vote(*vote)
	}
	for _, em := range methods {
		result, winners := em.GetResult()
		if *testMode {
			fmt.Printf("%s: ", em.ShortName())
			for i := 0; i < winners; i++ {
				if i > 0 {
					fmt.Print(", ")
				}
				fmt.Print((*result)[i].Name)
			}
			fmt.Print("\n")
		} else {
			fmt.Printf("winners:\n")
			fmt.Print(result, winners)
		}
	}
}

