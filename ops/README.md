# Operations

Use this area for operational practices that support the application throughout its lifecycle:

- `runbooks/` — concise procedures for deployment failure, rollback, database restore, and incident triage.
- `observability/` — metrics, dashboards, log queries, alert rules, and service-level objectives.
- `scripts/` — small, repeatable helper scripts used locally or in CI; document prerequisites and failure behavior.

Do not put application source code, Terraform resources, or credentials here. Keep procedures actionable and test them during practice rather than treating documentation as a substitute for a recovery drill.
