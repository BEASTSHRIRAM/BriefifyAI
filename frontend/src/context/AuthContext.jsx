// frontend/src/context/AuthContext.jsx
import React, { createContext, useState, useEffect, useContext } from 'react';

// Create the Auth Context
const AuthContext = createContext(null);

// Create a custom hook to use the Auth Context easily
export const useAuth = () => {
  return useContext(AuthContext);
};

// Auth Provider component
export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem('jwtToken'));
  const [isAuthenticated, setIsAuthenticated] = useState(!!token); // Convert token to boolean

  useEffect(() => {
    // This effect runs when 'token' state changes
    setIsAuthenticated(!!token);
    if (token) {
      localStorage.setItem('jwtToken', token);
    } else {
      localStorage.removeItem('jwtToken');
    }
  }, [token]);

  // Function to handle login (sets token)
  const login = (newToken) => {
    setToken(newToken);
  };

  // Function to handle logout (clears token)
  const logout = () => {
    setToken(null);
  };

  // The value provided to consumers of this context
  const authContextValue = {
    token,
    isAuthenticated,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={authContextValue}>
      {children}
    </AuthContext.Provider>
  );
};