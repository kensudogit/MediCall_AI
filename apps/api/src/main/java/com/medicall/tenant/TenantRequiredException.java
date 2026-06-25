package com.medicall.tenant;

public class TenantRequiredException extends RuntimeException {
    public TenantRequiredException(String message) {
        super(message);
    }
}
