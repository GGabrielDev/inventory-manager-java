package com.inventorymanager.backend.web;

import com.inventorymanager.backend.common.PageResponse;
import org.springframework.data.domain.Page;

public final class PageUtil {
    private PageUtil() {}

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() + 1
        );
    }
}
