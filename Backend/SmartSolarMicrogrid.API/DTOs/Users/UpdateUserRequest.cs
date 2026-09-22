using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Users;

/// Represents information that can be updated for a Backoffice or Grid Operator account.
public class UpdateUserRequest
{
    [Required]
    public string Name { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    public string Phone { get; set; } = string.Empty;

    public string Address { get; set; } = string.Empty;

    [Required]
    public string Role { get; set; } = string.Empty;
}