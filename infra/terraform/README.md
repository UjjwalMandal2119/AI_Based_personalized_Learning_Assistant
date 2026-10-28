# Terraform on AWS

Terraform infrastructure is not implemented yet. Build it incrementally in a dedicated AWS sandbox account; do not run `apply` against an account containing unrelated resources.

```text
terraform/
├── bootstrap/                 # Remote state bucket and locking setup
├── environments/
│   ├── dev/                   # First deployable environment
│   ├── staging/
│   └── prod/
└── modules/                   # Reusable, versioned infrastructure units
```

Keep each environment independently plan-able and give it isolated state. Commit `.terraform.lock.hcl`; do not commit state, plans, credentials, or real variable files. Keep example variable files suffixed `.tfvars.example`. Review plans before applying, apply only from a controlled workflow once that workflow is established, and document how to destroy disposable resources.

Suggested implementation order: remote-state bootstrap, networking, ECR, workload identity for GitHub Actions, then the runtime platform and managed data services. Keep modules small and specific to infrastructure actually used by this application.
