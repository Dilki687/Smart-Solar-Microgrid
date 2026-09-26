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
  const [reservations, setReservations] = useState<Reservation[]>(
    [],
  );
  const [selectedStatus, setSelectedStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [processingId, setProcessingId] = useState<string | null>(
    null,
  );

  // The reservation currently displayed in the detail modal.
  const [viewingReservation, setViewingReservation] =
    useState<Reservation | null>(null);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadReservations = async (): Promise<Reservation[]> => {
    try {
      setLoading(true);
      setError("");

      const data = await getReservations(
        selectedStatus || undefined,
      );

      setReservations(data);
      return data;
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load reservations.",
      );
      return [];
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReservations();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedStatus]);

  // After an action succeeds, re-fetch the list and either sync
  // the modal to the freshly loaded reservation or close it if it
  // no longer appears in the filtered list.
  const refreshAndSyncModal = async (reservationId: string) => {
    const latest = await loadReservations();
    const updated = latest.find(
      (r) => r.reservationId === reservationId,
    );
    setViewingReservation(updated ?? null);
  };

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
      await refreshAndSyncModal(reservationId);
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
      await refreshAndSyncModal(reservationId);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be rejected.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleApproveChange = async (reservationId: string) => {
    if (!window.confirm("Approve this reservation change?")) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await approveReservationChange(reservationId);

      setSuccess("Reservation change approved successfully.");
      await refreshAndSyncModal(reservationId);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation change could not be approved.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const handleRejectChange = async (reservationId: string) => {
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

      setSuccess("Reservation change rejected successfully.");
      await refreshAndSyncModal(reservationId);
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
        <div className="management-message error">{error}</div>
      )}

      {success && (
        <div className="management-message success">
          {success}
        </div>
      )}

      <div className="management-card">
        <div className="management-card-header">
          <h2>
            Reservations
            {reservations.some((r) => r.hasPendingChange) && (
              <span
                className="notification-dot notification-dot--inline notification-dot--pulse"
                title="At least one reservation has a pending change request"
                aria-label="Pending change requests"
              />
            )}
          </h2>

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
                  <th>Prosumer</th>
                  <th>Schedule</th>
                  <th>Energy</th>
                  <th>Status</th>
                  <th style={{ width: "1%" }}>Actions</th>
                </tr>
              </thead>

              <tbody>
                {reservations.map((reservation) => (
                  <tr key={reservation.reservationId}>
                    <td>
                      <strong>{reservation.reservationId}</strong>

                      {reservation.hasPendingChange && (
                        <div className="table-subtext">
                          <span className="status-badge pending_deactivation">
                            CHANGE PENDING
                          </span>
                        </div>
                      )}
                    </td>

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

                    <td>{reservation.energyAmountKwh} kWh</td>

                    <td>
                      <span
                        className={`status-badge ${getStatusClass(
                          reservation.status,
                        )}`}
                      >
                        {getStatusLabel(reservation.status)}
                      </span>
                    </td>

                    <td
                      className={
                        reservation.hasPendingChange
                          ? "has-notification"
                          : undefined
                      }
                    >
                      {reservation.hasPendingChange && (
                        <span
                          className="notification-dot notification-dot--corner notification-dot--pulse"
                          title="This reservation has a pending change request"
                          aria-label="Pending change request"
                        />
                      )}

                      <div className="table-actions">
                        <button
                          type="button"
                          className="primary-button small"
                          onClick={() =>
                            setViewingReservation(reservation)
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

      {viewingReservation && (
        <ReservationDetailModal
          reservation={viewingReservation}
          processing={
            processingId === viewingReservation.reservationId
          }
          onClose={() => setViewingReservation(null)}
          onApprove={() =>
            handleApprove(viewingReservation.reservationId)
          }
          onReject={() =>
            handleReject(viewingReservation.reservationId)
          }
          onApproveChange={() =>
            handleApproveChange(
              viewingReservation.reservationId,
            )
          }
          onRejectChange={() =>
            handleRejectChange(viewingReservation.reservationId)
          }
        />
      )}
    </div>
  );
};

// ---------------------------------------------------------------
// Reservation detail modal — read-only detail rows plus every
// action button (approve/reject/approve-change/reject-change).
// Buttons are always shown; they are disabled when the reservation
// is not in a state where that action applies.
// ---------------------------------------------------------------

interface ReservationDetailModalProps {
  reservation: Reservation;
  processing: boolean;
  onClose: () => void;
  onApprove: () => void;
  onReject: () => void;
  onApproveChange: () => void;
  onRejectChange: () => void;
}

const ReservationDetailModal = ({
  reservation,
  processing,
  onClose,
  onApprove,
  onReject,
  onApproveChange,
  onRejectChange,
}: ReservationDetailModalProps) => {
  const statusLabel = getStatusLabel(reservation.status);

  // A reservation is only approvable/rejectable while it is
  // PENDING. Once it moves out of PENDING the approve/reject
  // buttons no longer apply, so hide them entirely instead of
  // just disabling them.
  const canReviewReservation = statusLabel === "PENDING";

  // The change-review buttons stay on-screen while the reservation
  // is CONFIRMED (they may become enabled if a change is later
  // requested) and while it is still PENDING (disabled but visible
  // so operators see the full lifecycle). Terminal states
  // (CANCELLED / COMPLETED) have no possible actions.
  const showChangeButtons =
    statusLabel === "PENDING" || statusLabel === "CONFIRMED";

  const canReviewChange = reservation.hasPendingChange;

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
            <h2>Reservation Details</h2>
            <p>{reservation.reservationId}</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Close reservation details"
          >
            ×
          </button>
        </div>

        <div className="station-detail-body">
          <div className="station-detail-row">
            <span className="detail-label">Status</span>
            <span className="detail-value">
              <span
                className={`status-badge ${getStatusClass(
                  reservation.status,
                )}`}
              >
                {getStatusLabel(reservation.status)}
              </span>
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Change Request</span>
            <span className="detail-value">
              {reservation.changeRequestStatus ? (
                <span
                  className={`status-badge ${
                    reservation.changeRequestStatus ===
                    "REJECTED"
                      ? "inactive"
                      : reservation.changeRequestStatus ===
                          "APPROVED"
                        ? "active"
                        : "pending_deactivation"
                  }`}
                >
                  {reservation.changeRequestStatus}
                </span>
              ) : (
                <span className="table-subtext">None</span>
              )}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Prosumer</span>
            <span className="detail-value">
              {reservation.prosumerUserId}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Station</span>
            <span className="detail-value">
              <ChangeValue
                current={reservation.stationId}
                pending={
                  reservation.hasPendingChange
                    ? reservation.pendingStationId
                    : null
                }
              />
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Slot</span>
            <span className="detail-value">
              <ChangeValue
                current={reservation.slotId}
                pending={
                  reservation.hasPendingChange
                    ? reservation.pendingSlotId
                    : null
                }
              />
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Scheduled Start</span>
            <span className="detail-value">
              <ChangeValue
                current={reservation.scheduledStartTime}
                pending={
                  reservation.hasPendingChange
                    ? reservation.pendingScheduledStartTime
                    : null
                }
                format={formatDateTime}
              />
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Scheduled End</span>
            <span className="detail-value">
              <ChangeValue
                current={reservation.scheduledEndTime}
                pending={
                  reservation.hasPendingChange
                    ? reservation.pendingScheduledEndTime
                    : null
                }
                format={formatDateTime}
              />
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Energy</span>
            <span className="detail-value">
              <ChangeValue
                current={reservation.energyAmountKwh}
                pending={
                  reservation.hasPendingChange
                    ? reservation.pendingEnergyAmountKwh
                    : null
                }
                format={(kwh) => `${kwh} kWh`}
              />
            </span>
          </div>

          {reservation.cancellationReason && (
            <div className="station-detail-row">
              <span className="detail-label">
                Cancellation Reason
              </span>
              <span className="detail-value">
                {reservation.cancellationReason}
              </span>
            </div>
          )}

          {reservation.createdAt && (
            <div className="station-detail-row">
              <span className="detail-label">Created</span>
              <span className="detail-value">
                {formatDateTime(reservation.createdAt)}
              </span>
            </div>
          )}

          {reservation.updatedAt && (
            <div className="station-detail-row">
              <span className="detail-label">Updated</span>
              <span className="detail-value">
                {formatDateTime(reservation.updatedAt)}
              </span>
            </div>
          )}
        </div>

        <div className="station-detail-actions">
          {canReviewReservation && (
            <>
              <button
                type="button"
                className="primary-button"
                onClick={onApprove}
                disabled={processing}
                title="Approve this reservation"
              >
                Approve
              </button>

              <button
                type="button"
                className="danger-button"
                onClick={onReject}
                disabled={processing}
                title="Reject this reservation"
              >
                Reject
              </button>
            </>
          )}

          {showChangeButtons && (
            <>
              <button
                type="button"
                className="primary-button"
                onClick={onApproveChange}
                disabled={!canReviewChange || processing}
                title={
                  canReviewChange
                    ? "Approve this change request"
                    : "There is no pending change request."
                }
              >
                Approve Change
              </button>

              <button
                type="button"
                className="danger-button"
                onClick={onRejectChange}
                disabled={!canReviewChange || processing}
                title={
                  canReviewChange
                    ? "Reject this change request"
                    : "There is no pending change request."
                }
              >
                Reject Change
              </button>
            </>
          )}

          <button
            type="button"
            className="secondary-button"
            onClick={onClose}
            disabled={processing}
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
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

  if (label === "PENDING") {
    return "pending_deactivation";
  }

  if (label === "CONFIRMED" || label === "COMPLETED") {
    return "active";
  }

  return "";
};

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

// ---------------------------------------------------------------
// Inline old → new diff for a single field on the reservation
// detail modal. When there is no pending value, or the pending
// value is the same as the current, we render just the current
// value so unchanged fields don't shout for attention.
// ---------------------------------------------------------------

interface ChangeValueProps<T> {
  current: T;
  pending?: T | null;
  format?: (value: T) => string;
}

const ChangeValue = <T,>({
  current,
  pending,
  format,
}: ChangeValueProps<T>) => {
  const render = (value: T) =>
    format ? format(value) : String(value);

  const hasPending =
    pending !== null &&
    pending !== undefined &&
    pending !== current;

  if (!hasPending) {
    return <>{render(current)}</>;
  }

  return (
    <span className="change-diff">
      <span className="change-diff-old">{render(current)}</span>
      <span className="change-diff-arrow" aria-hidden="true">
        →
      </span>
      <span className="change-diff-new">
        {render(pending as T)}
      </span>
    </span>
  );
};

export default ReservationManagementPage;
