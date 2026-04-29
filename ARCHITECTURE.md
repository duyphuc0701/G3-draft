### 1. High-Level Architecture Overview
The system is designed around an **API-driven Frontend**, an **Orchestration Layer**, a suite of **Domain Microservices**, and an **Event-Driven Backbone**. 

* **Synchronous Flow:** The initial onboarding steps (identity verification, compliance checks, document gathering) are orchestrated synchronously via REST.
* **Asynchronous Flow:** The actual account creation in the Core Banking system, along with notifications and downstream service triggers, is handled asynchronously via Kafka events.

---

### 2. Frontend and API Gateway Layer
* **Web/Mobile App:** The client application where the user initiates the onboarding process.
* **Web/Mobile BFF (Backend for Frontend):** The app communicates via **REST** to the BFF. The BFF serves as an API Gateway tailored to the specific needs of the UI clients, aggregating data and routing the onboarding request to the core orchestrator.

---

### 3. Orchestration Layer
* **Onboarding Orchestrator:** This is the central "brain" of the synchronous onboarding flow. It receives the REST request from the BFF and coordinates the calls to various domain services.
    * It acts as a **REST Client** to five downstream microservices.
    * It acts as an **Event Publisher** to broadcast state changes.
    * It acts as an **Event Subscriber** to listen for the final result of the asynchronous account creation process.

---

### 4. Domain Microservices (Synchronous REST integrations)
The Onboarding Orchestrator makes **REST** calls to the following isolated domain services:

* **Identity Verification Service (IDV):** Responsible for verifying the user's identity. It acts as an adapter/proxy to an external **Third-party IDV Provider** (e.g., Onfido, Jumio, or a local government ID database).
* **Document Service:** Manages the storage and retrieval of KYC (Know Your Customer) documents uploaded during onboarding.
* **Risk & Compliance Service:** Executes business rules for AML (Anti-Money Laundering), fraud detection, and overall risk scoring.
* **Customer Information Service (CIS):** The master system of record for customer profile data (name, address, contact info).
* **Account Creation Service / Core Banking Adapter:** This service bridges the synchronous orchestrator with the asynchronous banking core. It receives the validated onboarding request via REST and translates it into an event.

---

### 5. Event-Driven Backbone (Asynchronous Kafka Integration)
The system uses Apache Kafka as its event backbone, utilizing two primary topics to decouple the heavy processing and notification systems from the user-facing API.

#### Topic: `topic-account-events`
This topic handles the lifecycle of the actual bank account creation.
* **Producer 1:** The `Account Creation Service / Core Banking Adapter` publishes an **`account.requested`** event to this topic once the synchronous checks pass.
* **Consumer 1 (Processor):** The **Core Banking** system subscribes to the `account.requested` event. It processes the account creation asynchronously.
* **Producer 2:** Once processing is complete, the **Core Banking** system publishes either an **`account.opened`** or **`account.failed`** event back to the same topic.
* **Consumer 2 (State Updater):** The **Onboarding Orchestrator** subscribes to `account.opened` and `account.failed` events. This allows the orchestrator to close the loop, update the database, and finalize the onboarding state.
* **Consumer 3 (Downstream Triggers):** **Future Card/Loan Services** subscribe to the `account.opened` event. This demonstrates an extensible choreography pattern where new services can be triggered (e.g., issuing a debit card) without modifying the onboarding flow.

#### Topic: `topic-onboarding-events`
This topic broadcasts the overall state of the user's application.
* **Producer:** The **Onboarding Orchestrator** produces an **`onboarding.status.changed`** event. This likely happens at multiple stages (e.g., documents verified, account pending, account successfully opened).
* **Consumer:** The **Notification Service** subscribes to `onboarding.status.changed`. It is responsible for sending emails, SMS, or push notifications to the user based on these status changes, completely decoupling communication logic from the core business logic.