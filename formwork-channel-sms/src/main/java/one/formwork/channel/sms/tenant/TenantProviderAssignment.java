package one.formwork.channel.sms.tenant;

import one.formwork.channel.sms.auth.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_provider_assignment")
public class TenantProviderAssignment {
    @Id @Column(name = "tenant_id") private UUID tenantId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId
    @JoinColumn(name = "tenant_id") private Tenant tenant;
    @Column(nullable = false, length = 50) private String provider;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by") private AppUser assignedBy;
    @Column(name = "assigned_at", nullable = false) private Instant assignedAt = Instant.now();
    protected TenantProviderAssignment() {}
    public TenantProviderAssignment(Tenant tenant, String provider, AppUser assignedBy) {
        this.tenant = tenant; this.tenantId = tenant.getId(); this.provider = provider; this.assignedBy = assignedBy;
    }
    public UUID getTenantId() { return tenantId; }
    public String getProvider() { return provider; }
}
