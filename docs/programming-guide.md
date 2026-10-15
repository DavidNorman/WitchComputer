# Programming the WITCH

This guide explains how to combine the WITCH's small set of orders into useful
programming techniques. It complements the historical manual: the manual defines
the orders, while this guide concentrates on patterns demonstrated by the programs
in [`examples/`](../examples/).

The examples use the symbolic assembler. The simulator executes the generated
numerical tape, not symbolic source.

## Primary references

This guide should be read alongside the original Computer Conservation Society
material:

- [Notes on Programming and Operating, second edition](https://www.computerconservationsociety.org/Wolv-Manual.pdf)
  is the full contemporary programming and operating manual.
- [The WITCH order code](https://www.computerconservationsociety.org/witch7.htm)
  is a compact online table of the machine's orders.

Where this guide uses modern terms such as “subroutine,” “stack,” and
“continuation,” those terms describe techniques constructed from the historical
orders documented in those references.

## Terminology

The historical documentation uses several words differently from modern
programming literature.

### Order

An **order** is what would now normally be called an instruction. It is written
as five decimal digits:

```text
11020
```

The first digit selects the operation, and the remaining four digits normally
contain two two-digit addresses. In this example, `1` means add and hold, so the
order means “add the contents of sending address 10 into receiving address 20.”

Orders can be read from tape or stored in addresses 10–99. Because an order has
the same numerical representation as ordinary data, a program can alter an
order arithmetically and then execute it.

### Group on tape

A **group** is one complete item read from paper tape. Depending on context, it
may be:

- A five-digit order
- A signed eight-digit number
- A five-digit positive number marked with an asterisk on physical tape

On physical tape, a group begins with a space character or a numbered block
marker. In this project's numerical tape format, each order or number appears
on its own line. A preceding `#n` line represents the block marker attached to
the following group.

When this guide says that a tape advances to the “next group,” it means the next
complete order or data value—not merely the next punched row or decimal digit.

### Block marker

A **block marker** is a special punched character numbered 0–9. It identifies a
position that a tape reader can search for. A block is therefore a convenient
entry point or branch target on a tape, rather than a region allocated by the
assembler.

### Store group

A **group of stores** is a different use of the word “group.” The stores are
arranged in tens: 10–19, 20–29, and so on. Most arithmetic orders may not use
two addresses from the same store group. This restriction is unrelated to tape
groups.

## 1. A mental model

Several differences from a modern computer shape every WITCH program:

- Stores 10 through 99 hold data or executable orders.
- Ordinary stores have seven digits after a fixed decimal point and magnitude
  below 10.
- The accumulator has fourteen digits after the decimal point.
- Arithmetic uses nines complement, including two representations of zero.
- Tape readers supply data or the next order to execute.
- There is no native call, return, stack, index register, or indirect load.
- An order is also a number, so arithmetic can construct executable orders.

```text
 tape readers ──► transfer unit ──► stores 10–99
                         │                │
                         ▼                │
                    accumulator ◄─────────┘
                         │
                         ├──► printer
                         └──► perforator
```

A tape reader is not merely a file handle. After control is transferred to it,
successive groups on that tape are interpreted as orders.

## 2. Fixed-point representation

The decimal point is fixed after the first digit:

```text
+00000005    0.0000005
+00120000    0.0120000
+50000000    5.0000000
-00000005   -0.0000005
```

Choose a scale before writing a program. Factorial, Fibonacci, and counter
examples use the last decimal position as the units position:

```text
+00000001    integer 1
+00000005    integer 5
+00000120    integer 120
```

This permits larger integer results before reaching the magnitude limit, at the
cost of explicit rescaling after multiplication.

Ordinary stores retain seven fractional digits; the accumulator retains fourteen.
Moving from the accumulator to a store discards digits that do not fit, so decide
where precision may safely be lost.

## 3. Clearing and moving values

`CLEAR` is an assembler pseudo-order. It sends a store's contents to address 00,
the drain:

```text
CLEAR RESULT
```

Copy while retaining the source:

```text
CLEAR RESULT
ADD SOURCE RESULT
```

Move while clearing the source:

```text
ADD-AND-CLEAR SOURCE RESULT
```

The clear forms cannot be used for tape input or printer/perforator output.
Arithmetic addresses also normally cannot be in the same group of ten stores:
an operation between stores 21 and 27 is invalid, while one between 21 and 37
is valid. Use a temporary in another group or the accumulator when necessary.

## 4. Nines complement and two zeroes

Nines-complement arithmetic represents zero twice:

```text
+0    00.0000000
-0    99.9999999
```

The distinction matters:

- Clearing produces positive zero.
- Subtracting positive zero from positive zero produces negative zero.
- Subtracting equal positive values normally produces negative zero.
- Positive-zero division stops the machine.
- Negative-zero division is accepted.

A positive sign test is not a general “greater than or equal to zero” test.

### Testing for either zero

Zero is the only number whose positive modulus can be added to negative zero
while leaving a negative result:

```text
; ZERO_TEST initially contains -0.
TRANSFER-POSITIVE-MODULUS VALUE ZERO_TEST
TEST-NEGATIVE ZERO_TEST
```

The counter and GCD examples use variants of this technique. If `VALUE` is
nonzero, restore `ZERO_TEST` to negative zero before reusing it.

## 5. Sign tests and conditional control

Conditional execution has two stages:

```text
TEST-POSITIVE VALUE
SEARCH-IF 3 02
```

or:

```text
TEST-NEGATIVE VALUE
TRANSFER-CONTROL-IF ROUTINE
```

The sign examination sets a remembered yes/no value. It remains until another
sign examination replaces it. A conditional order before any sign examination
stops the machine.

### If and if/else

```text
TEST-POSITIVE VALUE
SEARCH-IF 2 01

; False branch.
...
SEARCH 3 01

#2
; True branch.
...

#3
; Join point.
```

### Count-controlled loop

```text
#2
; Loop body.
...
SUBTRACT ONE COUNT
TEST-POSITIVE COUNT
SEARCH-IF 2 01
```

Choose the initial count and test carefully so negative zero changes the sign on
the intended iteration.

## 6. Blocks and control on one tape

A block marker is a physical tape character represented as `#0` through `#9`.
It is not an assembler address label. Searching repositions a tape:

```text
SEARCH 2 01
```

If tape 1 is executing, its next order comes from the group after block 2.
A useful convention is:

```text
#1   entry and initialization
#2   main loop
#3   helper or true branch
#4   return/unwind path
#5   output or final branch
```

Block numbers are local to a tape reader, so several tapes may each have block 1.

## 7. Using multiple tapes

A practical division of work is:

- Tape 1: initialization and construction of stored orders
- Tape 2: main algorithm
- Tape 3: reusable routines
- Other tapes: numerical input or specialised inner loops

Searching and transferring have different jobs:

```text
SEARCH 2 03             ; position tape 3 at block 2
TRANSFER-CONTROL 03     ; execute from tape reader 3
```

When control leaves a caller tape, that tape has advanced beyond its transfer
order. A routine can return with `TRANSFER-CONTROL 02`, and tape 2 resumes at
the following group. This tape-position return is the simplest substitute for a
return address.

Guidelines:

- Put a heavily repeated inner routine on a short tape.
- State which reader contains each routine.
- Search before transferring unless reader position is part of the convention.
- Keep sequential numerical input separate from instructions when useful.
- Document whether a routine returns to a fixed tape or a stored order.

## 8. A tape-based subroutine convention

The machine imposes no calling convention. Define one and document every routine:

```text
; Input:
;   40 = source value
; Output:
;   50 = result
; Clobbers:
;   09, 60
```

A simple call is:

```text
CLEAR ARGUMENT
ADD VALUE ARGUMENT
SEARCH 1 03
TRANSFER-CONTROL 03

; Execution resumes here.
ADD RETURN_VALUE DESTINATION
```

The routine ends with `TRANSFER-CONTROL 02` when tape 2 is its caller.
See [indirect.witch](../examples/indirect.witch) and
[recursive-factorial.witch](../examples/recursive-factorial.witch).

## 9. Stored orders and self-modifying code

The order `14150` means `ADD 41 50`, but in a store it is also the number
`1.4150`:

```text
1 41 50
│ │  └─ receiving address
│ └──── sending address
└────── operation
```

Arithmetic can insert an address, after which control can be transferred to the
store. The two address fields require different shifts:

- Shift `-3` before transferring accumulator address 08 to insert a source.
- Shift `-5` before transferring address 08 to insert a destination.

### Constructing a dynamic destination

If `ADDRESS` holds `0.0000050` and the template is `14100`:

```text
CLEAR DYNAMIC_ORDER
ADD 03 DYNAMIC_ORDER
14100

CLEAR 09
SHIFT -5
ADD ADDRESS 09
ADD 08 DYNAMIC_ORDER
```

`DYNAMIC_ORDER` now contains `14150`, or `ADD 41 50`.

### Constructing a dynamic source

Inserting source address 50 into template `10042` produces `15042`:

```text
CLEAR DYNAMIC_ORDER
ADD 03 DYNAMIC_ORDER
10042

CLEAR 09
SHIFT -3
ADD ADDRESS 09
ADD 08 DYNAMIC_ORDER
```

### A dangerous transposition

To clear store 50, construct:

```text
25000    ; send store 50 to the drain
```

Do not construct:

```text
20050    ; send address 00, the random round-off source, to store 50
```

The second may appear to work when the random source produces zero, making it a
deceptive error.

Before executing a generated order:

1. Clear its store.
2. Add the fixed template.
3. Insert dynamic fields with the correct shifts.
4. Inspect the result under tracing.
5. Transfer control to the order's store.

## 10. Indirect loads and stores

There is no indirect-addressing order, but a program can construct one.
[indirect.witch](../examples/indirect.witch) reserves stores for the desired
address, transferred data, and generated orders.

Conceptually:

```text
ADDRESS = 37
DYNAMIC_ORDER = ADD 37 LOAD_RESULT
execute DYNAMIC_ORDER
```

An indirect store constructs a receiving field. A complete store usually uses
two generated orders: clear the destination, then add the source into it.
Consecutive generated orders followed by transfer control form a tiny routine.

Indirect access is useful for arrays, software stacks, runtime-selected tables,
and dynamic returns. It is slow, so direct addresses remain preferable for
small fixed problems.

## 11. Arrays

An array is a consecutive store range:

```text
ARRAY_0 = 60
ARRAY_1 = 61
ARRAY_2 = 62
```

For a fixed array, named or unrolled operations are simplest.
[bubble-sort.witch](../examples/bubble-sort.witch) demonstrates comparison and
exchange over a fixed collection.

For a variable index:

1. Hold the current store address as a scaled integer.
2. Construct an indirect load using it.
3. Increment the address.
4. Repeat.

Swapping needs temporary storage, and generated orders remain subject to
same-group restrictions. Put temporaries in another group from the array.

## 12. A software stack

[recursive-factorial.witch](../examples/recursive-factorial.witch) implements a
stack in stores 50 through 69. Its stack pointer is the next free address:

```text
+00000050    SP points to store 50
+00000052    two words have been pushed
```

The stored fixed-point values are `0.0000050` and `0.0000052`.

### Push

For each word:

1. Construct an order clearing `stack[SP]`.
2. Construct an order copying the frame word to `stack[SP]`.
3. Execute the generated orders.
4. Increment `SP`.

### Pop

For each word:

1. Decrement `SP`.
2. Construct an order loading `stack[SP]`.
3. Execute it.

Popping need not erase the old value. Once `SP` moves below it, that word is
logically absent and a later push overwrites it.

The recursive example uses two-word frames:

```text
saved n
continuation
```

Stores 90 through 97 hold generated push/pop programs. The stack has ten words,
or five frames. A general implementation should stop on overflow beyond store
69 or underflow below store 50; this example has a fixed, known depth.

## 13. Calls, returns, and continuations

Two return mechanisms are useful.

### Tape-position return

Call a routine on another tape. It transfers back to the caller's reader, which
resumes after the call.

### Continuation marker

A frame records what to do when the result is available:

```text
+1    multiply by saved n and continue unwinding
-1    outermost return: print and finish
```

Recursive factorial tests the popped continuation's sign. Positive selects
multiplication; the negative sentinel selects termination. More complex
routines can use several continuation values and dispatch through sign tests
or generated transfer orders.

## 14. Recursive factorial

The example implements:

```text
factorial(n):
    if n <= 1:
        result = 1
        return

    push(saved_n=n, continuation=AFTER_RECURSION)
    n = n - 1
    factorial(n)

AFTER_RECURSION:
    pop saved_n
    result = result * saved_n
    return
```

Before the first call it pushes a negative sentinel. For `factorial(5)`:

```text
sentinel
saved 5, resume multiplication
saved 4, resume multiplication
saved 3, resume multiplication
saved 2, resume multiplication
```

The base case supplies 1. Unwinding multiplies by 2, 3, 4, and 5. The sentinel
then finishes with the stack pointer back at 50.

Compare [factorial.witch](../examples/factorial.witch) and
[recursive-factorial.witch](../examples/recursive-factorial.witch) to see the
cost of forcing recursion onto a machine naturally suited to iteration.

## 15. Shift discipline

An `08` order selects a shift for the next eligible add, subtract, or
positive-modulus transfer:

```text
SHIFT 1     ; shift A, multiply by 10
SHIFT 0     ; normal transfer
SHIFT -1    ; shift C, divide by 10
...
SHIFT -7    ; shift J
```

Rules:

- The next eligible `ADD`, `SUBTRACT`, or
  `TRANSFER-POSITIVE-MODULUS` consumes it.
- Tape input and printer/perforator output ignore it and leave it pending.
- Multiply, divide, and clear orders leave it pending.
- A shifted transfer cannot clear its sending store.
- Shift A discards the digit shifted out on the left.
- Right-shifted digits are discarded in an ordinary store, while the
  accumulator can retain additional positions.

Address 08 sends the last seven accumulator digits with their sign. Combining
it with a shift is a standard way to place a multiplication result into a store.
When debugging, check whether an earlier shift is still pending.

## 16. Multiplication

Multiplication adds its product to the accumulator and clears the multiplier,
which is the receiving operand:

```text
MULTIPLY MULTIPLICAND MULTIPLIER
```

Clear the accumulator unless accumulation is intentional:

```text
CLEAR 09
MULTIPLY A B
```

Least-significant-digit integers commonly use:

```text
CLEAR 09
MULTIPLY A RESULT
SHIFT -1
ADD 08 RESULT
```

`RESULT` is cleared as the multiplier and then receives the rescaled product.
Negative multipliers temporarily reduce accumulator capacity on the physical
machine, so avoid values near the magnitude limit.

## 17. Division

For division:

1. Put the dividend in the accumulator.
2. Clear the quotient store.
3. Execute `DIVIDE DIVISOR QUOTIENT`.
4. Read the remainder from the accumulator.

```text
CLEAR QUOTIENT
CLEAR 09
ADD DIVIDEND 09
DIVIDE DIVISOR QUOTIENT
```

Remember:

- A positive-zero dividend stops with shift A selected.
- A negative-zero dividend is accepted.
- Exact positive division has a one-unit error in the final quotient digit and
  leaves a remainder.
- A negative dividend avoids that particular quotient error.
- The quotient store must be positive zero; negative zero is not suitable.

Multiplication by a reciprocal is often simpler when precision permits it.

## 18. Printing

Select a layout before printer output:

```text
PRINT-LAYOUT-1
ADD VALUE 01
PRINT-LAYOUT-2
ADD OTHER_VALUE 01
```

The general form accepts layouts 0 through 9:

```text
PRINT-LAYOUT 5
```

Printer output ignores a pending arithmetic shift and does not consume it.

## 19. Debugging

Single-step a tape:

```sh
clojure -M:run -s /tmp/program.tape
```

Trace every order:

```sh
clojure -M:run -t /tmp/program.tape
```

Useful checks:

- Inspect raw nines-complement values and intended meanings.
- Look for negative zero after subtraction.
- Check the active tape and current block.
- Check whether a shift remains pending.
- Check which multiply or clear operation destroys an operand.
- Inspect generated orders before executing them.
- Decode operation, source, and destination fields separately.
- Remember that stack address 50 is stored as `0.0000050`, not value 50.
- Insert temporary print orders around suspicious calculations.
- Reduce a failing routine to one block with fixed arguments.

Address 00's random round-off source can expose an incorrectly generated clear
order nondeterministically. If behaviour changes between runs, inspect generated
orders containing source address 00.

## 20. Assembling and visualising tape

Assemble and run:

```sh
clojure -M:assemble examples/recursive-factorial.witch /tmp/factorial.tape
clojure -M:run /tmp/factorial.tape
```

Generate punched-tape visualization as well:

```sh
clojure -M:assemble examples/recursive-factorial.witch \
  /tmp/factorial.tape --punch /tmp/factorial.punch
```

Or print only the visualization:

```sh
clojure -M:assemble examples/recursive-factorial.witch --punch
```

The punched representation is visual output, not simulator input.

## 21. Technique index

| Technique | Example |
|---|---|
| Simple loop | [counter.witch](../examples/counter.witch) |
| Iterative factorial | [factorial.witch](../examples/factorial.witch) |
| Sequential state update | [fibonacci.witch](../examples/fibonacci.witch) |
| Equality and negative zero | [gcd.witch](../examples/gcd.witch) |
| Polynomial evaluation | [polynomial.witch](../examples/polynomial.witch) |
| Iterative approximation | [exercise-3.witch](../examples/exercise-3.witch) |
| Series approximation | [pi.witch](../examples/pi.witch), [sine.witch](../examples/sine.witch) |
| Arrays and exchange | [bubble-sort.witch](../examples/bubble-sort.witch) |
| Indirect access | [indirect.witch](../examples/indirect.witch) |
| Stored programs | [indirect.witch](../examples/indirect.witch) |
| Multiple cooperating tapes | [indirect.witch](../examples/indirect.witch) |
| Software stack | [recursive-factorial.witch](../examples/recursive-factorial.witch) |
| Continuations and recursion | [recursive-factorial.witch](../examples/recursive-factorial.witch) |

## 22. Checklist for a new program

Before running a new program, ask:

1. What fixed-point scale does each store use?
2. Can an intermediate value overflow magnitude 10?
3. Which stores may contain negative zero?
4. Are arithmetic operands in different groups of ten?
5. Which orders destroy an operand?
6. Is the accumulator deliberately cleared or accumulated?
7. Is a shift pending, and which transfer consumes it?
8. Which tape and block own each routine?
9. What are each routine's inputs, outputs, and clobbers?
10. Are dynamic source and destination fields shifted differently?
11. Can an array or stack address escape its reserved range?
12. Does every path reach `FINISH` or an intentional signal?

Answering these catches most errors before the slow part: watching the WITCH
carry them out.
