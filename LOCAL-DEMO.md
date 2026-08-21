# Dummy Provider SMS Demo

This is a local learning harness. It does not call Twilio, Vonage, AWS SNS,
MessageBird, or BudgetSMS.

## Start PostgreSQL

From the repository root:

```bash
docker compose up -d
```

The database is available at `localhost:5432` with database `formwork`, user
`formwork`, and password `formwork`.

## Observe the original Spring wiring

Start the read-only profile:

```bash
mvn -pl formwork-channel-sms spring-boot:run \
  -Dspring-boot.run.profiles=local-observe
```

In PowerShell, quote Maven properties if necessary:

```powershell
mvn -pl formwork-channel-sms spring-boot:run `
  '-Dspring-boot.run.profiles=local-observe'
```

Call:

```text
GET http://localhost:18081/demo/gateways
```

With `provider: TWILIO` and `fallback-provider: VONAGE`, the response shows
only `TWILIO` in `runtimeGateways`. This demonstrates that the original
conditional bean wiring creates only the globally selected provider.

This profile has no send endpoint and cannot send an external SMS.

## Start the safe dummy profile

Stop the observation profile first, then run:

```bash
mvn -pl formwork-channel-sms spring-boot:run \
  -Dspring-boot.run.profiles=dummy-profile
```

The demo listens on `http://localhost:8080` and registers dummy gateways using
the real provider names: `TWILIO`, `VONAGE`, `AWS_SNS`, `BUDGET_SMS`, and
`MESSAGEBIRD`. No external provider API is called.

## Postman

The service now uses JWT authentication and CSRF protection. Before any state-changing
request in Postman, call `GET http://localhost:8080/csrf-token/public`, retain the
`XSRF-TOKEN` cookie, and send its value in the `X-XSRF-TOKEN` header. Register the
temporary admin with `POST /auth/register` using a password of at least 12 characters,
then call `POST /auth/login` and send the returned token as `Authorization: Bearer ...`.
Tenant provider assignment is available to that admin at
`PUT /api/admin/tenants/{tenantId}/provider`.

Import:

```text
postman/studio-butterfly-sms-local-demo.postman_collection.json
```

Run the requests in order. The collection covers runtime gateway inspection,
tenant routing, retry, failover, multi-segment cost recording, invalid phone
validation, and unavailable-provider behavior.

Useful breakpoints:

- `SmsChannelAutoConfiguration` bean methods: observe conditional creation.
- `SmsChannelService.sendSms`: observe the complete send entry point.
- `SmsChannelService.sendWithFallback`: observe provider order and failover.
- `SmsChannelService.sendWithRetry`: observe retry attempts.
- `TenantProviderRegistry.resolveProvider`: observe tenant routing.
- `DummyProviderState.send`: observe simulated provider responses.
- `SmsCostService.recordCost`: observe segment-based cost calculation.
- `SmsCostRepository.save`: observe persistence.

Inspect costs with:

```text
GET http://localhost:8080/demo/costs/00000000-0000-0000-0000-000000000002
```

Inspect PostgreSQL directly if required:

```sql
SELECT tenant_id, provider, segment_count, total_cost, recipient
FROM sms_cost_record;
```
