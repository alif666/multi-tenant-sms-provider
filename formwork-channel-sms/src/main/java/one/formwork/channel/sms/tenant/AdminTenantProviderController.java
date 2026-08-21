package one.formwork.channel.sms.tenant;

import one.formwork.channel.sms.api.TenantProviderRegistry;
import one.formwork.channel.sms.auth.AppUser;
import one.formwork.channel.sms.auth.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tenants")
public class AdminTenantProviderController {
    private final TenantProviderRegistry registry;
    private final TenantRepository tenants;
    private final AppUserRepository users;

    public AdminTenantProviderController(TenantProviderRegistry registry, TenantRepository tenants,
                                         AppUserRepository users) {
        this.registry = registry; this.tenants = tenants; this.users = users;
    }

    @PutMapping("/{tenantId}/provider")
    public ResponseEntity<?> assignProvider(@PathVariable UUID tenantId,
                                             @RequestBody ProviderAssignmentRequest request,
                                             Authentication authentication) {
        AppUser admin = users.findByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        try {
            if (request == null || request.provider() == null || request.provider().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "provider is required; use DELETE to clear it"));
            }
            String provider = TenantProviderRegistry.normalizeProvider(request.provider());
            registry.assignProvider(tenantId, provider, admin);
            return ResponseEntity.ok(Map.of("tenantId", tenantId, "provider", provider, "assignedBy", admin.getUsername()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @DeleteMapping("/{tenantId}/provider")
    public ResponseEntity<?> clearProvider(@PathVariable UUID tenantId) {
        registry.assignProvider(tenantId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<?> createTenant(@RequestBody TenantRequest request) {
        if (request.id() == null || request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "id and name are required"));
        }
        if (tenants.existsById(request.id())) {
            return ResponseEntity.status(409).body(Map.of("error", "tenant already exists"));
        }
        Tenant tenant = tenants.save(new Tenant(request.id(), request.name().trim()));
        return ResponseEntity.status(201).body(Map.of("id", tenant.getId(), "name", tenant.getName()));
    }

    public record ProviderAssignmentRequest(String provider) {}
    public record TenantRequest(UUID id, String name) {}
}
