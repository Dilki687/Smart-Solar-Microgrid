using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Users;

/// Represents the information required to create a Backoffice or Grid Operator account.
public class CreateUserRequest
{
    [Required]
    public string Nic { get; set; } = string.Empty;

    [Required]
    public string Name { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    public string Phone { get; set; } = string.Empty;

    public string Address { get; set; } = string.Empty;

    [Required]
    [MinLength(8)]
    public string Password { get; set; } = string.Empty;

    [Required]
    public string Role { get; set; } = string.Empty;
}