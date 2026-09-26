using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Transactions;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController, Route("api/transactions"), Authorize]
public class TransactionsController(TransactionService service) : ControllerBase
{
    [HttpPost("qr"), Authorize(Roles = UserRole.Prosumer)]
    public async Task<IActionResult> Generate(GenerateQrRequest request)
    {
        var id = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(id)) return Unauthorized();
        var result = await service.GenerateAsync(request.ReservationId.Trim(), id);
        return StatusCode(result.StatusCode, result.Response);
    }

    [HttpPost("verify"), Authorize(Roles = UserRole.GridOperator)]
    public async Task<IActionResult> Verify(VerifyQrRequest request)
    {
        var id = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(id)) return Unauthorized();
        var result = await service.VerifyAsync(request.QrToken, id);
        return StatusCode(result.StatusCode, result.Response);
    }

    [HttpPost("{transactionId}/complete"), Authorize(Roles = UserRole.GridOperator)]
    public async Task<IActionResult> Complete(string transactionId)
    {
        var id = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(id)) return Unauthorized();
        var result = await service.CompleteAsync(transactionId, id);
        return StatusCode(result.StatusCode, result.Response);
    }
}
