# AI Usage

I used AI heavily for the first draft of the refactor and the documentation, then I manually corrected the parts that needed engineering judgment.

## What I generated with AI

- The initial shape of the tenant routing registry.
- The retry / failover dispatcher structure.
- The AWS SNS HTTP-stub integration test.
- First-pass versions of `README.md`, `REVIEW.md`, and the ADR.

## Where the AI was wrong

One concrete mistake: the first retry implementation applied sleep jitter after the cap and used a generic backoff wrapper that made the cap meaningless. I replaced that with bounded backoff in the dispatcher and kept the retry policy explicit.

Another mistake: the first AWS SNS test was still a mocked `WebClient` chain. That would not have caught request encoding or signature issues, so I replaced it with a live local HTTP server that captures the actual request bytes.

## What I wrote myself

- The review ranking and severity.
- The decision to keep tenant routing in-memory for this pass.
- The exact scope cuts called out in the README.
- The wording around what I trusted and what I did not trust in the existing tests.
