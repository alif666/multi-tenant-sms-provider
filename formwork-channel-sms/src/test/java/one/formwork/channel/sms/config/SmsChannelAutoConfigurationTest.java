package one.formwork.channel.sms.config;

import one.formwork.channel.sms.api.SmsChannelProperties;
import one.formwork.channel.sms.api.SmsGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmsChannelAutoConfigurationTest {
    @Test
    void createsEveryBuiltInProviderRegardlessOfDefault() {
        SmsChannelProperties properties = new SmsChannelProperties();
        properties.setProvider("VONAGE");
        SmsChannelAutoConfiguration configuration = new SmsChannelAutoConfiguration();
        List<SmsGateway> gateways = List.of(
                configuration.twilioGateway(properties),
                configuration.vonageGateway(properties),
                configuration.awsSnsGateway(properties),
                configuration.budgetSmsGateway(properties),
                configuration.messageBirdGateway(properties));

        assertEquals(5, gateways.size());
        assertTrue(gateways.stream().anyMatch(gateway -> gateway.supports("TWILIO")));
        assertTrue(gateways.stream().anyMatch(gateway -> gateway.supports("VONAGE")));
        assertTrue(gateways.stream().anyMatch(gateway -> gateway.supports("AWS_SNS")));
        assertTrue(gateways.stream().anyMatch(gateway -> gateway.supports("BUDGET_SMS")));
        assertTrue(gateways.stream().anyMatch(gateway -> gateway.supports("MESSAGEBIRD")));
    }
}
