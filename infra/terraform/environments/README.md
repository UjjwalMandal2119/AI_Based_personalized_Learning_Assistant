# Terraform environments

Create independent Terraform roots for `dev`, `staging`, and `prod`. Begin with `dev`; add other environments only after the deployment and recovery workflow is repeatable.

Each root should have a clear set of inputs and outputs, a separate remote state key, and environment-specific access boundaries. Keep real values out of Git and provide sanitized `*.tfvars.example` files when examples are useful.
