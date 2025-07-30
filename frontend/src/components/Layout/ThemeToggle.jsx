// frontend/src/components/Layout/ThemeToggle.jsx
import React from 'react';
import { useTheme } from '../../context/ThemeContext';

function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();

  return (
    <label className="theme-switch">
      <input
        type="checkbox"
        checked={theme === 'dark'}
        onChange={toggleTheme}
        aria-label="Toggle dark/light mode"
      />
      <span className="slider" />
      <span className="theme-label">{theme === 'light' ? 'Light' : 'Dark'}</span>
    </label>
  );
}

export default ThemeToggle;