#!/usr/bin/perl -w
#
# reads votes on standard in and writes html results on standard out.
#
# usage:
# votep.pl [--disable-all|--enable-all][--enable method][--disable method] < votes > results.html

use Vote;
use VRR;
use VRR2;
use IRNR;
use IRV;
use Histogram;

print <<EOF;
<html><head><title>vote results</title></head><body bgcolor="#ffffff" text="#000000">
EOF

%enabled = ( "hist" => 1, "irnr" => 1, "vrr" => 1, "irv" => 1 );
#my @they= ( new Histogram(-10,10), new IRNR, new VRR, new IRV );

while ( $arg = shift ) {
	if ( $arg eq "--disable-all" ) {
		%enabled = ();
	} elsif ( $arg eq "--enable-all" ) {
		%enabled = ( "hist" => 1, "irnr" => 1, "vrr" => 1, "irv" => 1 );
	} elsif ( $arg eq "--enable" ) {
		$arg = shift;
		$enabled{$arg} = 1;
	} elsif ( $arg eq "--disable" ) {
		$arg = shift;
		delete $enabled{$arg};
	}
}

@they = ();
if ( $enabled{"hist"} ) {
	push @they, new Histogram(-10,10);
}
if ( $enabled{"irnr"} ) {
	push @they, new IRNR;
}
if ( $enabled{"vrr"} ) {
	push @they, new VRR2;
}
if ( $enabled{"vrr1"} ) {
	push @they, new VRR;
}
if ( $enabled{"irv"} ) {
	push @they, new IRV;
}

while ( $line = <> ) {
	my $nvh = Vote::urlVotesToNameValueHash( $line );
	foreach $t ( @they ) {
		$t->voteNameValueHash( $nvh );
	}
}

foreach $t ( @they ) {
	print "<h2>" . $t->name() . "</h2>";
	print $t->htmlSummary();
}

print "</body></html>\n";
