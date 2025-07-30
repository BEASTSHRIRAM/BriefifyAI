// frontend/src/components/Layout/Footer.jsx
import React from 'react';

function Footer() {
  const linkedinLink = "https://www.linkedin.com/in/shriram-kulkarni-033b8328a";

  return (
    <footer className="app-footer">
      {/* NEW: Inner container to control content width */}
      <div className="footer-inner-content">
        <p className="footer-text">
          Co-Founder of Briefify AI{' '}
          <a href={linkedinLink} target="_blank" rel="noopener noreferrer" className="linkedin-link">
            <img src="/linkedin.png" alt="LinkedIn Logo" className="linkedin-logo" />
            Shriram Kulkarni
          </a>
        </p>
      </div>
    </footer>
  );
}

export default Footer;