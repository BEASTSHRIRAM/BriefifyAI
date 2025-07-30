// frontend/src/components/Layout/Header.jsx
import React from 'react';
import { Link as ReactRouterLink } from 'react-router-dom';
import logo from '/logo.png';
import ThemeToggle from './ThemeToggle';

function Header() {
  return (
    <header className="app-header">
      {/* NEW: Inner container to control content width and alignment */}
      <div className="header-inner-content">
        <div className="header-content">
          <img src={logo} alt="Briefify AI Logo" className="app-logo" />
          <h1 className="app-title">Briefify AI</h1>
        </div>
        <nav className="main-nav">
          <ReactRouterLink to="/upload" className="nav-link">Upload</ReactRouterLink>
          <ReactRouterLink to="/history" className="nav-link">History</ReactRouterLink>
          <ThemeToggle />
        </nav>
      </div>
    </header>
  );
}

export default Header;