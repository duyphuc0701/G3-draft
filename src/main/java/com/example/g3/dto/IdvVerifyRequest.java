package com.example.g3.dto;

import java.time.LocalDate;

public class IdvVerifyRequest {
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private IdDocument idDocument;
    private String selfieImage;

    public static class IdDocument {
        private String type;
        private String frontImage;
        private String backImage;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getFrontImage() { return frontImage; }
        public void setFrontImage(String frontImage) { this.frontImage = frontImage; }
        public String getBackImage() { return backImage; }
        public void setBackImage(String backImage) { this.backImage = backImage; }
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }
    public IdDocument getIdDocument() { return idDocument; }
    public void setIdDocument(IdDocument idDocument) { this.idDocument = idDocument; }
    public String getSelfieImage() { return selfieImage; }
    public void setSelfieImage(String selfieImage) { this.selfieImage = selfieImage; }
}
