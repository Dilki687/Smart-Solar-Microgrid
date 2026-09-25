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
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [slots, setSlots] = useState<BookingSlot[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [processingId, setProcessingId] =
    useState<string | null>(null);

  const [editingReservation, setEditingReservation] =
    useState<Reservation | null>(null);

  const [selectedSlotId, setSelectedSlotId] = useState("");
  const [energyAmount, setEnergyAmount] = useState("");

  const loadData = async () => {
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
          (slot) =>
            slot.isActive &&
            slot.availableCapacity > 0,
        ),
      );
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load your reservations.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

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

      await cancelReservation(
        reservationId,
        reason.trim(),
      );

      setSuccess(
        "Reservation cancelled successfully.",
      );

      await loadData();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be cancelled.",
      );
    } finally {
      setProcessingId(null);
    }
  };

  const openUpdateForm = (
    reservation: Reservation,
  ) => {
    setError("");
    setSuccess("");

    setEditingReservation(reservation);

    setSelectedSlotId(reservation.slotId);

    setEnergyAmount(
      String(reservation.energyAmountKwh),
    );
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
      setError(
        "Energy amount must be greater than zero.",
      );
      return;
    }

    if (energy > selectedSlot.availableCapacity) {
      setError(
        `Energy amount cannot exceed the available capacity of ${selectedSlot.availableCapacity}.`,
      );
      return;
    }

    try {
      setProcessingId(
        editingReservation.reservationId,
      );

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
            View your energy reservations and manage
            your booking requests.
          </p>
        </div>

        <button
          type="button"
          className="secondary-button"
          onClick={loadData}
        >
          Refresh
        </button>
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

      {editingReservation && (
        <div className="management-card">
          <div className="management-card-header">
            <h2>Request Reservation Update</h2>

            <button
              type="button"
              className="secondary-button"
              onClick={closeUpdateForm}
            >
              Cancel
            </button>
          </div>

          <div className="station-form">
            <div className="form-group">
              <label>
                Current Reservation
              </label>

              <input
                value={
                  editingReservation.reservationId
                }
                disabled
              />
            </div>

            <div className="form-group">
              <label>
                Current Station
              </label>

              <input
                value={editingReservation.stationId}
                disabled
              />
            </div>

            <div className="form-group">
              <label htmlFor="updateSlot">
                New Booking Slot
              </label>

              <select
                id="updateSlot"
                value={selectedSlotId}
                onChange={(event) =>
                  setSelectedSlotId(
                    event.target.value,
                  )
                }
                disabled={
                  processingId ===
                  editingReservation.reservationId
                }
              >
                <option value="">
                  Select booking slot
                </option>

                {slots.map((slot) => (
                  <option
                    key={slot.slotId}
                    value={slot.slotId}
                  >
                    {slot.stationId} —{" "}
                    {formatDateTime(slot.startTime)} —{" "}
                    {slot.availableCapacity} available
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
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
                  setEnergyAmount(
                    event.target.value,
                  )
                }
                disabled={
                  processingId ===
                  editingReservation.reservationId
                }
              />
            </div>

            <div className="form-actions">
              <button
                type="button"
                className="primary-button"
                onClick={handleRequestUpdate}
                disabled={
                  processingId ===
                  editingReservation.reservationId
                }
              >
                {processingId ===
                editingReservation.reservationId
                  ? "Submitting..."
                  : "Request Update"}
              </button>

              <button
                type="button"
                className="secondary-button"
                onClick={closeUpdateForm}
              >
                Cancel
              </button>
            </div>
          </div>
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
                  <th>Station</th>
                  <th>Schedule</th>
                  <th>Energy</th>
                  <th>Status</th>
                  <th>Change Request</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {reservations.map((reservation) => {
                  const status = getStatusLabel(
                    reservation.status,
                  );

                  return (
                    <tr
                      key={reservation.reservationId}
                    >
                      <td>
                        <strong>
                          {reservation.reservationId}
                        </strong>

                        <div className="table-subtext">
                          {reservation.slotId}
                        </div>
                      </td>

                      <td>
                        {reservation.stationId}
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
                          className={`status-badge ${
                            status === "CANCELLED"
                              ? "inactive"
                              : ""
                          }`}
                        >
                          {status}
                        </span>
                      </td>

                      {/* CHANGE REQUEST STATUS */}
                      <td>
                        {reservation.changeRequestStatus ===
                          "APPROVED" && (
                          <div className="change-result approved">
                            ✓ Update request approved
                          </div>
                        )}

                        {reservation.changeRequestStatus ===
                          "REJECTED" && (
                          <div className="change-result rejected">
                            ✕ Update request rejected

                            {reservation.cancellationReason && (
                              <div className="change-result-reason">
                                Reason:{" "}
                                {
                                  reservation.cancellationReason
                                }
                              </div>
                            )}
                          </div>
                        )}

                        {reservation.hasPendingChange && (
                          <div className="change-result pending">
                            ⏳ Update request pending approval
                          </div>
                        )}

                        {!reservation.hasPendingChange &&
                          !reservation.changeRequestStatus && (
                            <span className="table-subtext">
                              None
                            </span>
                          )}
                      </td>

                      <td>
                        <div className="table-actions">
                          {status === "CONFIRMED" &&
                            !reservation.hasPendingChange && (
                              <>
                                <button
                                  type="button"
                                  className="secondary-button"
                                  disabled={
                                    processingId ===
                                    reservation.reservationId
                                  }
                                  onClick={() =>
                                    openUpdateForm(
                                      reservation,
                                    )
                                  }
                                >
                                  Request Update
                                </button>

                                <button
                                  type="button"
                                  className="danger-button"
                                  disabled={
                                    processingId ===
                                    reservation.reservationId
                                  }
                                  onClick={() =>
                                    handleCancel(
                                      reservation.reservationId,
                                    )
                                  }
                                >
                                  {processingId ===
                                  reservation.reservationId
                                    ? "Processing..."
                                    : "Cancel"}
                                </button>
                              </>
                            )}

                          {reservation.hasPendingChange && (
                            <span className="table-subtext">
                              Waiting for approval
                            </span>
                          )}

                          {status !== "CONFIRMED" &&
                            !reservation.hasPendingChange && (
                              <span className="table-subtext">
                                No actions
                              </span>
                            )}
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

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

export default MyReservationsPage;