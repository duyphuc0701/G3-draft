package com.example.g3.dto;

import java.time.Instant;

public class OnboardingStatusEvent {
    private String onboardingId;
    private String customerId;
    private String status;
    private Instant timestamp;
    private CustomerContact customerContact;

    public String getOnboardingId() { return onboardingId; }
    public void setOnboardingId(String onboardingId) { this.onboardingId = onboardingId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public CustomerContact getCustomerContact() { return customerContact; }
    public void setCustomerContact(CustomerContact customerContact) { this.customerContact = customerContact; }

    public static class CustomerContact {
        private String email;
        private String phone;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }
}
