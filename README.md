# A WITCH computer simulator

The Harwell Dekatron computer, aka the WITCH computer is at the UK National Computer Museum.
More information about it can be found at http://www.computerconservationsociety.org/witch.htm.

In developing this simulator, I have been particularly influenced by the users manual, the table
of order codes, the Electronic Engineering article describing the arithmetic units, and the
example programs.  All of these resources can be found liked from the CCS site linked above.

I also corresponded with Delwyn Holroyd, and would not have been able to figure out the
details of some of the design without his helpful replies.

I started this as a Clojure programming exercise.  I chose the WITCH machine specifically
because my parents learned to program this machine while studying at Wolverhampton.


## Usage

Install the [Clojure CLI](https://clojure.org/guides/install_clojure), then run the code
using the following command line from the root of the repository.

    $ clojure -M:run <tape-file>


## Command line options

    -s  :  Single step; press Return to execute one order, or q to quit
    -t  :  Traces the execution of the system

## Assembler

The assembler translates a symbolic source file into an ordinary numerical tape file.
The simulator reads only the generated tape; it does not interpret symbolic source.

    $ clojure -M:assemble examples/demo-1.witch /tmp/demo-1.tape
    $ clojure -M:run /tmp/demo-1.tape

It can also produce an annotated visual representation of the WITCH's five-hole
punched tape. Use \`--punch\` by itself, or together with a numerical output:

    $ clojure -M:assemble examples/demo-1.witch --punch /tmp/demo-1.punch
    $ clojure -M:assemble examples/demo-1.witch /tmp/demo-1.tape --punch /tmp/demo-1.punch

Leave off the punched output filename to write the visualization to standard output:

    $ clojure -M:assemble examples/demo-1.witch --punch

The punched representation shows the five data positions in \`E D C B A\` order,
with the smaller feed hole between C and B. \`●\` is a punched hole, \`·\` is an
unpunched position, and \`o\` is the feed hole. It is a visual output format only;
the simulator continues to read numerical tape files.

Symbolic files retain the tape format's `==tape`, `#n`, comments, signed data, and raw
five-digit orders. Address aliases can be declared anywhere and may be used before their
declaration:

    X = 10
    RESULT = 20

    ==tape
    #1
    ADD X RESULT
    PRINT-LAYOUT-2
    ADD RESULT 01
    FINISH

The canonical orders follow the historical order table: `ADD`, `ADD-AND-CLEAR`,
`SUBTRACT`, `SUBTRACT-AND-CLEAR`, `MULTIPLY`, `DIVIDE`,
`TRANSFER-POSITIVE-MODULUS`, `TEST-POSITIVE`, `TEST-NEGATIVE`,
`TRANSFER-CONTROL`, `TRANSFER-CONTROL-IF`, `SEARCH`, `SEARCH-IF`,
`PRINT-LAYOUT-1`, `PRINT-LAYOUT-2`, and `FINISH`.

For convenience, `CLEAR` is a pseudo-order. `ADD-CLEAR`, `SUBTRACT-CLEAR`,
`TRANSFER-MODULUS`, `JUMP`, `JUMP-IF`, and `STOP` are accepted aliases.
The simulator's additional controls are exposed as `SIGNAL`, `PRINT-LAYOUT n`, and
`SHIFT n`; shift amounts are written directly, from 1 through -7.

Symbolic versions of every bundled program are available in `examples/`. The directory
also contains `demo-1.tape` as an example of the generated numerical tape format. Other
numerical tapes should be treated as build outputs and generated with the assembler.

## Examples

    $ clojure -M:run examples/demo-1.tape

    $ clojure -M:assemble examples/demo-2.witch /tmp/demo-2.tape
    $ clojure -M:run /tmp/demo-2.tape

    $ clojure -M:assemble examples/exercise-1.witch /tmp/exercise-1.tape
    $ clojure -M:run -t /tmp/exercise-1.tape

    $ clojure -M:run -s examples/demo-1.tape

## Tape file format

The tape file format tries to be similar to the examples given on the WITCH computer
website.  Several tapes are described in each file.  Blank lines are ignored, as are
lines which start ';'.  A line can contain one of:

| Syntax    | Meaning                                                  | Example  |
|-----------|----------------------------------------------------------|----------|
| ==tape    | The beginning of the next tape                           | ==tape   |
| #n        | A block marker                                           | #1       |
| NNNNN     | A five digit positive number, typically used for orders  | 21020    |
| +NNNNNNNN | An eight digit positive number. '+' is mandatory         | +1234567 |
| -NNNNNNNN | An eight digit negative number. '-' is mandatory         | -1234567 |

The first non-comment line must be a '==tape' marker.  The contents of tape 1 follows.
When another '==tape' marker is seen, then tape 1 is finished, and tape 2 follows the
marker.  As in the real machine, tape 1 should contain a block 1 marker, because the
machine will search for it an execute from that point on booting.

For example:
```
; This is an example

==tape

#1

; Search for block 2 on tape 2 and jump to it
03202
02102

==tape
#2

; Stop the machine
00100
```

There is an implicit decimal point after the first digit of any number.

See the files in the \`examples\` directory for complete programs.

## Running the unit tests

Unit tests can be run with the Clojure CLI.

    $ clojure -M:test

## Design

The code is broken into the following namespaces:

* core.clj - Implements the main function.  It initialises the state, executes the
  fixed search and transfer instructions, and enters the main loop.
* machine.clj - Implements functions the simulate the hardware of the machine. Input,
  output, register access, transfer unit, and machine state are all in here.
* decode.clj - Implements instruction decode.
* nines.clj - Implements some nines complement utility routines.

The machine execution loop (decode/run) calls the step function until the machine state
indicates that the machine has stopped.  The step function (decode/step) fetches an
instruction from the current PC and dispatches it to the decoding function.  The decoding
functions take the machine state and produce a new machine state with the result of the
instruction.

Because a read can sometimes be a state-modifying operation (reading a tape is the most
obvious one), the read functions return a modified machine state, with the value read in
an input holding place (:sending-value).  This partially mirrors the real machine. The
transfer unit takes this value and transforms it according to the 'complement' and
'shift' states, as I believe the real machine does.  Finally, the store/accumulator outputs
will perform the summation as in the real machine.

I believe that in the real machine, clear is an operation whereby the complementary pulse
chain is not fed back into the sending dekatrons as their value is read out.  I do not simulate
this.  The sending dekatrons effectively keep their value unless they are explicitly cleared.

## Example programs

The symbolic programs in `examples/` include the original demonstrations and exercises,
factorial and Fibonacci sequences, bubble sort, indirect addressing, Pascal's triangle,
a sine series, Newton square-root approximation, Euclidean GCD, Horner polynomial
evaluation, a Leibniz approximation of pi, a simple counter, and recursive
factorial using a two-word software stack.

## Outstanding questions

* Is there a separate transfer unit for the accumulator, or a single one for both
  the accumulator and the stores?
* If there is 1 transfer unit, how do digits 8-14 become set when there is a store
  as the sending value?  For accuracy they should be filled with the sign digit.
* When the accumulator is the sending address, are its digits 8-14 applied to the
  transfer unit inputs?  This is important when the shift value is set to +1, where
  you could get digit 8 affecting the destination store.

## License

Copyright © 2016 David Norman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
