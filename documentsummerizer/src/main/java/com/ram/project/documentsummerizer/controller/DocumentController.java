// src/main/java/com.ram.project.documentsummerizer/controller/DocumentController.java
package com.ram.project.documentsummerizer.controller;

import com.ram.project.documentsummerizer.model.Document;
import com.ram.project.documentsummerizer.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import net.sourceforge.tess4j.TesseractException;
import java.util.List;
import java.util.Optional; // Import Optional

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.ram.project.documentsummerizer.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import com.ram.project.documentsummerizer.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentService documentService, UserRepository userRepository) {
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Error: No file selected.");
        }
        try {
            String responseMessage = documentService.processAndSaveDocument(file);
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: File validation failed - " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error: File I/O issue - " + e.getMessage());
        } catch (TesseractException e) {
            return ResponseEntity.status(500).body("Error: OCR processing failed - " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: An unexpected server error occurred - " + e.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<List<Document>> getUserDocuments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        }
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        // Fetch user from DB to get userId
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        User currentUser = userOpt.get();
        List<Document> userDocuments = documentService.getUserDocuments(currentUser.getId());
        return ResponseEntity.ok(userDocuments);
    }

    // --- FIX: getDocumentById Method ---
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocumentById(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthenticated access attempt to document {}", id);
            return ResponseEntity.status(401).build();
        }
        Object principal = authentication.getPrincipal();
        String username = null;
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            username = (String) principal;
        }
        if (username == null) {
            logger.warn("No username found in principal for document {}", id);
            return ResponseEntity.status(401).build();
        }
        // Fetch user from DB to get userId
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            logger.warn("User {} not found in DB for document {}", username, id);
            return ResponseEntity.status(401).build();
        }
        User currentUser = userOpt.get();
        logger.info("User {} (id: {}) is requesting document {}", username, currentUser.getId(), id);
        Optional<Document> docOptional = documentService.getDocumentById(id);
        if (docOptional.isEmpty()) {
            logger.warn("Document {} not found", id);
            return ResponseEntity.notFound().build();
        }
        Document document = docOptional.get();
        logger.info("Document {} found, owned by userId {}", id, document.getUserId());
        if (!document.getUserId().equals(currentUser.getId())) {
            logger.warn("User {} (id: {}) is not authorized to access document {} (owner: {})", username, currentUser.getId(), id, document.getUserId());
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(document);
    }
}