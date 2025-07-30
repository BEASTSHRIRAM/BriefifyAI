package com.ram.project.documentsummerizer; 

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;


@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.ram.project.documentsummerizer.repository")
public class DocumentSummarizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentSummarizerApplication.class, args);
    }

}
