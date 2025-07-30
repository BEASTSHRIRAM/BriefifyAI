// frontend/src/components/Auth/LogoutButton.jsx
import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

function LogoutButton() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    // Removed absolute positioning, place it within a flex container
    <button onClick={handleLogout} className="auth-button logout-button">
      Logout
    </button>
  );
}

export default LogoutButton;