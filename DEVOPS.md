# Solo DevOps Practice

This repository is a learning project operated by one person. Keep the existing application layout and practice production-minded workflows around it; do not introduce team-only process for its own sake.

## Repository layout

```text
.
├── .github/
│   └── workflows/
│       └── ci.yml                 # Existing backend and frontend verification
├── backend/                       # Spring Boot API and tests
├── frontend/                      # Angular application and tests
├── deploy/
│   └── helm/                      # Kubernetes application chart (to be implemented)
├── docs/
│   ├── architecture/              # System context, diagrams, and boundaries
│   └── decisions/                 # Lightweight architecture decision records
├── infra/
│   └── terraform/
│       ├── bootstrap/             # One-time remote-state prerequisites
│       ├── environments/
│       │   ├── dev/
│       │   ├── staging/
│       │   └── prod/
│       └── modules/               # Reusable AWS building blocks
├── ops/
│   ├── observability/             # Metrics, logs, alerts, dashboards
│   ├── runbooks/                  # Incident and operational procedures
│   └── scripts/                   # Repeatable local/CI helper scripts
├── docker-compose.yml             # Local development dependencies
└── DEVOPS.md
```

The directories under `deploy`, `docs`, `infra`, and `ops` are deliberate scaffolding, not working infrastructure. Add implementation only as each practice stage is reached. Keep cloud-specific resources in Terraform, application runtime configuration in Helm, and operational procedures in `ops/runbooks`.

## Suggested AWS learning sequence

Complete one stage at a time; keep the existing CI green before progressing.

1. **Repository and local workflow** — use feature branches, small commits, pull requests to yourself if useful, `.env.example`, and Docker Compose. Never commit credentials or local state.
2. **CI quality gates** — understand and extend `.github/workflows/ci.yml` with formatting, tests, builds, dependency checks, and artifact retention. Keep pull-request validation separate from deployment.
3. **Container delivery** — make reproducible, non-root images for the API and frontend; add health checks, image scanning, and immutable tags. Validate locally before pushing.
4. **AWS foundations with Terraform** — start with a dedicated sandbox account and a budget alert. Learn remote state, state locking, least-privilege IAM, plan review, tagging, and teardown. Build one environment before considering staging or production.
5. **AWS image registry and identity** — publish images to ECR from GitHub Actions using OIDC and short-lived AWS credentials. Do not store long-lived AWS access keys in repository secrets.
6. **Kubernetes and release management** — learn EKS basics, then package the app with Helm. Deploy a development release first; add readiness/liveness probes, resource requests/limits, secrets handling, rollback, and environment-specific values.
7. **Observability and operations** — define service-level signals, dashboards, actionable alerts, logs, and runbooks. Practice diagnosing a failed deployment and restoring service.
8. **Security and resilience** — add image/dependency/IaC scanning, threat-aware IAM boundaries, backup/restore drills for PostgreSQL, disaster-recovery notes, and cost review.
9. **Promotion and cleanup** — promote the same immutable artifact through environments, require an explicit production approval, verify rollback, and destroy disposable resources after practice.

## Solo guardrails

- Start in an AWS sandbox account, set a budget alert, and use the smallest practical resources.
- Keep `prod` as a documented future environment until you can deploy, observe, roll back, restore backups, and estimate its cost.
- Use separate state per environment and do not commit Terraform state, plans, credentials, or real `.tfvars` files. The root `.gitignore` excludes these.
- Prefer GitHub Actions OIDC roles with scoped permissions over static AWS keys.
- Require yourself to inspect `terraform plan` and deployment diffs before applying them.
- Document one decision or operational lesson when a practice changes the architecture.

## Completion checklist

- [ ] Pull requests run backend verification, frontend tests, and production build.
- [ ] Containers are reproducible, scanned, and published with immutable tags.
- [ ] Terraform provisions a disposable AWS development environment with remote state and least-privilege access.
- [ ] GitHub Actions authenticates to AWS through OIDC and deploys without static cloud credentials.
- [ ] Helm can install, upgrade, and roll back the application.
- [ ] Health checks, dashboards, alerts, runbooks, and database recovery have been exercised.
- [ ] Resources can be safely destroyed and recreated, and costs are understood.
