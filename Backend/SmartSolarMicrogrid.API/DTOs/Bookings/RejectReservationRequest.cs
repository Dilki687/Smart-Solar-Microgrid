// Defines the request body used when rejecting a reservation.

namespace SmartSolarMicrogrid.API.DTOs.Bookings;

/// Represents the reason for rejecting a reservation.
public class RejectReservationRequest
{
    /// Gets or sets the rejection reason.
    public string Reason { get; set; } = string.Empty;
}