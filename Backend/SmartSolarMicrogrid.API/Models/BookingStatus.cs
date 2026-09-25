namespace SmartSolarMicrogrid.API.Models;

/// Represents the current state of an energy reservation.
public enum BookingStatus
{
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}