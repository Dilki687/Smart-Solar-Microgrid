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

const BookingSlotManagementPage = () => {
  const [slots, setSlots] = useState<BookingSlot[]>([]);
  const [stations, setStations] = useState<Station[]>([]);

  const [selectedStationId, setSelectedStationId] = useState("");
  const [editingSlotId, setEditingSlotId] = useState<string | null>(null);

  const [stationId, setStationId] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [totalCapacity, setTotalCapacity] = useState("");

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const loadData = async () => {
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
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          "Failed to load booking slots and stations.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
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

      await loadData();
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

  return (
    <div className="management-page">
      <div className="management-header">
        <div>
          <h1>Booking Slot Management</h1>
          <p>
            Create, update and deactivate energy booking slots for active
            stations.
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
          <h2>
            {editingSlotId ? "Edit Booking Slot" : "Create Booking Slot"}
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
              onChange={(event) => setStationId(event.target.value)}
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
        </form>
      </div>

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
                  <th>Station</th>
                  <th>Start</th>
                  <th>End</th>
                  <th>Capacity</th>
                  <th>Available</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {activeSlots.map((slot) => (
                  <tr key={slot.slotId}>
                    <td>
                      <strong>{slot.slotId}</strong>
                    </td>

                    <td>{slot.stationId}</td>

                    <td>
                      {formatDateTime(slot.startTime)}
                    </td>

                    <td>
                      {formatDateTime(slot.endTime)}
                    </td>

                    <td>{slot.totalCapacity}</td>

                    <td>{slot.availableCapacity}</td>

                    <td>
                      <span className="status-badge">
                        ACTIVE
                      </span>
                    </td>

                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => handleEdit(slot)}
                        >
                          Edit
                        </button>

                        <button
                          type="button"
                          className="danger-button"
                          onClick={() =>
                            handleDeactivate(slot)
                          }
                        >
                          Deactivate
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