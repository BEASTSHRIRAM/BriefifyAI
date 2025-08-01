package com.ram.project.documentsummerizer.repository;
import com.ram.project.documentsummerizer.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username); // Spring Data will implement this
}
