// frontend/src/main/java/com.ram.project.documentsummerizer/repository/DocumentRepository.java
package com.ram.project.documentsummerizer.repository;

import com.ram.project.documentsummerizer.model.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // Import Optional


@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {
    // Custom query method to find documents by userId
    List<Document> findByUserId(String userId);

    // findById(String id) is already provided by MongoRepository, but explicitly declaring
    // it here for clarity doesn't hurt. You can also remove this line if you prefer implicit.
    Optional<Document> findById(String id);
}