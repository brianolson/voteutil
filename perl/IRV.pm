#!/usr/bin/perl -w

package IRV;

use strict;
use warnings;

my $DEBUG = 0;

sub setDebug($) {
	$DEBUG = shift;
}

sub new {
	my $class = shift;
	return bless [], $class;
}

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

sub bucketize($$$) {
	my $counts = shift;
	my $buckets = shift;
	my $dq = shift;
	my $vote;
	foreach $vote ( @{$counts} ) {
		my $vi = undef;
		my $maxr = undef;
		my $i;
		my $ties = 0;
		for ( $i = 0; $i < $#{$vote}; $i += 2 ) {
			my $name = $vote->[$i];
			if ( $dq->{$name} ) {
#					skip
			} elsif ( (! defined $vi) || ( $vote->[$i + 1] > $maxr ) ) {
				$vi = $i;
				$maxr = $vote->[$i + 1];
				$ties = 1;
			} elsif ( $vote->[$i + 1] == $maxr ) {
				$ties++;
			}
		}
		if ( defined $vi ) {
			my $name = $vote->[$vi];
			if ( $ties > 1 ) {
				$name = "TIED_VOTE_LABEL";
			}
			if ( ! defined $buckets->{$name} ) {
				$buckets->{$name} = [];
			}
			if ( $DEBUG ) {
				print "IRV bucketize \"$name\"<br>\n";
			}
			push @{$buckets->{$name}}, $vote;
		}
	}
}

sub get_results($) {
	my $counts = shift;
	my $vote;
	my %dq = ();
	my @res = ();
	my @sq;
	my %buckets = ();
	my $active;
	my @rebucket = ();
	
#	print( ($#{$counts} + 1) . " votes\n" );
	{
# collect names which get no first place votes
		my %namesh = ();
		foreach $vote ( @{$counts} ) {
			my $i;
			for ( $i = 0; $i < $#{$vote}; $i += 2 ) {
				$namesh{$vote->[$i]} = 1;
			}
		}
		bucketize( $counts, \%buckets, \%dq );
		my $name;
		foreach $name ( sort keys %namesh ) {
			if ( ! $buckets{$name} ) {
				$dq{$name} = 1;
				unshift( @res, ($name, 0) );
				if ( $DEBUG ) {
					print "IRV dq \"$name\" = 0<br>\n";
				}
			}
		}
	}
	
	
	do {
		my( $name, $value, $i, $minn, $minv, $ba, $tied );
		$minn = undef;
		$minv = undef;
		$active = 0;
		$tied = 0;
		while ( $ba = shift @rebucket ) {
			bucketize( $ba, \%buckets, \%dq );
		}

		while ( ($name,$ba) = each %buckets ) {
			my $value = $#{$ba} + 1;
			if ( $name eq "TIED_VOTE_LABEL" ) {
#				skip
			} elsif ( (! defined $minv) || ($value < $minv) ) {
				$minn = $name;
				$minv = $value;
				$active++;
				$tied = 1;
			} elsif ( $value == $minv ) {
				$tied++;
				$active++;
			} else {
				$active++;
			}
#			print "$name\t$value\n";
		}
		if ( $tied == 1 ) {
			unshift( @res, ($minn, $minv) );
			$dq{$minn} = 1;
			$active--;
			if ( $DEBUG ) {
				print "IRV dq \"$minn\" = $minv<br>\n";
			}
			push @rebucket, $buckets{$minn};
			delete $buckets{$minn};
		} else {
			while ( ($name,$ba) = each %buckets ) {
				my $value = $#{$ba} + 1;
				if ( $name eq "TIED_VOTE_LABEL" ) {
#				skip
				} elsif ( $value == $minv ) {
					unshift( @res, ($name, $value) );
					$dq{$name} = 1;
					$active--;
					if ( $DEBUG ) {
						print "IRV dq \"$name\" = $value<br>\n";
					}
					push @rebucket, $buckets{$name};
					delete $buckets{$name};
				}
			}
		}
		{
			my $tb = $buckets{"TIED_VOTE_LABEL"};
			if ( defined $tb ) {
				push @rebucket, $tb;
				delete $buckets{"TIED_VOTE_LABEL"};
				if ( $DEBUG ) {
					print "IRV rebucketize ties...<br>\n";
				}
			}
		}
		if ( $DEBUG ) {
			print "IRV active = $active<br>\n";
		}
	} while ( $active > 1 );
	my ($name, $ba);
	while ( ($name,$ba) = each %buckets ) {
		my $value = $#{$ba} + 1;
		if ( $name eq "TIED_VOTE_LABEL" ) {
#				skip
		} else {
			unshift( @res, ($name, $value) );
			if ( $DEBUG ) {
				print "IRV winner \"$name\" = $value<br>\n";
			}
		}
	}
	return @res;
}

sub htmlSummary($) {
	my $self = shift;
	my $toret = "";
	my @r = $self->get_results();
	my $i;
	$toret .= "<table border=\"1\"><tr><th>Name</th><th>Best IRV Count</th></tr>";
	for ( $i = 0; $i < $#r; $i += 2 ) {
		$toret .= "<tr><td>" . $r[$i] . "</td><td>" . $r[$i+1] . "</td></tr>";
	}
	$toret .= "</table>";
	return $toret;
}

sub name($) {
	my $self = shift;
	return "Instant Runoff Voting";
}

1;
