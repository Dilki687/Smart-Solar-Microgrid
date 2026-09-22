import { useEffect, useState } from "react";
import {
  getProsumerByNic,
  requestProsumerDeactivation,
  updateProsumer,
} from "../../services/prosumerService";
import { useAuth } from "../../context/AuthContext";
import type { Prosumer, UpdateProsumerRequest } from "../../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Profile Page
 * Author: Dilki
 * Description: Allows a logged-in prosumer to view and update
 *              their profile and request account deactivation.
 */

const ProsumerProfilePage = () => {
  const { user } = useAuth();

  const [prosumer, setProsumer] = useState<Prosumer | null>(null);

  const [loading, setLoading] = useState(true);

  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  const [name, setName] = useState("");

  const [email, setEmail] = useState("");

  const [phone, setPhone] = useState("");

  const [address, setAddress] = useState("");

  /**
   * Load the logged-in prosumer profile.
   */
  const loadProfile = async () => {
    if (!user?.nic) {
      setError("Unable to determine your NIC.");

      setLoading(false);

      return;
    }

    try {
      setLoading(true);
      setError("");

      const data = await getProsumerByNic(user.nic);

      setProsumer(data);

      setName(data.name);
      setEmail(data.email);
      setPhone(data.phone);
      setAddress(data.address);
    } catch (err) {
      console.error("Failed to load profile:", err);

      setError("Unable to load your profile.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, [user?.nic]);

  /**
   * Save profile changes.
   */
  const handleSave = async () => {
    if (!user?.nic) {
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccessMessage("");

      const request: UpdateProsumerRequest = {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        address: address.trim(),
      };

      await updateProsumer(user.nic, request);

      setSuccessMessage("Profile updated successfully.");

      await loadProfile();
    } catch (err) {
      console.error("Failed to update profile:", err);

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
          "Unable to update your profile.",
      );
    } finally {
      setSaving(false);
    }
  };

  /**
   * Request account deactivation.
   */
  const handleDeactivationRequest = async () => {
    if (!user?.nic) {
      return;
    }

    const confirmed = window.confirm(
      "Are you sure you want to request account deactivation?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccessMessage("");

      await requestProsumerDeactivation(user.nic);

      setSuccessMessage("Your deactivation request has been submitted.");

      await loadProfile();
    } catch (err) {
      console.error("Failed to request deactivation:", err);

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
          "Unable to submit the deactivation request.",
      );
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="content-card">
          <div className="loading-state">Loading profile...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>My Profile</h1>

          <p>View and manage your prosumer account.</p>
        </div>

        {prosumer && (
          <span className={`status-badge ${prosumer.status.toLowerCase()}`}>
            {prosumer.status === "ACTIVE"
              ? "Active"
              : prosumer.status === "INACTIVE"
                ? "Inactive"
                : "Pending Deactivation"}
          </span>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {successMessage && (
        <div className="alert alert-success">{successMessage}</div>
      )}

      {prosumer && (
        <div className="content-card">
          <div className="profile-form">
            <div className="form-group">
              <label>NIC</label>

              <input type="text" value={prosumer.nic} disabled />

              <small className="form-help">NIC cannot be changed.</small>
            </div>

            <div className="form-group">
              <label htmlFor="profile-name">Full Name</label>

              <input
                id="profile-name"
                type="text"
                value={name}
                onChange={(event) => setName(event.target.value)}
                disabled={saving || prosumer.status === "INACTIVE"}
              />
            </div>

            <div className="form-group">
              <label htmlFor="profile-email">Email</label>

              <input
                id="profile-email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                disabled={saving || prosumer.status === "INACTIVE"}
              />
            </div>

            <div className="form-group">
              <label htmlFor="profile-phone">Phone</label>

              <input
                id="profile-phone"
                type="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                disabled={saving || prosumer.status === "INACTIVE"}
              />
            </div>

            <div className="form-group">
              <label htmlFor="profile-address">Address</label>

              <textarea
                id="profile-address"
                value={address}
                onChange={(event) => setAddress(event.target.value)}
                rows={4}
                disabled={saving || prosumer.status === "INACTIVE"}
              />
            </div>

            <div className="profile-actions">
              <button
                type="button"
                className="primary-button"
                onClick={handleSave}
                disabled={saving || prosumer.status !== "ACTIVE"}
              >
                {saving ? "Saving..." : "Save Changes"}
              </button>

              {prosumer.status === "ACTIVE" && (
                <button
                  type="button"
                  className="danger-button"
                  onClick={handleDeactivationRequest}
                  disabled={saving}
                >
                  Request Account Deactivation
                </button>
              )}

              {prosumer.status === "PENDING_DEACTIVATION" && (
                <div className="pending-message">
                  Your deactivation request is awaiting Backoffice approval.
                </div>
              )}

              {prosumer.status === "INACTIVE" && (
                <div className="inactive-message">
                  This account is currently inactive.
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProsumerProfilePage;
