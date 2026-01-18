## Some basic info

This is a small Kotlin project developed by a single person. My main goal is to get better at writing 
logic in Kotlin without wasting hours on Leetcode puzzle solving.
To achieve this I'll make a small VN engine that offers simple and intuitive UI layout, screen logic, and solid runtime behaviour.
It should be easy to use, have a gradual learning curve, and be sufficient to ship one complete VN.

## Development goals

### Phase 1 - Finished

1. Define minimal script format.
2. Parse it into an Abstract Syntax Tree (AST).
3. Execute it linearly.

**End goal:**
Run a simple VN script from a terminal.

### Phase 2 - Finished

1. Add choices, conditions, and variables.
2. Implement a VN execution loop.

End goal: run a simple VN script with new features from a terminal.

### Phase 3 - In Development

1. Save current execution state.
2. Add loading state function.
3. track seen lines/seen choices.

**End goal:**
Fully playable VN from the terminal with save/load and history support.

### Phase 4

1. Add non-customizable GUI with one window, one background, etc...
2. Write tests for all crucial methods of all classes.

**End goal:**
Run VN in a window and be happy.

## Future goals

1. Compile script into bytecode and execute it line-by-line.
2. Display error log and not crash, display error after compilation.
3. Make GUI customizable.
4. Embed Lua as a scripting language for writing VN logic.