#!/usr/bin/perl -w

use File::Temp qw/ tempfile tempdir /;
use File::Path;

@groups = (
	['c', "../c/countvotes"],
#	['cpp', "../cpp/dynamic_canditates/countvotes_dynamic"],
	['java', "java -jar ../java/vote.jar"],
#	['perl', "../perl/votep.pl"],
);

%implToApp = ();
foreach $g ( @groups ) {
	$implToApp{$g->[0]} = $g->[1];
}

$n = 1;

$tmpdir = undef;
$cleanup = 1;

while ( $arg = shift ) {
	if ( $arg eq "-n" ) {
		$n = shift;
	} elsif ( $arg eq "--tmpdir" ) {
		$tmpdir = shift;
		$cleanup = 0;
	} elsif ( $arg eq "--keep-temps" ) {
		$cleanup = 0;
	} else {
		print STDERR "bogus arg \"$arg\"\n";
		exit 1;
	}
}

$anybad = 0;

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
sub randNameSet($) {
	my $count = shift;
	my @out = ();
	my $i;
	for ($i = 1; $i <= $count; $i++) {
		push @out, randname($i);
	}
	return @out;
}
sub mysys($) {
	my $cmd = shift;
	system $cmd;
	if ($? == -1) {
		die "failed to execute:\n\t$cmd\n";
	} elsif ($? & 127) {
		$signal = $? & 127;
		$core = ($? & 128) ? "with" : "without";
		die "died with signal $signal, $core coredump:\n$cmd\n";
	} else {
		$val = $? >> 8;
		if ( $val != 0 ) {
			die "failed with $val:\n$cmd\n";
		}
	}
}

if ( ! defined $tmpdir ) {
	$tmpdir = tempdir( CLENAUP => 0 );
}

%redone = ();

for ($i = 0; $ i < $n; $i++ ) {
	($tf, $tfname) = tempfile( DIR => $tmpdir );
	$numChoices = int(rand(10)) + 2;
	@names = randNameSet($numChoices);
	$numVotes = int(rand(100000)) + 10;
	#print "creating $tfname with $numChoices choics and $numVotes votes\n";
	for ( $j = 0; $j < $numVotes; $j++ ) {
		print $tf join( '&', map { $_ . "=" . int(rand(11)) } @names ) . "\n";
	}
	close $tf;
	%results = ();
	@commands = ();
	foreach $impl ( @groups ) {
		$iname = $impl->[0];
		$app = $impl->[1];
		$outname = $tfname . "_" . $iname;
		$cmd = $app . " --test -o " . $outname . " -i " . $tfname;
		#print $cmd . "\n";
		push @commands, $cmd;
		mysys( $cmd );
		open FIN, '<', $outname;
		while ( $line = <FIN> ) {
			($meth, $result) = $line =~ /([^:]+): (.*)/;
			#print $iname . "\t" . $meth . "\n";
			if (! defined $results{$meth}) {
				$results{$meth} = {};
			}
			$results{$meth}->{$iname} = $result;
		}
		close FIN;
	}
	$localbad = 0;
	while (($meth, $they) = each %results) {
		#print $meth . ":";
		$pimpl = undef;
		$presult = undef;
		%x = %{$they};
		while (($iname, $result) = each %x) {
			if ((defined $pimpl) && ($presult ne $result)) {
				print STDERR<<EOF;
error: mismatch in $meth, numChoices=${numChoices}, numVotes=${numVotes}
$iname: $result
$pimpl: $presult
badvotes: $tfname
EOF
				$anybad = 1;
				$localbad = 1;
				$outname = $tfname . "_" . $iname . ".html";
				if ( ! $redone{$outname} ) {
					$cmd = $implToApp{$iname} . " --explain -o " . $outname . " -i " . $tfname;
					$redone{$outname} = 1;
				}
				mysys( $cmd );
				$outname = $tfname . "_" . $pimpl . ".html";
				if ( ! $redone{$outname} ) {
					$cmd = $implToApp{$pimpl} . " --explain -o " . $outname . " -i " . $tfname;
					$redone{$outname} = 1;
				}
				mysys( $cmd );
			} else {
				#print " " . $iname;
			}
			$pimpl = $iname;
			$presult = $result;
		}
		#print "\n";
	}
	if ( $localbad ) {
		print join("\n", @commands) . "\n";
	}
}

foreach $outname ( keys %redone ) {
	print $outname . "\n";
}

if ( ($cleanup) && (! $anybad) ) {
	rmtree($tmpdir, 0, 0);
}
