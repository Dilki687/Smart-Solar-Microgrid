// Defines the request body used when updating a reservation.

namespace SmartSolarMicrogrid.API.DTOs.Bookings;

/// Represents a reservation update request.
public class UpdateReservationRequest
{
    /// Gets or sets the new booking slot identifier.
    public string SlotId { get; set; } = string.Empty;

    /// Gets or sets the requested start time.
    public DateTime ScheduledStartTime { get; set; }

    /// Gets or sets the requested end time.
    public DateTime ScheduledEndTime { get; set; }

    /// Gets or sets the requested energy amount in kWh.
    public double EnergyAmountKwh { get; set; }
}