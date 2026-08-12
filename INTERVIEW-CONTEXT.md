# Studio Butterfly Interview Context

## Goal

Prepare Alif for the Studio Butterfly Senior Java/Spring Boot interview on 13
August 2026. The interview will focus on the SMS take-home assignment. The
user used AI heavily and needs simple explanations of the code and Spring
behavior.

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
