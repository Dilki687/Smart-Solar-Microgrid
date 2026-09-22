using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Microsoft.IdentityModel.Tokens;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.Auth;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Settings;

namespace SmartSolarMicrogrid.API.Services;

/// Handles user authentication and JWT token generation.
public class AuthService
{
    private readonly MongoDbService _mongoDbService;
    private readonly PasswordService _passwordService;
    private readonly IConfiguration _configuration;
    private readonly TokenBlacklistService _tokenBlacklistService;

    /// Initializes the authentication service and required dependencies.
    public AuthService(
        MongoDbService mongoDbService,
        PasswordService passwordService,
        IConfiguration configuration,
        TokenBlacklistService tokenBlacklistService)
    {
        _mongoDbService = mongoDbService;
        _passwordService = passwordService;
        _configuration = configuration;
        _tokenBlacklistService = tokenBlacklistService;
    }

    /// Authenticates a user using NIC or email and generates a JWT token.
    public async Task<(bool Success, int StatusCode, object Response)> LoginAsync(
        LoginRequest request)
    {
        var users = _mongoDbService.GetUsersCollection();

        string identifier = request.Identifier.Trim();

        var filter = Builders<User>.Filter.Or(
            Builders<User>.Filter.Eq(
                x => x.NIC,
                identifier),
            Builders<User>.Filter.Eq(
                x => x.Email,
                identifier));

        var user = await users.Find(filter).FirstOrDefaultAsync();

        if (user == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "User does not exist."
                });
        }

        if (user.AccountStatus != AccountStatus.Active)
        {
            return (
                false,
                403,
                new
                {
                    message = "Account is inactive or deactivated."
                });
        }

        bool validPassword =
            _passwordService.VerifyPassword(
                request.Password,
                user.PasswordHash);

        if (!validPassword)
        {
            return (
                false,
                401,
                new
                {
                    message = "Invalid credentials."
                });
        }

        string token = GenerateToken(user);

        return (
            true,
            200,
            new LoginResponse
            {
                Message = "Login successful",
                User = new LoginUserResponse
                {
                    Id = user.UserId,
                    Nic = user.NIC,
                    Name = user.Name,
                    Email = user.Email,
                    Role = user.Role,
                    Status = user.AccountStatus
                },
                Token = token
            });
    }

    /// Generates a signed JWT token containing the authenticated user's identity and role.
    private string GenerateToken(User user)
    {
        var jwtKey = _configuration["Jwt:Key"];
        var issuer = _configuration["Jwt:Issuer"];
        var audience = _configuration["Jwt:Audience"];

        if (string.IsNullOrWhiteSpace(jwtKey))
        {
            throw new InvalidOperationException("Jwt:Key is not configured.");
        }

        var securityKey = new SymmetricSecurityKey(
            Encoding.UTF8.GetBytes(jwtKey));

        var credentials = new SigningCredentials(
            securityKey,
            SecurityAlgorithms.HmacSha256);

        string tokenId = Guid.NewGuid().ToString();

        var claims = new List<Claim>
        {
            new(JwtRegisteredClaimNames.Sub, user.UserId),
            new(JwtRegisteredClaimNames.Jti, tokenId),
            new(ClaimTypes.NameIdentifier, user.UserId),
            new(ClaimTypes.Name, user.Name),
            new(ClaimTypes.Role, user.Role),
            new("nic", user.NIC),
            new("email", user.Email)
        };

        int expiryMinutes = 60;

        if (int.TryParse(
            _configuration["Jwt:ExpiryMinutes"],
            out int configuredExpiry))
        {
            expiryMinutes = configuredExpiry;
        }

        var token = new JwtSecurityToken(
            issuer: issuer,
            audience: audience,
            claims: claims,
            expires: DateTime.UtcNow.AddMinutes(expiryMinutes),
            signingCredentials: credentials);

        return new JwtSecurityTokenHandler().WriteToken(token);
    }

    /// Adds the current JWT token to the logout blacklist.
    public void Logout(string token)
    {
        var handler = new JwtSecurityTokenHandler();

        var jwt = handler.ReadJwtToken(token);

        var tokenId = jwt.Claims
            .FirstOrDefault(x => x.Type == JwtRegisteredClaimNames.Jti)
            ?.Value;

        if (string.IsNullOrWhiteSpace(tokenId))
        {
            return;
        }

        var expiresAt = jwt.ValidTo;

        _tokenBlacklistService.BlacklistToken(
            tokenId,
            expiresAt);
    }
}