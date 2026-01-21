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

**End goal:** run a simple VN script with new features from a terminal.

### Phase 3 - Finished

1. Implement persistence.
2. Save current execution state.
3. Add loading state function.
4. Track seen lines/seen choices.
5. Add save/load commands into console interface (update persistence when save happens).

**End goal:**
Fully playable VN from the terminal with save/load and history support.

### Phase 4 - In Development

1. Add non-customizable GUI with one window, one background, etc...
2. Write tests for all crucial methods of all classes.

**End goal:**
Run VN in a window and be happy.

## Future goals

1. Add rollback function
2. Compile script into bytecode and execute it line-by-line. 
3. Display error log and not crash, display error after compilation. 
4. Make GUI customizable. 
5. Embed Lua as a scripting language for writing VN logic. 
6. Add assignId support (each block can be marked with a unique id to ensure forward compatibility and simplify save transfer between versions). 
7. Add proper save transfer between versions (add export save button when game ends; it should basically compress ast and current global state into a huge blob, then the next game version should load previous ast and map each dialogue line).