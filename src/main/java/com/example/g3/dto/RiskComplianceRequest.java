package com.example.g3.dto;

import java.time.LocalDate;

public class RiskComplianceRequest {
    private String firstName;
    private String lastName;
    private LocalDate dob;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
}
