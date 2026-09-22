using MongoDB.Bson;
using MongoDB.Bson.Serialization.Attributes;

namespace SmartSolarMicrogrid.API.Models;

/// Represents a user (Backoffice, Grid Operator, and Prosumer accounts)
public class User
{
    /// Unique MongoDB identifier for the user record.
    [BsonId]
    [BsonRepresentation(BsonType.ObjectId)]
    public string Id { get; set; } = string.Empty;

    /// Application-level user identifier returned by the API.
    [BsonElement("UserId")]
    public string UserId { get; set; } = string.Empty;

    [BsonElement("NIC")]
    public string NIC { get; set; } = string.Empty;

    [BsonElement("Name")]
    public string Name { get; set; } = string.Empty;

    [BsonElement("Email")]
    public string Email { get; set; } = string.Empty;

    [BsonElement("Phone")]
    public string Phone { get; set; } = string.Empty;

    [BsonElement("Address")]
    public string Address { get; set; } = string.Empty;

    [BsonElement("PasswordHash")]
    public string PasswordHash { get; set; } = string.Empty;

    [BsonElement("Role")]
    public string Role { get; set; } = string.Empty;

    [BsonElement("AccountStatus")]
    public string AccountStatus { get; set; } = string.Empty;

    [BsonElement("CreatedAt")]
    public DateTime CreatedAt { get; set; }

    /// Date and time when a Prosumer requested account deactivation.
    /// This value is null when no deactivation request is pending or has never been submitted.
    [BsonElement("DeactivationRequestedAt")]
    [BsonIgnoreIfNull]
    public DateTime? DeactivationRequestedAt { get; set; }
}