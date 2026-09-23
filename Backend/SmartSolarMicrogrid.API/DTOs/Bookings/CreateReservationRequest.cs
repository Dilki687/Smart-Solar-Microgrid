namespace SmartSolarMicrogrid.API.DTOs.Bookings;

/// Represents the request to create an energy reservation.
public class CreateReservationRequest
{
    public string SlotId { get; set; } = string.Empty;

    public string StationId { get; set; } = string.Empty;

    public DateTime ScheduledStartTime { get; set; }

    public DateTime ScheduledEndTime { get; set; }

    public double EnergyAmountKwh { get; set; }
}