using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Stations;

public class UpdateAvailabilityRequest
{
    [Required, StringLength(100)]
    public string SlotId { get; set; } = string.Empty;

    // Capacity is a count of bookings in the existing model, not kWh.
    [Range(1, int.MaxValue)]
    public int TotalCapacity { get; set; }
}
