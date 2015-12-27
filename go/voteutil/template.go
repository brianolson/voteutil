package voteutil

type TemplateElectionMethod struct {
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *TemplateElectionMethod) Vote(vote NameVote) {
}

// Add a vote to this instance.
// ElectionMethod interface
func (it *TemplateElectionMethod) VoteIndexes(vote IndexVote) {
}

// Get sorted result for the choices, and the number of winners (may be >1 if there is a tie.
// ElectionMethod interface
func (it *TemplateElectionMethod) GetResult() (*NameVote, int) {
	return nil, 0
}

// Return HTML explaining the result.
// ElectionMethod interface
func (it *TemplateElectionMethod) HtmlExplaination() (scores *NameVote, numWinners int, html string) {
	return nil, 0, "TODO: html explain"
}

// Set shared NameMap
// ElectionMethod interface
func (it *TemplateElectionMethod) SetSharedNameMap(names *NameMap) {
}

// simple tag, lower case, no spaces
// ElectionMethod interface
func (it *TemplateElectionMethod) ShortName() string {
	return "template"
}

// Full proper name. May be extended to describe options.
// e.g. "Single Transferrable Vote (fractional transfer)"
// ElectionMethod interface
func (it *TemplateElectionMethod) Name() string {
	return "Template ElectionMethod Example"
}

// Set the number of desired winners
// MultiSeat interface
func (it *TemplateElectionMethod) SetSeats(seats int) {
}
