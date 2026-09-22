import { useState, type FormEvent } from "react";
import {
  createProsumer,
  getProsumerByNic,
  reactivateProsumer,
} from "../../services/prosumerService";
import type {
  CreateProsumerRequest,
  Prosumer,
  UpdateProsumerRequest,
} from "../../types/user";
import ProsumerFormModal from "../../components/ProsumerFormModal";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Management Page
 * Author: Dilki
 * Description: Allows Backoffice users to search, register,
 *              edit and request deactivation of prosumer accounts.
 */

const ProsumerManagementPage = () => {
  const [nic, setNic] = useState("");

  const [prosumer, setProsumer] = useState<Prosumer | null>(null);

  const [loading, setLoading] = useState(false);

  const [actionLoading, setActionLoading] = useState(false);

  const [error, setError] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  const [showRegisterModal, setShowRegisterModal] = useState(false);

  /**
   * Extract a useful API error message.
   */
  const getErrorMessage = (err: unknown, fallback: string) => {
    const error = err as {
      response?: {
        data?: {
          message?: string;
          error?: string;
          title?: string;
        };
      };
      message?: string;
    };

    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.response?.data?.title ||
      error.message ||
      fallback
    );
  };

  /**
   * Search for a prosumer.
   */
  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setError("");
    setSuccessMessage("");
    setProsumer(null);

    if (!nic.trim()) {
      setError("Please enter a NIC number.");

      return;
    }

    try {
      setLoading(true);

      const data = await getProsumerByNic(nic.trim());

      setProsumer(data);
    } catch (err) {
      console.error("Failed to find prosumer:", err);

      setError(getErrorMessage(err, "Prosumer not found."));
    } finally {
      setLoading(false);
    }
  };

  /**
   * Register a new prosumer.
   */
  const handleRegisterProsumer = async (
    data: CreateProsumerRequest | UpdateProsumerRequest,
  ) => {
    try {
      setActionLoading(true);
      setError("");
      setSuccessMessage("");

      await createProsumer(data as CreateProsumerRequest);

      setShowRegisterModal(false);

      setSuccessMessage("Prosumer registered successfully.");

      // Automatically search for the new account.
      setNic((data as CreateProsumerRequest).nic);

      const createdProsumer = await getProsumerByNic(
        (data as CreateProsumerRequest).nic,
      );

      setProsumer(createdProsumer);
    } catch (err) {
      console.error("Failed to register prosumer:", err);

      setError(getErrorMessage(err, "Unable to register the prosumer."));

      throw err;
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Clear search and messages.
   */
  const handleClear = () => {
    setNic("");
    setProsumer(null);
    setError("");
    setSuccessMessage("");
  };

  return (
    <div className="page-container">
      {/* Header */}
      <div className="page-header">
        <div>
          <h1>Prosumer Management</h1>

          <p>Search and manage registered prosumer accounts.</p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={() => {
            setError("");
            setSuccessMessage("");
            setShowRegisterModal(true);
          }}
        >
          + Register Prosumer
        </button>
      </div>

      {/* Error */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Success */}
      {successMessage && (
        <div className="alert alert-success">
          <span>{successMessage}</span>

          <button
            type="button"
            className="message-close-button"
            onClick={() => setSuccessMessage("")}
          >
            ×
          </button>
        </div>
      )}

      {/* Search */}
      <div className="content-card">
        <div className="card-header">
          <div>
            <h2>Find Prosumer</h2>

            <p>Search using the prosumer's NIC number.</p>
          </div>
        </div>

        <form className="prosumer-search-form" onSubmit={handleSearch}>
          <div className="form-group">
            <label htmlFor="prosumer-nic">NIC</label>

            <input
              id="prosumer-nic"
              type="text"
              value={nic}
              onChange={(event) => setNic(event.target.value)}
              placeholder="Enter prosumer NIC"
              disabled={loading}
            />
          </div>

          <div className="prosumer-search-actions">
            <button type="submit" className="primary-button" disabled={loading}>
              {loading ? "Searching..." : "Search"}
            </button>

            <button
              type="button"
              className="secondary-button"
              onClick={handleClear}
            >
              Clear
            </button>
          </div>
        </form>
      </div>

      {/* Details */}
      {prosumer && (
        <div className="content-card prosumer-details-card">
          <div className="card-header">
            <div>
              <h2>Prosumer Details</h2>

              <p>Account information</p>
            </div>

            <span className={`status-badge ${prosumer.status.toLowerCase()}`}>
              {prosumer.status === "ACTIVE"
                ? "Active"
                : prosumer.status === "INACTIVE"
                  ? "Inactive"
                  : "Pending Deactivation"}
            </span>
          </div>

          <div className="details-grid">
            <div className="detail-item">
              <span>NIC</span>
              <strong>{prosumer.nic}</strong>
            </div>

            <div className="detail-item">
              <span>Name</span>
              <strong>{prosumer.name}</strong>
            </div>

            <div className="detail-item">
              <span>Email</span>
              <strong>{prosumer.email}</strong>
            </div>

            <div className="detail-item">
              <span>Phone</span>
              <strong>{prosumer.phone || "—"}</strong>
            </div>

            <div className="detail-item detail-item-full">
              <span>Address</span>
              <strong>{prosumer.address || "—"}</strong>
            </div>
          </div>

          <div className="prosumer-actions">
            {prosumer.status === "PENDING_DEACTIVATION" && (
              <span className="pending-message">
                Deactivation request is pending Backoffice review.
              </span>
            )}

            {prosumer.status === "INACTIVE" && (
              <button
                type="button"
                className="primary-button"
                onClick={async () => {
                  const confirmed = window.confirm(
                    `Reactivate ${prosumer.name}'s account?`,
                  );

                  if (!confirmed) {
                    return;
                  }

                  try {
                    setActionLoading(true);
                    setError("");
                    setSuccessMessage("");

                    await reactivateProsumer(prosumer.nic);

                    setSuccessMessage("Prosumer reactivated successfully.");

                    const updated = await getProsumerByNic(prosumer.nic);

                    setProsumer(updated);
                  } catch (err) {
                    console.error("Failed to reactivate prosumer:", err);

                    setError(
                      getErrorMessage(
                        err,
                        "Unable to reactivate the prosumer.",
                      ),
                    );
                  } finally {
                    setActionLoading(false);
                  }
                }}
                disabled={actionLoading}
              >
                Reactivate
              </button>
            )}
          </div>
        </div>
      )}

      {/* Register modal */}
      {showRegisterModal && (
        <ProsumerFormModal
          mode="create"
          onClose={() => setShowRegisterModal(false)}
          onSubmit={handleRegisterProsumer}
        />
      )}
    </div>
  );
};

export default ProsumerManagementPage;
