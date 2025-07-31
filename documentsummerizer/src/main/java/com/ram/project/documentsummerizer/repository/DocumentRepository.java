
package com.ram.project.documentsummerizer.repository;

import com.ram.project.documentsummerizer.model.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; 


@Repository
public interface DocumentRepository extends MongoRepository<Document, String> {
    
    List<Document> findByUserId(String userId);


    Optional<Document> findById(String id);
}
