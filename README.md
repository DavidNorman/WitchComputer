# A WITCH computer simulator

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
  output, register access, and machine state are all in here.
* decode.clj - Implements instruction decode.
* alu.clj - Implements the elementary ALU operations.
* nines.clj - Implements nines complement arithmetic on values as stored in the registers.

The machine execution loop (decode/run) calls the step function until the machine state
indicates that the machine has stopped.  The step function (decode/step) fetches an
instruction from the current PC and dispatches it to the decoding function.  The decoding
functions take the machine state and produce a new machine state with the result of the
instruction.

Because a read can sometimes be a state-modifying operation (reading a tape is the most
obvious one), the read functions return a modified machine state, with the value read in
an ALU input holding place (:alu-src and :alu-dst).  This may or may not mirror the real
hardware.  I think it is best to think of :alu-src, :alu-dst and :alu-result as busses,
rather than registers - and consequently their state should not be relevant between different
instruction executions.

## Nines complement

The namespace nines.clj implements the nines complement mathematical operations. Addition
and subtraction are both implemented as addition, as you would expect in the ALU.

### Multiplication

I experimented with multiple different multiplication implementations.  Initially I was
converting the nines complement back into a bigdecimal signed number for multiplication.
However, I have moved to a mechanism where the multiplication is done in nines complement
with suitable sign extension.  In the future I hope to move to a shift and add model so
that the registers do not need to be extended beyond their actual sizes.

## ToDo

* Finish off the unit tests
* Nines complement arithmetic.
* Write some other example tapes (calculate bubble sort, quick sort, ...)
* Introduce punched hole tape format

## License

Copyright © 2016 David Norman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
