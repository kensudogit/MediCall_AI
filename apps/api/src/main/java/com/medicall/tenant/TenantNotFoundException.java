package com.medicall.tenant;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String slug) {
        super("テナントが見つかりません: " + slug);
    }
}
