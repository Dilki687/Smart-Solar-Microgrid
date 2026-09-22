namespace SmartSolarMicrogrid.API.Models;

/// Defines the possible account states in the system.
public static class AccountStatus
{
    public const string Active = "ACTIVE";
    public const string Inactive = "INACTIVE";
    public const string PendingDeactivation = "PENDING_DEACTIVATION";
}