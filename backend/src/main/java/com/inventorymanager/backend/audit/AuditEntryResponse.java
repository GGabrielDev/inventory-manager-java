package com.inventorymanager.backend.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEntryResponse(
        String operation,
        String changedBy,
        Instant changedAt,
        Map<String, Object> state
) {}
