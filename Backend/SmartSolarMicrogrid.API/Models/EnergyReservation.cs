using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models;

/// Represents a reservation made by a prosumer for an energy booking slot.
public class EnergyReservation
{
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    public string ReservationId { get; set; } = string.Empty;

    public string SlotId { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public string ProsumerUserId { get; set; } = string.Empty;

    public DateTime ScheduledStartTime { get; set; }

    public DateTime ScheduledEndTime { get; set; }

    public double EnergyAmountKwh { get; set; }

    public BookingStatus Status { get; set; } = BookingStatus.PENDING;
    // Indicates whether an update is waiting for operator approval.
public bool HasPendingChange { get; set; } = false;

// Stores the requested new slot.
public string? PendingSlotId { get; set; }

// Stores the requested new station.
public string? PendingStationId { get; set; }

// Stores the requested new schedule.
public DateTime? PendingScheduledStartTime { get; set; }
public DateTime? PendingScheduledEndTime { get; set; }

// Stores the requested new energy amount.
public double? PendingEnergyAmountKwh { get; set; }

    public string? CancellationReason { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}