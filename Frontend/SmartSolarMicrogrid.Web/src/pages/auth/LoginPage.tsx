import { useState } from "react";
import type { FormEvent } from "react";

import { useNavigate } from "react-router";

import { useAuth } from "../../context/AuthContext";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Login Page
 * Author: Dilki
 * Description: Provides the login interface for Backoffice,
 *              Grid Operator and Prosumer users.
 */

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [identifier, setIdentifier] = useState("");

  const [password, setPassword] = useState("");

  const [error, setError] = useState("");

  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    setError("");
    setLoading(true);

    try {
      // Authenticate the user through the backend API.
      await login(identifier, password);

      // Read the authenticated user from local storage.
      const storedUser = localStorage.getItem("smartSolarUser");

      if (!storedUser) {
        throw new Error("User information was not returned.");
      }

      const authenticatedUser = JSON.parse(storedUser);

      // Redirect the user according to their role.
      switch (authenticatedUser.role) {
        case "BACKOFFICE":
          navigate("/backoffice");
          break;

        case "GRID_OPERATOR":
          navigate("/grid-operator");
          break;

        case "PROSUMER":
          navigate("/prosumer");
          break;

        default:
          setError("Unknown user role.");
      }
    } catch (err: any) {
      // Display a readable error message when authentication fails.
      const message =
        err?.response?.data?.message ||
        "Login failed. Please check your credentials.";

      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <h1>Smart Solar Microgrid</h1>

          <p>Sign in to your account</p>
        </div>

        {error && <div className="error-message">{error}</div>}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>NIC or Email</label>

            <input
              type="text"
              value={identifier}
              onChange={(event) => setIdentifier(event.target.value)}
              placeholder="Enter NIC or email"
              required
            />
          </div>

          <div className="form-group">
            <label>Password</label>

            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Enter password"
              required
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "Signing in..." : "Sign In"}
          </button>
          <p className="registration-link">
            New to the system?{" "}
            <button
              type="button"
              onClick={() => navigate("/register/prosumer")}
            >
              Register as Prosumer
            </button>
          </p>
        </form>
      </div>
    </div>
  );
};

export default LoginPage;
