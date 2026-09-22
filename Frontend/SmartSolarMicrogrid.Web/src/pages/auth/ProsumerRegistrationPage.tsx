import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router";
import { createProsumer } from "../../services/prosumerService";
import type { CreateProsumerRequest } from "../../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Registration Page
 * Author: Dilki
 * Description: Allows a new prosumer to create an account.
 */

const ProsumerRegistrationPage = () => {
  const navigate = useNavigate();

  const [formData, setFormData] = useState<CreateProsumerRequest>({
    nic: "",
    name: "",
    email: "",
    phone: "",
    address: "",
    password: "",
  });

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState("");

  const [success, setSuccess] = useState("");

  /**
   * Update form fields.
   */
  const handleChange = (field: keyof CreateProsumerRequest, value: string) => {
    setFormData((previous) => ({
      ...previous,
      [field]: value,
    }));
  };

  /**
   * Submit registration.
   */
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (
      !formData.nic.trim() ||
      !formData.name.trim() ||
      !formData.email.trim() ||
      !formData.phone.trim() ||
      !formData.address.trim() ||
      !formData.password.trim()
    ) {
      setError("Please fill in all required fields.");

      return;
    }

    try {
      setLoading(true);

      await createProsumer(formData);

      setSuccess("Registration successful. You can now log in.");

      setFormData({
        nic: "",
        name: "",
        email: "",
        phone: "",
        address: "",
        password: "",
      });
    } catch (err) {
      console.error("Prosumer registration failed:", err);

      const error = err as {
        response?: {
          data?: {
            message?: string;
            error?: string;
            title?: string;
          };
        };
      };

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.response?.data?.title ||
          "Registration failed. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <h1>Prosumer Registration</h1>

          <p>Create your Smart Solar Microgrid account.</p>
        </div>

        {error && <div className="alert alert-error">{error}</div>}

        {success && (
          <div className="alert alert-success">
            {success}

            <button
              type="button"
              className="secondary-button"
              onClick={() => navigate("/login")}
            >
              Go to Login
            </button>
          </div>
        )}

        <form className="user-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="registration-nic">NIC</label>

            <input
              id="registration-nic"
              type="text"
              value={formData.nic}
              onChange={(event) => handleChange("nic", event.target.value)}
              placeholder="Enter NIC"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="registration-name">Full Name</label>

            <input
              id="registration-name"
              type="text"
              value={formData.name}
              onChange={(event) => handleChange("name", event.target.value)}
              placeholder="Enter full name"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="registration-email">Email</label>

            <input
              id="registration-email"
              type="email"
              value={formData.email}
              onChange={(event) => handleChange("email", event.target.value)}
              placeholder="Enter email"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="registration-phone">Phone</label>

            <input
              id="registration-phone"
              type="tel"
              value={formData.phone}
              onChange={(event) => handleChange("phone", event.target.value)}
              placeholder="Enter phone number"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="registration-address">Address</label>

            <textarea
              id="registration-address"
              value={formData.address}
              onChange={(event) => handleChange("address", event.target.value)}
              placeholder="Enter address"
              rows={3}
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="registration-password">Password</label>

            <input
              id="registration-password"
              type="password"
              value={formData.password}
              onChange={(event) => handleChange("password", event.target.value)}
              placeholder="Create a password"
              disabled={loading}
            />
          </div>

          <button
            type="submit"
            className="primary-button auth-submit-button"
            disabled={loading}
          >
            {loading ? "Creating Account..." : "Create Prosumer Account"}
          </button>

          <button
            type="button"
            className="secondary-button auth-secondary-button"
            onClick={() => navigate("/login")}
            disabled={loading}
          >
            Back to Login
          </button>
        </form>
      </div>
    </div>
  );
};

export default ProsumerRegistrationPage;
