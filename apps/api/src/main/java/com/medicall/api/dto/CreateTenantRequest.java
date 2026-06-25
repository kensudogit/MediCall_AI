package com.medicall.api.dto;

public record CreateTenantRequest(
        String slug,
        String name,
        String plan,
        String hoursText,
        String holidaysText,
        String accessText,
        String belongingsText
) {}
