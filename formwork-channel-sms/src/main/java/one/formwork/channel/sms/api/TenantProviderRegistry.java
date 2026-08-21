package one.formwork.channel.sms.api;

import one.formwork.channel.sms.auth.AppUser;
import one.formwork.channel.sms.tenant.Tenant;
import one.formwork.channel.sms.tenant.TenantProviderAssignment;
import one.formwork.channel.sms.tenant.TenantProviderAssignmentRepository;
import one.formwork.channel.sms.tenant.TenantRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class TenantProviderRegistry {

    public static final Set<String> SUPPORTED_PROVIDERS = Set.of(
            "TWILIO", "VONAGE", "AWS_SNS", "BUDGET_SMS", "MESSAGEBIRD");

    private final TenantRepository tenantRepository;
    private final TenantProviderAssignmentRepository assignmentRepository;

    public TenantProviderRegistry(TenantRepository tenantRepository,
                                  TenantProviderAssignmentRepository assignmentRepository) {
        this.tenantRepository = tenantRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional
    public void assignProvider(UUID tenantId, String provider) {
        assignProvider(tenantId, provider, null);
    }

    @Transactional
    public void assignProvider(UUID tenantId, String provider, AppUser assignedBy) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
        String normalized = normalizeProvider(provider);
        if (normalized == null) {
            assignmentRepository.deleteById(tenantId);
            return;
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseGet(() -> tenantRepository.save(new Tenant(tenantId, tenantId.toString())));
        assignmentRepository.save(new TenantProviderAssignment(tenant, normalized, assignedBy));
    }

    @Transactional(readOnly = true)
    public String resolveProvider(UUID tenantId, String defaultProvider) {
        if (tenantId == null) {
            return defaultProvider;
        }
        return assignmentRepository.findById(tenantId)
                .map(TenantProviderAssignment::getProvider)
                .orElse(defaultProvider);
    }

    public static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported SMS provider: " + provider);
        }
        return normalized;
    }
}
