#!/bin/bash

# setup-pipeline.sh
# Automates the setup of the Strict Auditing Pipeline for the project.

echo "🚀 Initializing Strict Auditing Pipeline..."

# 1. Setup MCP Server
echo "📦 Setting up Domain-Aware MCP Server..."
cd tools/mcp-server
npm install
npm run build
MCP_SERVER_PATH=$(pwd)/dist/index.js
cd ../..

# 2. Configure Git Hooks
echo "🔗 Linking Git Hooks..."
ln -sf ../../tools/hooks/pre-commit .git/hooks/pre-commit
ln -sf ../../tools/hooks/pre-push .git/hooks/pre-push
chmod +x tools/hooks/pre-commit
chmod +x tools/hooks/pre-push

# 3. Register MCP Server with Gemini CLI
# Note: This assumes 'gemini' is in the PATH.
echo \"🤖 Registering MCP Server with Gemini CLI...\"
gemini mcp add inventory-manager-auditor node $MCP_SERVER_PATH --include-tools verify_rbac_boundary,analyze_test_gaps,audit_javers_compliance,check_ui_style

echo "✅ Pipeline setup complete."
echo ""
echo "📝 FINAL STEP: To enforce token savings, please add the Caveman and RTK guidelines"
echo "to your global gemini config (system_instruction field)."
echo ""
echo "Caveman: 'Respond terse like smart caveman. Drop articles/filler. Technical terms exact.'"
echo "RTK: 'Always use rtk gain to track savings and prefix commands with rtk where possible.'"
