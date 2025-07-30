// frontend/src/components/Documents/DocumentUploadPage.jsx
import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';
import LogoutButton from '../auth/LogoutButton'; // Import LogoutButton
import '../../App.css'; // Ensure global styles are available for spinner

function DocumentUploadPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  // Revert to storing a single string response message
  const [responseMessage, setResponseMessage] = useState(''); // <--- Changed to store string
  const [error, setError] = useState('');
  const { token } = useAuth();

  const handleFileChange = (event) => {
    setSelectedFile(event.target.files[0]);
    setResponseMessage(''); // Clear previous string response
    setError('');
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select a PDF file to upload.');
      return;
    }
    // Frontend File Validation
    if (selectedFile.type !== 'application/pdf') {
      setError('Only PDF files are allowed.');
      setSelectedFile(null);
      return;
    }
    if (selectedFile.size > 50 * 1024 * 1024) { // 50 MB limit
      setError('File size exceeds 50MB limit.');
      setSelectedFile(null);
      return;
    }

    setUploading(true);
    setResponseMessage(''); // Clear previous response
    setError('');

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const res = await axios.post('http://localhost:8080/api/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
          'Authorization': `Bearer ${token}`
        },
      });
      setResponseMessage(res.data); // Store the raw string response from backend
      setSelectedFile(null); // Clear the selected file input
    } catch (err) {
      console.error('File upload error:', err.response || err);
      // Simpler error handling for string response from backend
      if (err.response && typeof err.response.data === 'string') {
        setError('Upload failed: ' + err.response.data);
      } else {
        setError('Upload failed. Please try again. ' + (err.message || ''));
      }
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="centered-content">
      <div className="main-dashboard-container">
        <h2 className="main-dashboard-title">Upload Document for Summarization</h2>

        {/* Upload Guidelines Section (as implemented previously) */}
        <div className="upload-guidelines">
          <p><strong>Tips for best results:</strong></p>
          <ul>
            <li>Only **PDF files** are supported (maximum 50 MB).</li>
            <li>For **scanned documents**, use clear, high-resolution scans of **printed text**.</li>
            <li>**Handwritten notes** may have limited accuracy due to OCR challenges.</li>
            <li>Long documents will be **truncated** for summarization (up to ~10,000 characters).</li>
          </ul>
        </div>

        <div className="upload-section">
          <input type="file" accept=".pdf" onChange={handleFileChange} className="file-input" />
          <button onClick={handleUpload} disabled={uploading} className="upload-button">
            {uploading ? 'Uploading...' : 'Upload PDF'}
          </button>
          {uploading && (
            <div className="modern-spinner" aria-label="Uploading, please wait..."></div>
          )}
        </div>

        {error && <p className="error-message">{error}</p>}

        {responseMessage && ( // Display the raw string response IF 'responseMessage' exists
          <div className="response-display">
            <h3>Processing Complete!</h3>
            <pre>{responseMessage}</pre> {/* Display the raw string here */}
          </div>
        )}

        {/* Logout button at the bottom center */}
        <div className="logout-button-wrapper">
          <LogoutButton />
        </div>
      </div>
    </div>
  );
}

export default DocumentUploadPage;