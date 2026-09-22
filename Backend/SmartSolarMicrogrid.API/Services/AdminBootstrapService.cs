using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

/// Creates the initial Backoffice administrator account when required during development setup.
public class AdminBootstrapService
{
    private readonly MongoDbService _mongoDbService;
    private readonly PasswordService _passwordService;
    private readonly IConfiguration _configuration;

    /// Initializes the administrator bootstrap service.
    public AdminBootstrapService(
        MongoDbService mongoDbService,
        PasswordService passwordService,
        IConfiguration configuration)
    {
        _mongoDbService = mongoDbService;
        _passwordService = passwordService;
        _configuration = configuration;
    }

    /// Creates the initial Backoffice account if one does not already exist.
    public async Task CreateInitialAdminAsync()
    {
        bool enabled = bool.TryParse(
            _configuration["BootstrapAdmin:Enabled"],
            out bool result) && result;

        if (!enabled)
        {
            return;
        }

        var users = _mongoDbService.GetUsersCollection();

        bool adminExists = await users
            .Find(x => x.Role == UserRole.Backoffice)
            .AnyAsync();

        if (adminExists)
        {
            return;
        }

        string nic =
            _configuration["BootstrapAdmin:Nic"] ?? string.Empty;

        string name =
            _configuration["BootstrapAdmin:Name"] ?? string.Empty;

        string email =
            _configuration["BootstrapAdmin:Email"] ?? string.Empty;

        string phone =
            _configuration["BootstrapAdmin:Phone"] ?? string.Empty;

        string address =
            _configuration["BootstrapAdmin:Address"] ?? string.Empty;

        string password =
            _configuration["BootstrapAdmin:Password"] ?? string.Empty;

        if (string.IsNullOrWhiteSpace(nic) ||
            string.IsNullOrWhiteSpace(name) ||
            string.IsNullOrWhiteSpace(email) ||
            string.IsNullOrWhiteSpace(password))
        {
            throw new InvalidOperationException(
                "Bootstrap administrator configuration is incomplete.");
        }

        bool emailExists = await users
            .Find(x => x.Email == email.ToLowerInvariant())
            .AnyAsync();

        if (emailExists)
        {
            throw new InvalidOperationException(
                "Bootstrap administrator email already belongs to another account.");
        }

        var admin = new User
        {
            UserId = "USER001",
            NIC = nic,
            Name = name,
            Email = email.ToLowerInvariant(),
            Phone = phone,
            Address = address,
            PasswordHash =
                _passwordService.HashPassword(password),
            Role = UserRole.Backoffice,
            AccountStatus = AccountStatus.Active,
            CreatedAt = DateTime.UtcNow,
            DeactivationRequestedAt = null
        };

        await users.InsertOneAsync(admin);
    }
}