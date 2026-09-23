import { useState } from "react";
import {
  getNearbyStations,
} from "../../services/stationService";
import type { NearbyStation } from "../../types/station";

const NearbyStationsPage = () => {
  const [latitude, setLatitude] = useState("6.9271");
  const [longitude, setLongitude] = useState("79.8612");
  const [radiusKm, setRadiusKm] = useState("10");

  const [stations, setStations] = useState<NearbyStation[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSearch = async () => {
    setError("");
    setSuccess("");

    const lat = Number(latitude);
    const lng = Number(longitude);
    const radius = Number(radiusKm);

    if (!Number.isFinite(lat) || lat < -90 || lat > 90) {
      setError("Latitude must be between -90 and 90.");
      return;
    }

    if (!Number.isFinite(lng) || lng < -180 || lng > 180) {
      setError("Longitude must be between -180 and 180.");
      return;
    }

    if (!Number.isFinite(radius) || radius <= 0) {
      setError("Radius must be greater than zero.");
      return;
    }

    try {
      setLoading(true);
      setSearched(true);

      const data = await getNearbyStations(
        lat,
        lng,
        radius,
      );

      setStations(data);

      setSuccess(
        `${data.length} nearby station${
          data.length === 1 ? "" : "s"
        } found.`,
      );
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to find nearby stations.",
      );
      setStations([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Nearby Stations</h1>
          <p>
            Find active microgrid stations based on location.
          </p>
        </div>
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
          <h2>Search Location</h2>
        </div>

        <div className="station-form">
          <div className="form-group">
            <label htmlFor="latitude">
              Latitude
            </label>

            <input
              id="latitude"
              type="number"
              step="any"
              value={latitude}
              onChange={(event) =>
                setLatitude(event.target.value)
              }
            />
          </div>

          <div className="form-group">
            <label htmlFor="longitude">
              Longitude
            </label>

            <input
              id="longitude"
              type="number"
              step="any"
              value={longitude}
              onChange={(event) =>
                setLongitude(event.target.value)
              }
            />
          </div>

          <div className="form-group">
            <label htmlFor="radius">
              Radius (km)
            </label>

            <input
              id="radius"
              type="number"
              min="0.1"
              step="0.1"
              value={radiusKm}
              onChange={(event) =>
                setRadiusKm(event.target.value)
              }
            />
          </div>

          <div className="form-actions">
            <button
              type="button"
              className="primary-button"
              onClick={handleSearch}
              disabled={loading}
            >
              {loading
                ? "Searching..."
                : "Find Nearby Stations"}
            </button>
          </div>
        </div>
      </div>

      <div className="management-card">
        <div className="management-card-header">
          <h2>Nearby Stations</h2>

          {searched && (
            <span className="table-subtext">
              Within {radiusKm} km
            </span>
          )}
        </div>

        {!searched ? (
          <div className="empty-state">
            Enter a location and search for nearby
            stations.
          </div>
        ) : loading ? (
          <div className="empty-state">
            Finding nearby stations...
          </div>
        ) : stations.length === 0 ? (
          <div className="empty-state">
            No active stations were found within the
            selected radius.
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="management-table">
              <thead>
                <tr>
                  <th>Station</th>
                  <th>Location</th>
                  <th>Distance</th>
                  <th>Available Slots</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {stations.map((station) => (
                  <tr key={station.nodeId}>
                    <td>
                      <strong>{station.name}</strong>

                      <div className="table-subtext">
                        {station.nodeId}
                      </div>
                    </td>

                    <td>
                      <div>
                        {station.latitude.toFixed(4)},{" "}
                        {station.longitude.toFixed(4)}
                      </div>
                    </td>

                    <td>
                      {station.distanceKm.toFixed(2)} km
                    </td>

                    <td>
                      {station.availableSlots}
                    </td>

                    <td>
                      <span className="status-badge">
                        {station.status}
                      </span>
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

export default NearbyStationsPage;