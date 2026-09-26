import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router";
import { getMyReservations } from "../../services/reservationService";
import type { Reservation } from "../../types/reservation";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Dashboard
 * Author: Brian
 * Description: Provides the prosumer's landing page with reservation
 *              counts, upcoming bookings and quick actions.
 */

// Normalises the reservation status which the backend may serialise
// either as a string enum ("PENDING") or as its numeric index (0..3).
const getStatusLabel = (
  status: Reservation["status"],
): "PENDING" | "CONFIRMED" | "COMPLETED" | "CANCELLED" | "UNKNOWN" => {
  if (status === "PENDING" || status === 0) {
    return "PENDING";
  }

  if (status === "CONFIRMED" || status === 1) {
    return "CONFIRMED";
  }

  if (status === "COMPLETED" || status === 2) {
    return "COMPLETED";
  }

  if (status === "CANCELLED" || status === 3) {
    return "CANCELLED";
  }

  return "UNKNOWN";
};

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

const ProsumerDashboard = () => {
  const navigate = useNavigate();

  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadDashboard = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getMyReservations();
      setReservations(data);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load your dashboard.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboard();
  }, []);

  // Group reservations by normalised status so counts stay correct
  // whether the API returns enum names or enum indexes.
  const counts = useMemo(() => {
    const summary = {
      total: reservations.length,
      pending: 0,
      confirmed: 0,
      upcoming: 0,
      completed: 0,
      cancelled: 0,
    };

    const now = Date.now();

    for (const reservation of reservations) {
      const status = getStatusLabel(reservation.status);

      if (status === "PENDING") {
        summary.pending += 1;
      } else if (status === "CONFIRMED") {
        summary.confirmed += 1;

        const startsAt = new Date(
          reservation.scheduledStartTime,
        ).getTime();

        if (!Number.isNaN(startsAt) && startsAt >= now) {
          summary.upcoming += 1;
        }
      } else if (status === "COMPLETED") {
        summary.completed += 1;
      } else if (status === "CANCELLED") {
        summary.cancelled += 1;
      }
    }

    return summary;
  }, [reservations]);

  // Upcoming reservations (confirmed with a future start), sorted by
  // the earliest start time; capped at 5 for the dashboard preview.
  const upcomingReservations = useMemo(() => {
    const now = Date.now();

    return reservations
      .filter((reservation) => {
        if (getStatusLabel(reservation.status) !== "CONFIRMED") {
          return false;
        }

        const startsAt = new Date(
          reservation.scheduledStartTime,
        ).getTime();

        return !Number.isNaN(startsAt) && startsAt >= now;
      })
      .sort((a, b) => {
        return (
          new Date(a.scheduledStartTime).getTime() -
          new Date(b.scheduledStartTime).getTime()
        );
      })
      .slice(0, 5);
  }, [reservations]);

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Prosumer Dashboard</h1>
          <p>
            Track your energy reservations, upcoming bookings and
            available quick actions.
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
      <div className="dashboard-stats dashboard-stats--five">
        <div className="dashboard-stat-card tone-blue">
          <div className="dashboard-stat-icon">📋</div>
          <div>
            <div className="dashboard-stat-label">
              Total Reservations
            </div>
            <div className="dashboard-stat-value">
              {loading ? "—" : counts.total}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card tone-amber">
          <div className="dashboard-stat-icon">⏳</div>
          <div>
            <div className="dashboard-stat-label">Pending</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : counts.pending}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card tone-violet">
          <div className="dashboard-stat-icon">⚡</div>
          <div>
            <div className="dashboard-stat-label">
              Upcoming Confirmed
            </div>
            <div className="dashboard-stat-value">
              {loading ? "—" : counts.upcoming}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card tone-green">
          <div className="dashboard-stat-icon">✓</div>
          <div>
            <div className="dashboard-stat-label">Completed</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : counts.completed}
            </div>
          </div>
        </div>

        <div className="dashboard-stat-card tone-rose">
          <div className="dashboard-stat-icon">✕</div>
          <div>
            <div className="dashboard-stat-label">Cancelled</div>
            <div className="dashboard-stat-value">
              {loading ? "—" : counts.cancelled}
            </div>
          </div>
        </div>
      </div>

      {/* Upcoming Reservations */}
      <div className="management-card">
        <div className="management-card-header">
          <div>
            <h2>Upcoming Reservations</h2>
            <span className="table-subtext">
              Confirmed bookings scheduled from now onwards
            </span>
          </div>

          <button
            type="button"
            className="primary-button"
            onClick={() => navigate("/prosumer/reservations")}
          >
            View All Reservations
          </button>
        </div>

        {loading ? (
          <div className="empty-state">
            Loading your reservations...
          </div>
        ) : upcomingReservations.length === 0 ? (
          <div className="empty-state">
            <h3>No upcoming reservations</h3>
            <p>
              You do not have any confirmed reservations scheduled
              yet. Book a slot to reserve energy from a nearby
              microgrid station.
            </p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="management-table">
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Station</th>
                  <th>Schedule</th>
                  <th>Energy</th>
                  <th>Change Request</th>
                </tr>
              </thead>

              <tbody>
                {upcomingReservations.map((reservation) => {
                  const changeStatus =
                    reservation.changeRequestStatus;

                  return (
                    <tr key={reservation.reservationId}>
                      <td>
                        <strong>{reservation.reservationId}</strong>

                        <div className="table-subtext">
                          {reservation.slotId}
                        </div>
                      </td>

                      <td>{reservation.stationId}</td>

                      <td>
                        <div>
                          {formatDateTime(
                            reservation.scheduledStartTime,
                          )}
                        </div>

                        <div className="table-subtext">
                          to{" "}
                          {formatDateTime(
                            reservation.scheduledEndTime,
                          )}
                        </div>
                      </td>

                      <td>{reservation.energyAmountKwh} kWh</td>

                      <td>
                        {changeStatus ? (
                          <span
                            className={`status-badge ${
                              changeStatus === "REJECTED"
                                ? "inactive"
                                : ""
                            }`}
                          >
                            {changeStatus}
                          </span>
                        ) : (
                          <span className="table-subtext">—</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Actions */}
      <h2>Quick Actions</h2>

      <div className="quick-actions">
        <Link to="/prosumer/booking" className="action-card">
          <h3>Book Energy</h3>

          <p>
            Reserve energy from an available booking slot within the
            next seven days.
          </p>
        </Link>

        <Link to="/prosumer/reservations" className="action-card">
          <h3>My Reservations</h3>

          <p>
            View, modify or cancel your existing energy reservations.
          </p>
        </Link>

        <Link to="/prosumer/profile" className="action-card">
          <h3>My Profile</h3>

          <p>
            View or update your prosumer account information.
          </p>
        </Link>
      </div>
    </div>
  );
};

export default ProsumerDashboard;
