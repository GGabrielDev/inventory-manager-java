package com.inventorymanager.backend.audit;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{entityName}/{id}")
    @PreAuthorize("hasAuthority('get_audit_logs')")
    public AuditService.PageResponse getAuditTrail(
            @PathVariable String entityName,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return auditService.auditTrail(entityName, id, Math.max(0, page - 1), pageSize);
    }
}
