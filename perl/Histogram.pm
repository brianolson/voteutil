package Histogram;

# Histogram, looks like another election method

use strict;
use warnings;

use Vote;
use POSIX('floor');

my $DEBUG = 0;

sub setDebug($) {
	$DEBUG = shift;
}

sub round($) {
	return sprintf( "%.0f", $_[0] );
}

#          0    1    2     3        4          5        6            7            8    9
# vars = [ min, max, step, buckets, maxbucket, intmode, minRecorded, maxRecorded, sum, votes ]
# my ( $min, $max, $step, $buckets, $maxbucket, $intmode, $minRecorded, $maxRecorded, $sum, $votes ) = @{shift()};
sub new {
	my $class = shift;
	my $min = shift;
	my $max = shift;
	my $buckets = shift;
	my $intmode = 0;
	my $step;
	if ( ! defined $buckets ) {
		$min = round( $min );
		$max = round( $max );
		$buckets = $max - $min + 1;
		$step = 1.0;
		$min -= 0.5;
		$max += 0.5;
		$intmode = 1;
	} else {
		$step = $buckets / ( $max - $min );
	}
	return bless { vars => [ $min, $max, $step, $buckets, 0, $intmode, 999999999, -999999999, 0, 0 ], names => {} }, $class;
}

# voteNameValueHash( $self, { "a" => 3, "B" => 5 } );
# values are ratings, higher is better
sub voteNameValueHash($$) {
	my $self = shift;
	my $vote = shift;
	my $vars = $self->{vars};
	my $names = $self->{names};
	my $name;
	my $value;

	while ( ($name, $value) = each %{$vote} ) {
		$vars->[8] += $value;
		$vars->[9]++;
		if ( $value < $vars->[6] ) {
			$vars->[6] = $value;
		}
		if ( $value > $vars->[7] ) {
			$vars->[7] = $value;
		}
		my $bucket = 0;
		if ( $value < $vars->[0] ) {
			$bucket = 0;
		} elsif ( $value > $vars->[1] ) {
			$bucket = $vars->[3] - 1;
		} else {
			$bucket = floor( $vars->[2] * ($value - $vars->[0]) );
		}
		my $counts = $names->{$name};
		if ( ! defined $counts ) {
			$names->{$name} = $counts = [];
		}
		$counts->[$bucket]++;
		$DEBUG && print "${name}[${bucket}]++ => $counts->[$bucket]\n";
		if ( $counts->[$bucket] > $vars->[4] ) {
			$vars->[4] = $counts->[$bucket];
		}
	}
}

sub get_results($) {
	my $self = shift;
	return [];
}

my $outerTableCols = 4;
my $barImgUrl = "b.png";
my $useSpan = 1;
my $printPercents = 0;
my $maxWidth = 100;
my $maximizeWidth = 1;

sub htmlSummary($) {
	my $self = shift;
	my ( $min, $max, $step, $buckets, $maxbucket, $intmode, $minRecorded, $maxRecorded, $sum, $votes ) = @{$self->{vars}};
	my $names = $self->{names};
	my @cnames = sort keys %{$names};

	my $toret = '';
	my $scale = 1.0;
	if ( $maximizeWidth || $maxbucket > $maxWidth ) {
		$scale = $maxWidth / $maxbucket;
	}
	my $oTC = 0;
	if ( $outerTableCols > 1 ) {
		$toret .= ( "<table border=\"1\">" );
	}
	my $maxBucket = floor( $step * ($maxRecorded - $min) );
	if ( $maxBucket > $buckets - 1 ) {
		$maxBucket = $buckets - 1;
	}
	my $minBucket = floor( $step * ($minRecorded - $min) );
	if ( $minBucket < 0 ) {
		$minBucket = 0;
	}
	for ( my $c = 0; $c <= $#cnames; $c++ ) {
		my $cc;
		my $total;
		my $name = $cnames[$c];
		if ( $outerTableCols > 1 ) {
			if ( $oTC == 0 ) {
				$toret .= ( "<tr>" );
			}
			$toret .= ( "<td width=\"" ) . ( 100 / $outerTableCols ) . ( "%\">" );
		}
		$toret .= ( "<table><tr><th colspan=\"2\">" );
		$toret .= $name . "\n";
		$toret .= ( "</th></tr><tr><th>Rating</th><th>Votes</th></tr>\n" );
		$cc = $names->{$name};
		$total = 0;
		for ( my $i = $maxBucket; $i >= $minBucket; $i-- ) {
			my $hval;
			$hval = $cc->[$i];
			if ( ! defined $hval ) {
				$hval = 0;
			}
			$total += $hval;
			my $valueLabel = '';
			if ( $intmode ) {
				$valueLabel = round( $i * $step + $min + 0.5 );
			} elsif ( $i == $buckets - 1 ) {
				$valueLabel = ">= " + ($max - (1.0/$step));
			} elsif ( $i == 0 ) {
				$valueLabel = "< " + ($min + (1.0/$step));
			} else {
				$valueLabel = "[" + ($min + ($i/$step)) + " .. " + ($min + (($i+1)/$step)) + ")";
			}
			if ( $printPercents ) {
				$toret .= ( "<tr><td>" ) . ( $valueLabel ) . ( "</td><td>" ) . ( $hval ) . ( "</td></tr>\n" );
			} else {
				$toret .= ( "<tr><td>" ) . ( $valueLabel );
				if ( $useSpan ) {
					$toret .= ( "</td><td><div style=\"background-color: #bb99ff; width: " ) . ( round($hval * $scale) ) . ( "\">" ) . ( $hval ) . ( "</div></td></tr>\n" );
				} else {
					$toret .= ( "</td><td><img src=\"") . ( $barImgUrl ) . ("\" height=\"10\" width=\"" ) . ( round($hval * $scale) ) . ( "\"> " ) . ( $hval ) . ( "</td></tr>\n" );
				}
			}
		}
		$toret .= ( "<tr><td>total</td><td>" ) . ( $total ) . ( "</td></tr></table>\n" );
		if ( $outerTableCols > 1 ) {
			$toret .= ( "</td>" );
			$oTC = ( $oTC + 1 ) % $outerTableCols;
			if ( $oTC == 0 ) {
				$toret .= ( "</tr>\n" );
			}
		}
	}
	if ( $outerTableCols > 1 ) {
		if ( $oTC != 0 ) {
			$toret .= ( "</tr>" );
		}
		$toret .= ( "</table>\n" );
	}
}

sub name($) {
	my $self = shift;
	return "Histogram";
}

# perl -mVRR -e 'VRR::main;'

sub main {
	
	$DEBUG = 0;
	print "<pre>\n";
	my $vrr = new Histogram(-10,10);
#voteNameValues( $vrr, [ "a", 3, "b", 5, "c", 4 ] );
#voteNameValues( $vrr, [ "a", 4, "b", 5, "c", 5 ] );
#	$vrr->voteOrderedNames( [ "b", "c", "a" ] );
#	$vrr->voteOrderedNames( [ "b", "c", "a" ] );
#voteOrderedNames( $vrr, [ "a", "b", "c" ] );
#	$vrr->voteNameList( "a>b=c" );
	$vrr->voteNameValueHash( Vote::gtEqVotesToNameValueHash( "c>a>b" ) );
	$vrr->voteNameValueHash( Vote::gtEqVotesToNameValueHash( "c>a>b" ) );
	$vrr->voteNameValueHash( { 'a' => 2, 'c' => -1 } );
	$vrr->voteNameValueHash( { 'a' => 2, 'c' => -1 } );
#	$vrr->voteNameValueHash( { 'a' => 2, 'c' => -1 } );
#	$vrr->voteNameValueHash( { 'a' => 1, 'ralph nader' => 2 } );
	$vrr->voteNameValueHash( { 'ralph nader' => 3, 'a' => 2, 'b' => 1 } );
	
	my @res = @{$vrr->get_results()};
	
#print( ($#{$irnr} + 1) . " votes\n" );
	my $ht = $vrr->htmlSummary();
	print "</pre>\n";
	print "$ht\n";

	print "<p>Winners:</p><table border=\"1\">\n";
	for ( my $i = 0; $i < $#res; $i += 2 ) {
		print "\t<tr><td>$res[$i]</td><td>$res[$i+1]</td></tr>\n";
	}
	print "</table>\n";
}

1;
