---
name: feature-factory
description: Deliver one small School App frontend feature using an isolated builder and independent contract/QA checks.
disable-model-invocation: true
argument-hint: "feature to build and acceptance criteria"
---
Feature request: $ARGUMENTS

Act as lead engineer. Read `CLAUDE.md`. Ship one real, reviewable vertical slice with minimal discussion.

1. **Preflight and scope (brief).** Read local repository instructions, `git status`, branch refs and recent commits. Protect local/untracked changes and other worktrees. Identify the exact intended base SHA and existing open/unmerged feature work before choosing the slice. Inspect only its relevant frontend code/tests and actual backend controller/DTO/authorization rules. Define acceptance, file ownership and blockers. Backend frozen by default; no fake data.
2. **Build one slice.** Delegate the goal, base SHA, acceptance, owned paths and tests to `feature-builder`. Its `isolation: worktree` starts from the repository **default branch by default**, not the lead session's HEAD. Confirm the new worktree starts at the intended base; if not, stop for a safe explicit base selection instead of silently applying changes onto another version. Use one builder initially; two only for independent slices with disjoint files and no simultaneous use of shared Docker/Playwright ports.
3. **Make work reviewable.** Require the builder to preserve its worktree and create a local commit containing only owned files. Independently inspect its actual base SHA, branch ref, final commit SHA, changed paths and diff. A builder summary or an uncommitted change in another worktree is **not** sufficient review evidence.
4. **Independent review and bounded repair.** Provide the exact committed branch/SHA and base to `api-contract-reviewer` and `qa-reviewer`; ensure they can inspect that commit/worktree, not only their own checkout. If they cannot access the actual diff, report NOT VERIFIED and stop. Fix substantiated blockers/regressions in the builder worktree, re-commit, and re-check the final commit. At most two correction passes.
5. **Verification.** Use the existing Makefile: focused `make frontend-test TEST=<pattern>`, `make frontend-build`, `make quality`; run relevant Playwright with the real stack when possible, without parallel port conflicts. Distinguish tests actually run on the final SHA from prior or unrun tests. GitHub CI cannot be claimed until the exact feature commit is pushed by authorization and its checks finish.
6. **Human gate.** Report exact base/final SHAs, local worktree/branch, changed files, true test/review outcomes, data/security implications, blockers and one next slice. Do not push, open a PR, merge, deploy or change `main` without separate explicit authorization.

Do not scan or rewrite unrelated code, weaken tests, fabricate API results or enter repeated planning/review loops.
