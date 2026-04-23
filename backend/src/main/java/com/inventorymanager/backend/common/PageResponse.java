package com.inventorymanager.backend.common;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        long total,
        int totalPages,
        int currentPage
) {}
