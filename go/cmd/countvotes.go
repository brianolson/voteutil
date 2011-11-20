package main

import "bufio"
import "flag"
import "fmt"
import "io"
import "os"
import "voting"


var filename = flag.String("in", "", "file name to read (default stdin")


func ReadLine(f *bufio.Reader) (string, os.Error) {
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

func main() {
	var rawin io.Reader
	var err os.Error
	if len(*filename) > 0 {
		rawin, err = os.Open(*filename)
		if err != nil {
			return
		}
	} else {
		rawin = os.Stdin
	}
	in := bufio.NewReader(rawin)
	vrr := new(voting.VRR)
	for true {
		line, err := ReadLine(in)
		if err != nil {
			break
		}
		vote, err := voting.UrlToNameVote(line)
		vrr.Vote(*vote)
	}
	winners := vrr.GetWinners()
	fmt.Printf("winners:\n")
	fmt.Print(winners)
}

