// frontend/src/components/Documents/DocumentHistoryPage.jsx
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';
import { Link as ReactRouterLink } from 'react-router-dom';
import LogoutButton from '../auth/LogoutButton'; // Ensure LogoutButton import

function DocumentHistoryPage() {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { token, isAuthenticated } = useAuth(); // Get token and auth status

  useEffect(() => {
    const fetchDocuments = async () => {
      if (!isAuthenticated || !token) {
        setLoading(false);
        setError('You must be logged in to view document history.');
        return;
      }
      try {
        const res = await axios.get('http://localhost:8080/api/documents/user', {
          headers: {
            'Authorization': `Bearer ${token}`
          },
        });
        setDocuments(res.data);
      } catch (err) {
        console.error('Failed to fetch documents:', err.response || err);
        setError('Failed to load document history. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchDocuments();
  }, [isAuthenticated, token]); // Re-fetch when auth state/token changes

  if (loading) {
    return (
      <div className="main-dashboard-container">
        <h2 className="main-dashboard-title">Loading Document History...</h2>
        <p className="status-message">Please wait.</p>
        <div className="logout-button-wrapper"><LogoutButton /></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="main-dashboard-container">
        <h2 className="main-dashboard-title">Document History</h2>
        <p className="error-message">{error}</p>
        <div className="logout-button-wrapper"><LogoutButton /></div>
      </div>
    );
  }

  return (
    <div className="main-dashboard-container">
      <div className="dashboard-top-bar-history"> {/* New class for history header */}
        <h2 className="main-dashboard-title">Your Document History</h2>
        <ReactRouterLink to="/upload" className="upload-new-doc-link">Upload New Document</ReactRouterLink>
      </div>

      {documents.length === 0 ? (
        <p className="status-message">No documents uploaded yet. Go to <ReactRouterLink to="/upload">Upload</ReactRouterLink> page to add one.</p>
      ) : (
        <div className="document-list"> {/* New class for document list container */}
          {documents.map((doc) => (
            <div key={doc.id} className="document-item"> {/* New class for each document item */}
              <p className="document-filename">{doc.originalFileName}</p>
              <div className="summary-preview-box">
                <span className="summary-label">Summary:</span>
                <span className="summary-text">{doc.summary ? doc.summary.substring(0, Math.min(doc.summary.length, 200)) + (doc.summary.length > 200 ? '...' : '') : 'No summary.'}</span>
              </div>
              <ReactRouterLink to={`/document/${doc.id}`} className="view-details-link">View Details</ReactRouterLink>
            </div>
          ))}
        </div>
      )}

      <div className="logout-button-wrapper">
        <LogoutButton />
      </div>
    </div>
  );
}

export default DocumentHistoryPage;