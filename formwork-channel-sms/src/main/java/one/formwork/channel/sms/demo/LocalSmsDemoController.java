package one.formwork.channel.sms.demo;

import one.formwork.channel.sms.api.SmsChannelProperties;
import one.formwork.channel.sms.api.SmsChannelService;
import one.formwork.channel.sms.api.SmsGateway;
import one.formwork.channel.sms.api.SmsMessage;
import one.formwork.channel.sms.api.SmsResult;
import one.formwork.channel.sms.cost.SmsCostService;
import one.formwork.channel.sms.api.TenantProviderRegistry;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/demo")
@Profile("dummy-profile")
public class LocalSmsDemoController {

    private final List<SmsGateway> gateways;
    private final SmsChannelService smsChannelService;
    private final SmsCostService smsCostService;
    private final TenantProviderRegistry tenantProviderRegistry;
    private final DummyProviderState demoState;
    private final SmsChannelProperties properties;

    public LocalSmsDemoController(List<SmsGateway> gateways,
                                  SmsChannelService smsChannelService,
                                  SmsCostService smsCostService,
                                  TenantProviderRegistry tenantProviderRegistry,
                                  DummyProviderState demoState,
                                  SmsChannelProperties properties) {
        this.gateways = gateways;
        this.smsChannelService = smsChannelService;
        this.smsCostService = smsCostService;
        this.tenantProviderRegistry = tenantProviderRegistry;
        this.demoState = demoState;
        this.properties = properties;
    }

    @GetMapping("/gateways")
    public Map<String, Object> gateways() {
        return Map.of(
                "configuredPrimary", properties.getProvider(),
                "configuredFallback", properties.getFallbackProvider(),
                "runtimeGateways", gateways.stream().map(SmsGateway::getProviderName).toList(),
                "message", "This list contains Spring beans, not every gateway source file"
        );
    }

    @PostMapping("/tenants/{tenantId}/provider")
    public Map<String, Object> assignProvider(@PathVariable UUID tenantId,
                                               @RequestBody ProviderAssignmentRequest request) {
        tenantProviderRegistry.assignProvider(tenantId, request.provider());
        return Map.of("tenantId", tenantId, "provider", request.provider());
    }

    @PostMapping("/messages")
    public ResponseEntity<?> send(@RequestBody SendMessageRequest request) {
        try {
            SmsResult result = smsChannelService.sendSms(new SmsMessage(
                    request.to(), request.body(), request.tenantId(), request.referenceId(), Map.of()));
            return ResponseEntity.ok(result);
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", exception.getClass().getSimpleName(),
                    "message", String.valueOf(exception.getMessage())
            ));
        }
    }

    @PostMapping("/providers/{provider}/behavior")
    public Map<String, Object> configureBehavior(@PathVariable String provider,
                                                  @RequestBody BehaviorRequest request) {
        demoState.configure(provider, request.mode(), request.failuresBeforeSuccess(), request.segmentCount());
        return behavior(provider);
    }

    @GetMapping("/providers/{provider}/behavior")
    public Map<String, Object> behavior(@PathVariable String provider) {
        DummyProviderState.Behavior behavior = demoState.behavior(provider);
        return Map.of(
                "provider", provider.toUpperCase(),
                "mode", behavior.mode(),
                "failuresBeforeSuccess", behavior.failuresBeforeSuccess(),
                "segmentCount", behavior.segmentCount(),
                "attempts", demoState.attempts(provider)
        );
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        demoState.reset();
        return Map.of("status", "reset");
    }

    @GetMapping("/costs/{tenantId}")
    public Map<String, Object> costs(@PathVariable UUID tenantId) {
        YearMonth month = YearMonth.now(ZoneOffset.UTC);
        Instant from = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return Map.of(
                "tenantId", tenantId,
                "month", month.toString(),
                "totalCost", smsCostService.getMonthlyCost(tenantId, month),
                "messageCount", smsCostService.getSmsCount(tenantId, from, to)
        );
    }

    public record ProviderAssignmentRequest(String provider) {}

    public record SendMessageRequest(UUID tenantId, String to, String body, String referenceId) {}

    public record BehaviorRequest(String mode, int failuresBeforeSuccess, int segmentCount) {}
}
