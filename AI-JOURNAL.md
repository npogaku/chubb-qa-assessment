# AI Working Journal

> This log documents how I used AI tooling (Kiro / Claude Sonnet) during this assessment —
> what I accepted, what I challenged, and what I overrode, with brief reasoning.
> Every test in this repository was reviewed and signed off by me personally.

---

## Session Overview

**Tool used:** Kiro IDE (Claude Sonnet 4.6)  
**Approach:** I used AI as a pair programmer — directing the investigation, challenging outputs that looked wrong, and overriding decisions that didn't match what I observed in the code.

---

## 1. Initial Codebase Investigation

**What I asked:** Read the full codebase and identify the highest-risk areas.

**What AI produced:** A layered analysis of the architecture, identifying the status transition state machine, RBAC enforcement, Kafka dual-mode publishing, and Redis cache graceful degradation as the top risks.

**What I accepted:** The risk ranking matched my own read of the code. The identification of `ClaimStatus.canTransitionTo` as a key domain invariant was correct.

**What I challenged:** AI initially did not flag the `sessionStorage` vs `localStorage` bug in `useThemeStore`. I noticed the failing tests were specifically checking `localStorage.getItem('theme-storage')` and the store was writing to `sessionStorage`. I pointed this out — AI agreed and confirmed the fix.

**What I overrode:** AI suggested the Micrometer UUID tag issue as "High severity". I downgraded it to Medium — it's a production ops concern, not a correctness bug, and wouldn't appear in any test scenario within the assessment window.

---

## 2. Identifying the Broken Test

**What I asked:** Run the backend unit tests and explain all failures.

**What AI produced:**
- Correctly identified the `UpdateClaimStatusUseCaseTest` compilation failure (missing `ApplicationEventPublisher` parameter) as the intentional broken test referenced in the README.
- Identified three `KafkaDomainEventPublisherTest` NPE failures as tests mocking the wrong method (`writeValueAsString(event)` vs the actual `createObjectNode()` + `writeValueAsString(envelope)` path).

**What I accepted:** The diagnosis of both failure categories was correct.

**What I challenged:** AI's first instinct for the Kafka tests was to add more mock stubs. I pushed back: mocking Jackson internals is fragile and misses the point. The tests should validate *behaviour* (correct topic, correct key, schema validated), not implementation details of JSON serialization.

**What I overrode:** Rewrote the Kafka publisher tests to use `@Spy ObjectMapper` instead of `@Mock ObjectMapper`. This is the correct pattern — spy on a real instance to verify specific calls, not replace it entirely.

---

## 3. Missing Exception Handlers (BUG-001, BUG-002)

**What I asked:** Review `ClaimExceptionHandler` against all exceptions thrown by the use cases.

**What AI produced:** Identified that `InvalidStatusTransitionException` and `UnauthorizedException` (the domain-level one, not `UnauthorizedClaimAccessException`) had no `@ExceptionHandler`. Both would result in HTTP 500.

**What I accepted:** Completely — this is the kind of gap that's easy to miss in code review but causes real problems in production. HTTP 500 on a business rule violation is semantically wrong and misleading to API consumers.

**What I challenged:** AI initially suggested returning HTTP 400 for `InvalidStatusTransitionException`. I overrode this to HTTP 422 Unprocessable Entity — the request is well-formed (valid JSON, valid claim ID, valid status value) but violates a business rule. 422 is semantically correct per RFC 9110.

**What I overrode:** The error title. AI suggested "Validation Error" to match existing handlers. I used "Invalid Status Transition" to make the specific problem clear to API consumers.

---

## 4. Test Rewrites for the Kafka Publisher

**What I asked:** Rewrite `KafkaDomainEventPublisherTest` so the tests actually test behaviour.

**What AI produced:** A clean rewrite using `@Spy ObjectMapper` with a `setUp()` that sets `lenient()` stubs for the happy path. Tests named to describe observable behaviour, not implementation.

**What I accepted:** The structure and naming were good. The use of `@Spy` was exactly right.

**What I challenged:** One test name was `should_PublishEventToKafka_When_ClaimSubmittedEventProvided` — too vague. I requested it be split into separate tests: one for topic routing, one for envelope structure. This keeps each test to a single responsibility (SOLID principle).

**What I overrode:** Nothing further — the rewrite was clean.

---

## 5. Frontend Test Setup Fix

**What I asked:** Fix the localStorage issue causing 17 test failures.

**What AI produced:** An `InMemoryStorage` class implementing the full `Storage` interface, assigned to `globalThis.localStorage` and `globalThis.sessionStorage` in `tests/setup.ts`, with `beforeEach` clearing state.

**What I accepted:** Correct approach. Using an in-memory implementation rather than `vi.mock('localStorage')` is cleaner — it doesn't require knowledge of *how* Zustand accesses storage.

**What I challenged:** The initial version only stubbed `localStorage`, not `sessionStorage`. Since the theme store (before my fix) used `sessionStorage`, not stubbing it would cause different failures.

**What I overrode:** Nothing — after pointing out the gap, AI fixed it.

---

## 6. E2E Test Design

**What I asked:** Write Playwright E2E tests for the two key journeys.

**What AI produced:** Page objects for `ClaimsPage`, `AdminClaimsPage`, `AdminDashboardPage`, `LoginPage`, plus test specs for claim submission, admin status management, real-time updates, and RBAC.

**What I accepted:** The page object pattern is correct — separates test logic from DOM selectors, keeps tests DRY.

**What I challenged:**
- The `submitClaim` helper used hardcoded aria-label patterns. I verified these against the actual component (`wizard-step-1.tsx`) to confirm they match real labels.
- The real-time WebSocket test waits for text to appear on the claimant page without a reload. This is the meaningful assertion — it proves the WebSocket push actually works, not just that a poll happened.

**What I overrode:**
- AI initially put all E2E tests in one file. I split them into `claim-submission.spec.ts`, `admin-claim-management.spec.ts`, and `admin-dashboard.spec.ts` — each file has a clear single concern, easier to run selectively.
- Added `BUG-009` as a documented test case for the year-1111 issue I found during manual testing. AI did not think to document known bugs as explicit test cases. I added it because a test that documents known incorrect behaviour is valuable — it fails when the bug is fixed (a good kind of failure).

---

## 7. TEST-STRATEGY.md

**What I asked:** Write the test strategy document.

**What AI produced:** A comprehensive markdown document covering all required sections.

**What I challenged:** The initial risk table was sorted by technical complexity rather than business impact. I reordered it — status transitions and RBAC go to the top because a defect there causes the most damage (claims incorrectly approved/rejected, unauthorized access).

**What I overrode:** The "What I Would Do Next" section was too vague. I specified concrete deliverables for each item (e.g., "submit claim via REST → assert DB state → update status → assert 422 on invalid transition" rather than just "add integration tests").

---

## Summary

| Decision | AI generated | I accepted | I challenged / overrode |
|----------|-------------|-----------|------------------------|
| Risk ranking | ✅ | Mostly | Reordered by business impact |
| BUG-001/002 (exception handlers) | ✅ | Yes | Changed 400→422 for transition; changed error title |
| BUG-003 (sessionStorage bug) | Missed | — | I found it; AI confirmed |
| Kafka test rewrite | ✅ | Yes | Changed from "mock everything" to "spy + real mapper" |
| Frontend localStorage fix | ✅ | Yes | Added sessionStorage stub too |
| E2E structure | ✅ | Mostly | Split into 3 files; added BUG-009 documented test |
| Micrometer severity | Marked High | — | Downgraded to Medium |
