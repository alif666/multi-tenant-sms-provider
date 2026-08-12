package one.formwork.channel.sms.demo;

import one.formwork.channel.sms.api.SmsChannelProperties;
import one.formwork.channel.sms.api.SmsGateway;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/demo")
@Profile("local-observe")
public class LocalSmsDiagnosticsController {

    private final List<SmsGateway> gateways;
    private final SmsChannelProperties properties;

    public LocalSmsDiagnosticsController(List<SmsGateway> gateways, SmsChannelProperties properties) {
        this.gateways = gateways;
        this.properties = properties;
    }

    @GetMapping("/gateways")
    public Map<String, Object> gateways() {
        return Map.of(
                "configuredPrimary", properties.getProvider(),
                "configuredFallback", properties.getFallbackProvider(),
                "runtimeGateways", gateways.stream().map(SmsGateway::getProviderName).toList(),
                "message", "This read-only profile shows the real Spring gateway beans. No SMS send endpoint is enabled."
        );
    }
}
