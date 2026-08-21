package one.formwork.channel.sms.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant")
public class Tenant {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 255) private String name;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected Tenant() {}
    public Tenant(UUID id, String name) { this.id = id; this.name = name; }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
