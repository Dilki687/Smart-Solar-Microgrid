using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Users;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

/// Backoffice user management endpoints.
[ApiController]
[Route("api/users")]
[Authorize(Roles = UserRole.Backoffice)]
public class UsersController : ControllerBase
{
    private readonly UserService _userService;

    /// Initializes the users controller.
    public UsersController(UserService userService)
    {
        _userService = userService;
    }

    /// Creates a new Backoffice or Grid Operator account.
    [HttpPost]
    public async Task<IActionResult> CreateUser(
        [FromBody] CreateUserRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var result = await _userService.CreateUserAsync(request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Retrieves Backoffice and Grid Operator accounts with optional filters.
    [HttpGet]
    public async Task<IActionResult> GetUsers(
        [FromQuery] string? role,
        [FromQuery] string? status)
    {
        var users = await _userService.GetUsersAsync(
            role,
            status);

        var response = users.Select(user => new
        {
            id = user.UserId,
            nic = user.NIC,
            name = user.Name,
            email = user.Email,
            role = user.Role,
            status = user.AccountStatus
        });

        return Ok(new
        {
            users = response
        });
    }

    /// Updates information for an existing Backoffice or Grid Operator account.
    [HttpPut("{userId}")]
    public async Task<IActionResult> UpdateUser(
        string userId,
        [FromBody] UpdateUserRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var result = await _userService.UpdateUserAsync(
            userId,
            request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Deactivates a Backoffice or Grid Operator account.
    [HttpPatch("{userId}/deactivate")]
    public async Task<IActionResult> DeactivateUser(
        string userId)
    {
        var result =
            await _userService.DeactivateUserAsync(userId);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }
}