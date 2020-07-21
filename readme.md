# Loopover 4×4 and 5×5 solver

## Overview

This is a solver for the 4×4 and 5×5 sizes of the [Loopover](https://loopover.xyz/) puzzle game. (Other implementations of the game include e.g. Simon Tatham's [Sixteen](https://www.chiark.greenend.org.uk/~sgtatham/puzzles/js/sixteen.html).)

## Usage

    loopsolver [options] board [board …]
    java -jar loopsolver.jar [options] board [board …]

`board`: The input board. May be given as a case-insensitive alphabetical string, e.g. `ABCDEFGHIJKLMNOP`, or as a comma-separated list of integers, e.g. `0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15`. The size will be automatically detected from the input string. With the integer input format, both 0-based indexing and 1-based indexing are supported.

`-v`: Print some status information. You can also specify `-v2`, `-v3`, etc. for extra verbosity, or `-v0` to reset. The lines in the output with the final solutions will be prefixed with an all-caps "SOLUTION:" if the verbosity level is positive. (Why does it print all the garbage to stdout instead of stderr? The short answer is that I have no idea what I'm doing.)

`-interactive`: Read options (must still be hyphen-prefixed) and input boards from standard input. (Use this if you want to feed input from a file; it doesn't literally check for interactive input.) You can type `exit` or `quit` to quit the program.

`-optimal`, `-twophase`: Use the optimal 5×5 solver or the two-phase 5×5 solver, respectively. The default is the two-phase solver. (The optimal solver is extremely slow and pretty much should not be used at all unless you suspect a state can be solved in under 14 moves or so.)

`-small`, `-big`: Force the usage of the small or big 5×5 phase 1 solver, respectively. The default is the big solver if the lookup table has already been generated, the small solver otherwise. The big solver generates a 128 MB lookup table on the first use, which takes around 30 seconds and is saved to disk in the same directory the program is run from. You may use filesystem compression to reduce the disk space usage once the table has been generated.

`-h`: Print a brief help message.

## Notation

The notation used here is based on [SiGN](https://mzrg.com/rubik/nota.shtml) and uses 1-based indexing.

"xUy" means to move the x-th row y spaces to the right; "xUy'" means to move the x-th row y spaces to the left.

"xLy" means to move the x-th column y spaces downwards; "xLy'" means to move the x-th column y spaces upwards.

## Solving algorithm

All solvers included in this program are optimised for the single-tile metric (STM), where moving a row/column by one cell countns as one move; all use the IDA* search algorithm, except that no priority queue is used for move ordering. (Using a priority queue increases per-node cost but does not seem to significantly decrease number of nodes searched.)

See `heuristics.md` for more information on the heuristic functions / pattern databases / pruning tables used.

## Speed, move count

4×4 solves take 170 ms per solve on average. The average move count is 13.76 moves.

5×5 two-phase solves take 0.83 s per solve with the small solver (default) and 0.38 s per solve with the big solver. The 90th percentile of solve times are 1.9 s and 0.8 s respectively; the 98th percentile of solve times are 3.6 s and 1.8 s respectively. Solves may, very rarely, take 10 seconds or longer (even with the big solver). The average move count is 31.5 moves.

(Your computer might be faster or slower than mine; ymmv.)

## Wow, this code looks like spaghetti. / Why is it written in Java? / etc.

Sorry.

## Copyright

MIT License

Copyright (c) 2020 torchlight

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

If, for whatever reason, you need a more formal version with my True Name on it, message me wherever you can find me. Almost all code in this project is original and written by me and only me, with the sole exception being the lexicographically-next-bit-permutation code which is from [Sean Eron Anderson's Bit Twiddling Hacks page](http://graphics.stanford.edu/~seander/bithacks.html) (public domain).