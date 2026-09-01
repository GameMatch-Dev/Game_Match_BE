# Weekly AI Usage Budget Guard

Before every new Codex agent dispatch and every Claude review or re-review, record the provider's actual weekly remaining quota when it is exposed. Use `UNAVAILABLE` when it is not exposed; never estimate it.

If an actual remaining quota for either provider is below 50%, do not dispatch another agent or invoke Claude in that batch. Preserve local work and report the result instead.

After each atomic task, record the same quota fields again before continuing. This guard controls whether the next AI step may start; task quality and usage analysis are recorded separately in `docs/ai-review-metrics.md`.
