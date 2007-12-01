#!/usr/bin/perl -w

package IRNR;

use strict;
use warnings;

my $l2norm = 1;

sub setDebug($) {
}

sub new {
	my $class = shift;
	return bless [], $class;
}
# $irnr = [];

# voteNameValues( $irnr, [ "a", 3, "B", 5 ] );
# values are ratings, higher is better
sub voteNameValues($$) {
	my $counts = shift;
	my $vote = shift;
	push @{$counts}, $vote;
}

# voteNameValueHash( $self, { "a" => 3, "B" => 5 } );
# values are ratings, higher is better
sub voteNameValueHash($$) {
	my $counts = shift;
	my $vhi = shift;
	my %vh = %{$vhi};
	my @nvv = ();
	my $name;
	my $value;
	while ( ($name,$value) = each %vh ) {
		push @nvv, $name;
		push @nvv, $value;
	}
	push @{$counts}, [ @nvv ];
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

sub get_results($) {
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
				} elsif ( $l2norm ) {
					my $tv = ${$vote}[$i + 1];
					$vs += $tv * $tv;
				} else {
					$vs += abs( ${$vote}[$i + 1] );
				}
			}
			if ( $l2norm ) {
				$vs = sqrt( $vs );
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

sub htmlSummary($) {
	my $self = shift;
	my $toret = "";
	my @r = $self->get_results();
	my $i;
	$toret .= "<table border=\"1\"><tr><th>Name</th><th>IRNR Rating</th></tr>";
	for ( $i = 0; $i < $#r; $i += 2 ) {
		$toret .= "<tr><td>" . $r[$i] . "</td><td>" . sprintf("%.2f",$r[$i+1]) . "</td></tr>";
	}
	$toret .= "</table>";
	return $toret;
}

sub name($) {
	my $self = shift;
	return "Instant Runoff Normalized Ratings";
}

sub main {
	
	my $irnr = new IRNR;
	#voteNameValues( $irnr, [ "a", 3, "b", 5, "c", 4 ] );
	#voteNameValues( $irnr, [ "a", 4, "b", 5, "c", 5 ] );
	$irnr->voteOrderedNames( [ "b", "c", "a" ] );
	#voteOrderedNames( $irnr, [ "a", "b", "c" ] );
	$irnr->voteNameList( "a>b=c" );
	$irnr->voteNameList( "a>b=c" );
	$irnr->voteNameList( "a>b=c" );

	my @res = $irnr->get_results();

	#print( ($#{$irnr} + 1) . " votes\n" );

	foreach my $r ( @res ) {
		print "$r\n";
	}
}

1;
