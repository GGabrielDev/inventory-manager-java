#!/bin/bash

# setup-pipeline.sh
# Automates local prerequisites for the strict auditing pipeline.

echo "🚀 Initializing Strict Auditing Pipeline..."

# 1. Setup MCP Server
echo "📦 Setting up Domain-Aware MCP Server..."
cd tools/mcp-server
npm install
npm run build
MCP_SERVER_PATH=$(pwd)/dist/index.js
cd ../..

# 2. Register MCP Server with Gemini CLI
# Note: This assumes 'gemini' is in the PATH.
echo \"🤖 Registering MCP Server with Gemini CLI...\"
gemini mcp add inventory-manager-auditor node $MCP_SERVER_PATH --include-tools verify_rbac_boundary,analyze_test_gaps,audit_javers_compliance,check_ui_style

# 3. Enable local passive post-commit guards (non-blocking)
echo "🔗 Linking local passive post-commit guard hook..."
ln -sf ../../tools/hooks/post-commit .git/hooks/post-commit
echo "🔗 Linking local pre-push Java checks hook..."
ln -sf ../../tools/hooks/pre-push .git/hooks/pre-push
chmod +x tools/hooks/post-commit
chmod +x tools/hooks/pre-push
chmod +x tools/hooks/run-passive-guards.sh
chmod +x tools/hooks/run-copilot-recursive-guards.sh

echo "✅ Pipeline setup complete."
echo ""
echo "🛡️ Enforcement now uses local pre-push Copilot guards + required CI in GitHub Actions."
echo "Set branch protection and require checks:"
echo "  - CI / Build and Unit Tests"
echo "Optional manual remote audit:"
echo "  - Run Gemini Hard Guard workflow_dispatch when needed"
echo ""
echo "💡 Local passive checks run after each commit and write reports to:"
echo "  .gemini/local-guards/latest-summary.md"
echo "🧪 Local pre-push hook runs Maven compile + backend tests + recursive headless Copilot guards."
echo "   Copilot guard compares against master merge-base."
echo "📄 Pre-push Copilot summary:"
echo "  .copilot/recursive-guards/latest-summary.md"
echo ""
