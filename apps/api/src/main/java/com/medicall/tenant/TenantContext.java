package com.medicall.tenant;

import com.medicall.domain.Tenant;
import java.util.Optional;

public final class TenantContext {

    private static final ThreadLocal<Tenant> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(Tenant tenant) {
        CURRENT.set(tenant);
    }

    public static Optional<Tenant> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Long requireTenantId() {
        return get().map(Tenant::getId)
                .orElseThrow(() -> new TenantRequiredException("テナントが指定されていません"));
    }

    public static String requireSlug() {
        return get().map(Tenant::getSlug)
                .orElseThrow(() -> new TenantRequiredException("テナントが指定されていません"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}
