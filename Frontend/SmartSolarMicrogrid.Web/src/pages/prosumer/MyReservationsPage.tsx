import { useEffect, useState } from "react";
import {
  cancelReservation,
  getMyReservations,
  requestReservationUpdate,
} from "../../services/reservationService";
import { getBookingSlots } from "../../services/bookingSlotService";
import type { BookingSlot } from "../../types/bookingSlot";
import type { Reservation } from "../../types/reservation";

const MyReservationsPage = () => {
  const [reservations, setReservations] = useState<Reservation[]>(
    [],
  );
  const [slots, setSlots] = useState<BookingSlot[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [processingId, setProcessingId] = useState<string | null>(
    null,
  );

  // The reservation currently displayed in the detail modal.
  const [viewingReservation, setViewingReservation] =
    useState<Reservation | null>(null);

  // The reservation currently being edited through the update modal.
  const [editingReservation, setEditingReservation] =
    useState<Reservation | null>(null);

  const [selectedSlotId, setSelectedSlotId] = useState("");
  const [energyAmount, setEnergyAmount] = useState("");

  const loadData = async (): Promise<Reservation[]> => {
    try {
      setLoading(true);
      setError("");

      const [reservationData, slotData] = await Promise.all([
        getMyReservations(),
        getBookingSlots(),
      ]);

      setReservations(reservationData);

      setSlots(
        slotData.filter(
          (slot) => slot.isActive && slot.availableCapacity > 0,
        ),
      );

      return reservationData;
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load your reservations.",
      );
      return [];
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // After a successful action re-fetch the list and either sync the
  // detail modal to the fresh reservation or close it if it is no
  // longer visible.
  const refreshAndSyncModal = async (reservationId: string) => {
    const latest = await loadData();
    const updated = latest.find(
      (r) => r.reservationId === reservationId,
    );
    setViewingReservation(updated ?? null);
  };

  const handleCancel = async (reservationId: string) => {
    const reason = window.prompt(
      "Enter the reason for cancelling this reservation:",
    );

    if (!reason || !reason.trim()) {
      return;
    }

    const confirmed = window.confirm(
      "Are you sure you want to cancel this reservation?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setProcessingId(reservationId);
      setError("");
      setSuccess("");

      await cancelReservation(reservationId, reason.trim());

      setSuccess("Reservation cancelled successfully.");
      await refreshAndSyncModal(reservationId);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be cancelled.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const openUpdateForm = (reservation: Reservation) => {
    setError("");
    setSuccess("");

    setEditingReservation(reservation);
    setSelectedSlotId(reservation.slotId);
    setEnergyAmount(String(reservation.energyAmountKwh));

    // Close the detail modal so the update modal takes over.
    setViewingReservation(null);
  };

  const closeUpdateForm = () => {
    setEditingReservation(null);
    setSelectedSlotId("");
    setEnergyAmount("");
  };

  const handleRequestUpdate = async () => {
    if (!editingReservation) {
      return;
    }

    const selectedSlot = slots.find(
      (slot) => slot.slotId === selectedSlotId,
    );

    if (!selectedSlot) {
      setError("Please select a valid booking slot.");
      return;
    }

    const energy = Number(energyAmount);

    if (!Number.isFinite(energy) || energy <= 0) {
      setError("Energy amount must be greater than zero.");
      return;
    }

    if (energy > selectedSlot.availableCapacity) {
      setError(
        `Energy amount cannot exceed the available capacity of ${selectedSlot.availableCapacity}.`,
      );
      return;
    }

    try {
      setProcessingId(editingReservation.reservationId);
      setError("");
      setSuccess("");

      await requestReservationUpdate(
        editingReservation.reservationId,
        {
          slotId: selectedSlot.slotId,
          scheduledStartTime: selectedSlot.startTime,
          scheduledEndTime: selectedSlot.endTime,
          energyAmountKwh: energy,
        },
      );

      setSuccess(
        "Reservation update request submitted successfully.",
      );

      closeUpdateForm();
      await loadData();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation update request could not be submitted.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>My Reservations</h1>

          <p>
            View your energy reservations and manage your booking
            requests.
          </p>
        </div>

        <button
          type="button"
          className="secondary-button"
          onClick={loadData}
          disabled={loading}
        >
          {loading ? "Refreshing..." : "Refresh"}
        </button>
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
          <h2>Reservation History</h2>
        </div>

        {loading ? (
          <div className="empty-state">
            Loading reservations...
          </div>
        ) : reservations.length === 0 ? (
          <div className="empty-state">
            You do not have any reservations yet.
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="management-table">
              <thead>
                <tr>
                  <th>Reservation</th>
                  <th>Schedule</th>
                  <th>Energy</th>
                  <th>Status</th>
                  <th>Change Request</th>
                  <th style={{ width: "1%" }}>Actions</th>
                </tr>
              </thead>

              <tbody>
                {reservations.map((reservation) => {
                  const status = getStatusLabel(
                    reservation.status,
                  );

                  return (
                    <tr key={reservation.reservationId}>
                      <td>
                        <strong>
                          {reservation.reservationId}
                        </strong>

                        <div className="table-subtext">
                          {reservation.slotId}
                        </div>
                      </td>

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
                          {status}
                        </span>
                      </td>

                      <td>
                        {renderChangeRequestSummary(reservation)}
                      </td>

                      <td>
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
                  );
                })}
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
          onRequestUpdate={() =>
            openUpdateForm(viewingReservation)
          }
          onCancel={() =>
            handleCancel(viewingReservation.reservationId)
          }
        />
      )}

      {editingReservation && (
        <RequestUpdateModal
          reservation={editingReservation}
          slots={slots}
          selectedSlotId={selectedSlotId}
          energyAmount={energyAmount}
          processing={
            processingId === editingReservation.reservationId
          }
          onSlotChange={setSelectedSlotId}
          onEnergyChange={setEnergyAmount}
          onSubmit={handleRequestUpdate}
          onClose={closeUpdateForm}
        />
      )}
    </div>
  );
};

// ---------------------------------------------------------------
// Reservation detail modal — read-only info + Request Update and
// Cancel actions. Buttons are hidden when they never apply for
// this status (matches the backoffice modal pattern).
// ---------------------------------------------------------------

interface ReservationDetailModalProps {
  reservation: Reservation;
  processing: boolean;
  onClose: () => void;
  onRequestUpdate: () => void;
  onCancel: () => void;
}

const ReservationDetailModal = ({
  reservation,
  processing,
  onClose,
  onRequestUpdate,
  onCancel,
}: ReservationDetailModalProps) => {
  const status = getStatusLabel(reservation.status);
  const isConfirmed = status === "CONFIRMED";
  const hasPendingChange = reservation.hasPendingChange;

  // Request Update is only meaningful when the reservation is
  // confirmed and there is no change already pending review.
  const canRequestUpdate = isConfirmed && !hasPendingChange;

  // Cancel is only meaningful for confirmed reservations.
  const canCancel = isConfirmed;

  // Only show the action buttons if at least one could ever apply
  // to this reservation lifecycle stage.
  const showActionButtons = isConfirmed;

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
                {status}
              </span>
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Change Request</span>
            <span className="detail-value">
              {renderChangeRequestSummary(reservation)}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Station</span>
            <span className="detail-value">
              {reservation.stationId}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Slot</span>
            <span className="detail-value">
              {reservation.slotId}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Scheduled Start</span>
            <span className="detail-value">
              {formatDateTime(reservation.scheduledStartTime)}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Scheduled End</span>
            <span className="detail-value">
              {formatDateTime(reservation.scheduledEndTime)}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Energy</span>
            <span className="detail-value">
              {reservation.energyAmountKwh} kWh
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

          {reservation.hasPendingChange && (
            <>
              {reservation.pendingSlotId && (
                <div className="station-detail-row">
                  <span className="detail-label">
                    Pending Slot
                  </span>
                  <span className="detail-value">
                    {reservation.pendingSlotId}
                  </span>
                </div>
              )}

              {reservation.pendingScheduledStartTime && (
                <div className="station-detail-row">
                  <span className="detail-label">
                    Pending Start
                  </span>
                  <span className="detail-value">
                    {formatDateTime(
                      reservation.pendingScheduledStartTime,
                    )}
                  </span>
                </div>
              )}

              {reservation.pendingScheduledEndTime && (
                <div className="station-detail-row">
                  <span className="detail-label">
                    Pending End
                  </span>
                  <span className="detail-value">
                    {formatDateTime(
                      reservation.pendingScheduledEndTime,
                    )}
                  </span>
                </div>
              )}

              {reservation.pendingEnergyAmountKwh != null && (
                <div className="station-detail-row">
                  <span className="detail-label">
                    Pending Energy
                  </span>
                  <span className="detail-value">
                    {reservation.pendingEnergyAmountKwh} kWh
                  </span>
                </div>
              )}
            </>
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
          {showActionButtons && (
            <>
              <button
                type="button"
                className="secondary-button"
                onClick={onRequestUpdate}
                disabled={!canRequestUpdate || processing}
                title={
                  canRequestUpdate
                    ? "Request a change to this reservation"
                    : hasPendingChange
                      ? "A change request is already pending."
                      : "Only confirmed reservations can be updated."
                }
              >
                Request Update
              </button>

              <button
                type="button"
                className="danger-button"
                onClick={onCancel}
                disabled={!canCancel || processing}
                title={
                  canCancel
                    ? "Cancel this reservation"
                    : "Only confirmed reservations can be cancelled."
                }
              >
                {processing ? "Processing..." : "Cancel"}
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

// ---------------------------------------------------------------
// Request Update modal — focused form for changing the slot and
// energy on an existing confirmed reservation.
// ---------------------------------------------------------------

interface RequestUpdateModalProps {
  reservation: Reservation;
  slots: BookingSlot[];
  selectedSlotId: string;
  energyAmount: string;
  processing: boolean;
  onSlotChange: (slotId: string) => void;
  onEnergyChange: (value: string) => void;
  onSubmit: () => void;
  onClose: () => void;
}

const RequestUpdateModal = ({
  reservation,
  slots,
  selectedSlotId,
  energyAmount,
  processing,
  onSlotChange,
  onEnergyChange,
  onSubmit,
  onClose,
}: RequestUpdateModalProps) => {
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
            <h2>Request Reservation Update</h2>
            <p>{reservation.reservationId}</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Close update request"
          >
            ×
          </button>
        </div>

        <div className="station-form">
          <div className="form-group full-width">
            <label>Current Station</label>
            <input value={reservation.stationId} disabled />
          </div>

          <div className="form-group full-width">
            <label htmlFor="updateSlot">New Booking Slot</label>

            <select
              id="updateSlot"
              value={selectedSlotId}
              onChange={(event) =>
                onSlotChange(event.target.value)
              }
              disabled={processing}
            >
              <option value="">Select booking slot</option>

              {slots.map((slot) => (
                <option key={slot.slotId} value={slot.slotId}>
                  {slot.stationId} —{" "}
                  {formatDateTime(slot.startTime)} —{" "}
                  {slot.availableCapacity} available
                </option>
              ))}
            </select>
          </div>

          <div className="form-group full-width">
            <label htmlFor="updateEnergy">
              New Energy Amount (kWh)
            </label>

            <input
              id="updateEnergy"
              type="number"
              min="0.1"
              step="0.1"
              value={energyAmount}
              onChange={(event) =>
                onEnergyChange(event.target.value)
              }
              disabled={processing}
            />
          </div>
        </div>

        <div className="station-detail-actions">
          <button
            type="button"
            className="primary-button"
            onClick={onSubmit}
            disabled={processing}
          >
            {processing ? "Submitting..." : "Request Update"}
          </button>

          <button
            type="button"
            className="secondary-button"
            onClick={onClose}
            disabled={processing}
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
};

// ---------------------------------------------------------------
// Small pieces of shared rendering / lookup logic.
// ---------------------------------------------------------------

const renderChangeRequestSummary = (reservation: Reservation) => {
  if (reservation.changeRequestStatus === "APPROVED") {
    return (
      <div className="change-result approved">
        ✓ Update request approved
      </div>
    );
  }

  if (reservation.changeRequestStatus === "REJECTED") {
    return (
      <div className="change-result rejected">
        ✕ Update request rejected
        {reservation.cancellationReason && (
          <div className="change-result-reason">
            Reason: {reservation.cancellationReason}
          </div>
        )}
      </div>
    );
  }

  if (reservation.hasPendingChange) {
    return (
      <div className="change-result pending">
        ⏳ Update request pending approval
      </div>
    );
  }

  return <span className="table-subtext">None</span>;
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

export default MyReservationsPage;
