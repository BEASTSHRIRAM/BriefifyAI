// frontend/src/context/ThemeContext.jsx
import React, { createContext, useContext, useState, useEffect } from 'react';

// 1. Create the Theme Context
const ThemeContext = createContext(null);

// 2. Custom Hook to easily use the theme context
export const useTheme = () => {
  return useContext(ThemeContext);
};

// 3. Theme Provider Component
export const ThemeProvider = ({ children }) => {
  // Initialize theme from localStorage or system preference
  const getInitialTheme = () => {
    if (typeof window !== 'undefined' && localStorage.getItem('theme')) {
      return localStorage.getItem('theme');
    }
    // Check system preference
    if (typeof window !== 'undefined' && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  };

  const [theme, setTheme] = useState(getInitialTheme);

  useEffect(() => {
    // Update data-theme attribute on <html> tag
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    // Save preference to localStorage
    localStorage.setItem('theme', theme);
  }, [theme]); // Rerun when theme changes

  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === 'light' ? 'dark' : 'light'));
  };

  const themeContextValue = {
    theme,
    toggleTheme,
  };

  return (
    <ThemeContext.Provider value={themeContextValue}>
      {children}
    </ThemeContext.Provider>
  );
};