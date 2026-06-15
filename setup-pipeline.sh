#!/bin/bash

# setup-pipeline.sh
# Prerequisites for the Java backend pipeline.

echo "🚀 Initializing project pipeline..."

# Make pre-push hook executable
chmod +x tools/hooks/pre-push

echo "✅ Pipeline setup complete."
echo ""
echo "🛡️ Branch protection via CI checks in GitHub Actions."
echo "   Local pre-push hook runs Maven compile + backend tests."
