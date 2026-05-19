package com.inventorymanager.backend.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEntryResponse(
        String operation,
        String changedBy,
        String changedByUsername,
        java.time.Instant changedAt,
        java.util.Map<String, Object> state,
        java.util.Map<String, Object> previousState
) {}
