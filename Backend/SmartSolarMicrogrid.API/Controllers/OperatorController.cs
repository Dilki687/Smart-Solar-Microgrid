using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController, Route("api/operator"), Authorize(Roles = UserRole.GridOperator)]
public class OperatorController(OperatorService service) : ControllerBase
{
    [HttpGet("dashboard")]
    public async Task<IActionResult> Dashboard()
    {
        var id = User.FindFirstValue(ClaimTypes.NameIdentifier);
        if (string.IsNullOrWhiteSpace(id)) return Unauthorized();
        var result = await service.DashboardAsync(id);
        return StatusCode(result.StatusCode, result.Response);
    }
}
