# Studio Butterfly Interview Context

## Goal

Prepare Alif for the Studio Butterfly Senior Java/Spring Boot interview process.
The third interview is scheduled for Tuesday, 18 August 2026 at 4:00 PM Dhaka
time (GMT+6), for approximately 45 minutes. The technical interview will focus
on Java and Spring Boot fundamentals, problem-solving, the SMS take-home
assignment, and reasoning about production consequences. The user used AI
heavily and needs simple explanations of the code and Spring behavior.

## Interview Progress and Third-Round Email

Studio Butterfly advanced Alif after the first interview to a technical
interview with their team. The interview invitation said:

- When: Tuesday, 18 August 2026, 4:00 PM Dhaka time (GMT+6).
- Duration: approximately 45 minutes.
- Location: the Studio Butterfly WhatsApp group; the interview link and
  updates will be posted there rather than sent by email.
- Expected format: deeper Java and Spring Boot fundamentals, problem-solving,
  real-world scenarios, thinking aloud, and possible screen sharing.
- Practical advice from the email: join and monitor the WhatsApp group around
  4:00 PM, use a quiet location, stable internet, and working microphone;
  camera is appreciated but not required.

The email requested confirmation of attendance. A suitable confirmation is:

> Hi, thank you for the invitation. I'm confirming that I'll attend the
> technical interview on Tuesday, 18 August 2026 at 4:00 PM Dhaka time. I’ll
> be available in the WhatsApp group and ready to join the interview link.
> Looking forward to speaking with the team.

## Third-Interview Preparation Guidance

The interview is expected to test understanding of the submitted code and the
ability to reason about production behavior, rather than memorization of
Spring annotations. The highest-priority topics are:

- assignment walkthrough and ownership;
- `@ConditionalOnProperty`, Spring bean creation, and `List<SmsGateway>`;
- secret management and the distinction between public and private provider
  credentials;
- retry classification, timeout ambiguity, idempotency, delivery records,
  and reconciliation;
- multi-tenant isolation, database transactions, cost calculation, and
  production testing.

Useful thinking-aloud phrases are: “My first assumption is…”, “The trade-off
is…”, “The failure mode I would worry about is…”, and “For a small assignment I
would implement X, but for production I would add Y.”

### Provider credentials answer

Do not say only “encrypt credentials in the database.” Prefer a secret manager
such as AWS Secrets Manager, HashiCorp Vault, or a KMS-backed solution. Access
secrets using IAM roles or workload identity, rotate them, restrict and audit
access, and never log or commit them. Stripe publishable keys may be exposed to
the frontend when required, but Stripe secret keys and webhook signing secrets
must remain server-side. Prefer IAM roles and temporary credentials over
long-lived AWS access keys. Use Stripe Checkout, PaymentIntents, or tokenization
so raw card numbers do not pass through the backend.

If tenant-specific credentials must be stored in PostgreSQL, use envelope
encryption: encrypt the credential with a data-encryption key, protect that
key with KMS, restrict access, audit usage, and support rotation.

### Dynamic SMS rates and rate limits

`@ConditionalOnProperty` is evaluated while the application context is created,
normally at startup. Removing the global-provider condition can allow all
provider gateway beans to exist, but it does not make rates dynamic.

Keep long-lived provider beans and make pricing runtime-configurable. Production
pricing should be stored in PostgreSQL with an effective timestamp, cached in
Redis or Caffeine, invalidated after an authenticated admin/configuration
change, and distributed to other instances through an event. Each cost record
should use the rate active at send time and store the applied rate, currency,
segment count, provider message ID, idempotency key, and pricing-version or
effective-rate ID.

For distributed request throttling, use Redis with a token-bucket or
sliding-window algorithm, or Bucket4j backed by Redis. An in-memory map is not
sufficient across multiple application instances. Distinguish provider bean
selection, SMS pricing, requests-per-second limiting, provider quota, and
circuit-breaker behavior. The current `ProviderRateRegistry` only demonstrates
runtime mutation in one process; production needs a persistent, audited store
and distributed cache.

### Retry and idempotency clarification

Retry only errors classified as transient. Do not normally retry invalid phone
numbers, invalid credentials, malformed requests, or unsupported sender IDs.
Consider retrying connection failures, HTTP 500/502/503, 408, and 429 while
respecting `Retry-After`; use bounded exponential backoff with jitter.

A timeout is ambiguous: the provider may have accepted the SMS even though the
application did not receive the response. A stable idempotency key identifies
one logical SMS and should not change merely because an attempt timed out. Use
provider-side idempotency where supported, but do not assume that a locally
stored key makes Twilio, Vonage, or direct AWS SNS SMS deduplicate requests.

Use one logical delivery/cost record and separate provider-attempt records:

```text
Delivery: id=1001, idempotency_key=sms-1001, final_status=UNKNOWN
Attempt 1: TWILIO -> TIMEOUT
Attempt 2: TWILIO -> ACCEPTED, provider_message_id=SM123
```

An accepted second attempt does not prove that the timed-out first attempt did
not send. Reconciliation may show that the first attempt was not accepted, or
that both attempts were accepted and may have caused two messages/charges.
Never create a new key solely because of a timeout. Do not hold a database
transaction open during a slow provider HTTP call. Create a pending delivery
or outbox record in a short transaction, send asynchronously, update attempts
and final state in short transactions, and reconcile uncertain outcomes.

### HTTP error classification

- 400: invalid request; fix it rather than retrying.
- 401/403: credentials or authorization issue; do not retry blindly.
- 404: endpoint/configuration issue; usually do not retry.
- 408/429: potentially retry, respecting provider guidance.
- 500/502/503/504: usually transient; retry with backoff and jitter.
- Timeout: ambiguous; mark `UNKNOWN` and apply a deliberate duplicate-risk
  policy.

Failover should be based on error classification, not simply any exception.
Failing over after a definite rejection can be reasonable; failing over after
an ambiguous timeout can send a duplicate through the second provider.

### Assignment walkthrough

> The assignment was an SMS channel module supporting multiple providers. I
> introduced tenant-aware provider selection, cost recording after successful
> delivery, retry with backoff, provider failover, and an HTTP-level test for
> the AWS SNS integration. The main design is an `SmsGateway` abstraction. Each
> provider implements that interface, while `SmsChannelService` selects the
> provider, retries transient failures, and falls back to another provider.
> `SmsCostService` calculates cost using provider, destination country, and
> segment count, then persists a tenant-scoped cost record.
>
> During review I identified that the existing conditional bean wiring creates
> only the globally configured provider, so tenant routing to another provider
> may fail because its gateway bean does not exist. Production would create all
> enabled provider beans using per-provider enablement properties and validate
> tenant assignments at startup. I also identified that the tenant registry is
> in memory, delivery and cost persistence are not atomic with external
> delivery, and retrying after an ambiguous timeout can duplicate an SMS.

### Important production details

Every request must use a trusted tenant identity from authentication, not an
arbitrary client-supplied tenant ID. Scope repository queries by tenant,
authorize access, index by `tenant_id`, and test cross-tenant isolation.

For cost records, preserve historical pricing by storing the exact applied
rate, currency, segment count, provider message ID, idempotency key, and pricing
version. Prefer half-open time ranges (`sentAt >= from AND sentAt < to`) over
inclusive `BETWEEN` boundaries.

For production SMS endpoints, mention validation, authentication,
authorization, idempotency key, correlation ID, strict connection/read
timeouts, secret and phone-number redaction in logs, metrics for latency and
retry/failover/cost, circuit breakers, and a dead-letter queue for exhausted
asynchronous deliveries.

### Testing answer

Unit tests should cover routing, retry classification, fallback, validation,
and cost calculation with fake gateways. Integration tests should cover Spring
wiring, migrations, repositories, and transaction behavior. HTTP integration
tests should verify method, headers, encoding, timeout behavior, and response
mapping against a controlled HTTP server. Be honest about known failing tests
and limitations; do not claim that all tests pass if they do not.

## Repositories

- Assignment starter: `C:\Users\USER\Documents\springboot\sbja-assignment\studio-butterfly-java-assignment`
- Submitted repository: `C:\Users\USER\Documents\springboot\sbja-github`
- Assignment module: `formwork-channel-sms`
- Analytics project used to calibrate Spring knowledge:
  `C:\Users\USER\Documents\springboot\analytics-github\analytics-app\backend`

## Assignment Purpose

The module sends SMS through five providers behind `SmsGateway` and records
per-tenant SMS cost. The assignment required review, three tested fixes,
tenant-aware provider selection, cost recording, retry/failover, one honest
HTTP integration test, documentation, ADR, CI, and AI usage disclosure.

## User Knowledge

The user understands normal Spring concepts: `@SpringBootApplication`,
`@Service`, `@Repository`, `@Configuration`, `@Bean`, JPA, and security. Explain
advanced Spring concepts slowly and with concrete runtime examples.

Important distinction:

- A Java gateway class existing in IntelliJ does not mean Spring created an
  object from that class.
- A Spring bean is an object in the application context.
- `List<SmsGateway>` contains only gateway beans created by Spring.

## Auto-Configuration Defect

The original starter already had method-level `@ConditionalOnProperty` in
`formwork-channel-sms/src/main/java/one/formwork/channel/sms/config/SmsChannelAutoConfiguration.java`.

With `provider: TWILIO` and `fallback-provider: VONAGE`, Spring creates only
`TwilioSmsGateway`, not `VonageSmsGateway`. The original starter used only one
global provider, so this was an incomplete single-provider model. The submitted
AI code added tenant routing and failover but left the conditional bean wiring
unchanged. The new features require multiple gateway beans, but production
Spring may provide only one.

Do not confuse this with `@ConditionalOnBean(DataSource.class)` in
`SmsFlywayAutoConfiguration`; that controls Flyway/database bean creation, not
SMS gateway creation.

Simple assignment-level resolution: create all gateway beans and let
`SmsChannelService` choose one at send time. A stronger production resolution is
per-provider enablement properties separate from the global default property.

## Important Submitted Files

- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/api/SmsChannelService.java`
- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/api/TenantProviderRegistry.java`
- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/cost/SmsCostService.java`
- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/provider/AwsSnsSmsGateway.java`
- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/config/SmsChannelAutoConfiguration.java`
- `formwork-channel-sms/src/test/java/one/formwork/channel/sms/api/SmsChannelServiceTest.java`

The submitted code added tenant routing, cost recording after successful sends,
retry/backoff, failover, AWS encoding changes, and an AWS local HTTP test.

## Known Submission Problems

The submitted repository currently fails `mvn test` with existing failures:

- Failover test expects failover on HTTP 400, while the implementation treats
  400 as non-retryable.
- Retry test has an unnecessary Mockito stub.
- Missing-gateway assertion loses the original provider name.
- Existing entity test conflicts with eager UUID initialization.
- AWS HTTP integration assertion fails on the expected raw query encoding.

Additional risks:

- Auto-configuration creates only the global provider, so real tenant routing
  and failover can be unavailable.
- Retry after an ambiguous timeout can duplicate an SMS.
- Cost persistence occurs after external delivery and is not atomic with it.
- Tenant provider registry is in-memory and disappears on restart.
- Cost repository uses inclusive `BETWEEN` boundaries.
- Some providers report inaccurate segment counts.
- Retry uses blocking `Thread.sleep` and caps delay at 100 ms.

## Local Demo Added On Current Branch

The demo was added to the current repository and is profile-gated. It does not
alter the default assignment behavior.

Added local demo code:

- `formwork-channel-sms/src/main/java/one/formwork/channel/sms/demo/`
- `formwork-channel-sms/src/main/resources/application-dummy-profile.yml`
- `formwork-channel-sms/src/main/resources/application-local-observe.yml`
- `postman/studio-butterfly-sms-local-demo.postman_collection.json`
- `LOCAL-DEMO.md`

`local-observe` is read-only and shows the real conditional Spring wiring. With
global TWILIO and fallback VONAGE it returns only `TWILIO` on port 18081.

`dummy-profile` uses dummy gateways under the real provider names `TWILIO`,
`VONAGE`, `AWS_SNS`, `BUDGET_SMS`, and `MESSAGEBIRD`. It never contacts real
SMS providers and runs the existing service and cost pipeline on port 8080.

## Local Commands

Start PostgreSQL from repository root:

```powershell
docker compose up -d
```

Maven is not on PATH. Use this full local Maven path:

```powershell
& "C:\Users\USER\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" -pl formwork-channel-sms spring-boot:run "-Dspring-boot.run.profiles=local-observe"
```

```powershell
& "C:\Users\USER\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd" -pl formwork-channel-sms spring-boot:run "-Dspring-boot.run.profiles=dummy-profile"
```

In IntelliJ, run `one.formwork.channel.sms.FormworkChannelSmsApplication` as
an Application configuration with module `formwork-channel-sms` and program
argument `--spring.profiles.active=local-observe` or `dummy-profile`.

Postman endpoints:

- Observe: `GET http://localhost:18081/demo/gateways`
- Demo: `GET http://localhost:8080/demo/gateways`
- Demo send: `POST http://localhost:8080/demo/messages`
- Demo costs: `GET http://localhost:8080/demo/costs/{tenantId}`

## Verified Demo Behavior

The local runtime was started successfully with PostgreSQL and Flyway.

- `local-observe` returned only `TWILIO` in runtime gateways.
- `dummy-profile` returns all five dummy gateways using the real provider names.
- Tenant routing selected the secondary gateway.
- One transient failure retried and then succeeded.
- Repeated transient failures failed over to the secondary gateway.
- Three segments produced the expected higher cost.
- Invalid phone numbers returned HTTP 400.

## Debug Breakpoints

1. `LocalSmsDiagnosticsController.gateways`
2. `SmsChannelAutoConfiguration` provider bean methods
3. `LocalSmsDemoController.send`
4. `SmsChannelService.sendSms`
5. `TenantProviderRegistry.resolveProvider`
6. `SmsChannelService.sendWithFallback`
7. `SmsChannelService.sendWithRetry`
8. `DummyProviderState.send`
9. `SmsCostService.recordCost`
10. `SmsCostRepository.save`

## New Context Instruction

At the start of a new chat, tell Codex:

```text
Read C:\Users\USER\Documents\springboot\sbja-github\INTERVIEW-CONTEXT.md first. Continue helping me prepare for the Studio Butterfly interview and preserve the current branch changes.
```
