# ADR 0001: Route SMS by tenant and record cost in the dispatcher

## Status

Accepted

## Context

The module exposes a `tenantId` on `SmsMessage`, but the original dispatcher ignored it. The cost service also existed as a separate component and was never invoked by the send path.

That left two gaps:

- Tenant-level provider isolation did not exist.
- Successful sends did not write cost records, so billing data stayed empty.

## Decision

The dispatcher now:

- Resolves a tenant-specific provider first.
- Falls back to the global provider when no tenant mapping exists.
- Records cost immediately after a successful send.
- Uses the reported segment count from the provider result.

## Alternatives considered

1. **Keep routing in configuration only**
   - Rejected because it still couples all tenants to one global provider and cannot isolate a single tenant.

2. **Push cost recording into each gateway**
   - Rejected because cost accounting is a business concern, not a transport concern, and duplicating it across providers would be brittle.

3. **Persist tenant routing immediately**
   - Rejected for this pass because the assignment time-box is small. An in-memory registry is enough to prove the routing model and keep the change focused.

## Consequences

- The send path is now responsible for orchestration, routing, retries, and cost recording.
- Tenant override behavior is visible and testable.
- A production follow-up should persist tenant assignments so the routing survives restarts.
