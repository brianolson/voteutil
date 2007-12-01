There are two libraries with separate implementations of election methods.

static_candidates/ contains implementations which requires a static ballot without write-ins. Some constant number of candidates is referred to by index

dynamic_candidates/ contains implementations which allow write-ins and require no knowledge of the ballot before they start counting. Each vote is a set of name=value pairs.

In all cases, the native input to election methods is a rating for each choice, and utility functions exist to parse text input and convert rankings (1st,2nd,3rd,...) to ratings.