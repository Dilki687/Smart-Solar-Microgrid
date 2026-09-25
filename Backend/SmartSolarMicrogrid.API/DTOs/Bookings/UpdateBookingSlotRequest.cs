namespace SmartSolarMicrogrid.API.DTOs.Bookings;

/// Represents the request to update an available energy booking slot.
public class UpdateBookingSlotRequest
{
    public DateTime StartTime { get; set; }

    public DateTime EndTime { get; set; }

    public int TotalCapacity { get; set; }
}