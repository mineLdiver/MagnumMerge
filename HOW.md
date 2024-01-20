# How it's supposed to work

## 1. Running ~~histogram~~ Myers diff between two method versions

~~Most of the time histogram diff produces more intuitive and context-aware results than Myers, so the end mixin result is more likely to be accurate.~~

~~Also, [java-diff-utils](https://github.com/java-diff-utils/java-diff-utils) implementation of histogram diff operates on ranges, making it easier to isolate instructions which should, for example, be extracted into an `@Inject` method node.~~

Histogram diff has caused some issues, marking entire ranges as changed when there were two simple insertions at the start and the end.

For now, Myers seems to give better results.

Things to consider:
  - Operand stack (implemented, need to implement diff for groups)

    Can improve context awareness by grouping instructions which operate on the same operand stack values.
    
    Can potentially be implemented by first running diff on groups of instructions, then running diff on groups which were considered "changed" to figure out if they were modified or actually replaced.

    Also, greatly simplifies isolating inserted instructions which should be extracted into an `@Inject`, since the extracted instructions don't depend on a stack state.

  - Control flow

    Building a control flow graph and generating diff between individual branches of it may also improve context awareness.

## 2. Analyze edit script

Diff produces an edit script consisting of [4 operation types](https://javadoc.io/static/io.github.java-diff-utils/java-diff-utils/4.12/com/github/difflib/patch/DeltaType.html) which, in combination with context from original insn list, can be used to generate a mixin equivalent of a specific part of the script.

### 2.1. Insertions

The easiest to handle.

Thanks to stack grouping ~~and the way histogram diff seems to be working so far~~, insertions should almost always be a simple `@Inject` with some context from the target method through locals capture (MixinExtras?), unless they alter control flow in an incompatible way, which has to be determined earlier on when building the control flow graph.

### 2.2. Changes

The harder to handle.

Change is incredibly ambiguous when it comes to generating a mixin. Some cases are easy, like a changed LDC instruction, which would just result in a `@ModifyConstant`, or a changed method/field access, which would result in a `@Redirect`.

But, due to stack grouping ~~and the way histogram diff works~~, changes are grouped in a very large range, while "redirector" mixins are usually targeting very specific instructions, which means we have to run a more thorough diff through each changed group to identify which mixins can be generated to replicate the changes.

### 2.3. Deletions

Pretty much impossible to handle.

Unless it's a simple method/field access removal, there isn't really a way to replicate deletions with mixins.