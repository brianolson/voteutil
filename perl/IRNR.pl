#!/usr/bin/perl -w

# $irnr = [];

# voteNameValues( $irnr, [ "a", 3, "B", 5 ] );
sub voteNameValues($$) {
	my $counts = shift;
	my $vote = shift;
	push @{$counts}, $vote;
}

# voteOrderedNames( $irnr, [ "first choice", "second choice" ] );
sub voteOrderedNames($$) {
	my $counts = shift;
	my $vote = shift;
	my $nv = [];
	my $i;
	for ( $i = 0; $i <= $#{$vote}; $i++ ) {
#		print ${$vote}[$i] . "\t" . ($#{$vote} - $i) . "\n";
		push( @{$nv}, ( ${$vote}[$i], $#{$vote} - $i ) );
	}
	voteNameValues( $counts, $nv );
}

# voteNameList( $irnr, "first choice>second choice=third choice" ] );
# Be careful to %hex-escape any '>' or '=' characters in choice names.
sub voteNameList($$) {
	my $counts = shift;
	my $vote = shift;
	my @vl;
	my $i;
	my $nv = [];
	my( $rating, $nextrating );
	@vl = split( /([>=])/, $vote );
	print "vote: $vote\nvl: {@vl} join \{" . join( " ", @vl ) . "\}\n";
	$rating = $#vl / 2;
	$nextrating = $rating - 1;
	while ( $i = shift @vl ) {
		if ( $i eq "=" ) {
			$nextrating--;
		} elsif ( $i eq ">" ) {
			$rating = $nextrating;
			$nextrating = $rating - 1;
		} else {
			print "$i\t$rating\n";
			push( @{$nv}, ( $i, $rating ) );
		}
	}
	voteNameValues( $counts, $nv );
}

sub getIRNR_results($) {
	my $counts = shift;
	my $vote;
	my %dq = ();
	my @res = ();
	my @sq;
	
#	print( ($#{$counts} + 1) . " votes\n" );

	do {
		my( $name, $value, $i, $minn, $minv );
		my %sum = ();
		foreach $vote ( @{$counts} ) {
			my $vs = 0;
			for ( $i = 0; $i < $#{$vote}; $i += 2 ) {
				$name = ${$vote}[$i];
				if ( $dq{$name} ) {
#					skip
				} else {
					$vs += ${$vote}[$i + 1];
				}
			}
			for ( $i = 0; $i < $#{$vote}; $i += 2 ) {
				$name = ${$vote}[$i];
				if ( $dq{$name} ) {
#					skip
				} else {
					$value = ${$vote}[$i + 1] / $vs;
					$sum{$name} += $value;
				}
			}
		}
		$minn = undef;
		$minv = undef;
		while ( ($name,$value) = each %sum ) {
			if ( (! defined $minv) || ($value < $minv) ) {
				$minn = $name;
				$minv = $value;
			}
#			print "$name\t$value\n";
		}
		unshift( @res, ($minn, $minv) );
		$dq{$minn} = 1;
		@sq = keys %sum;
		if ( $#sq <= 1 ) {
			delete $sum{$minn};
			if ( ($name,$value) = each %sum ) {
				unshift( @res, ($name, $value) );
			}
		}
	} while ( $#sq > 1 );
	return @res;
}

$irnr = [];
#voteNameValues( $irnr, [ "a", 3, "b", 5, "c", 4 ] );
#voteNameValues( $irnr, [ "a", 4, "b", 5, "c", 5 ] );
voteOrderedNames( $irnr, [ "b", "c", "a" ] );
#voteOrderedNames( $irnr, [ "a", "b", "c" ] );
voteNameList( $irnr, "a>b=c" );
voteNameList( $irnr, "a>b=c" );
voteNameList( $irnr, "a>b=c" );

@res = getIRNR_results( $irnr );

#print( ($#{$irnr} + 1) . " votes\n" );

foreach $r ( @res ) {
	print "$r\n";
}
