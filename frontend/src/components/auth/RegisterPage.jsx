// frontend/src/components/Auth/RegisterPage.jsx
import React, { useState } from 'react';
import { Link as ReactRouterLink, useNavigate } from 'react-router-dom';
import axios from 'axios';

function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false); // NEW: State for password visibility
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      setLoading(false);
      return;
    }

    try {
      await axios.post(import.meta.env.VITE_BACKEND_API_URL+'/api/auth/register',{
        username,
        password,
      });

      setSuccess('Registration successful! You can now log in.');
      setUsername('');
      setPassword('');
      setConfirmPassword('');
      setShowPassword(false); // Reset password visibility state
      setTimeout(() => navigate('/login'), 2000); // Redirect to login page

    } catch (err) {
      console.error('Registration error:', err.response || err);
      if (err.response && err.response.data) {
        setError(err.response.data);
      } else {
        setError('Registration failed. Please try again later.');
      }
    } finally {
      setLoading(false);
    }
  };

  const togglePasswordVisibility = () => { // NEW: Function to toggle password visibility
    setShowPassword(!showPassword);
  };

  return (
    <div className="centered-content">
      <div className="auth-form-container">
        <h2>Register for Briefify</h2>
        <p style={{ textAlign: 'center', color: 'var(--color-text-medium)', marginBottom: '18px', fontSize: '17px' }}>
          Create your account and start summarizing documents!
        </p>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="username">Username or Email</label>
            <input
              type="text"
              id="username"
              name="username"
              placeholder="Choose a username or email"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>
          <div className="form-group password-group">
            <label htmlFor="password">Password</label>
            <input
              type={showPassword ? 'text' : 'password'}
              id="password"
              name="password"
              placeholder="Choose a strong password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <span className="password-toggle" onClick={togglePasswordVisibility}>
              {showPassword ? 'Hide' : 'Show'}
            </span>
          </div>
          <div className="form-group password-group">
            <label htmlFor="confirmPassword">Confirm Password</label>
            <input
              type={showPassword ? 'text' : 'password'}
              id="confirmPassword"
              name="confirmPassword"
              placeholder="Confirm your password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
            />
            <span className="password-toggle" onClick={togglePasswordVisibility}>
              {showPassword ? 'Hide' : 'Show'}
            </span>
          </div>
          {error && <p className="error-message">{error}</p>}
          {success && <p className="success-message">{success}</p>}
          <button type="submit" className="auth-button register-button" disabled={loading}>
            {loading ? 'Registering...' : 'Register'}
          </button>
        </form>
        <p className="auth-link-text">
          Already have an account?{' '}
          <ReactRouterLink to="/login">
            Login here
          </ReactRouterLink>
        </p>
      </div>
    </div>
  );
}

export default RegisterPage;
