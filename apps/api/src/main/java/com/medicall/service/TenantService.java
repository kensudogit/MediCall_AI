package com.medicall.service;

import com.medicall.api.dto.CreateTenantRequest;
import com.medicall.domain.ClinicSettings;
import com.medicall.domain.Tenant;
import com.medicall.repository.ClinicSettingsRepository;
import com.medicall.repository.TenantRepository;
import com.medicall.tenant.TenantContext;
import com.medicall.tenant.TenantNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ClinicSettingsRepository clinicSettingsRepository;
    private final String defaultSlug;

    public TenantService(TenantRepository tenantRepository,
                         ClinicSettingsRepository clinicSettingsRepository,
                         @Value("${medicall.tenant.default-slug:demo}") String defaultSlug) {
        this.tenantRepository = tenantRepository;
        this.clinicSettingsRepository = clinicSettingsRepository;
        this.defaultSlug = defaultSlug;
    }

    public Tenant resolveBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            slug = defaultSlug;
        }
        String normalized = slug.trim().toLowerCase(Locale.ROOT);
        Tenant tenant = tenantRepository.findBySlug(normalized)
                .orElseThrow(() -> new TenantNotFoundException(normalized));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new IllegalStateException("テナントは利用停止中です: " + normalized);
        }
        return tenant;
    }

    public void setContext(String slug) {
        TenantContext.set(resolveBySlug(slug));
    }

    public void setContext(Tenant tenant) {
        TenantContext.set(tenant);
    }

    public void setContextById(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("id:" + tenantId));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new IllegalStateException("テナントは利用停止中です");
        }
        TenantContext.set(tenant);
    }

    public <T> T executeWithTenant(String slug, Supplier<T> action) {
        Tenant previous = TenantContext.get().orElse(null);
        setContext(slug);
        try {
            return action.get();
        } finally {
            TenantContext.clear();
            if (previous != null) {
                TenantContext.set(previous);
            }
        }
    }

    public <T> T executeWithTenantId(Long tenantId, Supplier<T> action) {
        Tenant previous = TenantContext.get().orElse(null);
        setContextById(tenantId);
        try {
            return action.get();
        } finally {
            TenantContext.clear();
            if (previous != null) {
                TenantContext.set(previous);
            }
        }
    }

    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    public Tenant current() {
        return TenantContext.get().orElseGet(() -> resolveBySlug(defaultSlug));
    }

    @Transactional
    public Tenant create(CreateTenantRequest req) {
        String slug = req.slug().trim().toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")) {
            throw new IllegalArgumentException("スラッグは英小文字・数字・ハイフンのみ（3〜63文字）");
        }
        if (tenantRepository.existsBySlug(slug)) {
            throw new IllegalArgumentException("スラッグは既に使用されています: " + slug);
        }

        Tenant tenant = new Tenant();
        tenant.setSlug(slug);
        tenant.setName(req.name().trim());
        tenant.setPlan(req.plan() != null ? req.plan() : "TRIAL");
        tenant.setStatus("ACTIVE");
        tenant = tenantRepository.save(tenant);

        ClinicSettings settings = new ClinicSettings();
        settings.setTenantId(tenant.getId());
        settings.setClinicName(tenant.getName());
        settings.setHoursText(req.hoursText() != null ? req.hoursText() : "平日 9:00〜18:00");
        settings.setHolidaysText(req.holidaysText() != null ? req.holidaysText() : "土日祝休診");
        settings.setAccessText(req.accessText() != null ? req.accessText() : "お問い合わせください");
        settings.setBelongingsText(req.belongingsText() != null ? req.belongingsText() : "保険証をご持参ください");
        clinicSettingsRepository.save(settings);

        return tenant;
    }
}
