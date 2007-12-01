package VRR2;

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

#my $dummyName = 'VRR_DUMMY_NAME';

sub new {
	my $class = shift;
	return bless { numc => 0, counts => [], names => {} }, $class;
}

sub growVRR($$) {
	my $self = shift;
	my $x = shift;
	my $numc = $self->{numc};
	my $counts = $self->{counts};
	if ( $DEBUG ) {
		print "growVRR $x\n";
	}
	while ( $x > $numc ) {
		my $ni = $numc + 1;
		my $j;
		my $cni = [];
		$cni->[$numc] = 0;
		$cni->[$numc + $ni] = 0;
		for ( $j = 0; $j < $numc; $j++ ) {
			$cni->[$j] = $counts->[$numc]->[$j];
			$cni->[$j + $ni] = $counts->[$numc]->[$j + $numc];
		}
		$counts->[$ni] = $cni;
		$numc = $ni;
	}
	$self->{numc} = $numc;
}

# voteNameValueHash( $self, { "a" => 3, "B" => 5 } );
# values are ratings, higher is better
sub voteNameValueHash($$) {
	my $self = shift;
	my $names = $self->{names};
	my $vhi = shift;
	my %vh = %{$vhi};
	my @votenames = ();
	my @voteratings = ();
	my ($n,$r);
	while ( ($n,$r) = each %vh ) {
		push @votenames, $n;
		push @voteratings, $r;
	}
	my @voteindecies;
	my ($i, $j);
	my $counts = $self->{counts};
	
	$self->{winners} = undef;
	
	if ( $DEBUG ) {
		print "vote name values: ";
		my ($name, $value);
		while ( ($name,$value) = each( %vh ) ) {
			print "\"$name\"=$value ";
		}
		print ("\n");
	}
	my $numc = $self->{numc};
	for ( $i = 0; $i <= $#votenames; $i++ ) {
		my $x;
		$x = $names->{$votenames[$i]};
		if ( ! defined $x ) {
			$names->{$votenames[$i]} = $numc;
			$x = $numc;
			if ( $DEBUG ) {
				print "define \"$votenames[$i]\" $x\n";
			}
			growVRR( $self, $x + 1 );
			$numc = $self->{numc};
		}
		$voteindecies[$i] = $x;
# vote vs unvoted dummy
		if ( $voteratings[$i] >= 0 ) {
			$counts->[$numc + 1]->[$numc + 1 + $x]++;
		} else {
			$counts->[$numc + 1]->[$x]++;
		}
	}
#			static /*__inline*/ void _incxy( VRR* it, int x, int y ) {
#				if (x > y) {
#					it->counts[x][y]++;
#				} else {
#					it->counts[y][y+x]++;
#				}
#			}
	my $ii;
	for ( $i = 0; $i < $#voteindecies; $i++ ) {
		$ii = $voteindecies[$i];
		my $ir = $voteratings[$i];
		for ( $j = $i + 1; $j <= $#voteindecies; $j++ ) {
			if ( $ir > $voteratings[$j] ) {
				if ( $ii > $voteindecies[$j] ) {
					$counts->[$ii]->[$voteindecies[$j]]++;
				} else {
					$counts->[$voteindecies[$j]]->[$voteindecies[$j] + $ii]++;
				}
			} elsif ( $voteratings[$j] > $ir ) {
				if ( $voteindecies[$j] > $ii ) {
					$counts->[$voteindecies[$j]]->[$ii]++;
				} else {
					$counts->[$ii]->[$ii + $voteindecies[$j]]++;
				}
			} else {
# tie rating policy?
			}
		}
	}
	while ( ($n,$ii) = each %{$names} ) {
		my $x = $vh{$n};
		if ( ! defined $x ) {
			for ( $j = 0; $j <= $#voteindecies; $j++ ) {
				if ( $voteratings[$j] >= 0 ) {
					if ( $voteindecies[$j] > $ii ) {
						$counts->[$voteindecies[$j]]->[$ii]++;
					} else {
						$counts->[$ii]->[$ii + $voteindecies[$j]]++;
					}
				} else {
					if ( $ii > $voteindecies[$j] ) {
						$counts->[$ii]->[$voteindecies[$j]]++;
					} else {
						$counts->[$voteindecies[$j]]->[$voteindecies[$j] + $ii]++;
					}
				}
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

sub verifySchwartzSet($$$$) {
	my $numc = shift;
	my $tally = shift;
	my $ss = shift;
	my $numWinners = shift;
	for ( my $i = 0; $i < $numWinners; $i++ ) {
		my $m;
		$m = $ss->[$i];
# check for defeats by choices outside the set
		for ( my $j = 0; $j <= $numc; $j++ ) {
			my $notinset;
			$notinset = 1;
			SSETLP: for ( my $k = 0; $k <= $numWinners; $k++ ) {
				if ( $ss->[$k] == $j ) {
					$notinset = 0;
					last SSETLP;
				}
			}
			if ( $notinset ) {
				my $vm = $tally->[($m*$numc)+$j];	# m beat j vm times // OR m prefered to j vm times
				my $vj = $tally->[($j*$numc)+$m];	# j beat m vj times // OR j prefered to m vj times
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
		for ( my $k = 0; $k <= $numWinners; $k++ ) {
			my $j;
			$j = $ss->[$k];
			if ( $m != $j ) {
				my $vm = $tally->[($m*$numc)+$j];	# m beat j vm times // OR m prefered to j vm times
				my $vj = $tally->[($j*$numc)+$m];	# j beat m vj times // OR j prefered to m vj times
				if ( $vj > $vm ) {
					$innerDefeats++;
				}
			}
		}
		if ( ($innerDefeats > 0) && ($innerDefeats == $numWinners) ) {
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
	my $self = shift;
	my $tally = shift;
	my $defeatCount = shift;
	my $ss = shift;
	my $numWinners = 1;
	my $numc = $self->{numc};
	my $mindefeats = $defeatCount->[0];
	my @choiceIndecies = ( 0 );
	my ($j, $k);
	for ( $j = 1; $j <= $numc; $j++ ) {
		if ( $defeatCount->[$j] < $mindefeats ) {
			$choiceIndecies[0] = $j;
			$numWinners = 1;
			$mindefeats = $defeatCount->[$j];
		} elsif ( $defeatCount->[$j] == $mindefeats ) {
			$choiceIndecies[$numWinners] = $j;
			$numWinners++;
		}
	}
	if ( $mindefeats != 0 ) {
# the best there is was defeated by some choice, make sure that is added to the set
		for ( my $i = 0; $i < $numWinners; $i++ ) {
# foreach k in set of least defeated ...
			$k = $choiceIndecies[$i];
			for ( $j = 0; $j < $numc; $j++ ) {
				my $vk = $tally->[($k*$numc)+$j];	# k beat j vk times // OR k prefered to j vk times
				my $vj = $tally->[($j*$numc)+$k];	# j beat k vj times // OR j prefered to k vj times
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
			}
		}
	}
	my @sset = ();#new int[$numWinners];
	for ( $j = 0; $j < $numWinners; $j++ ) {
		$sset[$j] = $choiceIndecies[$j];
	}
	if ( $DEBUG && (! verifySchwartzSet( $numc, $tally, \@sset, $numWinners )) ) {
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

sub countDefeats($) {
	my $self = shift;
	my $numc = $self->{numc};
	my ($ii, $i, $j);
	my @defeats = ();
	my $counts = $self->{counts};
	for ( $i = 0; $i < $numc; $i++ ) {
		$defeats[$i] = 0;
	}
	for ( $i = 0; $i < $numc; $i++ ) {
		for ( $j = $i + 1; $j < $numc; $j++ ) {
#define xy( x, y ) ( (x > y) ? (it->counts[x][y]) : (it->counts[y][y+x]) )
			my ($ij, $ji);
			$ij = $counts->[$j]->[$j+$i];
			$ji = $counts->[$j]->[$i];
			if ( $ij > $ji ) {
				$defeats[$j]++;
			} elsif ( $ji > $ij ) {
				$defeats[$i]++;
			} else {
# tie policy?
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
	my ($i, $j);
	my $numc = $self->{numc};
	my @defeats = countDefeats( $self );
#	my @winners = ();
	my $names = $self->{names};
	my @inames = sort {$names->{$a} <=> $names->{$b}} keys %{$names};
	my @di = sort {$defeats[$a] <=> $defeats[$b]} (0 .. $#defeats);
	if ( $DEBUG ) {
		print "<p>Initial defeat counts</p>\n<table border=\"1\">\n";
	}
	for ( my $oi = 0; $oi <= $#di; $oi++ ) {
		$i = $di[$oi];
		if ( $DEBUG ) {
			print "<tr><td>$inames[$i]</td><td>$defeats[$i]</td></tr>\n";
		}
		if ( ! defined $defeats[$i] ) {
			print "why is defeats[$i] (\"$inames[$i]\") undefined ?\n";
		} elsif ( $defeats[$i] == 0 ) {
#			push @winners, $names[$i];
		}
	}
	if ( $DEBUG ) {
		print "</table>\n";
	}
	if ( $defeats[$di[0]] == 0 ) {
# there is one unique undefeated winner
		$self->{winners} = [map { ( $inames[$_], $defeats[$_] ) } @di];
		return $self->{winners};
	}
	my @tally = ();
	my $counts = $self->{counts};
	for ( $i = 0; $i < $numc; $i++ ) {
		for ( $j = 0; $j < $numc; $j++ ) {
			if ( $i == $j ) {
				$tally[($i*$numc) + $j] = undef;
			} elsif ( $i > $j ) {
				$tally[($i*$numc) + $j] = $counts->[$i]->[$j];
			} else {
				$tally[($i*$numc) + $j] = $counts->[$j]->[$j+$i];
			}
		}
	}
	my @ss = ();
	my $numWinners = getSchwartzSet( $self, \@tally, \@defeats, \@ss );
	@ss = sort @ss[0 .. ($numWinners-1)];
# lower indecies first makes nested for cleaner
	my $notdone = 1;
	my $notdonelimit = $numc + 1;
	my $defeatCountIncIndex;
	while ( $notdone ) {
		$notdone = 0;
		my $mind = 2000000000;
		my $mindj = -1;
		my $mindk = -1;
		my $tie = 0;
# find weakest defeat between members of schwartz set
		for ( my $ji = 0; $ji < $#ss; $ji++ ) {
			$j = $ss[$ji];
			for ( my $ki = $ji + 1; $ki <= $#ss; $ki++ ) {
				my $k = $ss[$ki];
				my $vk = $tally[($k*$numc)+$j];	# k beat j vk times // OR k prefered to j vk times
				my $vj = $tally[($j*$numc)+$k];	# j beat k vj times // OR j prefered to k vj times
				if ( $vj > $vk ) {
					if ( $winningVotes ) {
						if ( $vj < $mind ) {
							$mind = $vj;
							$mindj = $j;
							$mindk = $k;
							$tie = 1;
							$defeatCountIncIndex = $k;
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
							$defeatCountIncIndex = $k;
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
							$defeatCountIncIndex = $j;
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
							$defeatCountIncIndex = $j;
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
		$tally[($mindk*$numc) + $mindj] = 0;
		$tally[($mindj*$numc) + $mindk] = 0;
		$defeats[$defeatCountIncIndex]--;
		if ( $DEBUG ) {
			print "$mindk/$mindj = 0\n";
#			htmlTable( debugsb, numc, tally, "intermediate", null );
		}
		$numWinners = getSchwartzSet( $self, \@tally, \@defeats, \@ss );
		@ss = sort @ss[0 .. ($numWinners-1)];
		if ( $#ss == 0 ) {
			@di = sort {$defeats[$a] <=> $defeats[$b]} (0 .. $#defeats);
			if ( $DEBUG ) {
				print "<p>return B</p>\n";
			}
			$self->{winners} = [map { ( $inames[$_], -1 * $defeats[$_] ) } @di];
			return $self->{winners};
#			return [map { ( $names[$_], $defeats[$_] ) } @ss];
		}
		if ( $DEBUG ) {
			print "ss={ " . join( ", ", @ss ) . " }\n";
		}
		$notdone = 1;
		if ( ($notdonelimit--) <= 0 ) {
			if ( $DEBUG ) { print "hit limit!\n"; }
			@di = sort {$defeats[$a] <=> $defeats[$b]} (0 .. $#defeats);
			$self->{winners} = [map { ( $inames[$_], -1 * $defeats[$_] ) } @di];
			return $self->{winners};
#			return [map { ( $names[$_], $defeats[$_] ) } @ss];
		}
	}
	@di = sort {$defeats[$a] <=> $defeats[$b]} (0 .. $#defeats);
	if ( $DEBUG ) {
		print "<p>return at end</p>\n";
	}
	$self->{winners} = [map { ( $inames[$_], $defeats[$_] ) } @di];
	return $self->{winners};
#	return [map { ( $names[$_], $defeats[$_] ) } @ss];
}

sub hashToTable($$) {
	my $self = shift;
	my @names = @{shift()};
	my $nh = $self->{names};
	my $numc = $self->{numc};
	my ($i, $j);
	my @ni = map {$nh->{$_}} @names;
	my $counts = $self->{counts};
	my $toret = "<table border=\"1\"><tr><td></td>" . join("", map { "<td>($_)</td>" } (0 .. $#names) ) . "</tr>";
	for ( $i = 0; $i <= $#names; $i++ ) {
		my $name = $names[$i];
		my $ii = $ni[$i];
		$toret = $toret . "<tr><td>($i) $name</td>";
		for ( $j = 0; $j <= $#names; $j++ ) {
			my $nameb = $names[$j];
			my $ji = $ni[$j];
			my $x = undef;
			my $ox;
			if ( $i == $j ) {
#				$toret = $toret . "<td></td>";
			} elsif ( $ii > $ji ) {
				$x = $counts->[$ii]->[$ji];
				$ox = $counts->[$ii]->[$ii+$ji];
			} else {
				$x = $counts->[$ji]->[$ii+$ji];
				$ox = $counts->[$ji]->[$ii];
			}
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
		$toret = $toret . "</tr>";
	}
	$toret = $toret . "</table>";
	return $toret;
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
	my $toret = hashToTable( $self, \@names );
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
