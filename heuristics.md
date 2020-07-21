# Loopover 4×4 and 5×5 solver – Heuristics

For the most part, some variation of the [X-Y](https://web.archive.org/web/20160112030001/https://heuristicswiki.wikispaces.com/X-Y) / [walking distance (WD)](http://www.ic-net.or.jp/home/takaken/nt/slide/solve15.html) heuristic, adapted to Loopover, is used by default. The 5×5 optimal solver and the big phase 1 solver are the main exceptions.

"tl;dr / I don't want to read about theory" version: Skip to the second half.

## Introduction

We first look at the 4×4 case. Consider the 16! possible board states to be permutations within the symmetric group <var>G</var> := Sym({A, B, C, …, P}).

At one extreme, we could in theory directly compute the distance within <var>G</var> (with respect to the generating set {(ABCD), (EFGH), (IJKL), (MNOP), (AEIM), (BFJN), (CGKO), (DHLP)}, assigning a weight of +1 to these eight moves and their inverses) via a breadth-first search, but this is not practically feasible since |<var>G</var>| = 16! = 2.1e13 is too large. (On smaller state spaces, e.g. 3×3 Loopover with 9!/2 = 181440 states, this is very doable.)

By reducing the board state to some quotient thereof, the size of the state space can be greatly decreased. To wit, given a distance (pseudo)metric <var>d</var> on <var>G</var>, we can define a new distance function <var>d</var>′ on <var>G</var>/~ by <var>d</var>′(\[<var>x</var>], \[<var>y</var>]) := min {<var>d</var>(<var>x</var>′, <var>y</var>′) : <var>x</var>′ ~ <var>x</var> and <var>y</var>′ ~ <var>y</var>}. This is actually also difficult to compute directly even if <var>G</var>/~ is small, because it requires knowing <var>d</var> already.

It can also fail to be a metric in pathological cases, and that's why [the actual definition of a quotient metric](https://en.wikipedia.org/wiki/Metric_space#Quotient_metric_spaces) is more intricate. It turns out that, despite the more involved definition, the quotient metric is actually easier to compute!

Construct a graph on <var>G</var>/~ with an edge between \[<var>x</var>] and \[<var>y</var>] whenever there is some <var>x</var>′ ~ <var>x</var> and <var>y</var>′ ~ <var>y</var> such that <var>x</var>′ and <var>y</var>′ are one move apart, with weight <var>d</var>′(\[<var>x</var>], \[<var>y</var>]) assigned to the edge. Do a breadth-first search on this graph starting from the equivalence classes of the solved state(s), and we get some new distance function that provides a lower bound on <var>d</var>′. This can then be used as an admissible heuristic function for IDA*.

By weighting the moves differently, rather than using a uniform +1 weight to all sixteen moves, we can provide a lower bound on the number of moves of a certain type needed to solve the input state. For example, by weighting the row moves (ABCD), (EFGH), (IJKL), (MNOP) with +1 and the column moves (AEIM), (BFJN), (CGKO), (DHLP) with 0, the distance function gives a lower bound on the number of row moves needed. Weighting the column moves with +1 and the row moves with 0 instead leads to a lower bound on the number of column moves needed. Since no move is simultaneously a column move and a row move, these may be added to give a better lower bound than either axis alone.

Given admissible heuristic functions <var>h</var> and <var>h</var>′, without any further information, the best admissible heuristic function we can get by combining these is just max(<var>h</var>, <var>h</var>′), which often is not much better than just <var>h</var> or <var>h</var>′ alone until the search is near a solution. The ability to *add*, as opposed to merely taking the maximum, can make additive heuristics prune more effectively for the same memory cost; for example, this is the case with 15-puzzle / 24-puzzle and Korf's additive disjoint pattern databases.

On the flip side, additive heuristics can significantly underestimate distance if poorly chosen. Suppose we instead partition the move set into {(ABCD), (EFGH), (AEIM), (BFJN), inverses thereof} and {(IJKL), (MNOP), (CGKO), (DHLP), inverses thereof}. Any permutation in Sym({C, D, G, H, I, J, M, N}) can be solved with zero moves in the former set, and also with zero moves in the latter set. For these permutations, the additive heuristic would give a value of 0 + 0 = 0: it cannot tell that there is no solution that uses zero moves in both sets *simultaneously* (unless the input is already solved).

The standard breadth-first search algorithm requires the weights to all be identical and cannot accommodate zero-weight edges. To that end, we can extend the equivalence relation so that <var>x</var> ~ <var>y</var> whenever there is a zero-weight move between those states. For certain types of equivalence relations, we can get this for free, without any additional computation. Which brings us to the next section.

## Coset spaces and double coset spaces

While a left-action convention is common in abstract algebra texts, for our purposes the right-action convention is more convenient: given a board state <var>g</var> and a move <var>m</var>, applying the move to the board state results in the state <var>g</var>∘<var>m</var>. This is the convention we will be using throughout. (The composition operator will be omitted where unambiguous.)

For any group <var>G</var> and subgroups <var>H</var>, <var>K</var> ≤ <var>G</var> thereof, we have the left coset space <var>G</var>/<var>K</var> := {<var>g</var><var>K</var> : <var>g</var> ∈ <var>G</var>}, the right coset space <var>H</var>\<var>G</var> := {<var>H</var><var>g</var> : <var>g</var> ∈ <var>G</var>}, as well as the double coset space <var>H</var>\<var>G</var>/<var>K</var> := {<var>H</var><var>g</var><var>K</var> : <var>g</var> ∈ <var>G</var>}.

These all provide partitions of <var>G</var> into disjoint subsets. Note that left coset spaces and right coset spaces are just special cases of double coset spaces, where one of the subgroups is chosen to be the trivial subgroup; as such, we will focus on double coset spaces.

Suppose we want to assign zero weight to the moves <var>m</var><sub>1</sub>, …, <var>m</var><sub><var>n</sub> and +1 weight to the remaining moves. As long as we choose <var>K</var> to be a supergroup of ⟨<var>m</var><sub>1</sub>, …, <var>m</var><sub><var>n</var></sub>⟩, applying a zero-weight move <var>m</var><sub><var>i</var></sub> to any state <var>g</var> will not change the double coset it's in, since <var>H</var>(<var>g</var><var>m</var><sub><var>i</var></sub>)<var>K</var> = <var>H</var><var>g</var>(<var>m</var><sub><var>i</var></sub><var>K</var>) = <var>H</var><var>g</var><var>K</var>. In a sense, the <var>K</var> subgroup absorbs all zero-weight moves.

Choosing larger subgroups for <var>H</var> and <var>K</var> reduces the amount of information conveyed by the double coset, leading to less pruning in IDA*, but also reduces the amount of memory needed to store a distance function. Conversely, choosing smaller subgroups leads to more effective pruning, at the cost of memory usage. Furthermore, some choices of <var>H</var>, <var>K</var> may make indexing the double cosets easy, some may make indexing difficult.

A "standard" choice is to take <var>K</var> to be trivial and <var>H</var> to be a symmetric group over some subset of pieces, or more generally, a (disjoint) product of symmetric groups over disjoint subsets of pieces. For instance, suppose we take <var>H</var> to be Sym({A, B, C, D, E, F, G, H}) Sym({I, J, K, L, M, N, O, P}). Any relabelling within ABCDEFGH will not change the right coset, and likewise for relabelling within IJKLMNOP, so any right coset can be represented as a 16-bit integer with a 0 for the ABCDEFGH pieces and a 1 for the IJKLMNOP pieces. Furthermore, there is a quick(-ish) algorithm, making use of the fact that this 16-bit integer has eight 0 bits and eight 1 bits, to further compress the representation into an integer 0..12869, which is the best possible since 16! / 8!^2 = 12870 is the size of the right coset space. This is an example where indexing the cosets is relatively easy.

(Why do we want indexing the cosets to be easy? This is to avoid having to use a hash table, which might be computationally expensive. Or, from a different perspective, we're making use of our knowledge of what the data is like to devise a fast [perfect hash function](https://en.wikipedia.org/wiki/Perfect_hash_function) that is minimal, or failing that, "not too far" from minimal.)

If <var>H</var> is the product of symmetric groups over a partition of the pieces into three or more subsets, it gets more complicated, but still doable fast-ish. However, taking <var>H</var> to be a subgroup not of that form or taking <var>K</var> to be a nontrivial subgroup makes it *much* more complicated, and there probably isn't a nice algorithm for this.

There is a natural bijection between the double coset space <var>H</var>\<var>G</var>/<var>K</var> and its opposite <var>K</var>\<var>G</var>/<var>H</var>, but computationally, the left and right subgroups serve different purposes. The left subgroup <var>H</var> is used to ensure a compact representation of the input state, while the right subgroup <var>K</var> is used to naturally mod out moves.

In general, we should choose the left subgroup <var>H</var> to contain the right subgroup <var>K</var>: provided that <var>H</var> and <var>K</var> commute, the distance never decreases (i.e. pruning effectiveness remains the same) by replacing the left subgroup <var>H</var> with ⟨<var>H</var>, <var>K</var>⟩ = <var>H</var><var>K</var>, while the number of double cosets does, reducing memory usage. Let <var>d</var><sub>1</sub> be the quotient metric on <var>H</var>\<var>G</var>/<var>K</var> and <var>d</var><sub>2</sub> be the quotient metric on (<var>H</var><var>K</var>)\<var>G</var>/<var>K</var>. Fix some <var>g</var> ∈ <var>G</var> and let <var>a</var> := <var>d</var><sub>2</sub>(id, <var>g</var>). We have <var>g</var> = (<var>h</var><var>k</var>)<var>k</var><sub>0</sub><var>m</var><sub>1</sub><var>k</var><sub>1</sub><var>m</var><sub>1</sub><var>k</var><sub>1</sub>…<var>m</var><sub><var>b</var></sub><var>k</var><sub><var>b</var></sub> for some moves <var>m</var><sub><var>i</var></sub> with total weight <var>a</var>, some <var>k</var><sub><var>i</var></sub> ∈ <var>K</var>, and some (<var>h</var><var>k</var>) ∈ <var>H</var><var>K</var>. We can then use the path (<var>k</var><var>k</var><sub>0</sub>)<var>m</var><sub>1</sub><var>k</var><sub>1</sub><var>m</var><sub>1</sub><var>k</var><sub>1</sub>…<var>m</var><sub><var>b</var></sub><var>k</var><sub><var>b</var></sub> to get from <var>h</var> (which shares the same double coset as the identity permutation) to <var>g</var>, with the same total weight of <var>a</var>, so <var>d</var><sub>1</sub>(id, <var>g</var>) ≤ <var>a</var> = <var>d</var><sub>2</sub>(id, <var>g</var>) ≤ <var>d</var><sub>1</sub>(id, <var>g</var>) and hence <var>d</var><sub>1</sub> = <var>d</var><sub>2</sub>.

All else equal, choosing left and right subgroups that do not commute (so the above proof does not apply) is still a bad idea because this increases the size of <var>H</var><var>K</var>, the double coset that contains the solved state, which lowers the average distance and hence pruning effectiveness.

## Walking distance heuristic for 15-puzzle, formulated in terms of double cosets

The walking distance (WD) heuristic is an additive heuristic function originally devised for the 15-puzzle, computing separate lower bounds for the number of horizontal moves and the number of vertical moves first, then adding them to get the final heuristic value. As the algorithms for the horizontal bound and the vertical bound are essentially identical, we will be considering only the vertical bound.

A 15-puzzle state can be treated as a permutation of the fifteen pieces 1, 2, 3, …, 15, as well as a blank space (which we will call "16"); the 16!/2 legal states of the puzzle can be treated as a subset of S<sub>16</sub>. This is *not* a subgroup, as it is not closed under composition or inverse, but we can nevertheless still send an element to the double coset that contains it.

(There is a different formulation of a 15-puzzle state that truly has a group structure, as an index-2 subgroup of S<sub>15</sub> × (ℤ/4) × (ℤ/4), where we fix the location of the blank cell and move everything else around it. Since this is not a document focusing on the 15-puzzle, we will not be going deeper into this, but do consider the similarity with the no-regrips (NRG) variant of Loopover.)

The left subgroup used here is Sym({1, 2, 3, 4}) Sym({5, 6, 7, 8}) Sym({9, 10, 11, 12}) Sym({13, 14, 15}), which amounts to forgetting which column a piece belongs to, and tracking only which row. The only exception is the blank space, which is still treated as distinct from the other fourth-row pieces. Instead of treating the board as having 15 distinct pieces and a blank space, there are only 4 distinct pieces and a blank space now.

<pre>A A A A
B B B B
C C C C
D D D _</pre>

Since we want to assign zero weight to all horizontal moves, we may take the right subgroup to be generated by those permutations, i.e. ⟨(1,2), (2,3), (3,4), (5,6), (6,7), (7,8), (9,10), (10,11), (11,12), (13,14), (14,15), (15,16)⟩. (For any given 15-puzzle state, only at most two of these twelve generating elements will correspond to legal moves. However, it does not affect correctness that we are modding out by illegal moves as well.) This subgroup works out to be Sym({1, 2, 3, 4}) Sym({5, 6, 7, 8}) Sym({9, 10, 11, 12}) Sym({13, 14, 15, 16}). In effect, we now disregard the order of pieces within each row and treat it as a multiset.

There are two reasonable ways of representing a row's contents: as a 4-tuple of the number of A pieces, the number of B pieces, etc. (so the total sum is 3 or 4), or by sorting it. Either way, if the row includes the blank space, then there are 20 possibilities, and if it doesn't, then there are 34 possibilities. These numbers may be obtained via the [stars-and-bars method](https://en.wikipedia.org/wiki/Stars_and_bars_(combinatorics\)), with the only catch being that a row with no blank space and 4 D pieces is impossible. ([This blog post by 陳柏叡](https://cbdcoding.blogspot.com/2015/02/15-puzzle-walking-distance.html) lists 35 possibilities due to not accounting for the impossible four-Ds case.) Anyhow, with a total of 54 cases per row, and the last row being fully determined by the first three, we may represent a double coset as a three-digit base-54 number (equivalently, an integer in 0..54<sup>3</sup>−1) and use a table of size 54<sup>3</sup> = 157464 to store the distances while doing a breadth-first search.

In reality, only around 25000 of those values correspond to legitimate double cosets and the rest are "illegal" values (e.g. with more than 4 A pieces), but some storage inefficiency is hard to avoid with the walking distance heuristic. This can be tightened (e.g. coding which row the blank is in first, then two rows without the space and one row with the space reduces the number of possibilities to 4×(34×34×20) = 92480), but 157464 is already small enough.

## Heuristic functions used in this program

### 4×4 optimal solver: WD heuristic

This is used in the optimal 4×4 solver (which is also the only 4×4 solver included).

As above, the left subgroup is chosen to forget which column a piece belongs to, and this time there is no distinguished blank space, so we choose the left subgroup to be <var>H</var> := Sym({A, B, C, D}) Sym({E, F, G, H}) Sym({I, J, K, L}) Sym({M, N, O, P}). We can choose the right subgroup to be any supergroup of ⟨(ABCD), (EFGH), (IJKL), (MNOP)⟩; an easy choice would be to use <var>H</var> as the right subgroup as well.

As in the previous section, there are 35 possibilities for each row, so we may use a table of size 35<sup>3</sup> = 42875. The actual number of WD patterns is 10147. (This is lower than the corresponding number for 15-puzzle as we do not have a distinguished blank space.)

Average pruning values (for random states): 4.647 per axis; 9.294 for both axes.

distance|double cosets|right cosets
--------|-------------|------------
0       |1            |1
1       |2            |512
2       |46           |72544
3       |540          |3322528
4       |2781         |24128154
5       |4350         |27447240
6       |1886         |7456910
7       |492          |625312
8       |49           |9799

The implementation in BasicSolver.java is very computationally inefficient (it was originally written for a different and much larger pattern database, with WD added after the fact), but it gets the job done. It is likely possible to increase speed by a factor of 1.5 or more with a rewrite.

As we're using the same subgroup on both the left and the right, this WD heuristic is invariant under taking the inverse of a state. This is not an inherent requirement; if we had chosen ⟨(ABCD), (EFGH), (IJKL), (MNOP)⟩ to be the right subgroup instead (the smallest possible choice), the resulting WD variant would not be inverse-invariant.

### 5×5 two-phase solver

<pre>A B C d e
F G H i j
K L M n o
p q r s t
u v w x y</pre>

The first phase solves a 3×3 block of pieces (ABC, FGH, KLM), leaving two rows and two columns free to move. The second phase solves the remaining 16 pieces using only the two free rows and two free columns left from phase 1.

There are three solvers using three different pattern databases for phase 1, but one of the three is too slow to be practical and is only of theoretical interest.

For simplicity, even though the legal states all have even permutation (and hence lie in the alternating group), we will take double cosets within the symmetric group instead. Consequently, some members of the double cosets will not correspond to legal states.

#### Big phase 1 solver: six-piece PDB heuristic

Two basic heuristic functions are used. The first is with Sym({all pieces but ABC FGH}) as the left subgroup and a trivial right subgroup; the second is with Sym({all pieces but FGH KLM}) as the left subgroup and a trivial right subgroup. As these coset spaces have an (easily-computed) isometry between them, we need only perform a breadth-first search once. (Unlike WD, these heuristic functions are not additive; we take the maximum of the two heuristic functions rather than adding them.)

This PDB has 25! / 19! = 127512000 entries, and while it has the highest average pruning, it also uses the most memory and initialisation time by far. (Memory usage can be reduced to 1.6 bits/entry with the modulo-3 trick, but this doesn't fix initialisation and also significantly slows down access.)

Average pruning value (on random states): 9.850 for each of the 2×3 blocks; 10.336 for both.

distance|cosets  |samples
--------|--------|-------
0       |1       |0
1       |10      |0
2       |94      |0
3       |880     |1
4       |7558    |29
5       |58432   |573
6       |402862  |11608
7       |2335840 |188016
8       |10374813|2114962
9       |30419907|13706973
10      |47958515|39280516
11      |30962779|37351023
12      |4936603 |7262257
13      |53706   |84042

#### Small phase 1 solver: WD heuristic

Unlike the WD heuristics above, here, we do want to distinguish between some pieces of the same row.

Left subgroup: Sym({A, B, C}) Sym({F, G, H}) Sym({K, L, M}) Sym({the other sixteen pieces}).

Right subgroup: Sym({A,B,C,D,E}) Sym({F,G,H,I,J}) … Sym({U,V,W,X,Y}).

The left subgroup is equivalent to classifying the pieces by whether they're needed for the first row of the 3×3 block, the second row, the third row, or whether they're completely unneeded for the block (regardless of which row they should be in in the solved state). Thus we may represent a board state with a 25-digit base-4 number, which fits in a 64-bit int; label the ABC pieces with '0', the DEF pieces with '1', the KLM pieces with '2', and the remaining with '3'.

For each row, there are 56 ways of assigning a 5-element multiset containing only '0', '1', '2' and '3', but in 12 of those ways, there are four or five of the '0', '1' or '2' pieces, which is impossible, leading to only 44 valid row patterns. Taking two rows together, only 832 of the 44<sup>2</sup> combinations are legal (for the same reason); taking two blocks of two rows together, only 40600 of the 832<sup>2</sup> combinations are legal (we now have an additional constraint that there are at most sixteen of '3' pieces).

This is not inverse-invariant as the left and right subgroups differ, but we cannot exploit this in any way since it's generally not true that a phase 1 solution to the inverse state leads to a phase 1 solution on the normal state.

Average pruning value (on random states): 4.581 per axis; 9.162 for both axes. The following tables are the one-axis stats and the two-axis stats, respectively, sampled from 10<sup>8</sup> scrambles.

distance|double cosets|samples
--------|-------------|-------
0       |1            |57
1       |14           |9988
2       |211          |584865
3       |2749         |13700647
4       |11376        |78610736
5       |15627        |84504598
6       |8986         |21206602
7       |1360         |1315439
8       |252          |66082
9       |24           |986

distance|samples
--------|-------
3       |31
4       |1674
5       |44513
6       |702409
7       |5608574
8       |21203333
9       |34636308
10      |26505944
11      |9574129
12      |1602136
13      |118244
14      |2692
15      |13

While we can choose a smaller subgroup for the right subgroup, this greatly increases table size and initialisation time, without significantly improving pruning. For example, taking the right subgroup to be five copies of the dihedral group ⟨(ABCDE), (AE)(BD), …, (UVWXY), (UY)(VX)⟩, the average pruning value increases to 4.659 per axis, 9.318 for both axes, with the following distributions.

distance|double cosets|samples
--------|-------------|-------
0       |1            |6
1       |52           |2685
2       |1786         |314438
3       |29800        |10419706
4       |163875       |73596988
5       |209528       |90279577
6       |81016        |23698595
7       |9644         |1620763
8       |864          |66267
9       |72           |975

distance|samples
--------|-------
2       |1
3       |9
4       |506
5       |19313
6       |396745
7       |3946649
8       |18095520
9       |34381588
10      |29596536
11      |11426151
12      |1988505
13      |145512
14      |2957
15      |8

This is included in Phase1EWD.java, but is not used in the solver at all as the initialisation time is comparable to the six-piece PDB but the average pruning value is much lower.

(There aren't many other groups to choose from; between the cyclic group ⟨(ABCDE)⟩ (order 5) and the symmetric group Sym({A,B,C,D,E}) (order 120), [the only other intermediate groups](https://groupprops.subwiki.org/wiki/Subgroup_structure_of_symmetric_group:S5) are the dihedral group ⟨(ABCDE), (AE)(BD)⟩ (order 10), the affine group ⟨(ABCDE), (BCED)⟩ (order 20), and the alternating group Alt({A,B,C,D,E}) (order 60). Modding out by the alternating group will actually lead to the same double cosets since there will always be at least two tiles of the same label within each row (five tiles, four possible labels, pigeonhole principle), so that doesn't bring anything new. The affine group has the dihedral group as a subgroup of index 2, and since the dihedral group already doesn't increase pruning much, it's useless too. That leaves only the cyclic group, but the initialisation time is guaranteed to suck.)

#### Tiny phase 1 solver: EMD heuristic

This uses a minor enhancement of the Manhattan distance (MD) heuristic; an enhanced Manhattan distance (EMD) heuristic. This is an additive heuristic (counts horizontal moves and vertical moves separately; we focus on the latter), but not of the double coset kind.

Of the nine tiles to solve (ABC FGH KLM), we count the number of tiles that are in the correct row, the number that need to move one row up, the number that need to move two rows up, the number that need to move two rows down, and the number that need to move one row down; let these five numbers be <var>a</var><sub>0</sub>, …, <var>a</var><sub>4</sub> respectively. (The subscript should be understood modulo 5.)

For the vanilla version of the MD heuristic, since each move increases/decreases the distance of five tiles by at most 1, we may take the sum of distances <var>a</var><sub>1</sub> + <var>a</var><sub>−1</sub> + 2<var>a</var><sub>2</sub> + 2<var>a</var><sub>−2</sub> and divide by 5. This leads to an average pruning value of ~2.6 per axis, which is much too low to be useful.

With a more careful analysis, within phase 1, it's impossible for the sum of distances to change by more than 4. Without loss of generality, suppose we move the first column down and suppose for a contradiction that the sum of distances increases by 5. This means that each of the five pieces in the first column all move further from their correct row. There cannot be any '3'-labelled pieces in the first column, since any row is already correct for them. The piece in the fifth row cannot be labelled '0' or '1', since a down move would reduce its distance, but it also cannot be labelled '2', since a down move would leave its distance unchanged (still two tiles away, but now in the opposite direction). Contradiction; therefore the sum of distances must change by at most 4. Taking this into account, we can divide by 4 instead, which increases the per-axis pruning value to ~3.1.

This analysis is still a bit ad hoc, and misses one key weakness of MD when applied to Loopover. Suppose that we have <var>a</var><sub>0</sub> = 7, <var>a</var><sub>1</sub> = <var>a</var><sub>−1</sub> = 1 and <var>a</var><sub>2</sub> = <var>a</var><sub>−2</sub> = 0. An example of such a board would be the following, with the C and H pieces in each other's rows.

<pre>A B . H .
F G . C .
K L M . .
. . . . .
. . . . .</pre>

This has a sum of distance of <var>a</var><sub>1</sub> + <var>a</var><sub>−1</sub> + 2<var>a</var><sub>2</sub> + 2<var>a</var><sub>−2</sub> = 2, leading to a pruning value of only 1. However, it is clear that at least two moves are needed: with one move, it's possible to fix either the piece with a +1 displacement or the piece with a −1 displacement, but not both simultaneously.

The EMD heuristic is obtained by mapping the states to the set of possible displacement 5-tuples (<var>a</var><sub>0</sub>, …, <var>a</var><sub>4</sub>) (there are (9+4)!/(9!4!) = 715 of these) and computing the quotient metric.

Average pruning value (on random states): 3.647 per axis; 7.294 for both axes.

See the following tables for the one-axis and two-axis statistics.

distance|5-tuples|samples
--------|--------|-------
0       |1       |4
1       |10      |2401
2       |74      |179124
3       |304     |3267551
4       |297     |6454576
5       |29      |96344

distance|samples
--------|-------
2       |1
3       |88
4       |4929
5       |121467
6       |1297809
7       |4210039
8       |4241535
9       |123187
10      |945

This is on the edge of being usable, but given that WD has more information than EMD (the states in a double coset have the same displacement 5-tuple, so the EMD state space is a quotient of the WD state space) and WD is already fast enough to initialise with a low memory footprint, there is no reason to actually use this.

#### Phase 2: WD heuristic

Left subgroup: Sym({D,E}) Sym({I,J}) Sym({N,O}) Sym({P,Q,R,S,T}) Sym({U,V,W,X,Y}).

Right subgroup: ⟨(DE)(IJ)(NO)⟩ Sym({P,Q,R,S,T}) Sym({U,V,W,X,Y}).

More concretely, we can think of the board state as being represented by:

<pre>. . . 0 0
. . . 1 1
. . . 2 2
3 3 3 3 3
4 4 4 4 4</pre>

with the last two rows treated as multisets rather than ordered tuples. The first three rows are still ordered; we are *not* modding out by Sym({D,E}) etc. on the right side. This PDB has a total of 121854 entries, with a weighted average of 6.189 moves. (If we exclude the ⟨(DE)(IJ)(NO)⟩ term in the right subgroup, that does not affect the distance, but increases the number of entries to 242148.)

As the left and right subgroups are different, taking inverses is not a natural isometry, so we can make use of the WD heuristic on the inverse state to increase the pruning value. Unlike in the phase 1 WD solver, we *can* take inverses here because the inverse of a solution on the inverse state is a solution to the normal state, with exactly the same numbers of horizontal and vertical moves.

(Note that WD on the inverse does not behave like a metric! There is no *a priori* guarantee that it is [consistent](https://en.wikipedia.org/wiki/Consistent_heuristic). This is because applying a move on the normal state can result in an arbitrary 5-cycle on the inverse state (e.g. moving the bottom row on normal corresponds to cycling the U, V, W, X and Y pieces on inverse, wherever they are), so from the point of view of the inverse state, it looks like multiple moves have been made. As the solver does not make require heuristics to be consistent, this is fine.)

Another heuristic function used to augment WD is the "wrong column" heuristic. The WD heuristic cannot distinguish between e.g. the D piece being in the E location or the E piece actually being solved. Since we do not have a move that can directly move the D piece to the E location (moving the first row would break up the ABCFGHKLM block solved in phase 1), we need to use at least one vertical move to bring the D piece to a free row, and one more to bring it to the correct location. This gives a lower bound of 2. This reasoning similarly gives a lower bound of 2 moves for one of N, O in the correct row and wrong column, 4 moves for one of I, J pieces in the correct row and wrong column. Since a lower bound of 2 moves is almost never useful, in the solver itself, we check only for the I, J pieces being in the wrong column.

Average pruning value (on random phase 2 states):  
WD: 6.189 per axis; 12.378 for both axes.  
WD, wrong-column: 6.189 per axis; 12.379 for both axes.  
WD, WD on inverse, wrong-column: 6.371 per axes; 12.741 for both axes.

The following table has the one-axis statistics for WD, (WD & wrong-column), and (WD & WD on inverse & wrong-column). The samples here are from three independent sets of 10<sup>8</sup> scrambles.

distance|WD double cosets|samples (WD)|samples (WD, wc)|samples (WD, WDinv, wc)
--------|----------------|------------|----------------|-----------------------
0       |1               |0           |0               |0
1       |2               |59          |29              |5
2       |27              |2247        |1724            |798
3       |244             |64760       |51052           |16129
4       |2274            |1259684     |1274594         |503999
5       |15093           |12588202    |12585672        |6959819
6       |51609           |52593742    |52590664        |48627185
7       |46646           |32749753    |32754692        |42711190
8       |5927            |741369      |741394          |1180540
9       |31              |184         |179             |335

The combination of three heuristics is used in the solver as the increased pruning offsets the per-node computational cost.

### 5×5 optimal solver: EMD heuristic

As with the tiny phase 1 solver, this uses the enhanced Manhattan distance (EMD) heuristic.

This EMD heuristic has only a very slightly increased pruning distance compared to the basic MD heuristic (6.426 versus 6.4), but it also requires no extra computational cost outside of initialisation. The inequality MD ≤ EMD ≤ WD always holds, and all three of these heuristic functions are invariant under taking the inverse of the state.

An unoptimised WD heuristic implementation is in the code as well, but commented out. The average pruning distance is 7.182 moves per axis, so it searches considerably less nodes, but evaluating the heuristic is also a lot more expensive due to bad design decisions.

The following table lists the distributions for EMD and WD heuristic values. The second and fourth columns are the raw, unweighted number of cases (which are not equiprobable); the third column is from sampling EMD from 10<sup>8</sup> scrambles; the fifth column is the exact distribution of WD (by decomposing double cosets into right cosets, which are all of the same size 5!<sup>5</sup>/2).

distance|EMD 5-tuples|EMD samples|WD double cosets|WD right cosets
--------|------------|-----------|----------------|---------------
0       |1           |0          |1               |1
1       |2           |0          |2               |6250
2       |17          |7          |95              |7899500
3       |101         |2662       |2813            |3176742500
4       |385         |503231     |57094           |475812551875
5       |715         |10467480   |588190          |16484139578002
6       |1069        |42636133   |3087603         |123596043881010
7       |1083        |38886264   |6698940         |259083565322770
8       |804         |7297714    |6922292         |178907603632010
9       |440         |206104     |3807495         |41980242831875
10      |130         |405        |904406          |2830149890557
11      |4           |0          |320             |788770

The pruning is too ineffective to be used standalone on fully random scrambles. It is worth noting, however, that while God's number was previously already known to be at least 22 moves via a nonconstructive argument (and more precisely, that over half of the states require at least 22 moves), EMD provided the first explicit examples of 5×5 states that provably require at least 22 moves to solve.