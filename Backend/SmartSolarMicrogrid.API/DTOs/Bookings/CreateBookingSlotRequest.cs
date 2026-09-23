namespace SmartSolarMicrogrid.API.DTOs.Bookings;

/// Represents the request to create an available energy booking slot.
public class CreateBookingSlotRequest
{
    public string StationId { get; set; } = string.Empty;

    public DateTime StartTime { get; set; }

    public DateTime EndTime { get; set; }

    public int TotalCapacity { get; set; }
}