package com.example.g3.dto;

public class DocumentUploadResponse {
    private String documentId;
    private String storageLocation;

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
}
