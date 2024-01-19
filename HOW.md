# How it's supposed to work

## 1. Running histogram diff between two method versions

Most of the time histogram diff produces more intuitive and context-aware results than Myers, so the end mixin result is more likely to be accurate.

Also, [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) implementation of histogram diff operates on ranges, making it easier to isolate instructions which should, for example, be extracted into an `@Inject` method node.

Things to consider:
  - Operand stack (implemented, need to implement diff for groups)

    Can improve context awareness by grouping instructions which operate on the same operand stack values.
    
    Can potentially be implemented by first running diff on groups of instructions, then running diff on groups which were considered "changed" to figure out if they were modified or actually replaced.

    Also, greatly simplifies isolating inserted instructions which should be extracted into an `@Inject`, since the extracted instructions don't depend on a stack state.

  - Control flow

    Building a CFG and generating diff between individual branches of it may also improve context awareness.

## 2. Analyze edit script

Diff produces an edit script consisting of [4 operation types](https://javadoc.io/static/io.github.java-diff-utils/java-diff-utils/4.12/com/github/difflib/patch/DeltaType.html) which, in combination with context from original insn list, can be used to generate a mixin equivalent of a specific part of the script.