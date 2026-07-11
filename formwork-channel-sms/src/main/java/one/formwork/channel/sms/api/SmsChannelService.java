package one.formwork.channel.sms.api;

import one.formwork.channel.sms.validation.PhoneNumberValidator;
import one.formwork.channel.sms.cost.SmsCostService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SmsChannelService {

    private final List<SmsGateway> gateways;
    private final SmsChannelProperties properties;
    private final SmsCostService costService;
    private final TenantProviderRegistry tenantProviderRegistry;

    public SmsChannelService(List<SmsGateway> gateways,
                             SmsChannelProperties properties,
                             SmsCostService costService,
                             TenantProviderRegistry tenantProviderRegistry) {
        this.gateways = gateways;
        this.properties = properties;
        this.costService = costService;
        this.tenantProviderRegistry = tenantProviderRegistry;
    }

    public SmsResult sendSms(SmsMessage message) {
        PhoneNumberValidator.validate(message.to());
        SmsResult result = sendWithFallback(message);
        if (result.isSuccess()) {
            costService.recordCost(message.tenantId(), message.to(), result);
        }
        return result;
    }

    public List<SmsResult> sendBulk(List<SmsMessage> messages) {
        return messages.stream().map(this::sendSms).toList();
    }

    public void handleDeliveryCallback(String provider, Map<String, String> params) {
        // Provider-specific callback handling
    }

    private SmsResult sendWithFallback(SmsMessage message) {
        List<String> providerOrder = List.of(
                tenantProviderRegistry.resolveProvider(message.tenantId(), properties.getProvider()),
                properties.getFallbackProvider()
        );
        IllegalStateException lastMissingGateway = null;
        for (String providerType : providerOrder) {
            if (providerType == null || providerType.isBlank()) {
                continue;
            }
            SmsGateway gateway = resolveGateway(providerType);
            if (gateway == null) {
                lastMissingGateway = new IllegalStateException("No SmsGateway for provider: " + providerType);
                continue;
            }
            SmsResult result = sendWithRetry(gateway, message);
            if (result.isSuccess()) {
                return result;
            }
            if (!isRetryable(result)) {
                return result;
            }
        }
        if (lastMissingGateway != null && providerOrder.stream().allMatch(p -> resolveGateway(p) == null)) {
            throw lastMissingGateway;
        }
        return SmsResult.failure(providerOrder.get(0), "FAILOVER_EXHAUSTED", "All configured providers failed");
    }

    private SmsResult sendWithRetry(SmsGateway gateway, SmsMessage message) {
        int attempts = Math.max(properties.getRetry().getMaxAttempts(), 1);
        Duration backoff = parseBackoff(properties.getRetry().getBackoff());
        SmsResult last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            last = gateway.send(message);
            if (last.isSuccess() || !isRetryable(last) || attempt == attempts) {
                return last;
            }
            sleepQuietly(backoff.multipliedBy(attempt));
        }
        return last;
    }

    private SmsGateway resolveGateway(String providerType) {
        return gateways.stream()
                .filter(g -> g.supports(providerType))
                .findFirst()
                .orElse(null);
    }

    private static boolean isRetryable(SmsResult result) {
        if (result == null || result.isSuccess()) {
            return false;
        }
        String code = result.errorCode();
        if (code == null) {
            return true;
        }
        if ("SEND_ERROR".equalsIgnoreCase(code) || "TIMEOUT".equalsIgnoreCase(code)) {
            return true;
        }
        try {
            int status = Integer.parseInt(code);
            return status >= 500;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static Duration parseBackoff(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ofSeconds(1);
        }
        String trimmed = value.trim().toLowerCase();
        if (trimmed.endsWith("ms")) {
            return Duration.ofMillis(Long.parseLong(trimmed.substring(0, trimmed.length() - 2)));
        }
        if (trimmed.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        if (trimmed.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(trimmed.substring(0, trimmed.length() - 1)));
        }
        return Duration.ofSeconds(Long.parseLong(trimmed));
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(Math.min(duration.toMillis(), 100));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
