# A WITCH computer simulator

The Harwell Dekatron computer, aka the WITCH computer is at the UK National Computer Museum.
More information about it can be found at http://www.computerconservationsociety.org/witch.htm.

In developing this simulator, I have been particularly influenced by the users manual, the table
of order codes, the Electronic Engineering article describing the arithmetic units, and the
example programs.

I also corresponded with Delwyn Holroyd, and would not have been able to figure out the
details of some of the design without his helpful replies.

I started this as a CLojure programming exercise.  I chose the WITCH machine specifically
because my parents learned to program this machine while studying at Wolverhampton.


## Usage

If a standalone JAR file has been created, it can be run with the following
command line.

    $ java -jar witch-0.1.0-standalone.jar <tape-file>

Alternatively, download the Leiningen environment from http://leiningen.org
and then run the code using the following command line.

    $ lein run <tape-file>

## Command line options

    -t  :  Traces the execution of the system

## Examples

    $ lein run resources/demo-1

    $ lein run resources/demo-2

    $ lein run -t resources/exercise-1

## Tape file format

The tape file format tries to be similar to the examples given on the WITCH computer
website.  Several tapes are described in each file.  Blank lines are ignored, as are
lines which start ';'.  A line can contain one of:

| Syntax    | Meaning                                                  | Example  |
|-----------|----------------------------------------------------------|----------|
| #n        | A block marker                                           | #1       |
| NNNNN     | A five digit positive number, typically used for orders  | 21020    |
| +NNNNNNNN | An eight digit positive number. '+' is mandatory         | +1234567 |
| -NNNNNNNN | An eight digit negative number. '-' is mandatory         | -1234567 |

There is an implicit decimal point after the first digit of any number.

See the files in the 'resources' directory for some examples.

## Running the unit tests

Unit tests can be run from Leiningen environment.

    $ lein test

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

## ToDo

* Finish off the unit tests
* Write some other example tapes (calculate bubble sort, quick sort, ...)
* Introduce punched hole tape format

## License

Copyright © 2016 David Norman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
