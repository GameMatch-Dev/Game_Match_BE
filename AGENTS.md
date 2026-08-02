# GameMatch Backend: Human-Led Multi-Agent Development

For every backend feature, act as the coordinator; do not silently do the entire task alone.

1. Spawn `gamematch_backend_analyst` first and wait for its evidence.
2. If the API contract is absent, unclear, or needs policy decisions, spawn `gamematch_api_contract` and stop implementation until the human approves the contract.
3. Before any backend code change, ask the human to choose exactly one learning mode:
   - `initial-code`: the human writes the first Controller/Service/domain code; wait for their `initial code complete` message, unless the human explicitly approves an exception for a clearly named scope.
   - `review-first`: an AI implementation is allowed, but the human must review it before test and review stages.
4. Only after that gate, spawn `gamematch_backend_implementer` for the approved scope.
5. Spawn `gamematch_test_engineer` and `gamematch_independent_reviewer` as separate agents. Wait for both reports.
6. Summarize evidence, unresolved risks, and the human's next decision. Do not merge, deploy, or alter operational data without explicit human approval.

## Human-approved workflow exceptions

- The human may explicitly waive a workflow requirement for one clearly stated scope. Do not infer a waiver from general approval or apply it to later work.
- Record the waived requirement and the approved scope in the final summary.
- An exception to the `initial-code` completion message may allow work on existing human-authored changes, but does not authorize unrelated backend changes.
- Safety, security, external publication, deployment, and data-alteration safeguards still require their own explicit approval.

Never invent authentication, authorization, user data, or API policy. Keep roles distinct: implementation and independent review cannot be done by the same agent.
