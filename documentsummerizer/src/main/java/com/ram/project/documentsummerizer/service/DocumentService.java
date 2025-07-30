// frontend/src/main/java/com.ram.project.documentsummerizer/service/DocumentService.java
package com.ram.project.documentsummerizer.service;

import com.ram.project.documentsummerizer.model.Document;
import com.ram.project.documentsummerizer.repository.DocumentRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.pdfbox.rendering.PDFRenderer;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger; // Using SLF4J Logger
import org.slf4j.LoggerFactory; // Using SLF4J LoggerFactory
import java.util.regex.Pattern;
import java.util.Optional; // Import Optional

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.ram.project.documentsummerizer.model.User;


@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final GroqService groqService;

    private static final Pattern MEANINGFUL_TEXT_PATTERN = Pattern.compile(".*[a-zA-Z0-9].*");
    private static final int MIN_TEXT_LENGTH_FOR_SUMMARY_PDFBOX = 500;
    private static final int MIN_TEXT_LENGTH_FOR_SUMMARY_OCR = 100;
    private static final int SUMMARY_INPUT_MAX_LENGTH = 20000;

    public DocumentService(DocumentRepository documentRepository, GroqService groqService) {
        this.documentRepository = documentRepository;
        this.groqService = groqService;
    }

    public String processAndSaveDocument(MultipartFile file) throws IOException, TesseractException {
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
        String extractedText = "";
        String summary = "Summary could not be generated.";
        StringBuilder responseMessageBuilder = new StringBuilder();
        String processingStatus = "Processing failed.";

        if (!originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed.");
        }

        boolean pdfBoxExtractionPerformed = false;
        boolean textSufficientForSummary = false;

        try (InputStream is = file.getInputStream()) {
            extractedText = extractTextFromNativePdf(is);
            logger.info("PDFBox extracted text length: {}", extractedText.length());
            pdfBoxExtractionPerformed = true;
            if (isTextQualitySufficient(extractedText, MIN_TEXT_LENGTH_FOR_SUMMARY_PDFBOX)) { // Pass specific min length
                textSufficientForSummary = true;
                processingStatus = "Processed via PDFBox (native text).";
                logger.info("PDFBox text deemed sufficient (Length: {} >= {}). Skipping OCR.", extractedText.length(), MIN_TEXT_LENGTH_FOR_SUMMARY_PDFBOX);
            } else {
                logger.warn("PDFBox extracted text is too short (cleaned length: {}) for native text summary. Attempting OCR.", extractedText.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]", "").length());
            }
        } catch (IOException e) {
            logger.warn("PDFBox initial read/extraction failed: {}", e.getMessage());
            pdfBoxExtractionPerformed = true; // Still counts as an attempt
        }

        if (!textSufficientForSummary) {
            logger.warn("Attempting OCR because PDFBox was insufficient or failed to extract enough text.");
            responseMessageBuilder.append("Warning: PDFBox extraction was insufficient. Attempted OCR.\n");
            processingStatus = "Attempting OCR.";

            try (InputStream isForOcr = file.getInputStream()) {
                extractedText = performOcrOnPdfStream(isForOcr);
                logger.info("Tesseract OCR extracted total text length: {}", extractedText.length());
                processingStatus = "Document processed via OCR.";

                if (isTextQualitySufficient(extractedText, MIN_TEXT_LENGTH_FOR_SUMMARY_OCR)) { // Pass specific min length
                    textSufficientForSummary = true;
                } else {
                    processingStatus = "OCR yielded insufficient text.";
                    responseMessageBuilder.append("Warning: OCR attempted but extracted text is still insufficient for a good summary. Please ensure document quality.\n");
                }
                if (isLikelyHandwrittenOrPoorQuality(extractedText, originalFileName)) {
                    responseMessageBuilder.append("Warning: Handwriting OCR is experimental and may produce inaccurate results for handwritten notes or low-quality scans.\n");
                }

            } catch (TesseractException te) {
                logger.error("Tesseract OCR failed: {}", te.getMessage(), te);
                extractedText = "OCR failed to extract readable text. Reason: " + te.getMessage();
                processingStatus = "OCR failed.";
                responseMessageBuilder.append("Error: Failed to perform OCR. Please check scan quality or try a different document.\n");
            } catch (IOException e) {
                logger.error("Error re-reading PDF file for OCR: {}", e.getMessage(), e);
                extractedText = "Failed to re-read PDF file for OCR.";
                processingStatus = "File re-reading for OCR failed.";
                responseMessageBuilder.append("Error: Could not re-read PDF file for OCR. It might be corrupted.\n");
            }
        }


        // 3. Summarize content using Groq LLaMA
        if (textSufficientForSummary) {
            String textForSummary = extractedText;
            if (extractedText.length() > SUMMARY_INPUT_MAX_LENGTH) {
                textForSummary = extractedText.substring(0, SUMMARY_INPUT_MAX_LENGTH) + "\n\n... [Document truncated for summarization due to length]";
                logger.warn("Text truncated for summarization due to length: {}", textForSummary.length());
                responseMessageBuilder.append("Warning: Document was very long; content truncated for summarization.\n");
            }

            try {
                summary = groqService.summarizeText(textForSummary);
            } catch (Exception e) {
                logger.error("Groq API summarization failed: {}", e.getMessage(), e);
                summary = "Failed to generate summary from Groq API.";
                responseMessageBuilder.append("Error: Summarization service failed to respond.\n");
            }
        } else {
            summary = "Summary not generated due to insufficient extracted text.";
        }

        // 4. Save to MongoDB
        Document doc = new Document(originalFileName, extractedText);
        doc.setSummary(summary);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String)) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            if (userDetails instanceof User) {
                doc.setUserId(((User) userDetails).getId());
            } else {
                doc.setUserId(userDetails.getUsername());
            }
        } else {
            doc.setUserId("anonymous");
        }
        documentRepository.save(doc);

        // 5. Prepare final String response
        responseMessageBuilder.insert(0, "--- Processing Complete! ---\nFile: " + originalFileName + "\nStatus: " + processingStatus + "\n\n");
        
        if (!responseMessageBuilder.toString().contains("Warning:") && !responseMessageBuilder.toString().contains("Error:")) {
            int statusLineEnd = responseMessageBuilder.indexOf("Status:") + "Status: ".length() + processingStatus.length();
            int insertIndex = responseMessageBuilder.indexOf("\n\n", statusLineEnd);
            if (insertIndex == -1) insertIndex = responseMessageBuilder.length();
            responseMessageBuilder.insert(insertIndex, "\nAll good!\n");
        } else {
            int statusLineEnd = responseMessageBuilder.indexOf("Status:") + "Status: ".length() + processingStatus.length();
            int insertIndex = responseMessageBuilder.indexOf("\n\n", statusLineEnd);
            if (insertIndex == -1) insertIndex = responseMessageBuilder.length();
            responseMessageBuilder.insert(insertIndex, "\n--- Important Messages ---\n");
        }


        responseMessageBuilder.append("\n--- Extracted Text (first 500 chars) ---\n");
        responseMessageBuilder.append(extractedText.substring(0, Math.min(extractedText.length(), 500))).append("...\n\n");
        responseMessageBuilder.append("--- Summary ---\n").append(summary);

        return responseMessageBuilder.toString();
    }

    // --- Helper Methods ---

    private String extractTextFromNativePdf(InputStream is) throws IOException {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    private String performOcrOnPdfStream(InputStream is) throws IOException, TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
        tesseract.setLanguage("eng");

        StringBuilder ocrText = new StringBuilder();

        try (PDDocument document = PDDocument.load(is)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(i, 300);
                try {
                    String pageText = tesseract.doOCR(image);
                    ocrText.append(pageText).append("\n");
                } catch (TesseractException e) {
                    logger.warn("Tesseract OCR failed on page {}: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error during OCR process (PDF rendering to image): {}", e.getMessage(), e);
            throw new TesseractException("Failed to render PDF page for OCR: " + e.getMessage(), e);
        }
        return ocrText.toString();
    }

    // NEW: Overloaded method for text sufficiency check (default for PDFBox)
    private boolean isTextQualitySufficient(String text) {
        return isTextQualitySufficient(text, MIN_TEXT_LENGTH_FOR_SUMMARY_PDFBOX);
    }

    // Checks if the extracted text is sufficient and meaningful for summarization based on a given minLength.
    private boolean isTextQualitySufficient(String text, int minLength) {
        if (text == null || text.trim().isEmpty()) {
            logger.debug("isTextQualitySufficient: Text is null or empty. Returning false.");
            return false;
        }
        String cleanText = text.replaceAll("\\s+", "").replaceAll("[^a-zA-Z0-9]", "");
        logger.debug("isTextQualitySufficient: Original text length: {}", text.length());
        logger.debug("isTextQualitySufficient: Cleaned text length: {}", cleanText.length());
        logger.debug("isTextQualitySufficient: Required Min Length: {}", minLength);
        
        boolean lengthSufficient = cleanText.length() >= minLength;
        boolean containsMeaningfulChars = MEANINGFUL_TEXT_PATTERN.matcher(text).matches();
        logger.debug("isTextQualitySufficient: Regex Pattern: '{}'", MEANINGFUL_TEXT_PATTERN.pattern());
        logger.debug("isTextQualitySufficient: Regex Match Result (on original text): {}", containsMeaningfulChars);
        logger.debug("isTextQualitySufficient: Length Sufficient? {}, Contains Meaningful Chars? {}. Final Result: {}",
                     lengthSufficient, containsMeaningfulChars, (lengthSufficient && containsMeaningfulChars));
        
        return lengthSufficient && containsMeaningfulChars;
    }

    private boolean isLikelyHandwrittenOrPoorQuality(String ocrText, String fileName) {
        if (ocrText.length() < 200 && fileName.toLowerCase().contains("handwritten")) {
            return true;
        }
        long alphanumericChars = ocrText.chars().filter(Character::isLetterOrDigit).count();
        double density = (double) alphanumericChars / ocrText.length();
        if (ocrText.length() > 0 && density < 0.2) {
            return true;
        }
        return false;
    }

    public List<Document> getUserDocuments(String userId) {
        return documentRepository.findByUserId(userId);
    }

    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
    }
}