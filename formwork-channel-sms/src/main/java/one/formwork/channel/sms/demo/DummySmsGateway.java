package one.formwork.channel.sms.demo;

import one.formwork.channel.sms.api.SmsGateway;
import one.formwork.channel.sms.api.SmsMessage;
import one.formwork.channel.sms.api.SmsResult;

public class DummySmsGateway implements SmsGateway {

    private final String providerName;
    private final DummyProviderState state;

    public DummySmsGateway(String providerName, DummyProviderState state) {
        this.providerName = providerName;
        this.state = state;
    }

    @Override
    public SmsResult send(SmsMessage message) {
        return state.send(providerName, message);
    }

    @Override
    public boolean supports(String providerType) {
        return providerName.equalsIgnoreCase(providerType);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }
}
