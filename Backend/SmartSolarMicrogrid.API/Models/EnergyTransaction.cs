namespace SmartSolarMicrogrid.API.Models;

/// Embedded in its reservation so completion and QR consumption are one atomic write.
public class EnergyTransaction
{
    public string TransactionId { get; set; } = string.Empty;
    public string ReservationId { get; set; } = string.Empty;
    public string ProsumerNIC { get; set; } = string.Empty;
    public string StationId { get; set; } = string.Empty;
    public string SlotId { get; set; } = string.Empty;
    public DateTime ScheduledStartTime { get; set; }
    public DateTime ScheduledEndTime { get; set; }
    public double EnergyAmountKwh { get; set; }
    public string QRToken { get; set; } = string.Empty;
    public string QRStatus { get; set; } = TransactionStates.Active;
    public string TransactionStatus { get; set; } = TransactionStates.Created;
    public DateTime GeneratedAt { get; set; }
    public DateTime ExpiresAt { get; set; }
    public DateTime? VerifiedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public string? VerifiedByOperatorId { get; set; }
    public string? CompletedByOperatorId { get; set; }
}

public static class TransactionStates
{
    public const string Active = "ACTIVE";
    public const string Created = "CREATED";
    public const string Verified = "VERIFIED";
    public const string Used = "USED";
    public const string Completed = "COMPLETED";
}
