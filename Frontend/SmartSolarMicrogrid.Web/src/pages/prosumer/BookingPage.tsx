import { useEffect, useState } from "react";
import { getBookingSlots } from "../../services/bookingSlotService";
import { createReservation } from "../../services/reservationService";
import type { BookingSlot } from "../../types/bookingSlot";

const BookingPage = () => {
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [selectedSlotId, setSelectedSlotId] = useState("");
  const [energyAmount, setEnergyAmount] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadSlots = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await getBookingSlots();
      const activeSlots = data.filter(
        (slot) => slot.isActive && slot.availableCapacity > 0,
      );

      setSlots(activeSlots);

      if (activeSlots.length > 0) {
        setSelectedSlotId(activeSlots[0].slotId);
      }
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load available booking slots.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadSlots();
  }, []);

  const selectedSlot = slots.find(
    (slot) => slot.slotId === selectedSlotId,
  );

  const handleBooking = async () => {
    setError("");
    setSuccess("");

    if (!selectedSlot) {
      setError("Please select a booking slot.");
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
      setSaving(true);

      await createReservation({
        slotId: selectedSlot.slotId,
        stationId: selectedSlot.stationId,
        scheduledStartTime: selectedSlot.startTime,
        scheduledEndTime: selectedSlot.endTime,
        energyAmountKwh: energy,
      });

      setSuccess(
        "Reservation created successfully and is waiting for approval.",
      );

      setEnergyAmount("");

      await loadSlots();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The reservation could not be created.",
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Book Energy</h1>
          <p>
            Select an available booking slot and reserve energy.
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

      <div className="station-split-layout">
        {/* LEFT — Available slots */}
        <div className="management-card">
          <div className="management-card-header">
            <h2>Available Booking Slots</h2>
          </div>

          {loading ? (
            <div className="empty-state">
              Loading available slots...
            </div>
          ) : slots.length === 0 ? (
            <div className="empty-state">
              No booking slots are currently available.
            </div>
          ) : (
            <div className="booking-grid">
              {slots.map((slot) => (
                <button
                  type="button"
                  key={slot.slotId}
                  className={`booking-slot-card ${
                    selectedSlotId === slot.slotId
                      ? "selected"
                      : ""
                  }`}
                  onClick={() =>
                    setSelectedSlotId(slot.slotId)
                  }
                >
                  <div className="booking-slot-title">
                    {slot.stationId}
                  </div>

                  <div className="booking-slot-time">
                    {formatDateTime(slot.startTime)}
                  </div>

                  <div className="booking-slot-time">
                    to {formatDateTime(slot.endTime)}
                  </div>

                  <div className="booking-slot-capacity">
                    {slot.availableCapacity} available
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Reservation details for the selected slot */}
        <div className="management-card">
          <div className="management-card-header">
            <h2>Reservation Details</h2>
          </div>

          {selectedSlot ? (
            <div className="station-form">
              <div className="form-group">
                <label>Station</label>
                <input
                  value={selectedSlot.stationId}
                  disabled
                />
              </div>

              <div className="form-group">
                <label>Available Capacity</label>
                <input
                  value={`${selectedSlot.availableCapacity}`}
                  disabled
                />
              </div>

              <div className="form-group">
                <label>Start Time</label>
                <input
                  value={formatDateTime(
                    selectedSlot.startTime,
                  )}
                  disabled
                />
              </div>

              <div className="form-group">
                <label>End Time</label>
                <input
                  value={formatDateTime(
                    selectedSlot.endTime,
                  )}
                  disabled
                />
              </div>

              <div className="form-group full-width">
                <label htmlFor="energyAmount">
                  Energy Amount (kWh)
                </label>

                <input
                  id="energyAmount"
                  type="number"
                  min="0.1"
                  step="0.1"
                  max={selectedSlot.availableCapacity}
                  value={energyAmount}
                  onChange={(event) =>
                    setEnergyAmount(event.target.value)
                  }
                  placeholder={`Maximum ${selectedSlot.availableCapacity}`}
                  disabled={saving}
                />
              </div>

              <div className="form-group full-width">
                <div className="form-actions">
                  <button
                    type="button"
                    className="primary-button"
                    onClick={handleBooking}
                    disabled={saving}
                  >
                    {saving
                      ? "Booking..."
                      : "Create Reservation"}
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="empty-state">
              <h3>Select a slot to continue</h3>
              <p>
                Pick a booking slot from the left to see its
                details and reserve energy.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

export default BookingPage;