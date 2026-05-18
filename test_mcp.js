const { spawn } = require('child_process');

const server = spawn('node', ['tools/mcp-server/dist/index.js']);
let id = 1;

function callTool(name, args) {
  const req = {
    jsonrpc: "2.0",
    id: id++,
    method: "tools/call",
    params: { name, arguments: args }
  };
  server.stdin.write(JSON.stringify(req) + '\n');
}

server.stdout.on('data', (data) => {
  console.log(`STDOUT: ${data.toString()}`);
});

server.stderr.on('data', (data) => {
  console.error(`STDERR: ${data.toString()}`);
});

setTimeout(() => {
  // Initialize MCP protocol
  const initReq = {
    jsonrpc: "2.0",
    id: id++,
    method: "initialize",
    params: {
      protocolVersion: "2024-11-05",
      capabilities: {},
      clientInfo: { name: "test", version: "1.0.0" }
    }
  };
  server.stdin.write(JSON.stringify(initReq) + '\n');
  
  setTimeout(() => {
    server.stdin.write(JSON.stringify({ jsonrpc: "2.0", method: "notifications/initialized" }) + '\n');
    callTool('verify_rbac_boundary', { controller_path: 'backend/src/main/java/com/inventorymanager/backend/web/TestController.java' });
    callTool('verify_rbac_boundary', { controller_path: 'backend/src/main/java/com/inventorymanager/backend/audit/AuditController.java' });
    callTool('verify_rbac_boundary', { controller_path: 'backend/src/main/java/com/inventorymanager/backend/web/BranchController.java' });
    callTool('analyze_test_gaps', { module_name: 'backend' });
    callTool('audit_javers_compliance', { entity_path: 'backend/src/main/java/com/inventorymanager/backend/domain/Branch.java' });
    callTool('check_ui_style', { fxml_or_java_path: 'frontend/src/main/java/com/inventorymanager/frontend/ui/views/AuditView.java' });
    callTool('check_ui_style', { fxml_or_java_path: 'frontend/src/main/java/com/inventorymanager/frontend/ui/views/FormView.java' });
    callTool('check_ui_style', { fxml_or_java_path: 'frontend/src/main/java/com/inventorymanager/frontend/ui/views/ResourceView.java' });
    setTimeout(() => process.exit(0), 2000);
  }, 1000);
}, 500);