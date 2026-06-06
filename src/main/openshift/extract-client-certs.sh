#!/bin/bash
set -e

NAMESPACE="swim-demo"
SECRET_NAME="consumer-validator-client-tls"
OUTPUT_DIR="/tmp"

echo "Extracting Consumer Validator mTLS client certificates..."
echo "Purpose: Enable testing Consumer Validator API endpoints with mutual TLS authentication"
echo ""

echo "Extracting CA certificate..."
oc get secret ${SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.ca\.crt}' | base64 -d > ${OUTPUT_DIR}/ca.crt
echo "✓ CA certificate: ${OUTPUT_DIR}/ca.crt"

echo "Extracting client certificate..."
oc get secret ${SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.tls\.crt}' | base64 -d > ${OUTPUT_DIR}/client.crt
echo "✓ Client certificate: ${OUTPUT_DIR}/client.crt"

echo "Extracting client private key..."
oc get secret ${SECRET_NAME} -n ${NAMESPACE} -o jsonpath='{.data.tls\.key}' | base64 -d > ${OUTPUT_DIR}/client.key
echo "✓ Client key: ${OUTPUT_DIR}/client.key"

echo ""
echo "Certificates extracted successfully!"

