1. Think Before Coding
   Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

State your assumptions explicitly. If uncertain, ask.
If multiple interpretations exist, present them - don't pick silently.
If a simpler approach exists, say so. Push back when warranted.
If something is unclear, stop. Name what's confusing. Ask.

2. Simplicity First
   Minimum code that solves the problem. Nothing speculative.

No features beyond what was asked.
No abstractions for single-use code.
No "flexibility" or "configurability" that wasn't requested.
No error handling for impossible scenarios.
If you write 200 lines and it could be 50, rewrite it.
Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

3. Surgical Changes
   Touch only what you must. Clean up only your own mess.

When editing existing code:

Don't "improve" adjacent code, comments, or formatting.
Don't refactor things that aren't broken.
Match existing style, even if you'd do it differently.
If you notice unrelated dead code, mention it - don't delete it.
When your changes create orphans:

Remove imports/variables/functions that YOUR changes made unused.
Don't remove pre-existing dead code unless asked.
The test: Every changed line should trace directly to the user's request.

4. Goal-Driven Execution
   Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

"Add validation" → "Write tests for invalid inputs, then make them pass"
"Fix the bug" → "Write a test that reproduces it, then make it pass"
"Refactor X" → "Ensure tests pass before and after"
For multi-step tasks, state a brief plan:

   [Step] → verify: [check]
   [Step] → verify: [check]
   [Step] → verify: [check]
   
   Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.


5. Additionals
   Please use my code style.
   Never do more that it is required.

6. Shared Context
   Architecture discussions and decisions happen outside Claude Code, in a separate design agent.
   Their outcome is written to docs/context/ - this is how knowledge is exchanged between agents.
   Reading it is not required for every task.
   Check it when you need background: why something is designed this way, what the project aims for.
   Code wins over these files - if they disagree, the files are out of date.
   Don't edit docs/context/ unless asked. If your change makes something there outdated, say so in your summary.


7. Sessions
   You should write some kind of summary in docs/context/sessions/ after your job is done. 
   Use {date}-{summary}.md file name format.
   When you finish a task, write a short summary in docs/context/sessions/. Name the file {date}-{summary}.md, 
   where {date} is YYYY-MM-DD and {summary} is a few words in kebab-case, e.g. 2026-11-21-fix-trading-simulator-parent-pom.md.