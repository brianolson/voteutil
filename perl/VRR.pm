package VRR;

# Virtual Round Robin election, also known as Condorcet's Method

use strict;
use warnings;

use Vote;

my $DEBUG = 0;

sub setDebug($) {
	$DEBUG = shift;
}

my $winningVotes = 1;
my $margins = 0;

my $dummyName = 'VRR_DUMMY_NAME';

sub new {
	my $class = shift;
	return bless { newbiePenalty => 0, versus => {}, names => { $dummyName => 0 } }, $class;
}

# voteNameValueHash( $self, { "a" => 3, "B" => 5 } );
# values are ratings, higher is better
sub voteNameValueHash($$) {
	my $self = shift;
	my $vhi = shift;
	my %vh = %{$vhi};
	my @votenames = keys %vh;
	
	$self->{winners} = undef;
	
	if ( $DEBUG ) {
		print "vote name values: ";
		my ($name, $value);
		while ( ($name,$value) = each( %vh ) ) {
			print "\"$name\"=$value ";
		}
		print ("\n");
	}
	
	# new names lose to all who went before.
	foreach my $name ( @votenames ) {
		if ( ! $self->{names}->{$name} ) {
			my @dummyparts = grep( /$dummyName/, (keys %{$self->{versus}}) );
#print "dummyparts: @dummyparts\n";
			foreach my $dp ( @dummyparts ) {
				my $np = $dp;
				$np =~ s/$dummyName/$name/;
				if ( $DEBUG ) { print "copy \"$dp\"=$self->{versus}->{$dp} to $np\n"; }
				$self->{versus}->{$np} = $self->{versus}->{$dp};
			}
			$self->{names}->{$name} = 1;
		} else {
			# increment old name's vote count.
			$self->{names}->{$name} = $self->{names}->{$name} + 1;
		}
	}

	my @names = keys %{$self->{names}};
	my ($i, $j);
	for ( $i = 0; $i < $#names; $i++ ) {
		my $namea = $names[$i];
		my $votea = $vh{$namea};
		if ( ! defined $votea ) {
			$votea = 0;
		}
		for ( $j = $i + 1; $j <= $#names; $j++ ) {
			my $nameb = $names[$j];
			my $voteb = $vh{$nameb};
			if ( ! defined $voteb ) {
				$voteb = 0;
			}
			if ( $votea > $voteb ) {
				if ( $DEBUG ) { print "${namea} over ${nameb}\n"; }
				$self->{versus}->{"${namea}o${nameb}"}++;
			} elsif ( $votea < $voteb ) {
				if ( $DEBUG ) { print "${nameb} over ${namea}\n"; }
				$self->{versus}->{"${nameb}o${namea}"}++;
			}
		}
	}
}


# voteNameValues( $self, [ "a", 3, "B", 5 ] );
# values are ratings, higher is better
sub voteNameValues($$) {
	my $self = shift;
	my $vote = shift;
	my %vh = @{$vote};
	return voteNameValueHash( $self, \%vh );
}

# voteOrderedNames( $self, [ "first choice", "second choice" ] );
sub voteOrderedNames($$) {
	my $self = shift;
	my $vote = shift;
	voteNameValues( $self, Vote::orderedNamesToNameValues( $vote ) );
#	my $nv = [];
#	my $i;
#	for ( $i = 0; $i <= $#{$vote}; $i++ ) {
##		print ${$vote}[$i] . "\t" . ($#{$vote} - $i) . "\n";
#		push( @{$nv}, ( ${$vote}[$i], $#{$vote} - $i ) );
#	}
#	voteNameValues( $self, $nv );
}

sub verifySchwartzSet($$$) {
	my $versus = shift;
	my @names = @{shift()};
	my @ss = @{shift()};
	for ( my $i = 0; $i <= $#ss; $i++ ) {
		my $m;
		$m = $ss[$i];
		my $namem = $names[$m];
# check for defeats by choices outside the set
		for ( my $j = 0; $j <= $#names; $j++ ) {
			my $namej = $names[$j];
			my $notinset;
			$notinset = 1;
			SSETLP: for ( my $k = 0; $k <= $#ss; $k++ ) {
				if ( $ss[$k] == $j ) {
					$notinset = 0;
					last SSETLP;
				}
			}
			if ( $notinset ) {
				my $vm = $versus->{"${namem}o${namej}"};	# m beat j vm times // OR m prefered to j vm times
				my $vj = $versus->{"${namej}o${namem}"};	# j beat m vj times // OR j prefered to m vj times
				if ( $vj > $vm ) {
					if ( $DEBUG ) {
						print "choice $m in bad schwartz set defeated by $j not in set\n";
					}
					return 0;
				}
			}
		}
# check if defated by all choices inside the set
		my $innerDefeats = 0;
		for ( my $k = 0; $k <= $#ss; $k++ ) {
			my $j;
			$j = $ss[$k];
			my $namej = $names[$j];
			if ( $m != $j ) {
				my $vm = $versus->{"${namem}o${namej}"};	# m beat j vm times // OR m prefered to j vm times
				my $vj = $versus->{"${namej}o${namem}"};	# j beat m vj times // OR j prefered to m vj times
				if ( $vj > $vm ) {
					$innerDefeats++;
				}
			}
		}
		if ( ($innerDefeats > 0) && ($innerDefeats == $#ss) ) {
			if ( $DEBUG ) {
				print "choice $m in bad schwartz is defeated by all in set.\n";
			}
			return 0;
		}
	}
# not disproven by exhaustive test, thus it's good
	return 1;
}

sub notIgnore($$$) {
	my $na = shift;
	my $nb = shift;
	my @ignorePairs = @{shift()};
	foreach my $ip ( @ignorePairs ) {
		if ( ((${$ip}[0] eq $na) && (${$ip}[1] eq $nb)) ||
			 ((${$ip}[0] eq $nb) && (${$ip}[1] eq $na)) ) {
			return 0;
		}
	}
	return 1;
}

sub getSchwartzSet($$$$) {
	my $versus = shift;
	my @names = @{shift()};
	my @defeatCount = @{shift()};
	my @ignorePairs = @{shift()};
	my $numWinners = 1;
	my $mindefeats = $defeatCount[0];
	my @choiceIndecies = ( 0 );
	my ($j, $k);
	for ( $j = 1; $j <= $#defeatCount; $j++ ) {
		if ( $defeatCount[$j] < $mindefeats ) {
			$choiceIndecies[0] = $j;
			$numWinners = 1;
			$mindefeats = $defeatCount[$j];
		} elsif ( $defeatCount[$j] == $mindefeats ) {
			$choiceIndecies[$numWinners] = $j;
			$numWinners++;
		}
	}
	if ( $mindefeats != 0 ) {
# the best there is was defeated by some choice, make sure that is added to the set
		for ( my $i = 0; $i < $numWinners; $i++ ) {
# foreach k in set of least defeated ...
			my $namei = $names[$i];
			$k = $choiceIndecies[$i];
			for ( $j = 0; $j < $#defeatCount; $j++ ) {
				my $namej = $names[$j];
			if ( ($k != $j) && notIgnore( $namei, $namej, \@ignorePairs ) ) {
				my $vk = $versus->{"${namei}o${namej}"};	# k beat j vk times // OR k prefered to j vk times
				my $vj = $versus->{"${namej}o${namei}"};	# j beat k vj times // OR j prefered to k vj times
				if ( $vj > $vk ) {
# j defeats k, j must be in the set
					my $gotj = 0;
					GOTJLP: for ( my $si = 0; $si < $numWinners; $si++ ) {
						if ( $choiceIndecies[$si] == $j ) {
							$gotj = 1;
							last GOTJLP;
						}
					}
					if ( ! $gotj ) {
						$choiceIndecies[$numWinners] = $j;
						$numWinners++;
					}
				}
			}}
		}
	}
	my @sset = ();#new int[$numWinners];
	for ( $j = 0; $j < $numWinners; $j++ ) {
		$sset[$j] = $choiceIndecies[$j];
	}
	if ( $DEBUG && (! verifySchwartzSet( $versus, \@names, \@sset )) ) {
		print "getSchwartzSet is returning an invalid Schwartz set!\nbogus set: " . join( ", ", @sset ) . "\n";
		if ( $DEBUG ) {
#htmlTable( debugsb, numc, tally, "tally not met by schwartz set", null );
#			debugsb.append( "bad sset: " );
#			debugsb.append( sset[0] );
#			for ( j = 1; j < sset.length; j++ ) {
#				debugsb.append(", ");
#				debugsb.append(sset[j]);
#			}
		}
		return [];
	}
	return \@sset;
}

sub countDefeats($$) {
	my $versus = shift;
	my @names = @{shift()};
	my @defeats = ();
	for ( my $i = 0; $i <= $#names; $i++ ) {
		$defeats[$i] = 0;
	}
	for ( my $i = 0; $i <= $#names; $i++ ) {
		my $namei = $names[$i];
		for ( my $j = $i + 1; $j <= $#names; $j++ ) {
			my $namej = $names[$j];
			my $xi = $versus->{"${namei}o${namej}"};
			my $xj = $versus->{"${namej}o${namei}"};
			if ( ! defined $xi ) {
				if ( ! defined $xj ) {
# tie? neither has been voted for over the other?
				} else {
					$defeats[$i]++;
				}
			} elsif ( ! defined $xj ) {
				$defeats[$j]++;
			} elsif ( $xi > $xj ) {
				$defeats[$j]++;
			} elsif ( $xj > $xi ) {
				$defeats[$i]++;
			} else {
# tie, each has been voted for over the other an equal number of times.
			}
		}
	}
	return @defeats;
}
sub get_results($) {
	my $self = shift;
	if ( defined $self->{winners} ) {
		return $self->{winners};
	}
	my @names = sort ( grep( !/$dummyName/, (keys %{$self->{names}}) ) );
	my ($i, $j);
	my @defeats = countDefeats( $self->{versus}, \@names );
	my @winners = ();
	if ( $DEBUG ) {
		print "<p>Initial defeat counts</p>\n<table border=\"1\">\n";
	}
	for ( $i = 0; $i <= $#names; $i++ ) {
		if ( $DEBUG ) {
			print "<tr><td>$names[$i]</td><td>$defeats[$i]</td></tr>\n";
		}
		if ( ! defined $defeats[$i] ) {
			print "why is defeats[$i] (\"$names[$i]\") undefined ?\n";
		} elsif ( $defeats[$i] == 0 ) {
			push @winners, $names[$i];
		}
	}
	if ( $DEBUG ) {
		print "</table>\n";
	}
	if ( $#winners == 0 ) {
# there is one unique undefeated winner
		my @reti = sort { $defeats[$a] <=> $defeats[$b] } (0 .. $#names);
		$self->{winners} = [map { ( $names[$_], $defeats[$_] ) } @reti];
		if ( $DEBUG ) {
			print "<p>return a<br>".join(", ",(0 .. $#names))."<br>".join(", ",@reti)."<br>".join(", ",@{$self->{winners}})."</p>\n";
		}
		return $self->{winners};
	}
	my @ignorePairs = ();
	my @ss = @{getSchwartzSet( $self->{versus}, \@names, \@defeats, \@ignorePairs )};
	my %versus = %{$self->{versus}};
	my %versusCopy = %versus;
	my $notdone = 1;
	my $notdonelimit = $#names + 1;
	while ( $notdone ) {
		$notdone = 0;
		my $mind = 2000000000;
		my $mindj = -1;
		my $mindk = -1;
		my $tie = 0;
# find weakest defeat between members of schwartz set
		for ( my $ji = 0; $ji < $#ss; $ji++ ) {
			$j = $ss[$ji];
			my $namej = $names[$j];
			for ( my $ki = $ji + 1; $ki <= $#ss; $ki++ ) {
				my $k = $ss[$ki];
				my $namek = $names[$k];
				my $vk = $self->{versus}->{"${namek}o${namej}"};	# k beat j vk times // OR k prefered to j vk times
				my $vj = $self->{versus}->{"${namej}o${namek}"};	# j beat k vj times // OR j prefered to k vj times
				if ( $vj > $vk ) {
					if ( $winningVotes ) {
						if ( $vj < $mind ) {
							$mind = $vj;
							$mindj = $j;
							$mindk = $k;
							$tie = 1;
						} elsif ( $vj == $mind ) {
							$tie++;
						}
					} elsif ( $margins ) {
						my $m = $vj - $vk;
						if ($m < $mind) {
							$mind = $m;
							$mindj = $j;
							$mindk = $k;
							$tie = $1;
						} elsif ( $m == $mind ) {
							$tie++;
						}
					}
				} elsif ( $vk > $vj ) {
					if ( $winningVotes ) {
						if ( $vk < $mind ) {
							$mind = $vk;
							$mindj = $j;
							$mindk = $k;
							$tie = 1;
						} elsif ( $vk == $mind ) {
							$tie++;
						}
					} elsif ( $margins ) {
						my $m = $vk - $vj;
						if ($m < $mind) {
							$mind = $m;
							$mindj = $j;
							$mindk = $k;
							$tie = 1;
						} elsif ( $m == $mind ) {
							$tie++;
						}
					}
				}
			}
		}
		if ( $tie == 0 ) {
			if ( $DEBUG ) {
				print("tie = 0, no weakest defeat found to cancel\n");
			}
			$self->{winners} = \@ss;
			return $self->{winners};
		}
		# all are tied
		if ( $tie == ($#ss + 1) ) {
			if ( $DEBUG ) {
				print("tie==ss.length, mind=$mind, mindj=$mindj, mindk=$mindk\n");
			}
			$self->{winners} = \@ss;
			return $self->{winners};
		}
#		push @ignorePairs, [ $names[$mindk], $names[$mindj] ];
		$versusCopy{"$names[$mindk]o$names[$mindj]"} = 0;
		$versusCopy{"$names[$mindj]o$names[$mindk]"} = 0;
		if ( $DEBUG ) { print "copy " . $versusCopy{"$names[$mindk]o$names[$mindj]"} . " versus " . $versus{"$names[$mindk]o$names[$mindj]"} . "\n"; }
#		tally[mindk*numc + mindj] = 0;
#		tally[mindj*numc + mindk] = 0;
		if ( $DEBUG ) {
			print "$mindk/$mindj = 0\n";
#			htmlTable( debugsb, numc, tally, "intermediate", null );
		}
		@defeats = countDefeats( \%versusCopy, \@names );
		@ss = @{getSchwartzSet( \%versusCopy, \@names, \@defeats, \@ignorePairs )};
#		ss = getSchwartzSet( numc, tally, defeatCount, debugsb );
		if ( $#ss == 0 ) {
			my @reti = sort {
				$defeats[$a] <=> $defeats[$b]
			} 0..$#names;
			if ( $DEBUG ) {
				print "<p>return B</p>\n";
			}
			$self->{winners} = [map { ( $names[$_], $defeats[$_] ) } @reti];
			return $self->{winners};
#			return [map { ( $names[$_], $defeats[$_] ) } @ss];
		}
		if ( $DEBUG ) {
			print "ss={ " . join( ", ", @ss ) . " }\n";
		}
		$notdone = 1;
		if ( ($notdonelimit--) <= 0 ) {
			if ( $DEBUG ) { print "hit limit!\n"; }
			my @reti = sort {
				$defeats[$a] <=> $defeats[$b]
			} 0..$#names;
			$self->{winners} = [map { ( $names[$_], $defeats[$_] ) } @reti];
			return $self->{winners};
#			return [map { ( $names[$_], $defeats[$_] ) } @ss];
		}
	}
	my @reti = sort {
		$defeats[$a] <=> $defeats[$b]
	} 0..$#names;
	if ( $DEBUG ) {
		print "<p>return at end</p>\n";
	}
	$self->{winners} = [map { ( $names[$_], $defeats[$_] ) } @reti];
	return $self->{winners};
#	return [map { ( $names[$_], $defeats[$_] ) } @ss];
}

sub hashToTable($$) {
	my $versus = shift;
	my @names = @{shift()};
	my ($i, $j);
	my $toret = "<table border=\"1\"><tr><td></td>" . join("", map { "<td>($_)</td>" } (0 .. $#names) ) . "</tr>";
	for ( $i = 0; $i <= $#names; $i++ ) {
		my $name = $names[$i];
		$toret = $toret . "<tr><td>($i) $name</td>";
		for ( $j = 0; $j <= $#names; $j++ ) {
			my $nameb = $names[$j];
			if ( $i == $j ) {
				$toret = $toret . "<td></td>";
			} else {
				my $x = $versus->{"${name}o${nameb}"};
				my $ox = $versus->{"${nameb}o${name}"};
				if ( ! defined $x ) {
					$toret = $toret . "<td></td>";
				} elsif ( (! defined $ox) || ($x > $ox) ) {
					$toret = $toret . "<td bgcolor=\"#bbffbb\">$x</td>";
				} elsif ( $x < $ox ) {
					$toret = $toret . "<td bgcolor=\"#ffbbbb\">$x</td>";
				} else {
					$toret = $toret . "<td>$x</td>";
				}
			}
		}
		$toret = $toret . "</tr>";
	}
	$toret = $toret . "</table>";
	if ( $DEBUG ) {
		$toret = $toret . "<table border=\"1\">";
		foreach my $k ( sort keys %{$versus} ) {
			my $v = $versus->{$k};
			$toret = $toret . "<tr><td>$k</td><td>$v</td></tr>";
		}
		$toret = $toret . "</table>";
	}
	return $toret;
}
sub htmlVersusTable($) {
	my $self = shift;
	my @names = sort ( grep( !/$dummyName/, (keys %{$self->{names}}) ) );
	return hashToTable( $self->{versus}, \@names );
}

sub htmlSummary($) {
	my $self = shift;
	my $tr = $self->get_results();
	my @r = ( @{$tr} );
	my $name;
	my $val;
	my @names = ();
	my $i;
	for ( $i = 0; $i < $#r; $i += 2 ) {
		push @names, $r[$i];
	}
	my $toret = hashToTable( $self->{versus}, \@names );
	$name = shift @r;
	$val = shift @r;
	$toret .= "<table border=\"1\">";#<tr><th>name</th><th>defeats</th></tr>
	while ( defined $name && defined $val ) {
		$toret .= "<tr><td>" . $name . "</td><td>" . ($val * -1) . "</td></tr>";
		$name = shift @r;
		$val = shift @r;
	}
	$toret .= "</table>";
	return $toret;
}

sub name($) {
	my $self = shift;
	return "Virtual Round Robin";
}

# perl -mVRR -e 'VRR::main;'

sub main {
	
	$DEBUG = 0;
	print "<pre>\n";
	my $vrr = new VRR;
#voteNameValues( $vrr, [ "a", 3, "b", 5, "c", 4 ] );
#voteNameValues( $vrr, [ "a", 4, "b", 5, "c", 5 ] );
	$vrr->voteOrderedNames( [ "b", "c", "a" ] );
	$vrr->voteOrderedNames( [ "b", "c", "a" ] );
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
	my $ht = $vrr->htmlVersusTable();
	print "</pre>\n";
	print "$ht\n";

	print "<p>Winners:</p><table border=\"1\">\n";
	for ( my $i = 0; $i < $#res; $i += 2 ) {
		print "\t<tr><td>$res[$i]</td><td>$res[$i+1]</td></tr>\n";
	}
	print "</table>\n";
}

1;
