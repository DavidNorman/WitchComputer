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

## ToDo

* Finish off the unit tests
* Write some other example tapes (calculate sin/cos, bubble sort, quick sort, ...)
* Introduce punched hole tape format

## License

Copyright © 2016 David Norman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
