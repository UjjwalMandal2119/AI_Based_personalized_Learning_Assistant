# Remote-state bootstrap

Create the remote Terraform state backend here as a one-time, manually reviewed operation. Use a dedicated S3 bucket with encryption, versioning, public access blocked, and narrowly scoped access. Configure Terraform's S3 backend lockfile support and keep state isolated by environment.

Do not store state in this repository. Document the account, region, recovery procedure, and access policy without recording credentials. Verify the backend and recovery process before using it for environment resources.
