package one.formwork.channel.sms.demo;

import one.formwork.channel.sms.api.SmsMessage;
import one.formwork.channel.sms.api.SmsResult;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DummyProviderState {

    private static final List<String> SUPPORTED_PROVIDERS = List.of(
            "TWILIO", "VONAGE", "AWS_SNS", "BUDGET_SMS", "MESSAGEBIRD");

    private final Map<String, Behavior> behaviors = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    public DummyProviderState() {
        reset();
    }

    public void reset() {
        behaviors.clear();
        attempts.clear();
        for (String provider : SUPPORTED_PROVIDERS) {
            behaviors.put(provider, Behavior.success());
        }
    }

    public void configure(String provider, String mode, int failuresBeforeSuccess, int segmentCount) {
        String normalizedProvider = normalize(provider);
        String normalizedMode = normalize(mode);
        if (!SUPPORTED_PROVIDERS.contains(normalizedProvider)) {
            throw new IllegalArgumentException("Unknown dummy provider: " + provider);
        }
        if (!normalizedMode.equals("SUCCESS")
                && !normalizedMode.equals("TRANSIENT_FAILURE")
                && !normalizedMode.equals("PERMANENT_FAILURE")) {
            throw new IllegalArgumentException("mode must be SUCCESS, TRANSIENT_FAILURE, or PERMANENT_FAILURE");
        }
        if (failuresBeforeSuccess < 0 || segmentCount < 1) {
            throw new IllegalArgumentException("failuresBeforeSuccess must be >= 0 and segmentCount must be >= 1");
        }
        behaviors.put(normalizedProvider, new Behavior(normalizedMode, failuresBeforeSuccess, segmentCount));
        attempts.put(normalizedProvider, new AtomicInteger());
    }

    public SmsResult send(String provider, SmsMessage message) {
        String normalizedProvider = normalize(provider);
        Behavior behavior = behaviors.getOrDefault(normalizedProvider, Behavior.success());
        int attempt = attempts.computeIfAbsent(normalizedProvider, ignored -> new AtomicInteger())
                .incrementAndGet();

        if (behavior.mode().equals("PERMANENT_FAILURE")) {
            return SmsResult.failure(normalizedProvider, "400", "Dummy permanent failure");
        }
        if (behavior.mode().equals("TRANSIENT_FAILURE") && attempt <= behavior.failuresBeforeSuccess()) {
            return SmsResult.failure(normalizedProvider, "503", "Dummy transient failure, attempt " + attempt);
        }
        return SmsResult.success("dummy-" + UUID.randomUUID(), normalizedProvider, behavior.segmentCount());
    }

    public int attempts(String provider) {
        return attempts.getOrDefault(normalize(provider), new AtomicInteger()).get();
    }

    public Behavior behavior(String provider) {
        return behaviors.getOrDefault(normalize(provider), Behavior.success());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("provider and mode must not be blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record Behavior(String mode, int failuresBeforeSuccess, int segmentCount) {
        static Behavior success() {
            return new Behavior("SUCCESS", 0, 1);
        }
    }
}
