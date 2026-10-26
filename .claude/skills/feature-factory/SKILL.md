---
name: feature-factory
description: Deliver one small School App frontend feature using an isolated builder and independent contract/QA checks.
disable-model-invocation: true
argument-hint: "feature to build and acceptance criteria"
---
Feature request: $ARGUMENTS

Act as the lead engineer. Read `CLAUDE.md`. Optimize for shipping a real, reviewable vertical slice, not analysis volume.

1. **Scope (brief).** Read the relevant frontend route, components, API services, types, tests and backend DTO/controller. State feature acceptance criteria, existing endpoint(s), files to own and any genuine blockers. Backend is frozen by default. No fake data.
2. **Build.** Delegate the scoped feature to the `feature-builder` subagent, including precise file ownership and tests. The subagent uses an isolated Git worktree. Use **one** builder by default; use two only for genuinely independent slices with disjoint files.
3. **Inspect.** Obtain the actual builder branch/diff and test evidence. Do not accept a text summary in place of checking code. Run `api-contract-reviewer` and `qa-reviewer` against the resulting changes; they are read-only.
4. **Fix with a bound.** Address blockers and meaningful test regressions, allowing up to two correction passes. If still blocked, stop and explain why.
5. **Gate and report.** Run or report `make frontend-test`, `make frontend-build`, `make quality`; for changed user workflows run `make e2e` where environment permits. Report outcomes honestly, paths changed, branches/worktrees to integrate, blockers and the next thin slice.
6. **Approval.** Do not push, merge, open a PR, deploy, or change `main` without a separate explicit user request. Present changes for human decision.

Do not spend time scanning or rewriting unrelated components. Do not weaken tests or fabricate API results. Keep changes small.
