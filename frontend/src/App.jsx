// frontend/src/App.jsx
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import LoginPage from './components/auth/LoginPage';
import RegisterPage from './components/auth/RegisterPage';
import Header from './components/Layout/Header';
import Footer from './components/Layout/Footer';
import DocumentUploadPage from './components/Documents/DocumentUploadPage';

import { useAuth } from './context/AuthContext';

const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  return children;
};


function App() {
  return (
    <Router>
      <div className="app-container">
        <Header />

        <main className="app-main-content">
          <Routes>
            {/* Public Routes */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* Protected Routes */}
            <Route
              path="/upload"
              element={
                <ProtectedRoute>
                  <DocumentUploadPage />
                </ProtectedRoute>
              }
            />

            {/* Default Route: Redirect authenticated users to /upload, others to /login */}
            <Route
              path="/"
              element={<Navigate to="/upload" replace />}
            />
          </Routes>
        </main>

        <Footer />
      </div>
    </Router>
  );
}

export default App;
    </Router>
  );
}

export default App;
