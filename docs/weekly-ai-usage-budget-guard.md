# Weekly AI Usage Budget Guard

Before every new Codex agent dispatch and every Claude review or re-review, record the provider's actual weekly remaining quota when it is exposed. Use `UNAVAILABLE` when it is not exposed; never estimate it.

If an actual remaining quota for either provider is below 50%, report the available usage figures and the planned Codex agent dispatch or Claude review to the human, and ask whether to proceed. Hold only those calls while awaiting an answer; preserve local work and continue authorized work that does not depend on them.

Explicit human approval overrides this repository's usage threshold for the approved scope. After approval, proceed with the approved calls without repeatedly asking for the same scope. Record the approval, scope, quota figures (or UNAVAILABLE), and any actual provider limit or error. Approval to change this policy is not approval for a specific pending review or dispatch. A provider-enforced limit cannot be bypassed; report it if an approved call fails.

After each atomic task, record the same quota fields again before continuing. This guard controls whether the next AI step may start; task quality and usage analysis are recorded separately in `docs/ai-review-metrics.md`.
