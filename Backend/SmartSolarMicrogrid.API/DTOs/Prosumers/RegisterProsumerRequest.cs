using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Prosumers;

/// Represents the information required to register a new Solar Prosumer.
public class RegisterProsumerRequest
{
    [Required]
    public string Nic { get; set; } = string.Empty;

    [Required]
    public string Name { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Phone { get; set; } = string.Empty;

    [Required]
    public string Address { get; set; } = string.Empty;

    [Required]
    [MinLength(8)]
    public string Password { get; set; } = string.Empty;
}