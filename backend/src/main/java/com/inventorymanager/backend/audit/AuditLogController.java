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

    @GetMapping({"/{entityName}", "/{entityName}/{id}"})
    @PreAuthorize("hasAuthority('get_audit_logs')")
    public AuditService.PageResponse getAuditTrail(
            @PathVariable String entityName,
            @PathVariable(required = false) Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 10;
        if (pageSize > 100) pageSize = 100;
        return auditService.auditTrail(entityName, id, page - 1, pageSize);
    }
}
