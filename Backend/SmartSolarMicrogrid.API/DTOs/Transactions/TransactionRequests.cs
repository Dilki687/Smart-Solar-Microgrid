using System.ComponentModel.DataAnnotations;

namespace SmartSolarMicrogrid.API.DTOs.Transactions;

public class GenerateQrRequest
{
    [Required, StringLength(100)]
    public string ReservationId { get; set; } = string.Empty;
}

public class VerifyQrRequest
{
    [Required, StringLength(100)]
    public string QrToken { get; set; } = string.Empty;
}
