package one.formwork.channel.sms.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "app_role")
public class AppRole {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    protected AppRole() {}
    public AppRole(UUID id, String name) { this.id = id; this.name = name; }
    public UUID getId() { return id; }
    public String getName() { return name; }
}
