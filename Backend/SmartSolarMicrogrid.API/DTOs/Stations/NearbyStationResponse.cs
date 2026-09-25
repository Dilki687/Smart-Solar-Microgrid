namespace SmartSolarMicrogrid.API.DTOs.Stations;

/// Represents a nearby solar station.
public class NearbyStationResponse
{
    public string NodeId { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public double Latitude { get; set; }

    public double Longitude { get; set; }

    public double DistanceKm { get; set; }

    public int AvailableSlots { get; set; }

    public string Status { get; set; } = string.Empty;
}