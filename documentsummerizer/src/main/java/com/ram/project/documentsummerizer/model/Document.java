package com.ram.project.documentsummerizer.model;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@org.springframework.data.mongodb.core.mapping.Document(collection = "documents")
public class Document {
    @Id
    private String id;
    private String userId;
    private String originalFileName;
    private String extractedText;
    private String summary;
    private LocalDateTime createdAt;

   
    public Document() {
        this.createdAt = LocalDateTime.now();
    }

    public Document(String originalFileName, String extractedText) {
        this.originalFileName = originalFileName;
        this.extractedText = extractedText;
        this.createdAt = LocalDateTime.now();
    }

  
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
