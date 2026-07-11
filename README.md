# Formwork SMS Channel Review

This repository contains a focused review and repair of the `formwork-channel-sms` Spring Boot module from the Studio Butterfly take-home.

## What changed

- Added tenant-aware provider routing with isolation per tenant.
- Wired successful SMS sends into the cost pipeline.
- Implemented retry with backoff and provider failover.
- Fixed AWS SNS request encoding and made the gateway testable against a real HTTP stub.
- Added a real HTTP integration test for AWS SNS.
- Added `REVIEW.md`, `AI-USAGE.md`, an ADR, and a GitHub Actions workflow.

## Build and test

The module is Maven-based. From `formwork-channel-sms/`:

```bash
mvn test
```

I could not run Maven in this environment because `mvn` is not installed here, so the code was updated by inspection and static verification.

## What I cut

- I did not fully implement tenant-specific provider configuration persistence. The current registry is in-memory and intended as the minimal wiring needed for the assignment.
- I did not replace every provider mock test with a live integration test. I added one honest HTTP integration test for AWS SNS, which was the highest-risk gateway.
- I did not add a full release pipeline or deploy config. The requested CI checks are present, but the module still needs real environment wiring in a production repo.

## Next steps with another week

1. Persist tenant provider assignments in a repository-backed table.
2. Add similar real HTTP integration tests for the remaining providers.
3. Split provider-specific concerns out of the dispatcher so retry and failover policies are easier to reason about.
4. Add a small application demo or sample configuration showing tenant override behavior end to end.
