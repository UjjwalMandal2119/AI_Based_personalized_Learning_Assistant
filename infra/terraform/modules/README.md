# Terraform modules

Place reusable AWS infrastructure modules here only when an environment has a real need for them. Define module inputs and outputs explicitly, document security assumptions, and avoid hiding environment-specific behavior inside generic modules.

Potential future modules include networking, ECR, workload identity, and the selected runtime platform. Keep module scope small and review generated plans from the environment that consumes each module.
