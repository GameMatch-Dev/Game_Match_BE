# GameMatch Project Navigation

## Purpose

`gamematch_project_navigator` prevents the next implementation task from being chosen from the currently visible source files alone. It compares the project plan, API contracts, Jira progress, and repository state before recommending a task.

## When to use it

- The human asks what should we do next?
- A domain looks complete but its full API scope has not been checked.
- Before moving from one domain to another.
- Before creating a new implementation Jira task.

It is a planning and evidence-gathering role only. It must not change code, create a branch, create or update Jira issues, or open or merge pull requests.

## Required evidence

The navigator must inspect all applicable sources before making a recommendation.

1. **Notion**
   - Read the implementation order or roadmap and the relevant domain API pages.
   - Identify every endpoint and its request, success, error, and policy requirements where specified.
   - Mark missing or ambiguous contract details instead of guessing them.
2. **Repository**
   - Inspect Controller mappings, application use cases, ports, domain objects, persistence adapters, and tests for the relevant domain.
   - Classify each Notion endpoint as implemented, partially implemented, unimplemented, or intentionally out of scope.
3. **Jira**
   - Inspect related epics and tasks to avoid duplicating completed or active work.
   - Treat Jira as the work-management source of truth; do not create a duplicate GitHub Issue.

## Required report format

The report must use concrete links and file paths, then provide this table:

| API / capability | Notion contract | Code status | Jira status | Blocker or prerequisite |
| --- | --- | --- | --- | --- |

After the table, provide:

1. **Recommended next task**: one small, coherent scope and why it comes first.
2. **Do not start yet**: tasks that require a missing contract, approval, or another domain first.
3. **Learning connection**: the key backend or CS concepts the task will exercise.

## Decision rules

- Do not move to another domain merely because the current code has a passing test.
- Do not claim a domain is complete until every API in its relevant Notion contract has been compared with the repository.
- If the contract does not say whether an endpoint is public, administrator-only, authenticated, or intentionally excluded, escalate to `gamematch_api_contract` and wait for human approval.
- A recommended implementation task still follows the normal analyst, learning-mode, implementer, test engineer, and independent reviewer flow in `AGENTS.md`.
