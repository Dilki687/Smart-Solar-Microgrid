using System.IdentityModel.Tokens.Jwt;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Auth;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

/// authentication endpoints for all system users.
[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AuthService _authService;

    public AuthController(AuthService authService)
    {
        _authService = authService;
    }

    /// Authenticates a user using NIC or email and returns a JWT token.
    [HttpPost("login")]
    [AllowAnonymous]
    public async Task<IActionResult> Login(
        [FromBody] LoginRequest request)
    {
        if (!ModelState.IsValid)
        {
            return BadRequest(new
            {
                message = "Missing or invalid login information."
            });
        }

        var result = await _authService.LoginAsync(request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Logs out the authenticated user and invalidates the current JWT token.
    [HttpPost("logout")]
    [Authorize]
    public IActionResult Logout()
    {
        string? authorizationHeader =
            Request.Headers.Authorization.FirstOrDefault();

        if (string.IsNullOrWhiteSpace(authorizationHeader))
        {
            return Unauthorized(new
            {
                message = "User is not authenticated."
            });
        }

        string token = authorizationHeader
            .Replace("Bearer ", string.Empty)
            .Trim();

        _authService.Logout(token);

        return Ok(new
        {
            message = "Logout successful"
        });
    }
}