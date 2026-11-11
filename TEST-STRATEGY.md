# Test Strategy — Insurance Claims Management Application

> **Assessor:** Nagaraju Pogaku  
> **Date:** November 2026  
> **Time spent:** ~3 hours  
> **Stack:** Next.js 16 / React 19 (UI) · Spring Boot 4 / Java 21 (BFF + Claims Service) · PostgreSQL 15 · Redis 7 · Apache Kafka · Keycloak 26

---

## 1. Application Assessment

Before writing a single test I read every layer of the codebase end-to-end:
domain model, use cases, REST controllers, Kafka adapters, Redis cache,
frontend stores, wizard components, and all existing tests.

### Architecture summary

```
Browser → Next.js UI (3001)
            ↓ REST + WebSocket
         BFF Service (8090)   ← Redis (session cache)
            ↓ REST             ← Keycloak (JWT validation)
         Claims Service (8080)
            ↓ SQL
         PostgreSQL (5432)
            ↓ domain events
         Kafka (9092)          ← optional CDC via Debezium
            ↑ consumed by BFF  → WebSocket push → UI
```

The backend follows **hexagonal architecture**: domain → use-case → adapter.
The frontend uses **Zustand** stores with a generated OpenAPI client.

---

## 2. Highest-Risk Areas

Risk is assessed on two axes: **damage if defective** × **complexity / integration brittleness**.

| Rank | Area | Why High Risk |
|------|------|---------------|
| 1 | **Status transition state machine** | Business-critical; enforced independently in backend (`ClaimStatus.canTransitionTo`) and frontend (`VALID_TRANSITIONS`). Drift between the two layers would silently break the UI while the API enforces the real rules. |
| 2 | **RBAC / Authorization** | Two distinct mechanisms: Spring Security `@PreAuthorize("hasRole('ADMIN')")` at the HTTP layer AND domain-level `UnauthorizedException` inside `UpdateClaimStatusUseCase`. A gap in either means claimants could modify claims they don't own or admins could be blocked unexpectedly. |
| 3 | **Kafka event publishing** | Dual-mode (CDC vs direct). When `cdcEnabled=false`, events are published from the **original** domain object before it is returned — if the repository returns a new JPA-mapped instance, events could be lost silently. No retry logic. |
| 4 | **Domain validation in `Claim` constructor** | All invariants enforced here (amount, date, description length, location). Any gap becomes a data integrity problem persisted to the DB. |
| 5 | **Redis cache (`DashboardStatsCache`)** | Graceful degradation pattern — a silent Redis failure returns stale `Optional.empty()`. The dashboard could show zeros instead of real data with no visible error. |
| 6 | **WebSocket reconnection logic** | 5-retry exponential backoff. If the BFF restarts, the UI silently stops receiving real-time updates until the user refreshes. |

---

## 3. Bugs Found

### 🔴 BUG-001 — `InvalidStatusTransitionException` unhandled → HTTP 500
**File:** `ClaimExceptionHandler.java`  
**Symptom:** Attempting an invalid status transition (e.g. `CLOSED → APPROVED`) returns HTTP 500 instead of 422.  
**Root cause:** No `@ExceptionHandler(InvalidStatusTransitionException.class)` in the advice class.  
**Fix applied:** Added handler returning HTTP 422 Unprocessable Entity.  
**Test:** `ClaimExceptionHandlerTest.should_Return422_When_InvalidStatusTransition`

---

### 🔴 BUG-002 — `UnauthorizedException` unhandled → HTTP 500
**File:** `ClaimExceptionHandler.java`  
**Symptom:** When a non-admin calls the status-update endpoint and Spring Security's `@PreAuthorize` doesn't fire (e.g. in tests with a misconfigured context), the domain throws `UnauthorizedException` which is NOT `UnauthorizedClaimAccessException`. The handler only catches the latter → HTTP 500.  
**Root cause:** Two different exception classes — the handler maps one but not the other.  
**Fix applied:** Added handler for `UnauthorizedException` → HTTP 403.  
**Test:** `ClaimExceptionHandlerTest.should_Return403_When_NonAdminTriesToUpdateClaimStatus`

---

### 🟡 BUG-003 — `useThemeStore` persists to `sessionStorage` instead of `localStorage`
**File:** `use-theme-store.ts`  
**Symptom:** Dark mode preference and colour theme are lost every time the user closes the browser tab.  
**Root cause:** `createJSONStorage(() => sessionStorage)` — `sessionStorage` is cleared on tab/window close; `localStorage` persists across sessions.  
**Evidence:** The existing tests for this store were failing — they correctly called `localStorage.getItem('theme-storage')` but nothing was there because the store wrote to `sessionStorage`. The tests were right; the production code was wrong.  
**Fix applied:** Changed to `localStorage`.

---

### 🟡 BUG-004 — `KafkaDomainEventPublisherTest` tests the wrong abstraction
**File:** `KafkaDomainEventPublisherTest.java`  
**Symptom:** 6 of 7 tests failed with `NullPointerException` — `ObjectNode.put()` on null.  
**Root cause:** Tests mocked `objectMapper.writeValueAsString(event)`, but the publisher builds an envelope `ObjectNode` via `objectMapper.createObjectNode()` first. A mocked `ObjectMapper` returns null from `createObjectNode()`.  
**Fix applied:** Rewrote tests to use a real `ObjectMapper` via `@Spy`. Behaviour under test (topic routing, schema validation) is now cleanly separated from Jackson internals.

---

### 🟡 BUG-005 — `CreateClaimUseCaseTest` missing `ApplicationEventPublisher` mock
**File:** `CreateClaimUseCaseTest.java`  
**Symptom:** `NullPointerException` in `CreateClaimUseCase.execute()` — the `eventPublisher` field was null.  
**Root cause:** `@InjectMocks` requires all constructor dependencies to have a matching `@Mock`. `ApplicationEventPublisher` was missing.  
**Fix applied:** Added `@Mock ApplicationEventPublisher eventPublisher`. Also fixed the event assertion — after `cdcEnabled=false`, events are published and cleared; the correct assertion is `verify(eventPublisher).publishEvent(any(ClaimSubmitted.class))`, not checking `result.getDomainEvents()`.  
**Note:** This is the "broken test exists as a starting point" referenced in the README.

---

### 🟡 BUG-006 — Frontend test setup: `localStorage` missing from jsdom environment
**File:** `tests/setup.ts`  
**Symptom:** 17 tests across 12 test suites failed or registered 0 tests. Zustand `persist` middleware crashed with `TypeError: Cannot read properties of undefined (reading 'setItem')`.  
**Root cause:** The vitest setup file had no `localStorage` stub. Node's jsdom does not expose a real `localStorage` without `--localstorage-file`.  
**Fix applied:** Added a full `InMemoryStorage` implementation, stubbed to `globalThis.localStorage` and `globalThis.sessionStorage` in `tests/setup.ts`, cleared `beforeEach`.

---

### 🟡 BUG-007 — Generated API client not in repository
**File:** `src/lib/api/generated/` (did not exist)  
**Symptom:** Any test importing `@/src/lib/api/bff-client` silently registered 0 tests — Vitest caught the module resolution error at import time, before any tests ran.  
**Root cause:** The `generated/` directory is produced by `npm run generate:api` from the BFF OpenAPI spec. It was not committed and not documented as a prerequisite.  
**Fix applied:** Added `npm run generate:api` as a mandatory step before tests.

---

### 🟡 BUG-008 — N+1 database queries in `AdminClaimsController.listAllClaims`
**File:** `AdminClaimsController.java`  
**Symptom:** For N claims, the admin list endpoint executes N+1 queries — 1 for all claims, then 1 `SELECT` per claim to fetch user details.  
**Root cause:** `userRepository.findById(UserId.of(claim.getUserId()))` called inside `toAdminClaimResponse()` which is invoked per-claim in a stream.  
**Not fixed:** Requires a schema change or batch-load approach. Documented for follow-up.

---

### 🟠 BUG-009 — No minimum date validation allows year 1111
**File:** `Claim.java` + `wizard-step-1.tsx`  
**Symptom:** Observed live during manual testing — incident date "Nov 11, 1111" was accepted.  
**Root cause:** Domain validation only blocks future dates: `if (incidentDate.isAfter(LocalDate.now()))`. There is no lower bound.  
**Impact:** In a real insurance system, dates outside the policy coverage window should be rejected. A claim from 900 years ago is semantically invalid.  
**Test added:** `claim-submission.spec.ts → BUG-009: wizard accepts year 1111 as incident date` (documents the behaviour).

---

### 🟠 BUG-010 — Micrometer counter tag uses UUID → unbounded cardinality
**File:** `CreateClaimUseCase.java`, `UpdateClaimStatusUseCase.java`  
**Symptom:** `meterRegistry.counter("claims.submitted", "userId", userId.toString())` — using a UUID as a Micrometer tag value creates one time-series per user in Prometheus/CloudWatch. With thousands of users this causes memory exhaustion.  
**Not fixed:** Requires product discussion (remove userId tag, or use logs instead).

---

### 🟠 BUG-011 — Confirmation dialog not centered on viewport (UI defect — reproducible on every status change)
**File:** `confirmation-dialog.tsx` (admin claims page)  
**How found:** Manual exploratory testing on the admin claims page.  
**Steps to reproduce:**
1. Login as `admin@demo.com`
2. Go to Claims Management
3. Click "Update Status" dropdown on any claim → select any status (e.g. Rejected)
4. Confirm dialog appears — misaligned
5. After confirming, select the next status (e.g. Closed)
6. Same misaligned dialog appears again

**Symptom:** The "Confirm Status Change" modal appears on every status transition, and on every occurrence it is anchored to the middle of the table section rather than centered on the full viewport. Consistent, reproducible, not a fluke.  
**Root cause:** The dialog likely uses `position: absolute` relative to its parent container instead of `position: fixed` relative to the viewport. A proper modal overlay should use:
```css
position: fixed;
top: 50%;
left: 50%;
transform: translate(-50%, -50%);
z-index: 50;
```
**Impact:** Affects every admin status update workflow — this is a core admin action. Every time an admin changes a claim status, they see a mispositioned dialog. While functional, it looks unprofessional in a production system handling real insurance claims.  
**Not fixed:** Frontend styling change only. Low priority relative to functional bugs above.

---

### 🟠 BUG-011 — Confirmation dialog not centered on viewport
**File:** `confirmation-dialog.tsx` (admin claims page)  
**How found:** Manual exploratory testing — clicked "Update Status → Rejected" on the admin claims page.  
**Symptom:** The "Confirm Status Change" modal dialog appears anchored to the middle of the table section rather than centered on the full viewport. On pages with limited content (e.g. only 1 claim in the list) the dialog renders in the lower half of the screen, misaligned from the page centre.  
**Root cause:** The dialog likely uses `position: absolute` relative to its parent container instead of `position: fixed` relative to the viewport. A proper modal overlay should use:
```css
position: fixed;
top: 50%;
left: 50%;
transform: translate(-50%, -50%);
z-index: 50;
```
**Impact:** UI/UX defect — not a functional bug, the dialog still works. But it looks unprofessional and could be missed by users on smaller screens. A senior engineer would flag this in a PR review.  
**Not fixed:** Frontend styling change. Low priority relative to functional bugs.

---

## 4. What I Chose to Test

### ✅ Tested

| Layer | Framework | Coverage focus |
|-------|-----------|----------------|
| Domain unit tests | JUnit 5, AssertJ | `Claim` invariants, `ClaimStatus` state machine (all transitions) |
| Use-case unit tests | JUnit 5, Mockito | `CreateClaimUseCase`, `UpdateClaimStatusUseCase`, `GetClaimUseCase` — happy paths + all error branches |
| Exception handler | JUnit 5 | All 6 handlers including the 2 newly added ones (BUG-001, BUG-002) |
| Kafka publisher | JUnit 5, Mockito Spy | Topic routing, envelope structure, schema validation, serialisation failure |
| Frontend stores | Vitest, Testing Library | `useClaimsStore`, `useClaimWizardStore`, `useAuthStore`, `useThemeStore` |
| Frontend components | Vitest, Testing Library | Wizard steps 1-3, `ClaimStatusBadge`, `ClaimDetailModal`, `ProgressIndicator`, `ConfirmationDialog`, `ErrorBoundary` |
| Frontend hooks | Vitest | `useRealtimeClaimUpdates` WebSocket reconnection logic, `useCopyClipboard` |
| E2E — claimant journey | Playwright | Claim submission wizard, field validation, status display |
| E2E — admin journey | Playwright | Status transitions, full lifecycle, closed=disabled |
| E2E — real-time | Playwright | WebSocket push: admin changes → claimant sees update |
| E2E — RBAC | Playwright | Claimant blocked from admin routes |
| E2E — dashboard | Playwright | Cache Miss → Cache Hit → Flush cycle |
| Architecture | ArchUnit | Hexagonal layer dependency rules |

### ❌ Deliberately Left Out

| Area | Reason |
|------|--------|
| BFF unit tests | 213 existing tests already pass. BFF is a thin proxy — the risk is at the Claims Service domain. Time better spent on integration gaps. |
| Pact contract tests | Scaffolding exists but setting up a full Pact broker is a multi-hour infrastructure investment. High value but out of scope for a 3-hour sprint. |
| Testcontainers integration tests (full HTTP) | Testcontainers config is in place. With more time, a full create→status-update→verify-in-DB flow would be the highest-value integration test to write. |
| Debezium / CDC path | Requires a separate startup mode (`APP_EVENTS_CDC_ENABLED=true`). Testing the CDC path end-to-end requires Debezium running and is a separate concern from the application logic tests. |
| Load / performance tests | Bonus scope. Would use K6 targeting the claims list and status-update endpoints. |
| Visual regression | No Storybook configured. Would need setup investment first. |

---

## 5. Test Execution

### Unit tests

```bash
# Frontend — 266 tests
cd code/demo-app-ui
npm install
npm run generate:api   # required: generates TypeScript API client
npm test

# Claims Service — 200 tests
cd code/claims-service
mvn test -Dgroups="!integration" --no-transfer-progress

# BFF Service — 213 tests  
cd code/bff-service
mvn test -Dgroups="!integration" --no-transfer-progress
```

### E2E tests (requires full stack running)

```bash
# Start the stack first
./start-infra.sh
./start-app.sh

# Run E2E suite
cd code/demo-app-ui
npx playwright test
npx playwright show-report   # HTML report
```

### Integration tests (Testcontainers — requires Docker)

```bash
cd code/claims-service
mvn test -Dgroups="integration" --no-transfer-progress
```

---

## 6. What I Would Do Next (Priority Order)

1. **Testcontainers full-HTTP integration tests** — submit claim via REST → assert DB state → update status → assert 422 on invalid transition. This is the single highest-value test not yet written.
2. **Fix BUG-008 (N+1 queries)** — batch-load users by IDs in `AdminClaimsController`.
3. **Fix BUG-009 (no minimum date)** — add `incidentDate.isBefore(LocalDate.now().minusYears(5))` guard in `Claim` constructor (or policy-driven).
4. **Fix BUG-010 (Micrometer cardinality)** — remove `userId` tag from counters.
5. **Pact contract tests** — the stubs in `bff-service/mocks/` are already generated. Setting up a Pact broker and writing the consumer-driven contracts for BFF ↔ Claims Service would catch API contract drift.
6. **K6 load test** — target `GET /api/claims` and `PATCH /api/admin/claims/{id}/status` to establish latency baselines and surface the N+1 performance regression under load.
