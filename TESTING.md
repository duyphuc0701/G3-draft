# Postman Testing Guide: Onboarding Orchestrator

This guide walks you through the step-by-step process of testing the entire Onboarding Orchestrator API flow using Postman.

## Prerequisites
1. **Start the Application**: Run the Spring Boot application locally.
   ```bash
   ./gradlew bootRun
   ```
   *The server will start on `http://localhost:8080`.*
2. **Infrastructure**: The Orchestrator requires a running Kafka broker (for `account.requested` and `onboarding.status.changed` events) and running mock servers for IDV, Risk, CIS, and Document services on ports `8081-8084` respectively. If these are not running, Step 4 (`verify`) will result in a failure status.

---

## The Workflow

### Step 1: Start a New Session
Initialize a new onboarding session.

- **Method**: `POST`
- **URL**: `http://localhost:8080/onboarding`
- **Headers**: None
- **Body**: None
- **Expected Response**: `201 Created`
  ```json
  {
      "sessionId": "4b3c2a1d-5e6f-7g8h-9i0j-1k2l3m4n5o6p"
  }
  ```
> **Action**: Copy the `sessionId` from the response. You will need it for all subsequent requests.

---

### Step 2: Submit Personal Details
Update the session with the user's basic demographic information.

- **Method**: `PUT`
- **URL**: `http://localhost:8080/onboarding/{sessionId}/personal-details`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
      "firstName": "John",
      "lastName": "Doe",
      "dob": "1990-01-01",
      "email": "john.doe@example.com",
      "contactPhone": "+1234567890"
  }
  ```
- **Expected Response**: `200 OK`

---

### Step 3: Upload Documents
Provide the base64 encoded document strings (simulated).

- **Method**: `POST`
- **URL**: `http://localhost:8080/onboarding/{sessionId}/documents`
- **Headers**: `Content-Type: application/json`
- **Body** (raw JSON):
  ```json
  {
      "type": "PASSPORT",
      "frontImage": "base64-encoded-front-image-string",
      "backImage": "base64-encoded-back-image-string",
      "selfieImage": "base64-encoded-selfie-image-string"
  }
  ```
- **Expected Response**: `200 OK`

---

### Step 4: Trigger Verification
This is the core execution block. It tells the Orchestrator to contact the downstream services (IDV, Risk, CIS) synchronously, and push the async account creation message to Kafka.

- **Method**: `POST`
- **URL**: `http://localhost:8080/onboarding/{sessionId}/verify`
- **Headers**: None
- **Body**: None
- **Expected Response**: `202 Accepted`

*(Note: Because this triggers Resilience4j timeouts/retries, if your mock servers are offline, this request may take several seconds before ultimately failing the state machine).*

---

### Step 5: Poll for Status
Check the current state of the onboarding session.

- **Method**: `GET`
- **URL**: `http://localhost:8080/onboarding/{sessionId}/status`
- **Headers**: None
- **Body**: None
- **Expected Response**: `200 OK`
  ```json
  {
      "status": "ACCOUNT_REQUESTED"
  }
  ```
*(Note: If the mock downstream dependencies were offline, the status will read `"FAILED"`).*

---

## Simulating the Kafka Callback (Optional)

To push the state from `ACCOUNT_REQUESTED` to `ACCOUNT_OPENED`, you must simulate the Core system responding on the Kafka topic `account.opened`.

If you have a local Kafka CLI tool installed, you can produce the following message to the `account.opened` topic:

```json
{
	"requestId": "4b3c2a1d-5e6f-7g8h-9i0j-1k2l3m4n5o6p",
	"customerId": "cus_123456",
	"accountId": "acc_567890",
	"status": "OPENED",
	"openedAt": "2026-03-20T03:45:00Z"
}
```

If you send that Kafka message, polling **Step 5** again via Postman will now return:
```json
{
    "status": "ACCOUNT_OPENED"
}
```
