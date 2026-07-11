# Review

1. **Critical** - `formwork-channel-sms/src/main/java/one/formwork/channel/sms/api/SmsChannelService.java:30-37`
   - The send path ignored `SmsMessage.tenantId()` entirely and always used the global provider from configuration. A tenant-specific routing decision could not exist, so one tenant could not be isolated from another.
   - Fix: route by tenant first, then fall back to the global provider only when no tenant mapping exists.

2. **Critical** - `formwork-channel-sms/src/main/java/one/formwork/channel/sms/api/SmsChannelService.java:30-37`
   - Cost recording never happened after a successful send. The module advertised per-tenant cost tracking, but the send path returned immediately after the gateway call, so the billing tables stayed empty.
   - Fix: persist cost after every successful send, using the actual segment count reported by the provider.

3. **High** - `formwork-channel-sms/src/main/java/one/formwork/channel/sms/api/SmsChannelService.java:35-39`
   - `RetryProperties` existed but was dead configuration. Transient gateway failures would fail the request immediately, even when the provider was clearly retryable.
   - Fix: add retry with bounded backoff and only retry transient failures.

4. **High** - `formwork-channel-sms/src/main/java/one/formwork/channel/sms/provider/AwsSnsSmsGateway.java:41-112`
   - The AWS SNS gateway built its SigV4 query string with `URLEncoder`, which turns spaces into `+`. SigV4 canonical query encoding is not form encoding, so bodies with spaces can produce signatures that AWS rejects.
   - Fix: use RFC3986-style encoding for the canonical query and sign the exact bytes being sent.

5. **High** - `formwork-channel-sms/src/main/java/one/formwork/channel/sms/provider/AwsSnsSmsGateway.java:39-85`
   - Credentials were read only from environment variables inside the send method. That makes the gateway hard to test and brittle in containerized deployments where credentials are injected differently.
   - Fix: allow credential injection through the constructor and keep the environment fallback only for production wiring.

6. **Medium** - `formwork-channel-sms/src/test/java/one/formwork/channel/sms/provider/TwilioSmsGatewayWireMockTest.java:43-55`
   - This test proves only that a mocked `WebClient` can be made to return a response. It does not assert the URL, headers, or body, so a broken request contract would still pass.
   - Fix: replace this style of test with a real HTTP stub that inspects the captured request.

7. **Medium** - `formwork-channel-sms/src/test/java/one/formwork/channel/sms/provider/MessageBirdSmsGatewayWireMockTest.java:40-52`
   - Same issue as above: the test does not verify the actual HTTP bytes sent to the provider. A regression in request construction would not fail here.
   - Fix: assert on the live request, not only on the mocked `WebClient` chain.

8. **Low** - `formwork-channel-sms/src/test/java/one/formwork/channel/sms/provider/AwsSnsSmsGatewayTest.java:17-35`
   - The AWS SNS test suite only checks `supports()` and `getProviderName()`. It never exercises `send()`, so it would miss the signing and encoding bugs in the gateway.
   - Fix: add an integration-style HTTP test that validates the actual request path, headers, and query string.
