# SWIM DNOTAM Consumer Validator, Raw YAML Deployment

## Prerequisites

- `oc` CLI authenticated to an OpenShift cluster
- Namespace `swim-demo` exists
- cert-manager installed with `swim-ca-issuer` ClusterIssuer
- AMQP broker (Artemis) deployed and accessible in `swim-demo`

## Option A: Deploy with Kustomize

The simplest approach, `kustomization.yaml` handles ordering and labels:

```bash
oc apply -k . -n swim-demo
```

Note: Certificates are commented out in `kustomization.yaml` by default.
If the certificates do not exist yet, uncomment them first or apply manually (see step 1 below).

## Option B: Deploy Manually

Apply the manifests in this exact order:

```bash
# 1. Certificates (skip if they already exist in the cluster)
oc apply -f consumer-validator-server-cert.yaml -n swim-demo
oc apply -f consumer-validator-client-certificate.yaml -n swim-demo

# 2. ConfigMap (application configuration, must exist before the Deployment)
oc apply -f consumer-validator-config.yaml -n swim-demo

# 3. AMQP Credentials (must exist before the Deployment)
oc apply -f consumer-validator-amqp-credentials.yaml -n swim-demo

# 4. Service (must exist before Routes)
oc apply -f consumer-validator-service.yaml -n swim-demo

# 5. Deployment (depends on ConfigMap, AMQP credentials, and TLS secrets from cert-manager)
oc apply -f deployment.yaml -n swim-demo

# 6. Routes (depend on the Service)
oc apply -f consumer-validator-route-http.yaml -n swim-demo
oc apply -f consumer-validator-route.yaml -n swim-demo
```

## Extracting Client Certificates

To test mTLS endpoints, extract the client certificates:

```bash
./extract-client-certs.sh
```

This writes `ca.crt`, `client.crt`, and `client.key` to `/tmp/`.

## Teardown

```bash
# Kustomize
oc delete -k . -n swim-demo

# Or manually, in reverse order:
oc delete -f consumer-validator-route.yaml -n swim-demo
oc delete -f consumer-validator-route-http.yaml -n swim-demo
oc delete -f deployment.yaml -n swim-demo
oc delete -f consumer-validator-service.yaml -n swim-demo
oc delete -f consumer-validator-amqp-credentials.yaml -n swim-demo
oc delete -f consumer-validator-config.yaml -n swim-demo
oc delete -f consumer-validator-client-certificate.yaml -n swim-demo
oc delete -f consumer-validator-server-cert.yaml -n swim-demo
```
