import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useParams, useNavigate } from 'react-router-dom'; 
import { useAuth } from '../../context/AuthContext';
import LogoutButton from '../auth/LogoutButton'; 

function DocumentDetailPage() {
  const { id } = useParams();
  const [document, setDocument] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { token, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDocument = async () => {
      if (!isAuthenticated || !token) {
        setLoading(false);
        navigate('/login');
        return;
      }
      try {
       const res = await axios.get(
  `${import.meta.env.VITE_BACKEND_API_URL}/api/documents/${id}`,
  {
    headers: {
      'Authorization': `Bearer ${token}`
    },
  }
);
        setDocument(res.data);
      } catch (err) {
        if (err.response) {
          console.error('Failed to fetch document details:', err.response.status, err.response.data);
        } else if (err.request) {
          console.error('No response received:', err.request);
        } else {
          console.error('Error:', err.message);
        }
        if (err.response && (err.response.status === 403 || err.response.status === 404)) {
            setError('Document not found or you do not have permission to view it.');
        } else {
            setError('Failed to load document details. Please try again.');
        }
      } finally {
        setLoading(false);
      }
    };

    fetchDocument();
  }, [id, isAuthenticated, token, navigate]);

  if (loading) {
    return (
      <div className="main-dashboard-container">
        <h2 className="main-dashboard-title">Loading Document Details...</h2>
        <p className="status-message">Please wait.</p>
        <div className="logout-button-wrapper"><LogoutButton /></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="main-dashboard-container">
        <h2 className="main-dashboard-title">Document Details</h2>
        <p className="error-message">{error}</p>
        <button onClick={() => navigate('/history')} className="auth-button login-button" style={{width: 'auto', marginTop: '10px'}}>Back to History</button>
        <div className="logout-button-wrapper"><LogoutButton /></div>
      </div>
    );
  }

  if (!document) {
      return (
          <div className="main-dashboard-container">
              <h2 className="main-dashboard-title">Document Not Found</h2>
              <p className="status-message">The requested document could not be loaded.</p>
              <button onClick={() => navigate('/history')} className="auth-button login-button" style={{width: 'auto', marginTop: '10px'}}>Back to History</button>
              <div className="logout-button-wrapper"><LogoutButton /></div>
          </div>
      );
  }


  return (
    <div className="main-dashboard-container">
      <div className="dashboard-top-bar-history"> 
        <h2 className="main-dashboard-title">{document.originalFileName}</h2>
        <button onClick={() => navigate('/history')} className="auth-button login-button" style={{width: 'auto', margin: '0 10px'}}>Back to History</button> {/* Inline style for button size */}
      </div>

      <div className="response-display"> 
        <h3>Document Content Overview</h3>
        {document.extractedText && (
          <div className="summary-content">
            <h4 className="extracted-text-heading">Extracted Text:</h4>
            <pre className="extracted-text">{document.extractedText}</pre>
          </div>
        )}
        
        {document.summary && (
          <div className="summary-content">
            <h4 className="summary-heading">Summary:</h4>
            <pre className="summary-text">{document.summary}</pre>
          </div>
        )}
      </div>

      <div className="logout-button-wrapper">
        <LogoutButton />
      </div>
    </div>
  );
}

export default DocumentDetailPage;
