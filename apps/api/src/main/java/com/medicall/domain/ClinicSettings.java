package com.medicall.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "clinic_settings")
public class ClinicSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "clinic_name", nullable = false)
    private String clinicName;

    @Column(name = "hours_text", nullable = false, columnDefinition = "TEXT")
    private String hoursText;

    @Column(name = "holidays_text", nullable = false, columnDefinition = "TEXT")
    private String holidaysText;

    @Column(name = "access_text", nullable = false, columnDefinition = "TEXT")
    private String accessText;

    @Column(name = "belongings_text", nullable = false, columnDefinition = "TEXT")
    private String belongingsText;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getClinicName() { return clinicName; }
    public void setClinicName(String clinicName) { this.clinicName = clinicName; }
    public String getHoursText() { return hoursText; }
    public void setHoursText(String hoursText) { this.hoursText = hoursText; }
    public String getHolidaysText() { return holidaysText; }
    public void setHolidaysText(String holidaysText) { this.holidaysText = holidaysText; }
    public String getAccessText() { return accessText; }
    public void setAccessText(String accessText) { this.accessText = accessText; }
    public String getBelongingsText() { return belongingsText; }
    public void setBelongingsText(String belongingsText) { this.belongingsText = belongingsText; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
