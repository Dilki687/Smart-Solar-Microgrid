import { useEffect, useState, type FormEvent } from "react";
import {
  createBookingSlot,
  deactivateBookingSlot,
  getBookingSlots,
  updateBookingSlot,
} from "../../services/bookingSlotService";
import type {
  BookingSlot,
  CreateBookingSlotRequest,
  UpdateBookingSlotRequest,
} from "../../types/bookingSlot";
import { getStations } from "../../services/stationService";
import type { Station } from "../../types/station";
import TruncatedId from "../../components/TruncatedId";

const BookingSlotManagementPage = () => {
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [stations, setStations] = useState<Station[]>([]);

  const [selectedStationId, setSelectedStationId] = useState("");
  const [editingSlotId, setEditingSlotId] = useState<string | null>(
    null,
  );

  const [stationId, setStationId] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [totalCapacity, setTotalCapacity] = useState("");

  // Currently displayed slot in the detail modal.
  const [viewingSlot, setViewingSlot] = useState<BookingSlot | null>(
    null,
  );

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async (): Promise<BookingSlot[]> => {
    try {
      setLoading(true);
      setError("");

      const [slotData, stationData] = await Promise.all([
        getBookingSlots(),
        getStations("ACTIVE"),
      ]);

      setSlots(slotData);
      setStations(stationData);

      if (!stationId && stationData.length > 0) {
        setStationId(stationData[0].stationId);
      }

      return slotData;
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load booking slots and stations.",
      );
      return [];
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const resetForm = () => {
    setEditingSlotId(null);
    setStartTime("");
    setEndTime("");
    setTotalCapacity("");

    if (stations.length > 0) {
      setStationId(stations[0].stationId);
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (!stationId) {
      setError("Please select a station.");
      return;
    }

    if (!startTime || !endTime) {
      setError("Please enter both start and end times.");
      return;
    }

    if (new Date(startTime) >= new Date(endTime)) {
      setError("Start time must be before end time.");
      return;
    }

    const capacity = Number(totalCapacity);

    if (!Number.isInteger(capacity) || capacity <= 0) {
      setError("Total capacity must be greater than zero.");
      return;
    }

    try {
      setSaving(true);

      if (editingSlotId) {
        const request: UpdateBookingSlotRequest = {
          startTime: new Date(startTime).toISOString(),
          endTime: new Date(endTime).toISOString(),
          totalCapacity: capacity,
        };

        await updateBookingSlot(editingSlotId, request);

        setSuccess("Booking slot updated successfully.");
      } else {
        const request: CreateBookingSlotRequest = {
          stationId,
          startTime: new Date(startTime).toISOString(),
          endTime: new Date(endTime).toISOString(),
          totalCapacity: capacity,
        };

        await createBookingSlot(request);

        setSuccess("Booking slot created successfully.");
      }

      resetForm();
      await loadData();
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The booking slot operation could not be completed.",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleEdit = (slot: BookingSlot) => {
    setError("");
    setSuccess("");

    setEditingSlotId(slot.slotId);
    setStationId(slot.stationId);

    const start = new Date(slot.startTime);
    const end = new Date(slot.endTime);

    setStartTime(toDateTimeLocalValue(start));
    setEndTime(toDateTimeLocalValue(end));
    setTotalCapacity(String(slot.totalCapacity));

    // Close the detail modal so the user sees the populated form.
    setViewingSlot(null);

    window.scrollTo({
      top: 0,
      behavior: "smooth",
    });
  };

  const handleDeactivate = async (slot: BookingSlot) => {
    const confirmed = window.confirm(
      `Deactivate booking slot ${slot.slotId}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      setError("");
      setSuccess("");

      await deactivateBookingSlot(slot.slotId);

      setSuccess("Booking slot deactivated successfully.");

      if (editingSlotId === slot.slotId) {
        resetForm();
      }

      const latest = await loadData();

      // Sync the modal to the fresh row, or close it if the slot
      // is no longer in the (active) list.
      const updated = latest.find(
        (s) => s.slotId === slot.slotId && s.isActive,
      );
      setViewingSlot(updated ?? null);
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "The booking slot could not be deactivated.",
      );
    }
  };

  const filteredSlots = selectedStationId
    ? slots.filter((slot) => slot.stationId === selectedStationId)
    : slots;

  const activeSlots = filteredSlots.filter((slot) => slot.isActive);

  const stationLookup = new Map(
    stations.map((station) => [station.stationId, station]),
  );

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Booking Slot Management</h1>
          <p>
            Create, update and deactivate energy booking slots for
            active stations.
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

      <div className="station-split-layout">
        {/* LEFT — Create / Edit form */}
        <div className="management-card">
          <div className="management-card-header">
            <h2>
              {editingSlotId
                ? "Edit Booking Slot"
                : "Create Booking Slot"}
            </h2>

            {editingSlotId && (
              <button
                type="button"
                className="secondary-button"
                onClick={resetForm}
              >
                Cancel Edit
              </button>
            )}
          </div>

          <form className="station-form" onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="stationId">Station</label>

              <select
                id="stationId"
                value={stationId}
                onChange={(event) =>
                  setStationId(event.target.value)
                }
                disabled={!!editingSlotId || saving}
              >
                <option value="">Select station</option>

                {stations.map((station) => (
                  <option
                    key={station.stationId}
                    value={station.stationId}
                  >
                    {station.name} ({station.stationId})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label htmlFor="totalCapacity">
                Total Capacity
              </label>

              <input
                id="totalCapacity"
                type="number"
                min="1"
                step="1"
                value={totalCapacity}
                onChange={(event) =>
                  setTotalCapacity(event.target.value)
                }
                placeholder="e.g. 5"
                disabled={saving}
              />
            </div>

            <div className="form-group">
              <label htmlFor="startTime">Start Time</label>

              <input
                id="startTime"
                type="datetime-local"
                value={startTime}
                onChange={(event) =>
                  setStartTime(event.target.value)
                }
                disabled={saving}
              />
            </div>

            <div className="form-group">
              <label htmlFor="endTime">End Time</label>

              <input
                id="endTime"
                type="datetime-local"
                value={endTime}
                onChange={(event) =>
                  setEndTime(event.target.value)
                }
                disabled={saving}
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
                    : editingSlotId
                      ? "Update Slot"
                      : "Create Slot"}
                </button>

                {editingSlotId && (
                  <button
                    type="button"
                    className="secondary-button"
                    onClick={resetForm}
                    disabled={saving}
                  >
                    Cancel
                  </button>
                )}
              </div>
            </div>
          </form>
        </div>

        {/* RIGHT — Compact slots list */}
        <div className="management-card">
          <div className="management-card-header">
            <h2>Active Booking Slots</h2>

            <select
              value={selectedStationId}
              onChange={(event) =>
                setSelectedStationId(event.target.value)
              }
            >
              <option value="">All Stations</option>

              {stations.map((station) => (
                <option
                  key={station.stationId}
                  value={station.stationId}
                >
                  {station.name}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <div className="empty-state">
              Loading booking slots...
            </div>
          ) : activeSlots.length === 0 ? (
            <div className="empty-state">
              No active booking slots found.
            </div>
          ) : (
            <div className="table-wrapper">
              <table className="management-table">
                <thead>
                  <tr>
                    <th>Slot</th>
                    <th style={{ width: "1%" }}>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {activeSlots.map((slot) => {
                    const station = stationLookup.get(
                      slot.stationId,
                    );

                    return (
                      <tr key={slot.slotId}>
                        <td>
                          <strong>
                            {formatDateTime(slot.startTime)}
                          </strong>

                          <div className="table-subtext">
                            <TruncatedId value={slot.slotId} />
                            {" · "}
                            {station
                              ? station.name
                              : slot.stationId}
                            {" · "}
                            {slot.availableCapacity}/
                            {slot.totalCapacity} available
                          </div>
                        </td>

                        <td>
                          <div className="table-actions">
                            <button
                              type="button"
                              className="primary-button small"
                              onClick={() =>
                                setViewingSlot(slot)
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
      </div>

      {viewingSlot && (
        <BookingSlotDetailModal
          slot={viewingSlot}
          station={stationLookup.get(viewingSlot.stationId)}
          onClose={() => setViewingSlot(null)}
          onEdit={() => handleEdit(viewingSlot)}
          onDeactivate={() => handleDeactivate(viewingSlot)}
        />
      )}
    </div>
  );
};

// ---------------------------------------------------------------
// Booking slot detail modal — read-only detail rows with the
// Edit / Deactivate / Close actions from the row.
// ---------------------------------------------------------------

interface BookingSlotDetailModalProps {
  slot: BookingSlot;
  station?: Station;
  onClose: () => void;
  onEdit: () => void;
  onDeactivate: () => void;
}

const BookingSlotDetailModal = ({
  slot,
  station,
  onClose,
  onEdit,
  onDeactivate,
}: BookingSlotDetailModalProps) => {
  const reservedCapacity = Math.max(
    0,
    slot.totalCapacity - slot.availableCapacity,
  );

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
            <h2>Booking Slot</h2>
            <p>{slot.slotId}</p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            aria-label="Close booking slot details"
          >
            ×
          </button>
        </div>

        <div className="station-detail-body">
          <div className="station-detail-row">
            <span className="detail-label">Status</span>
            <span className="detail-value">
              <span
                className={`status-badge ${
                  slot.isActive ? "active" : "inactive"
                }`}
              >
                {slot.isActive ? "ACTIVE" : "INACTIVE"}
              </span>
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Station</span>
            <span className="detail-value">
              {station ? (
                <>
                  {station.name}
                  <div className="table-subtext">
                    {slot.stationId}
                  </div>
                </>
              ) : (
                slot.stationId
              )}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Start Time</span>
            <span className="detail-value">
              {formatDateTime(slot.startTime)}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">End Time</span>
            <span className="detail-value">
              {formatDateTime(slot.endTime)}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Total Capacity</span>
            <span className="detail-value">
              {slot.totalCapacity}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Reserved</span>
            <span className="detail-value">
              {reservedCapacity}
            </span>
          </div>

          <div className="station-detail-row">
            <span className="detail-label">Available</span>
            <span className="detail-value">
              {slot.availableCapacity}
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

          {slot.isActive && (
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

const toDateTimeLocalValue = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");

  return `${year}-${month}-${day}T${hours}:${minutes}`;
};

const formatDateTime = (value: string): string => {
  return new Date(value).toLocaleString();
};

export default BookingSlotManagementPage;
