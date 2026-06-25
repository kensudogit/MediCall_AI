package com.medicall.tenant;

import com.medicall.service.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantResolverFilter extends OncePerRequestFilter {

    private final TenantService tenantService;
    private final String defaultSlug;

    public TenantResolverFilter(TenantService tenantService,
                                @Value("${medicall.tenant.default-slug:demo}") String defaultSlug) {
        this.tenantService = tenantService;
        this.defaultSlug = defaultSlug;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPlatformTenantAdminPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String slug = request.getHeader("X-Tenant-Slug");
        if (slug == null || slug.isBlank()) {
            slug = defaultSlug;
        }

        try {
            tenantService.setContext(slug);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isPlatformTenantAdminPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/admin/tenants")
                || (path.startsWith("/api/admin/tenants/") && "GET".equals(request.getMethod()));
    }
}
