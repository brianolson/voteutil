package Vote;

use strict;
use warnings;

# orderedNamesToNameValues( [ "first choice", "second choice" ] );
# returns [ "first choice", 2, "second choice", 1 ]
sub orderedNamesToNameValues($) {
	my $vote = shift;
	my $nv = [];
	my $i;
	for ( $i = 0; $i <= $#{$vote}; $i++ ) {
#		print ${$vote}[$i] . "\t" . ($#{$vote} - $i) . "\n";
		push( @{$nv}, ( ${$vote}[$i], $#{$vote} - $i ) );
	}
	return $nv;
}	

# "a > b = c > d"
# returns [ "a", 4, "b", 3, "c", 3, "d", 1 ]
sub gtEqVotesToNameValues($) {
	my $str = shift;
	my @they = split /\s*([>=])\s*/, $str;
	my $num = $#they / 2;
	my $next = $num - 1;
	my $i;
	my $toret = [];
	for ( $i = 0; $i <= $#they; $i++ ) {
		if ( $they[$i] eq "=" ) {
			$next--;
		} elsif ( $they[$i] eq ">" ) {
			$num = $next;
			$next = $num - 1;
		} else {
			push( @{$toret}, ( $they[$i], $num ) );
		}
	}
	return $toret;
}

# "a > b = c > d"
# returns { "a" => 4, "b" => 3, "c" => 3, "d" => 1 }
sub gtEqVotesToNameValueHash($) {
	my $str = shift;
	my @they = split /\s*([>=])\s*/, $str;
	my $num = $#they / 2;
	my $next = $num - 1;
	my $i;
	my $toret = {};
	for ( $i = 0; $i <= $#they; $i++ ) {
		my $part = $they[$i];
		if ( $part eq "=" ) {
			$next--;
		} elsif ( $part eq ">" ) {
			$num = $next;
			$next = $num - 1;
		} else {
			$toret->{$part} = $num;
		}
	}
	return $toret;
}

sub depercenthexify {
	my $s = shift;
	$s =~ s/\+/ /g;
	my @p = split /(%[0-9a-fA-F][0-9a-fA-F])/, $s;
	my @po = ();
	my $i;
	foreach $i ( @p ) {
		my $hh;
		if ( ! $i ) {
		} elsif ( ($hh) = $i =~ /%(..)/ ) {
			my $c = chr(hex($hh));
			push @po, "$c";
		} else {
			push @po, $i;
		}
	}
	$s = join "", @po;
	return $s;
}

# "a=9&b=2&c=1&d=3"
# returns name-value hash
sub urlVotesToNameValueHash($) {
	my $str = shift;
	my $pv = {};
	my $pb;
	foreach $pb ( split '&', $str ) {
		my $name;
		my $value;
		($name,$value) = split '=', $pb;
		$pv->{$name} = depercenthexify $value;
	}
	return $pv;
}

1;
