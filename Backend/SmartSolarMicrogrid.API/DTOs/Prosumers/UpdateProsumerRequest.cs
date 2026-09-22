using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Prosumers;

/// information that a Prosumer can update in their profile.
public class UpdateProsumerRequest
{
    [Required]
    public string Name { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Phone { get; set; } = string.Empty;

    [Required]
    public string Address { get; set; } = string.Empty;
}