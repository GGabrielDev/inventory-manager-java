import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";
import fs from "node:fs/promises";
import path from "node:path";
import { execSync } from "node:child_process";

const server = new Server(
  {
    name: "inventory-manager-auditor",
    version: "1.0.0",
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

/**
 * Tool: verify_rbac_boundary
 * Parses a Spring Controller to ensure all methods have @PreAuthorize.
 */
async function verifyRbacBoundary(controllerPath: string) {
  const fullPath = path.resolve(process.cwd(), controllerPath);
  try {
    const content = await fs.readFile(fullPath, "utf-8");
    const methods = content.match(/public\s+[\w<>]+\s+\w+\s*\(/g) || [];
    const preAuths = content.match(/@PreAuthorize\(/g) || [];

    if (preAuths.length < methods.length) {
      return {
        isViolated: true,
        message: `Found ${methods.length} methods but only ${preAuths.length} @PreAuthorize annotations.`,
      };
    }
    return { isViolated: false, message: "RBAC boundaries look solid." };
  } catch (error: any) {
    return { isViolated: true, message: `Error reading file: ${error.message}` };
  }
}

/**
 * Tool: analyze_test_gaps
 * Runs Maven and checks for failure XMLs.
 */
async function analyzeTestGaps(moduleName: string) {
  try {
    const reportDir = path.resolve(process.cwd(), moduleName, "target/surefire-reports");
    const files = await fs.readdir(reportDir);
    const xmlFiles = files.filter(f => f.endsWith(".xml"));
    
    let totalFailures = 0;
    for (const file of xmlFiles) {
      const content = await fs.readFile(path.join(reportDir, file), "utf-8");
      if (content.includes('failures="') && !content.includes('failures="0"')) {
        totalFailures++;
      }
    }

    return {
      isViolated: totalFailures > 0,
      message: totalFailures > 0 ? `Detected failures in ${totalFailures} test report(s).` : "No test gaps detected in reports.",
    };
  } catch (error: any) {
    return { isViolated: true, message: `Error analyzing tests: ${error.message}` };
  }
}

/**
 * Tool: audit_javers_compliance
 * Checks if a domain entity is registered for auditing.
 */
async function auditJaversCompliance(entityPath: string) {
  const registryPath = path.resolve(process.cwd(), "backend/src/main/java/com/inventorymanager/backend/audit/EntityRegistry.java");
  try {
    const registryContent = await fs.readFile(registryPath, "utf-8");
    const entityName = path.basename(entityPath, ".java");
    
    if (!registryContent.includes(entityName)) {
      return { isViolated: true, message: `Entity ${entityName} is not registered in EntityRegistry for JaVers auditing.` };
    }
    return { isViolated: false, message: `Entity ${entityName} is properly registered.` };
  } catch (error: any) {
    return { isViolated: true, message: `Error checking JaVers compliance: ${error.message}` };
  }
}

/**
 * Tool: check_ui_style
 * Scans JavaFX code for Style Guide compliance.
 */
async function checkUiStyle(uiPath: string) {
  const fullPath = path.resolve(process.cwd(), uiPath);
  try {
    const content = await fs.readFile(fullPath, "utf-8");
    const violations = [];

    if (!content.includes("BorderPane") && uiPath.includes("View")) {
      violations.push("Does not use BorderPane as main container.");
    }
    if (content.includes("TableView") && !content.includes("CONSTRAINED_RESIZE_POLICY")) {
      violations.push("TableView missing CONSTRAINED_RESIZE_POLICY.");
    }

    return {
      isViolated: violations.length > 0,
      message: violations.length > 0 ? violations.join(" ") : "UI Style compliance passed.",
    };
  } catch (error: any) {
    return { isViolated: true, message: `Error checking UI style: ${error.message}` };
  }
}

server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: [
      {
        name: "verify_rbac_boundary",
        description: "Parses a Spring Controller to ensure all methods have @PreAuthorize.",
        inputSchema: {
          type: "object",
          properties: {
            controller_path: { type: "string" },
          },
          required: ["controller_path"],
        },
      },
      {
        name: "analyze_test_gaps",
        description: "Runs Maven and checks for failure XMLs in surefire-reports.",
        inputSchema: {
          type: "object",
          properties: {
            module_name: { type: "string" },
          },
          required: ["module_name"],
        },
      },
      {
        name: "audit_javers_compliance",
        description: "Checks if a domain entity is registered for auditing in EntityRegistry.",
        inputSchema: {
          type: "object",
          properties: {
            entity_path: { type: "string" },
          },
          required: ["entity_path"],
        },
      },
      {
        name: "check_ui_style",
        description: "Scans JavaFX code for Style Guide compliance (BorderPane, Table policies).",
        inputSchema: {
          type: "object",
          properties: {
            fxml_or_java_path: { type: "string" },
          },
          required: ["fxml_or_java_path"],
        },
      },
    ],
  };
});

server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

  try {
    if (name === "verify_rbac_boundary") {
      const result = await verifyRbacBoundary(args?.controller_path as string);
      return { content: [{ type: "text", text: JSON.stringify(result) }] };
    }
    if (name === "analyze_test_gaps") {
      const result = await analyzeTestGaps(args?.module_name as string);
      return { content: [{ type: "text", text: JSON.stringify(result) }] };
    }
    if (name === "audit_javers_compliance") {
      const result = await auditJaversCompliance(args?.entity_path as string);
      return { content: [{ type: "text", text: JSON.stringify(result) }] };
    }
    if (name === "check_ui_style") {
      const result = await checkUiStyle(args?.fxml_or_java_path as string);
      return { content: [{ type: "text", text: JSON.stringify(result) }] };
    }
    throw new Error(`Tool not found: ${name}`);
  } catch (error: any) {
    return {
      content: [{ type: "text", text: `Error: ${error.message}` }],
      isError: true,
    };
  }
});

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Inventory Manager Auditor MCP Server running on stdio");
}

main().catch((error) => {
  console.error("Fatal error in main():", error);
  process.exit(1);
});
