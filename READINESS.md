# Orchestrator Readiness Checklist & Open Questions

While we have successfully implemented all the core integrations (IDV, Risk, CIS, Documents, Core, and Notifications), there are several architectural and operational areas that need clarification to ensure the system is truly production-ready, resilient, and complete.

Please review the following categories and provide guidance on how you would like to handle them:

## 1. Security (Inbound)
Currently, the REST endpoints (`POST /onboarding`, `PUT /onboarding/{id}/personal-details`, etc.) are completely open.
* **Authentication:** Should the Orchestrator validate incoming JWT tokens from the frontend/BFF?
* **Rate Limiting:** Do we need to implement rate-limiting on the public onboarding endpoints to prevent abuse or denial-of-service (DoS) attacks?

## 2. Distributed Transactions & Consistency
Our `verify()` method performs several steps synchronously (IDV -> Risk -> CIS) before publishing to Kafka.
* **Failure Mid-Flight:** What happens if the Orchestrator pod crashes *after* creating the CIS customer profile but *before* publishing the `account.requested` Kafka event? 
* **Mitigation:** Should we implement the **Transactional Outbox Pattern** (saving the event to the database in the same transaction as the status update) to guarantee at-least-once delivery to Kafka? Or is a scheduled reconciliation job preferred?

## 3. Database & Persistence
We are currently using an in-memory H2 database, which is volatile and designed for testing/development.
* **Production Database:** Which database engine should the system target for production (e.g., PostgreSQL, MySQL)?
* **Schema Management:** Do you want to introduce a database migration tool like **Flyway** or **Liquibase** to manage the `OnboardingSession` table schema explicitly rather than relying on Hibernate auto-DDL?

## 4. Observability & Monitoring
To trace issues in a microservices architecture, deep observability is critical.
* **Distributed Tracing:** Should we integrate Micrometer Tracing (OpenTelemetry/Zipkin) to pass `X-Correlation-Id` seamlessly across threads, REST calls, and Kafka headers?
* **Structured Logging:** Do you want logs formatted as JSON with the `correlationId` and `sessionId` injected via MDC (Mapped Diagnostic Context)?
* **Metrics:** Should we expose Spring Boot Actuator endpoints for Prometheus to scrape business metrics (e.g., number of successful vs. failed onboardings)?

## 5. Audit & Event History
Currently, the `OnboardingSession` entity only holds the *latest* status.
* **Audit Trail:** Is it sufficient to rely on the Notification Service for historical tracking, or should the Orchestrator maintain its own `OnboardingAuditTrail` table recording every state transition and its corresponding timestamp?

## 6. Edge Cases in the Workflow
* **Manual Review Resolution:** If a session ends up in `PENDING_REVIEW` (due to the Risk check), how is it resolved? Do we need an administrative REST endpoint (e.g., `POST /admin/onboarding/{id}/approve`) to transition it back to `VERIFIED` and resume the flow?
* **Document Upload Limits:** Do we need to enforce file size or format validations on the orchestrator side before sending documents to the Document Service?

---
*Please let me know which of these areas are in scope for your current assignment or which ones you'd like to tackle next!*
