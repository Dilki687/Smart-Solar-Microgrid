using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Auth;

/// Represents the login information submitted by a user.
public class LoginRequest
{
    /// NIC or email address used to identify the account.
    [Required]
    public string Identifier { get; set; } = string.Empty;

    /// Plain-text password
    [Required]
    public string Password { get; set; } = string.Empty;
}