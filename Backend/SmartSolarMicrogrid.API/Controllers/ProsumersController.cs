using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Prosumers;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

/// Solar Prosumer account management endpoints.
[ApiController]
[Route("api/prosumers")]
public class ProsumersController : ControllerBase
{
    private readonly UserService _userService;

    /// Initializes the Prosumer controller.
    public ProsumersController(UserService userService)
    {
        _userService = userService;
    }

    /// Registers a new Solar Prosumer account.
    [HttpPost]
    [AllowAnonymous]
    public async Task<IActionResult> Register(
        [FromBody] RegisterProsumerRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var result =
            await _userService.RegisterProsumerAsync(request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Retrieves a Prosumer profile for the Prosumer or authorized staff member.
    [HttpGet("{nic}")]
    [Authorize]
    public async Task<IActionResult> GetProfile(string nic)
    {
        if (!CanAccessProsumer(nic))
        {
            return Forbid();
        }

        var prosumer =
            await _userService.GetProsumerAsync(nic);

        if (prosumer == null)
        {
            return NotFound(new
            {
                message = "Prosumer not found."
            });
        }

        return Ok(new
        {
            nic = prosumer.NIC,
            name = prosumer.Name,
            email = prosumer.Email,
            phone = prosumer.Phone,
            address = prosumer.Address,
            status = prosumer.AccountStatus
        });
    }

    /// Updates the authenticated Prosumer's own profile.
    [HttpPut("{nic}")]
    [Authorize(Roles = UserRole.Prosumer)]
    public async Task<IActionResult> UpdateProfile(
        string nic,
        [FromBody] UpdateProsumerRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        string? authenticatedNic =
            User.FindFirstValue("nic");

        if (!string.Equals(
            authenticatedNic,
            nic,
            StringComparison.OrdinalIgnoreCase))
        {
            return Forbid();
        }

        var result =
            await _userService.UpdateProsumerAsync(
                nic,
                request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Prosumer submit a request for account deactivation.
    [HttpPatch("{nic}/deactivation-request")]
    [Authorize(Roles = UserRole.Prosumer)]
    public async Task<IActionResult> RequestDeactivation(
        string nic)
    {
        string? authenticatedNic =
            User.FindFirstValue("nic");

        if (!string.Equals(
            authenticatedNic,
            nic,
            StringComparison.OrdinalIgnoreCase))
        {
            return Forbid();
        }

        var result =
            await _userService.RequestProsumerDeactivationAsync(
                nic);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Retrieves Prosumer accounts waiting for Backoffice approval.
    [HttpGet("deactivation-requests")]
    [Authorize(Roles = UserRole.Backoffice)]
    public async Task<IActionResult> GetDeactivationRequests()
    {
        var users =
            await _userService.GetDeactivationRequestsAsync();

        var response = users.Select(user => new
        {
            nic = user.NIC,
            name = user.Name,
            requestedAt = user.DeactivationRequestedAt,
            status = user.AccountStatus
        });

        return Ok(new
        {
            requests = response
        });
    }

    /// Deactivates a Prosumer account after Backoffice approval.
    [HttpPatch("{nic}/deactivate")]
    [Authorize(Roles = UserRole.Backoffice)]
    public async Task<IActionResult> DeactivateProsumer(
        string nic)
    {
        var result =
            await _userService.DeactivateProsumerAsync(nic);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Reactivates an inactive Prosumer account through Backoffice authorization.
    [HttpPatch("{nic}/reactivate")]
    [Authorize(Roles = UserRole.Backoffice)]
    public async Task<IActionResult> ReactivateProsumer(
        string nic)
    {
        var result =
            await _userService.ReactivateProsumerAsync(nic);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Determines whether the current authenticated user can access a Prosumer profile.
    private bool CanAccessProsumer(string nic)
    {
        if (User.IsInRole(UserRole.Backoffice) ||
            User.IsInRole(UserRole.GridOperator))
        {
            return true;
        }

        if (User.IsInRole(UserRole.Prosumer))
        {
            string? authenticatedNic =
                User.FindFirstValue("nic");

            return string.Equals(
                authenticatedNic,
                nic,
                StringComparison.OrdinalIgnoreCase);
        }

        return false;
    }
}