#!/usr/bin/perl -w

$joiner = "&";
$num = 10;
$nnames = 4;
$indexnames = 0;
$namelen = 6;

while ( $arg = shift ) {
	if ( $arg eq "-n" || $arg eq "-v" ) {
		$num = shift;
	} elsif ( $arg eq "--numnames" || $arg eq "-nnames" || $arg eq "-c" ) {
		$nnames = shift;
	} elsif ( $arg eq "-F" ) {
		$joiner = shift;
	} elsif ( $arg eq "-i" ) {
		$indexnames = 1;
	} else {
		print STDERR "bogus arg \"$arg\"\n";
		exit 1;
	}
}

sub randname($) {
	my $length = shift;
	my $out = "";
	my $i = 0;
	while ( $i < $length ) {
		$out  = $out . chr(ord('a') + int(rand(26)));
		$i++;
	}
	return $out;
}

@names = ();
for ( $i = 0; $i < $nnames; $i++ ) {
	if ( $indexnames ) {
		push @names, $i;
	} else {
		push @names, randname($namelen);
	}
}
%bias = ();
foreach $n ( @names ) {
	$bias{$n} = 1;
}
$bias{$names[1]} = 1.2;
$bias{$names[3]} = 1.4;
for ( $i = 0; $i < $num; $i++ ) {
	print join( $joiner, map { $_ . "=" . (rand(10) * $bias{$_}) } @names ) . "\n";
}
