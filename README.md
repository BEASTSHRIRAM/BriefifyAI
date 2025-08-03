# Briefify AI: Document Summarizer Web App
Project Video : https://youtu.be/lJIJzMTuKPs?si=Lnj1U3gIfHYBwPtK 


### Done By Shriram Kulkarni
[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/shriram-kulkarni-033b8328a)

---

## 🚀 Project Overview

Briefify AI is a full-stack web application designed to simplify document processing by providing a robust, secure platform for uploading and summarizing PDF files. This project serves as a comprehensive demonstration of my skills in backend and frontend development, AI integration, and secure application design.

The application intelligently extracts text from various types of PDFs (including scanned documents via OCR) and generates concise summaries using a powerful Large Language Model (LLM) from Groq.

## ✨ Key Features

- **User Authentication:** Secure registration and login using Spring Security with JSON Web Tokens (JWT).
- **Document Upload:** Users can securely upload PDF files (with a size limit of 50 MB) to the application.
- **Intelligent Text Extraction:**
    - Handles native (text-based) PDFs efficiently.
    - Automatically falls back to **Tesseract OCR** for scanned, image-based PDFs.
- **AI-Powered Summarization:** Utilizes **Groq's LLaMA 3** model to generate accurate and concise summaries of document content.
- **Personalized History:** Stores and retrieves document history specific to each authenticated user.
- **Modern & Responsive UI:** A clean and professional user interface with light and dark mode, built with React and custom CSS.

---

## ✅ Code Quality & CI/CD

This project uses **SonarCloud** integrated with **GitHub Actions** to automatically analyze code quality, security, and maintainability on every push. This ensures the codebase remains clean, secure, and ready for future development.

- **Quality Gate:**
  ![Quality Gate Status](https://sonarcloud.io/api/project_badges/quality_gate?project=BEASTSHRIRAM_BriefifyAI)
- **Maintainability:**
  ![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=BEASTSHRIRAM_BriefifyAI&metric=sqale_rating)
- **Security Rating:**
  ![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=BEASTSHRIRAM_BriefifyAI&metric=security_rating)
- **Code Coverage:**
  ![Coverage](https://sonarcloud.io/api/project_badges/measure?project=BEASTSHRIRAM_BriefifyAI&metric=coverage)

**Key Insights:**
- My focus is on writing reliable and maintainable code.
- Identified security hotspots have been reviewed and addressed.
- The next step is to add comprehensive unit tests to improve code coverage.

---

## 🖼️ Application Screenshots

## ⚙️ How to Run the Project

### **Prerequisites**
- **Java 21 JDK:** [Download and install](https://www.oracle.com/java/technologies/downloads/)
- **Node.js (LTS version):** [Download and install](https://nodejs.org/en/download/)
- **Maven:** The project uses the Maven Wrapper (`mvnw`), so a global installation is not required.
- **MongoDB Atlas Cluster:** A free-tier cluster is sufficient.
- **Groq API Key:** [Sign up for Groq Cloud](https://console.groq.com/keys) to get an API key.

### **1. Backend Setup**
1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/yourusername/your-repo-name.git](https://github.com/yourusername/your-repo-name.git)
    cd your-repo-name/documentsummerizer
    ```
2.  **Configure environment variables:**
    * Create a file named `application.properties` in `src/main/resources`.
    * Add your MongoDB Atlas connection string and Groq API key:
    ```properties
    # application.properties
    spring.data.mongodb.uri=mongodb+srv://<USERNAME>:<PASSWORD>@<CLUSTER-URL>/<DB-NAME>?retryWrites=true&w=majority
    spring.data.mongodb.database=<DB-NAME>
    groq.api.key=YOUR_GROQ_API_KEY_HERE
    ```
    * You will also need to configure your Tesseract data path in `DocumentService.java`.
3.  **Run the application:**
    * In the terminal, from the `documentsummerizer` directory:
    ```bash
    .\mvnw spring-boot:run
    ```

### **2. Frontend Setup**
1.  **Navigate to the frontend directory:**
    ```bash
    cd ../frontend
    ```
2.  **Install dependencies:**
    ```bash
    npm install
    ```
3.  **Start the development server:**
    ```bash
    npm run dev
    ```
    You can try still some problems in hosting
    https://briefify-ai.vercel.app/

The frontend will be available at `http://localhost:5173`. You can now register a new user and start summarizing documents!

## 🤝 Contribution & License

Feel free to open issues or submit pull requests for improvements. This project is a learning endeavor and is open to enhancements.
