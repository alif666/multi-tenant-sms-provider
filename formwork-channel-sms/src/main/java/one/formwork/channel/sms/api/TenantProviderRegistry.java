package one.formwork.channel.sms.api;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class TenantProviderRegistry {

    private final ConcurrentMap<UUID, String> tenantProviders = new ConcurrentHashMap<>();

    public void assignProvider(UUID tenantId, String provider) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        if (provider == null || provider.isBlank()) {
            tenantProviders.remove(tenantId);
            return;
        }
        tenantProviders.put(tenantId, provider);
    }

    public String resolveProvider(UUID tenantId, String defaultProvider) {
        if (tenantId == null) {
            return defaultProvider;
        }
        return tenantProviders.getOrDefault(tenantId, defaultProvider);
    }
}
