import { useEffect, useState } from "react";
import {
  approveReservation,
  approveReservationChange,
  getReservations,
  rejectReservation,
  rejectReservationChange,
} from "../../services/reservationService";
import type { Reservation } from "../../types/reservation";

const ReservationManagementPage = () => {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [selectedStatus, setSelectedStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(null);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadReservations = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getReservations(
        selectedStatus || undefined,
      );

      setReservations(data);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load reservations.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReservations();
  }, [selectedStatus]);

  const handleApprove = async (reservationId: string) => {
    if (!window.confirm("Approve this reservation?")) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await approveReservation(reservationId);

      setSuccess("Reservation approved successfully.");
      await loadReservations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be approved.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleReject = async (reservationId: string) => {
    const reason = window.prompt(
      "Enter the reason for rejecting this reservation:",
    );

    if (!reason || !reason.trim()) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await rejectReservation(reservationId, {
        reason: reason.trim(),
      });

      setSuccess("Reservation rejected successfully.");
      await loadReservations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be rejected.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleApproveChange = async (
    reservationId: string,
  ) => {
    if (!window.confirm("Approve this reservation change?")) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await approveReservationChange(reservationId);

      setSuccess(
        "Reservation change approved successfully.",
      );

      await loadReservations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation change could not be approved.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectChange = async (
    reservationId: string,
  ) => {
    const reason = window.prompt(
      "Enter the reason for rejecting this change:",
    );

    if (!reason || !reason.trim()) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await rejectReservationChange(reservationId, {
        reason: reason.trim(),
      });

      setSuccess(
        "Reservation change rejected successfully.",
      );

      await loadReservations();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation change could not be rejected.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Reservation Management</h1>
          <p>
            Review reservations and manage approval and change
            requests.
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
          <h2>Reservations</h2>

          <select
            value={selectedStatus}
            onChange={(event) =>
              setSelectedStatus(event.target.value)
            }
          >
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="CONFIRMED">Confirmed</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </div>

        {loading ? (
          <div className="empty-state">
            Loading reservations...
          </div>
        ) : reservations.length === 0 ? (
          <div className="empty-state">
            No reservations found.
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="management-table">
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Station</th>
                  <th>Slot</th>
                  <th>Prosumer</th>
                  <th>Schedule</th>
                  <th>Energy</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {reservations.map((reservation) => (
                  <tr key={reservation.reservationId}>
                    <td>
                      <strong>
                        {reservation.reservationId}
                      </strong>
                    </td>

                    <td>{reservation.stationId}</td>

                    <td>{reservation.slotId}</td>

                    <td>{reservation.prosumerUserId}</td>

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

                    <td>
                      {reservation.energyAmountKwh} kWh
                    </td>

                    <td>
                      <span
                        className={`status-badge ${getStatusClass(
                          reservation.status,
                        )}`}
                      >
                        {getStatusLabel(
                          reservation.status,
                        )}
                      </span>
                    </td>

                    <td>
                      <div className="table-actions">
                        {isPending(reservation.status) && (
                          <>
                            <button
                              type="button"
                              className="primary-button"
                              disabled={
                                processingId ===
                                reservation.reservationId
                              }
                              onClick={() =>
                                handleApprove(
                                  reservation.reservationId,
                                )
                              }
                            >
                              Approve
                            </button>

                            <button
                              type="button"
                              className="danger-button"
                              disabled={
                                processingId ===
                                reservation.reservationId
                              }
                              onClick={() =>
                                handleReject(
                                  reservation.reservationId,
                                )
                              }
                            >
                              Reject
                            </button>
                          </>
                        )}

                        {reservation.hasPendingChange && (
                          <>
                            <button
                              type="button"
                              className="primary-button"
                              disabled={
                                processingId ===
                                reservation.reservationId
                              }
                              onClick={() =>
                                handleApproveChange(
                                  reservation.reservationId,
                                )
                              }
                            >
                              Approve Change
                            </button>

                            <button
                              type="button"
                              className="danger-button"
                              disabled={
                                processingId ===
                                reservation.reservationId
                              }
                              onClick={() =>
                                handleRejectChange(
                                  reservation.reservationId,
                                )
                              }
                            >
                              Reject Change
                            </button>
                          </>
                        )}

                        {!isPending(reservation.status) &&
                          !reservation.hasPendingChange && (
                            <span className="table-subtext">
                              No actions
                            </span>
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

const isPending = (
  status: Reservation["status"],
): boolean => {
  return status === "PENDING" || status === 0;
};

const getStatusLabel = (
  status: Reservation["status"],
): string => {
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

  return String(status);
};

const getStatusClass = (
  status: Reservation["status"],
): string => {
  const label = getStatusLabel(status);

  if (label === "CANCELLED") {
    return "inactive";
  }

  return "";
};

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

export default ReservationManagementPage;