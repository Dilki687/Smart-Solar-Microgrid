import { useEffect, useState } from "react";
import {
  deactivateProsumer,
  getDeactivationRequests,
  reactivateProsumer,
} from "../../services/prosumerService";
import type { DeactivationRequest } from "../../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Deactivation Requests Page
 * Author: Dilki
 * Description: Allows Backoffice users to review pending
 *              prosumer deactivation requests and deactivate
 *              or reactivate accounts.
 */

const DeactivationRequestsPage = () => {
  const [requests, setRequests] = useState<DeactivationRequest[]>([]);

  const [loading, setLoading] = useState(true);

  const [actionLoading, setActionLoading] = useState(false);

  const [error, setError] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  /**
   * Get a useful API error message.
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
   * Load pending deactivation requests.
   */
  const loadRequests = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getDeactivationRequests();

      setRequests(data);
    } catch (err) {
      console.error("Failed to load deactivation requests:", err);

      setError(getErrorMessage(err, "Unable to load deactivation requests."));
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load requests when page opens.
   */
  useEffect(() => {
    loadRequests();
  }, []);

  /**
   * Approve a deactivation request.
   */
  const handleApprove = async (request: DeactivationRequest) => {
    const confirmed = window.confirm(`Deactivate ${request.name}'s account?`);

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");
      setSuccessMessage("");

      await deactivateProsumer(request.nic);

      setSuccessMessage(`${request.name}'s account has been deactivated.`);

      await loadRequests();
    } catch (err) {
      console.error("Failed to deactivate prosumer:", err);

      setError(getErrorMessage(err, "Unable to deactivate the prosumer."));
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Reactivate an inactive prosumer.
   *
   * This is also useful for the Backoffice if an account
   * needs to be restored later.
   */
  const handleReactivate = async (nic: string, name: string) => {
    const confirmed = window.confirm(`Reactivate ${name}'s account?`);

    if (!confirmed) {
      return;
    }

    try {
      setActionLoading(true);
      setError("");
      setSuccessMessage("");

      await reactivateProsumer(nic);

      setSuccessMessage(`${name}'s account has been reactivated.`);

      await loadRequests();
    } catch (err) {
      console.error("Failed to reactivate prosumer:", err);

      setError(getErrorMessage(err, "Unable to reactivate the prosumer."));
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1>Deactivation Requests</h1>

            <p>Review prosumer account deactivation requests.</p>
          </div>
        </div>

        <div className="content-card">
          <div className="loading-state">Loading requests...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h1>Deactivation Requests</h1>

          <p>Review pending prosumer account deactivation requests.</p>
        </div>

        <button
          type="button"
          className="secondary-button"
          onClick={loadRequests}
          disabled={actionLoading}
        >
          Refresh
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

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

      <div className="content-card">
        <div className="card-header">
          <div>
            <h2>Pending Requests</h2>

            <p>
              {requests.length} pending request
              {requests.length !== 1 ? "s" : ""}
            </p>
          </div>
        </div>

        {requests.length === 0 ? (
          <div className="empty-state">
            <h3>No pending requests</h3>

            <p>
              There are currently no prosumer deactivation requests awaiting
              review.
            </p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>NIC</th>
                  <th>Name</th>
                  <th>Requested At</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {requests.map((request) => (
                  <tr key={request.nic}>
                    <td>{request.nic}</td>

                    <td>
                      <strong>{request.name}</strong>
                    </td>

                    <td>
                      {request.requestedAt
                        ? new Date(request.requestedAt).toLocaleString()
                        : "—"}
                    </td>

                    <td>
                      <span className="status-badge pending_deactivation">
                        Pending
                      </span>
                    </td>

                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="danger-button"
                          onClick={() => handleApprove(request)}
                          disabled={actionLoading}
                        >
                          Approve Deactivation
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DeactivationRequestsPage;
