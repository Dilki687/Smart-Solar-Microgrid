import { useEffect, useState, type FormEvent } from "react";
import type {
  CreateStationRequest,
  Station,
} from "../../types/station";
import {
  createStation,
  deactivateStation,
  getStations,
  updateStation,
} from "../../services/stationService";
import TruncatedId from "../../components/TruncatedId";

const emptyForm: CreateStationRequest = {
  name: "",
  address: "",
  latitude: 0,
  longitude: 0,
  capacityKw: 0,
  operatorUserId: "",
};

const StationManagementPage = () => {
  const [stations, setStations] = useState<Station[]>([]);
  const [form, setForm] =
    useState<CreateStationRequest>(emptyForm);

  const [editingStationId, setEditingStationId] =
    useState<string | null>(null);

  // Currently displayed station in the detail modal.
  const [viewingStation, setViewingStation] =
    useState<Station | null>(null);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadStations = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getStations();
      setStations(data);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ??
          "Failed to load stations.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStations();
  }, []);

  const handleChange = (
    field: keyof CreateStationRequest,
    value: string,
  ) => {
    setForm((current) => ({
      ...current,
      [field]:
        field === "latitude" ||
        field === "longitude" ||
        field === "capacityKw"
          ? Number(value)
          : value,
    }));
  };

  const resetForm = () => {
    setForm(emptyForm);
    setEditingStationId(null);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (!form.name.trim()) {
      setError("Station name is required.");
      return;
    }

    if (form.name.trim().length < 2) {
      setError(
        "Station name must contain at least 2 characters.",
      );
      return;
    }

    if (form.address.trim().length < 5) {
      setError(
        "Address must contain at least 5 characters.",
      );
      return;
    }

    if (form.latitude < -90 || form.latitude > 90) {
      setError("Latitude must be between -90 and 90.");
      return;
    }

    if (form.longitude < -180 || form.longitude > 180) {
      setError(
        "Longitude must be between -180 and 180.",
      );
      return;
    }

    if (form.capacityKw <= 0) {
      setError("Capacity must be greater than zero.");
      return;
    }

    if (!form.operatorUserId.trim()) {
      setError("Operator user ID is required.");
      return;
    }

    try {
      setSaving(true);

      if (editingStationId) {
        await updateStation(editingStationId, form);
        setSuccess("Station updated successfully.");
      } else {
        await createStation(form);
        setSuccess("Station created successfully.");
      }

      resetForm();
      await loadStations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ??
          "Unable to save station.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (station: Station) => {
    setEditingStationId(station.stationId);

    setForm({
      name: station.name,
      address: station.address,
      latitude: station.latitude,
      longitude: station.longitude,
      capacityKw: station.capacityKw,
      operatorUserId: station.operatorUserId,
    });

    // Close the detail modal and clear any prior messages so
    // the user sees the freshly populated form.
    setViewingStation(null);
    setError("");
    setSuccess("");

    // Scroll the form into view so the user sees what happened.
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const handleDeactivate = async (station: Station) => {
    const confirmed = window.confirm(
      `Deactivate "${station.name}"?`,
    );

    if (!confirmed) {
      return;
    }

    setError("");
    setSuccess("");

    try {
      await deactivateStation(station.stationId);
      setSuccess("Station deactivated successfully.");
      setViewingStation(null);
      await loadStations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ??
          "Unable to deactivate station.",
      );
    }
  };

  return (
    <div>
      <div className="dashboard-header">
        <h1>Station Management</h1>

        <p>
          Manage solar microgrid stations, locations and
          operating capacity.
        </p>
      </div>

      {error && (
        <div className="management-message error">
          {error}
        </div>
      )}

      {success && (
        <div className="management-message success">
          {success}
        </div>
      )}

      <div className="station-split-layout">
        {/* LEFT — Create / Edit form */}
        <div className="management-card">
          <div className="management-card-header">
            <div>
              <h2>
                {editingStationId
                  ? "Edit Station"
                  : "Create Station"}
              </h2>

              <p>Enter the station information below.</p>
            </div>

            {editingStationId && (
              <button
                type="button"
                className="secondary-button"
                onClick={resetForm}
              >
                Cancel Edit
              </button>
            )}
          </div>

          <form
            className="station-form"
            onSubmit={handleSubmit}
          >
            <div className="form-group">
              <label htmlFor="station-name">
                Station Name
              </label>

              <input
                id="station-name"
                value={form.name}
                onChange={(event) =>
                  handleChange("name", event.target.value)
                }
                placeholder="Main Solar Station"
              />
            </div>

            <div className="form-group">
              <label htmlFor="station-address">Address</label>

              <input
                id="station-address"
                value={form.address}
                onChange={(event) =>
                  handleChange(
                    "address",
                    event.target.value,
                  )
                }
                placeholder="Station address"
              />
            </div>

            <div className="form-group">
              <label htmlFor="station-latitude">
                Latitude
              </label>

              <input
                id="station-latitude"
                type="number"
                step="any"
                value={form.latitude}
                onChange={(event) =>
                  handleChange(
                    "latitude",
                    event.target.value,
                  )
                }
              />
            </div>

            <div className="form-group">
              <label htmlFor="station-longitude">
                Longitude
              </label>

              <input
                id="station-longitude"
                type="number"
                step="any"
                value={form.longitude}
                onChange={(event) =>
                  handleChange(
                    "longitude",
                    event.target.value,
                  )
                }
              />
            </div>

            <div className="form-group">
              <label htmlFor="station-capacity">
                Capacity (kW)
              </label>

              <input
                id="station-capacity"
                type="number"
                min="0.1"
                step="0.1"
                value={form.capacityKw}
                onChange={(event) =>
                  handleChange(
                    "capacityKw",
                    event.target.value,
                  )
                }
              />
            </div>

            <div className="form-group">
              <label htmlFor="station-operator">
                Operator User ID
              </label>

              <input
                id="station-operator"
                value={form.operatorUserId}
                onChange={(event) =>
                  handleChange(
                    "operatorUserId",
                    event.target.value,
                  )
                }
                placeholder="GRID-OPERATOR-001"
              />
            </div>

            <div className="form-group full-width">
              <div className="form-actions">
                <button
                  type="submit"
                  className="primary-button"
                  disabled={saving}
                >
                  {saving
                    ? "Saving..."
                    : editingStationId
                      ? "Update Station"
                      : "Create Station"}
                </button>

                {editingStationId && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={resetForm}
                  >
                    Clear
                  </button>
                )}
              </div>
            </div>
          </form>
        </div>

        {/* RIGHT — Compact stations list */}
        <div className="management-card">
          <div className="management-card-header">
            <div>
              <h2>Stations</h2>

              <p>
                {stations.length} station
                {stations.length === 1 ? "" : "s"} found.
              </p>
            </div>
          </div>

          {loading ? (
            <div className="empty-state">
              Loading stations...
            </div>
          ) : stations.length === 0 ? (
            <div className="empty-state">
              No stations found.
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="management-table">
                <thead>
                  <tr>
                    <th>Station</th>
                    <th style={{ width: "1%" }}>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {stations.map((station) => (
                    <tr key={station.stationId}>
                      <td>
                        <strong>{station.name}</strong>

                        <div className="table-subtext">
                          <TruncatedId
                            value={station.stationId}
                          />
                          {" · "}
                          <span
                            className={`status-badge ${station.status.toLowerCase()}`}
                          >
                            {station.status}
                          </span>
                        </div>
                      </td>

                      <td>
                        <div className="table-actions">
                          <button
                            type="button"
                            className="primary-button small"
                            onClick={() =>
                              setViewingStation(station)
                            }
                          >
                            View
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

      {viewingStation && (
        <StationDetailModal
          station={viewingStation}
          onClose={() => setViewingStation(null)}
          onEdit={() => handleEdit(viewingStation)}
          onDeactivate={() =>
            handleDeactivate(viewingStation)
          }
        />
      )}
    </div>
  );
};

// ---------------------------------------------------------------
// Station detail modal — read-only view of one station with the
// same Edit and Deactivate actions the row previously exposed.
// ---------------------------------------------------------------

interface StationDetailModalProps {
  station: Station;
  onClose: () => void;
  onEdit: () => void;
  onDeactivate: () => void;
}

const StationDetailModal = ({
  station,
  onClose,
  onEdit,
  onDeactivate,
}: StationDetailModalProps) => {
  return (
    <div
      className="modal-overlay"
      role="dialog"
      aria-modal="true"
      onClick={onClose}
    >
      <div
        className="modal-container"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="modal-header">
          <div>
            <h2>{station.name}</h2>
            <p>Station details</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Close station details"
          >
            ×
          </button>
        </div>

        <div className="station-detail-body">
          <div className="station-detail-row">
            <span className="detail-label">Station ID</span>
            <span className="detail-value">
              {station.stationId}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Status</span>
            <span className="detail-value">
              <span
                className={`status-badge ${station.status.toLowerCase()}`}
              >
                {station.status}
              </span>
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Address</span>
            <span className="detail-value">
              {station.address}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Coordinates</span>
            <span className="detail-value">
              {station.latitude}, {station.longitude}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Capacity</span>
            <span className="detail-value">
              {station.capacityKw} kW
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Operator</span>
            <span className="detail-value">
              {station.operatorUserId}
            </span>
          </div>
        </div>

        <div className="station-detail-actions">
          <button
            type="button"
            className="secondary-button"
            onClick={onEdit}
          >
            Edit
          </button>

          {station.status === "ACTIVE" && (
            <button
              type="button"
              className="danger-button"
              onClick={onDeactivate}
            >
              Deactivate
            </button>
          )}

          <button
            type="button"
            className="secondary-button"
            onClick={onClose}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};

export default StationManagementPage;
