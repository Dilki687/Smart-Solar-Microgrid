namespace SmartSolarMicrogrid.API.DTOs.Auth;

/// Represents the successful authentication response.
public class LoginResponse
{
    public string Message { get; set; } = string.Empty;

    public LoginUserResponse User { get; set; } = new();

    public string Token { get; set; } = string.Empty;
}

/// Represents safe user information returned after login.
/// Password information is intentionally excluded.
public class LoginUserResponse
{
    public string Id { get; set; } = string.Empty;

    public string Nic { get; set; } = string.Empty;

    public string Name { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public string Role { get; set; } = string.Empty;

    public string Status { get; set; } = string.Empty;
}