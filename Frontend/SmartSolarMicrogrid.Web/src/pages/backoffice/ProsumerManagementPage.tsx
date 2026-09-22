import { useState, type FormEvent } from "react";
import { getProsumerByNic } from "../../services/prosumerService";
import type { Prosumer } from "../../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Management Page
 * Author: Dilki
 * Description: Allows Backoffice users to search for and
 *              view individual prosumer accounts.
 */

const ProsumerManagementPage = () => {
  const [nic, setNic] = useState("");

  const [prosumer, setProsumer] = useState<Prosumer | null>(null);

  const [loading, setLoading] = useState(false);

  const [error, setError] = useState("");

  /**
   * Search for a prosumer using their NIC.
   */
  const handleSearch = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setError("");
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
          "Prosumer not found.",
      );
    } finally {
      setLoading(false);
    }
  };

  /**
   * Clear the current search.
   */
  const handleClear = () => {
    setNic("");
    setProsumer(null);
    setError("");
  };

  return (
    <div className="page-container">
      {/* Page header */}
      <div className="page-header">
        <div>
          <h1>Prosumer Management</h1>

          <p>Search and manage registered prosumer accounts.</p>
        </div>
      </div>

      {/* Search card */}
      <div className="content-card">
        <div className="card-header">
          <div>
            <h2>Find Prosumer</h2>

            <p>Search for a prosumer using their NIC number.</p>
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

      {/* Error */}
      {error && <div className="alert alert-error">{error}</div>}

      {/* Prosumer details */}
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
            <button type="button" className="secondary-button" disabled>
              Edit
            </button>

            {prosumer.status === "ACTIVE" && (
              <button type="button" className="danger-button" disabled>
                Request Deactivation
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ProsumerManagementPage;
