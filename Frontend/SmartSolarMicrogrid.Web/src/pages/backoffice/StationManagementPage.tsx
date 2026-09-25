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

  const handleSubmit = async (
    event: FormEvent,
  ) => {
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

    if (
      form.latitude < -90 ||
      form.latitude > 90
    ) {
      setError("Latitude must be between -90 and 90.");
      return;
    }

    if (
      form.longitude < -180 ||
      form.longitude > 180
    ) {
      setError(
        "Longitude must be between -180 and 180.",
      );
      return;
    }

    if (form.capacityKw <= 0) {
      setError(
        "Capacity must be greater than zero.",
      );
      return;
    }

    if (!form.operatorUserId.trim()) {
      setError("Operator user ID is required.");
      return;
    }

    try {
      setSaving(true);

      if (editingStationId) {
        await updateStation(
          editingStationId,
          form,
        );

        setSuccess(
          "Station updated successfully.",
        );
      } else {
        await createStation(form);

        setSuccess(
          "Station created successfully.",
        );
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

    setError("");
    setSuccess("");
  };

  const handleDeactivate = async (
    station: Station,
  ) => {
    const confirmed = window.confirm(
      `Deactivate "${station.name}"?`,
    );

    if (!confirmed) {
      return;
    }

    setError("");
    setSuccess("");

    try {
      await deactivateStation(
        station.stationId,
      );

      setSuccess(
        "Station deactivated successfully.",
      );

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
          Manage solar microgrid stations,
          locations and operating capacity.
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

      <div className="management-card">
        <div className="management-card-header">
          <div>
            <h2>
              {editingStationId
                ? "Edit Station"
                : "Create Station"}
            </h2>

            <p>
              Enter the station information below.
            </p>
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
                handleChange(
                  "name",
                  event.target.value,
                )
              }
              placeholder="Main Solar Station"
            />
          </div>

          <div className="form-group">
            <label htmlFor="station-address">
              Address
            </label>

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
        </form>
      </div>

      <div className="management-card">
        <div className="management-card-header">
          <div>
            <h2>Stations</h2>

            <p>
              {stations.length} station
              {stations.length === 1 ? "" : "s"}
              found.
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
                  <th>Location</th>
                  <th>Capacity</th>
                  <th>Operator</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {stations.map((station) => (
                  <tr key={station.stationId}>
                    <td>
                      <strong>
                        {station.name}
                      </strong>

                      <span className="table-subtext">
                        {station.stationId}
                      </span>
                    </td>

                    <td>
                      <div>
                        {station.address}
                      </div>

                      <span className="table-subtext">
                        {station.latitude},{" "}
                        {station.longitude}
                      </span>
                    </td>

                    <td>
                      {station.capacityKw} kW
                    </td>

                    <td>
                      {station.operatorUserId}
                    </td>

                    <td>
                      <span
                        className={`status-badge ${
                          station.status.toLowerCase()
                        }`}
                      >
                        {station.status}
                      </span>
                    </td>

                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="secondary-button small"
                          onClick={() =>
                            handleEdit(station)
                          }
                        >
                          Edit
                        </button>

                        {station.status ===
                          "ACTIVE" && (
                          <button
                            type="button"
                            className="danger-button small"
                            onClick={() =>
                              handleDeactivate(
                                station,
                              )
                            }
                          >
                            Deactivate
                          </button>
                        )}
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

export default StationManagementPage;