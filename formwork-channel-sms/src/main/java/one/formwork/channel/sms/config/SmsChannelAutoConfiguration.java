package one.formwork.channel.sms.config;

import one.formwork.channel.sms.api.*;
import one.formwork.channel.sms.provider.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@AutoConfiguration
@Profile("!dummy-profile")
@EnableConfigurationProperties(SmsChannelProperties.class)
public class SmsChannelAutoConfiguration {

    @Bean
    public SmsGateway twilioGateway(SmsChannelProperties props) {
        return new TwilioSmsGateway(props.getTwilio());
    }

    @Bean
    public SmsGateway vonageGateway(SmsChannelProperties props) {
        return new VonageSmsGateway(props.getVonage());
    }

    @Bean
    public SmsGateway awsSnsGateway(SmsChannelProperties props) {
        return new AwsSnsSmsGateway(props.getAwsSns());
    }

    @Bean
    public SmsGateway budgetSmsGateway(SmsChannelProperties props) {
        return new BudgetSmsGateway(props.getBudgetSms());
    }

    @Bean
    public SmsGateway messageBirdGateway(SmsChannelProperties props) {
        return new MessageBirdSmsGateway(props.getMessagebird());
    }

}
