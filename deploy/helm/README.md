# Kubernetes deployment

Helm deployment is scaffolding only; the application is not currently deployed to Kubernetes.

Add a chart for the application after container images and the target runtime are defined. Keep defaults safe, use environment-specific values files only for non-secret configuration, and inject secrets through a managed secret solution rather than committing them to Git. Pin image tags to immutable digests or unique build versions; avoid `latest`.

The chart should eventually define resource requests and limits, startup/readiness/liveness probes, service configuration, and a controlled rolling update strategy. Validate rendered manifests and chart linting in CI before deploying to AWS EKS.
