#!/usr/bin/perl -w
#
$usage = <<EOF;
Usage:
./test.pl [--tmpdir dir][--keep-temps][--perf][--check][--both][-n runs]
          [--urlhandler cmd]

    --tmpdir dir      defaults to using perl tempdir() otherwise
    --keep-temps      do not delete temporary files if all tests pass (normally
                      kept on failures)
    --perf            Test speed, not correctness.
    --check           Test correctness, not speed (default).
    --both            Test both speed and correctness.
    --maxbad n        Maximum number of tests that can fail before stopping.
    -n runs           Number fo times to repeat correctness test.
    --urlhandler cmd  Execute `cmd \$url` for each failed test explain output.
EOF


use File::Temp qw/ tempfile tempdir /;
use File::Path;

%longToShort = (
	"Instant Runoff Normalized Ratings" => "irnr",
	"Virtual Round Robin" => "vrr",
	"Virtual Round Robin, Ranked Pairs Resolution" => "rp",
	"Raw Rating Summation" => "raw",
	"Instant Runoff Voting" => "irv",
	"Single Transferrable Vote" => "stv",
);

@groups = (
	['c', "../c/countvotes", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"] ],
#	['cpp', "../cpp/dynamic_canditates/countvotes_dynamic"],
	['java', "java -jar ../java/vote.jar", ["hist", "irnr", "vrr", "rp", "raw", "irv", "stv"] ],
    ['go', "../go/countvotes/countvotes", ["irnr", "vrr", "raw"] ],
#	['perl', "../perl/votep.pl"],
);

%implToApp = ();
foreach $g ( @groups ) {
	$implToApp{$g->[0]} = $g->[1];
}

$n = 1;

$urlhandler = undef;
$tmpdir = undef;
$cleanup = 1;
$do_correcness = 1;
$do_perf = 0;
$failcount = 0;
$maxfail = 1;
$do_build = 0;

while ( $arg = shift ) {
	if ( $arg eq "-n" ) {
		$n = shift;
	} elsif ( $arg eq "--tmpdir" ) {
		$tmpdir = shift;
		$cleanup = 0;
	} elsif ( $arg eq "--maxbad" ) {
		$maxfail = shift;
		$cleanup = 0;
	} elsif ( $arg eq "--keep-temps" ) {
		$cleanup = 0;
	} elsif ( $arg eq "--both" ) {
		$do_perf = 1;
		$do_correcness = 1;
	} elsif ( $arg eq "--perf" ) {
		$do_perf = 1;
		$do_correcness = 0;
	} elsif ( $arg eq "--no-perf" ) {
		$do_perf = 0;
	} elsif ( $arg eq "--build" ) {
		$do_build = 1;
	} elsif ( $arg eq "--check" ) {
		$do_correctness = 1;
		$do_perf = 0;
	} elsif ( $arg eq "--no-check" ) {
		$do_correctness = 0;
	} elsif ( $arg eq "--urlhandler" ) {
		$urlhandler = shift;
	} elsif ( ($arg eq "--help") or ($arg eq "-h") ) {
		print STDOUT $usage;
		exit 0;
	} else {
		print STDERR "bogus arg \"$arg\"\n";
		print STDERR $usage;
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
sub noisys($) {
	my $cmd = shift;
	system $cmd;
	if ($? == -1) {
		print STDERR "failed to execute:\n\t$cmd\n";
		return 0;
	} elsif ($? & 127) {
		$signal = $? & 127;
		$core = ($? & 128) ? "with" : "without";
		print STDERR "died with signal $signal, $core coredump:\n$cmd\n";
		return 0;
	} else {
		$val = $? >> 8;
		if ( $val != 0 ) {
			print STDERR "failed with $val:\n$cmd\n";
			return 0;
		}
	}
	return 1;
}

if ( ! defined $tmpdir ) {
	$tmpdir = tempdir( CLENAUP => 0 );
}

sub build() {
	my $cwd = $ENV{PWD};
	print "building java...\n";
	chdir("../java") or die "could not chdir ../java : $!\n";
	mysys("ant");
	print "building c...\n";
	chdir("../c") or die "could not chdir ../c : $!\n";
	mysys("make");
	chdir($cwd) or die "could not chdir $cwd : $!\n";
	print "done.\n";
}

if ($do_build) {
	build();
}

%redone = ();
$anybad = 0;

sub test_correctness() {
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
		$prev_impl = undef;
		$prev_result = undef;
		%x = %{$they};
		while (($iname, $result) = each %x) {
			if ((defined $prev_impl) && ($prev_result ne $result)) {
				print STDERR<<EOF;
error: mismatch in $meth, numChoices=${numChoices}, numVotes=${numVotes}
$iname: $result
$prev_impl: $prev_result
badvotes: $tfname
EOF
				$anybad = 1;
				$localbad = 1;
				$outname = $tfname . "_" . $iname . ".html";
				$enablestr = "";
				if ( $longToShort{$meth} ) {
					$enablestr = " --disable-all --enable " . $longToShort{$meth};
					$outname = $tfname . "_" . $iname . "_" . $longToShort{$meth} . ".html";
				}
				if ( ! $redone{$outname} ) {
					$cmd = $implToApp{$iname} . $enablestr . " --explain -o " . $outname . " -i " . $tfname;
					$redone{$outname} = 1;
				}
				push @commands, $cmd;
				mysys( $cmd );
				if ( $urlhandler ) {
					$uc = $urlhandler . " " . $outname;
					mysys( $uc );
				}
				$outname = $tfname . "_" . $prev_impl . ".html";
				if ( $longToShort{$meth} ) {
					$outname = $tfname . "_" . $prev_impl . "_" . $longToShort{$meth} . ".html";
				}
				if ( ! $redone{$outname} ) {
					$cmd = $implToApp{$prev_impl} . $enablestr . " --explain -o " . $outname . " -i " . $tfname;
					$redone{$outname} = 1;
				}
				push @commands, $cmd;
				mysys( $cmd );
				if ( $urlhandler ) {
					$uc = $urlhandler . " " . $outname;
					mysys( $uc );
				}
			} else {
				#print " " . $iname;
			}
			$prev_impl = $iname;
			$prev_result = $result;
		}
		#print "\n";
	}
	if ( $localbad ) {
		$failcount++;
		print join("\n", @commands) . "\n";
	} elsif ( $cleanup ) {
		unlink <${tfname}*>;
	}
}

if ( $do_correcness ) {
	$i = 0;
	while (($i < $n) and ($failcount < $maxfail)) {
		test_correctness();
		$i++;
	}
	
	foreach $outname ( keys %redone ) {
		print $outname . "\n";
	}
}

sub test_perf($$) {
	my $numChoices = shift;
	my $numVotes = shift;
	my ($tf, $tfname) = tempfile( DIR => $tmpdir );
	my @names = randNameSet($numChoices);
	#print "creating $tfname with $numChoices choics and $numVotes votes\n";
	for ( $j = 0; $j < $numVotes; $j++ ) {
		print $tf join( '&', map { $_ . "=" . int(rand(11)) } @names ) . "\n";
	}
	close $tf;
	%results = ();
	%meths = ();
	foreach $impl ( @groups ) {
		$iname = $impl->[0];
		$app = $impl->[1];
		$results{$iname} = {};
		@methods = @{$impl->[2]};
		$outname = $tfname . "_" . $iname;
		foreach $meth ( @methods ) {
			$cmd = $app . " --disable-all --enable " . $meth . " -o " . $outname . "_" . $meth . " -i " . $tfname;
			print $cmd . "\n";
			#push @commands, $cmd;
			@start = times();
			$ok = noisys( $cmd );
			if ( $ok ) {
				@fin = times();
				$dt = $fin[2] - $start[2];
				$results{$iname}->{$meth} = $dt;
			} else {
				$results{$iname}->{$meth} = "<span style=\"color:red\">error</span>";
			}
			$meths{$meth} = 1;
		}
	}
	@implNames = sort(keys %results);
	$implHeaders = join("</th><th>", @implNames);
	my $out =<<EOF;
<p>Times in seconds to count $numVotes votes with $numChoices choices:</p>
<table border="1"><tr><th></th><th>$implHeaders</th></tr>
EOF
	foreach $meth ( keys %meths ) {
		$out .= "<tr><td>$meth</td>";
		foreach $iname ( @implNames ) {
			$dt = $results{$iname}->{$meth};
			if ( defined $dt ) {
				$out .= "<td>$dt</td>";
			} else {
				$out .= "<td></td>";
			}
		}
	}
	$out .= "</table>\n";
	return $out;
}

if ( $do_perf ) {
	local $out = test_perf(20, 100000);
	$out .= test_perf(100, 10000);
	open FOUT, '>', '_perf.html';
	print FOUT $out;
	close FOUT;
}

if ( ($cleanup) && (! $anybad) ) {
	rmtree($tmpdir, 0, 0);
}
