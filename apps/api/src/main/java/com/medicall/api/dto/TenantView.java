package com.medicall.api.dto;

import java.time.Instant;

public record TenantView(
        Long id,
        String slug,
        String name,
        String status,
        String plan,
        Instant createdAt
) {}
