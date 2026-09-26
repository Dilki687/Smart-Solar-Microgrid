import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { getStations } from "../../services/stationService";
import { getBookingSlots } from "../../services/bookingSlotService";
import type { Station } from "../../types/station";
import type { BookingSlot } from "../../types/bookingSlot";
import StationMap from "../../components/StationMap";
import TruncatedId from "../../components/TruncatedId";

const GridOperatorDashboard = () => {
  const navigate = useNavigate();

  const [stations, setStations] = useState<Station[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = async () => {
    try {
      setLoading(true);
      setError("");

      const [stationData, slotData] = await Promise.all([
        getStations(),
        getBookingSlots(),
      ]);

      setStations(stationData);
      setSlots(slotData);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load Grid Operator dashboard.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  const activeStations = stations.filter(
    (station) => station.status === "ACTIVE",
  );

  const inactiveStations = stations.filter(
    (station) => station.status !== "ACTIVE",
  );

  const activeSlots = slots.filter((slot) => slot.isActive);

  const totalAvailableCapacity = activeSlots.reduce(
    (total, slot) => total + slot.availableCapacity,
    0,
  );

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Grid Operator Dashboard</h1>
          <p>
            Monitor microgrid stations, booking availability, and station
            locations.
          </p>
        </div>

        <button
          type="button"
          className="secondary-button"
          onClick={loadDashboard}
          disabled={loading}
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
      </div>

      {error && (
        <div className="management-message error">
          {error}
        </div>
      )}

      {/* Summary Cards */}
      <div className="dashboard-stats">
        <div className="dashboard-stat-card">
          <div className="dashboard-stat-icon">📍</div>
          <div>
            <div className="dashboard-stat-label">Total Stations</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : stations.length}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card">
          <div className="dashboard-stat-icon">✓</div>
          <div>
            <div className="dashboard-stat-label">Active Stations</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : activeStations.length}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card">
          <div className="dashboard-stat-icon">⚠</div>
          <div>
            <div className="dashboard-stat-label">Inactive Stations</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : inactiveStations.length}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card">
          <div className="dashboard-stat-icon">⚡</div>
          <div>
            <div className="dashboard-stat-label">
              Available Slot Capacity
            </div>
            <div className="dashboard-stat-value">
              {loading ? "—" : totalAvailableCapacity}
            </div>
          </div>
        </div>
      </div>

      {/* Google Maps */}
      <div className="management-card">
        <div className="management-card-header">
          <div>
            <h2>Station Map</h2>
            <span className="table-subtext">
              Active microgrid station locations
            </span>
          </div>

          <button
            type="button"
            className="primary-button"
            onClick={() => navigate("/grid-operator/nearby")}
          >
            View Nearby Stations
          </button>
        </div>

        <StationMap stations={stations} />
      </div>

      {/* Stations */}
      <div className="management-card">
        <div className="management-card-header">
          <div>
            <h2>Microgrid Stations</h2>
            <span className="table-subtext">
              Current station availability
            </span>
          </div>
        </div>

        {loading ? (
          <div className="empty-state">
            Loading station information...
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
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {stations.map((station) => (
                  <tr key={station.stationId}>
                    <td>
                      <strong>{station.name}</strong>

                      <div className="table-subtext">
                        <TruncatedId value={station.stationId} />
                      </div>
                    </td>

                    <td>
                      <div>{station.address}</div>

                      <div className="table-subtext">
                        {station.latitude.toFixed(4)},{" "}
                        {station.longitude.toFixed(4)}
                      </div>
                    </td>

                    <td>{station.capacityKw} kW</td>

                    <td>
                      <span
                        className={
                          station.status === "ACTIVE"
                            ? "status-badge"
                            : "status-badge inactive"
                        }
                      >
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

export default GridOperatorDashboard;