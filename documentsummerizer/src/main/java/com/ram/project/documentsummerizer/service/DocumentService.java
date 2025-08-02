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
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.ram.project.documentsummerizer.model.User;

@Service
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final GroqService groqService;

    private static final Pattern MEANINGFUL_TEXT_PATTERN = Pattern.compile("[a-zA-Z0-9]{3,}");
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

            if (isTextQualitySufficient(extractedText, MIN_TEXT_LENGTH_FOR_SUMMARY_PDFBOX)) {
                textSufficientForSummary = true;
                processingStatus = "Processed via PDFBox (native text).";
                logger.info("PDFBox text deemed sufficient. Skipping OCR.");
            } else {
                logger.warn("PDFBox extracted text insufficient. Attempting OCR.");
            }

        } catch (IOException e) {
            logger.warn("PDFBox extraction failed: {}", e.getMessage());
            pdfBoxExtractionPerformed = true;
        }

        if (!textSufficientForSummary) {
            logger.warn("Attempting OCR as fallback.");
            responseMessageBuilder.append("Warning: PDFBox extraction was insufficient. Attempted OCR.\n");
            processingStatus = "Attempting OCR.";

            try (InputStream isForOcr = file.getInputStream()) {
                extractedText = performOcrOnPdfStream(isForOcr);
                logger.info("OCR extracted text length: {}", extractedText.length());
                processingStatus = "Document processed via OCR.";

                if (isTextQualitySufficient(extractedText, MIN_TEXT_LENGTH_FOR_SUMMARY_OCR)) {
                    textSufficientForSummary = true;
                } else {
                    processingStatus = "OCR yielded insufficient text.";
                    responseMessageBuilder.append("Warning: OCR text is too weak for summarization.\n");
                }

                if (isLikelyHandwrittenOrPoorQuality(extractedText, originalFileName)) {
                    responseMessageBuilder.append("Warning: Detected potential handwriting or low-quality scan.\n");
                }

            } catch (TesseractException te) {
                logger.error("OCR failed: {}", te.getMessage(), te);
                extractedText = "OCR failed to extract readable text. Reason: " + te.getMessage();
                processingStatus = "OCR failed.";
                responseMessageBuilder.append("Error: OCR process failed.\n");
            } catch (IOException e) {
                logger.error("OCR read error: {}", e.getMessage(), e);
                extractedText = "Failed to re-read PDF file for OCR.";
                processingStatus = "OCR input failure.";
                responseMessageBuilder.append("Error: Could not process file for OCR.\n");
            }
        }

        if (textSufficientForSummary) {
            String textForSummary = extractedText.length() > SUMMARY_INPUT_MAX_LENGTH
                    ? extractedText.substring(0, SUMMARY_INPUT_MAX_LENGTH) + "\n\n... [Document truncated for summarization]"
                    : extractedText;

            try {
                summary = groqService.summarizeText(textForSummary);
            } catch (Exception e) {
                logger.error("Summarization failed: {}", e.getMessage(), e);
                summary = "Failed to generate summary.";
                responseMessageBuilder.append("Error: Summarization failed.\n");
            }

            if (extractedText.length() > SUMMARY_INPUT_MAX_LENGTH) {
                responseMessageBuilder.append("Warning: Text was truncated due to length.\n");
            }

        } else {
            summary = "Summary not generated due to insufficient text.";
        }

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

        responseMessageBuilder.insert(0, "--- Processing Complete! ---\nFile: " + originalFileName + "\nStatus: " + processingStatus + "\n\n");

        if (!responseMessageBuilder.toString().contains("Warning:") && !responseMessageBuilder.toString().contains("Error:")) {
            responseMessageBuilder.insert(responseMessageBuilder.length(), "\nAll good!\n");
        } else {
            responseMessageBuilder.insert(responseMessageBuilder.length(), "\n--- Important Messages ---\n");
        }

        responseMessageBuilder.append("\n--- Extracted Text (first 500 chars) ---\n");
        responseMessageBuilder.append(extractedText.substring(0, Math.min(extractedText.length(), 500))).append("...\n\n");
        responseMessageBuilder.append("--- Summary ---\n").append(summary);

        return responseMessageBuilder.toString();
    }

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
                    logger.warn("Tesseract failed on page {}: {}", i, e.getMessage());
                }
            }
        }
        return ocrText.toString();
    }

    private boolean isTextQualitySufficient(String text, int minLength) {
        if (text == null || text.trim().isEmpty()) {
            logger.debug("isTextQualitySufficient: Empty input");
            return false;
        }

        String cleaned = text.replaceAll("[^A-Za-z0-9\\s]", "").trim().replaceAll("\\s+", " ");
        String[] words = cleaned.split(" ");
        long meaningfulWords = words.length == 0 ? 0 : 
            java.util.Arrays.stream(words).filter(w -> w.length() >= 3 && MEANINGFUL_TEXT_PATTERN.matcher(w).matches()).count();

        boolean hasEnoughLength = cleaned.length() >= minLength;
        boolean hasEnoughWords = meaningfulWords >= 5;

        logger.debug("isTextQualitySufficient: Cleaned length={}, Words={}, MeaningfulWords={}, LengthOK={}, WordsOK={}",
                cleaned.length(), words.length, meaningfulWords, hasEnoughLength, hasEnoughWords);

        return hasEnoughLength && hasEnoughWords;
    }

    private boolean isLikelyHandwrittenOrPoorQuality(String ocrText, String fileName) {
        if (ocrText.length() < 200 && fileName.toLowerCase().contains("handwritten")) {
            return true;
        }
        long alphanumericChars = ocrText.chars().filter(Character::isLetterOrDigit).count();
        double density = (double) alphanumericChars / ocrText.length();
        return ocrText.length() > 0 && density < 0.2;
    }

    public List<Document> getUserDocuments(String userId) {
        return documentRepository.findByUserId(userId);
    }

    public Optional<Document> getDocumentById(String id) {
        return documentRepository.findById(id);
    }
}
