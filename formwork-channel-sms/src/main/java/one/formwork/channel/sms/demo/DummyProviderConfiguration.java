package one.formwork.channel.sms.demo;

import one.formwork.channel.sms.api.SmsGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dummy-profile")
public class DummyProviderConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "formwork.sms-channel", name = "demo-enabled", havingValue = "true")
    public DummyProviderState dummyProviderState() {
        return new DummyProviderState();
    }

    @Bean
    public SmsGateway dummyTwilioSmsGateway(DummyProviderState state) {
        return new DummySmsGateway("TWILIO", state);
    }

    @Bean
    public SmsGateway dummyVonageSmsGateway(DummyProviderState state) {
        return new DummySmsGateway("VONAGE", state);
    }

    @Bean
    public SmsGateway dummyAwsSnsSmsGateway(DummyProviderState state) {
        return new DummySmsGateway("AWS_SNS", state);
    }

    @Bean
    public SmsGateway dummyBudgetSmsGateway(DummyProviderState state) {
        return new DummySmsGateway("BUDGET_SMS", state);
    }

    @Bean
    public SmsGateway dummyMessageBirdSmsGateway(DummyProviderState state) {
        return new DummySmsGateway("MESSAGEBIRD", state);
    }
}
