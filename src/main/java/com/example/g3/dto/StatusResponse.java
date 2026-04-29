package com.example.g3.dto;

import com.example.g3.domain.OnboardingStatus;

public class StatusResponse {
    private OnboardingStatus status;

    public StatusResponse() {}

    public StatusResponse(OnboardingStatus status) {
        this.status = status;
    }

    public OnboardingStatus getStatus() {
        return status;
    }

    public void setStatus(OnboardingStatus status) {
        this.status = status;
    }
}
