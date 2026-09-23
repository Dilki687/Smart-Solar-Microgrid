using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Stations;

public class UpdateStationRequest
{
    [Required]
    [MinLength(2)]
    public string Name { get; set; } = string.Empty;

    [Required]
    [MinLength(5)]
    public string Address { get; set; } = string.Empty;

    [Range(-90, 90)]
    public double Latitude { get; set; }

    [Range(-180, 180)]
    public double Longitude { get; set; }

    [Range(0.1, double.MaxValue)]
    public double CapacityKw { get; set; }

    [Required]
    public string OperatorUserId { get; set; } = string.Empty;
}
