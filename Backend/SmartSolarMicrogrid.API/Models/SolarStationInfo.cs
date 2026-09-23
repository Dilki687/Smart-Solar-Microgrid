using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models;

/// Represents a solar microgrid station.
public class SolarStationInfo
{
    /// MongoDB document identifier.
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    /// Application-level station identifier.
    [BsonElement("StationId")]
    public string StationId { get; set; } = string.Empty;

    /// Name of the solar station.
    [BsonElement("Name")]
    public string Name { get; set; } = string.Empty;

    /// Physical address of the station.
    [BsonElement("Address")]
    public string Address { get; set; } = string.Empty;

    /// GPS latitude of the station.
    [BsonElement("Latitude")]
    public double Latitude { get; set; }

    /// GPS longitude of the station.
    [BsonElement("Longitude")]
    public double Longitude { get; set; }

    /// Station capacity measured in kilowatts.
    [BsonElement("CapacityKw")]
    public double CapacityKw { get; set; }

    /// Station status, for example Active or Inactive.
    [BsonElement("Status")]
    public string Status { get; set; } = "Active";

    /// ID of the operator responsible for the station.
    [BsonElement("OperatorUserId")]
    public string OperatorUserId { get; set; } = string.Empty;

    /// Date and time when the station was created.
    [BsonElement("CreatedAt")]
    public DateTime CreatedAt { get; set; }

    /// Date and time when the station was last updated.
    [BsonElement("UpdatedAt")]
    public DateTime? UpdatedAt { get; set; }
}