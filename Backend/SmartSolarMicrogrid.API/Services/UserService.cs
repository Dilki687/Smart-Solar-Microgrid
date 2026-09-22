using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.Prosumers;
using SmartSolarMicrogrid.API.DTOs.Users;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

/// Handles user and prosumer business logic and MongoDB operations.
public class UserService
{
    private readonly MongoDbService _mongoDbService;
    private readonly PasswordService _passwordService;

    /// Initializes the user service and required dependencies.
    public UserService(
        MongoDbService mongoDbService,
        PasswordService passwordService)
    {
        _mongoDbService = mongoDbService;
        _passwordService = passwordService;
    }

    /// Creates a new Backoffice or Grid Operator account.
    public async Task<(bool Success, int StatusCode, object Response)>
        CreateUserAsync(CreateUserRequest request)
    {
        string role = request.Role.Trim().ToUpperInvariant();

        if (role != UserRole.Backoffice &&
            role != UserRole.GridOperator)
        {
            return (
                false,
                400,
                new
                {
                    message = "Only BACKOFFICE or GRID_OPERATOR accounts can be created using this endpoint."
                });
        }

        var users = _mongoDbService.GetUsersCollection();

        bool nicExists = await users
            .Find(x => x.NIC == request.Nic.Trim())
            .AnyAsync();

        if (nicExists)
        {
            return (
                false,
                409,
                new
                {
                    message = "NIC already exists."
                });
        }

        bool emailExists = await users
            .Find(x => x.Email == request.Email.Trim().ToLowerInvariant())
            .AnyAsync();

        if (emailExists)
        {
            return (
                false,
                409,
                new
                {
                    message = "Email already exists."
                });
        }

        var user = new User
        {
            UserId = await GenerateUserIdAsync(),
            NIC = request.Nic.Trim(),
            Name = request.Name.Trim(),
            Email = request.Email.Trim().ToLowerInvariant(),
            Phone = request.Phone.Trim(),
            Address = request.Address.Trim(),
            PasswordHash = _passwordService.HashPassword(request.Password),
            Role = role,
            AccountStatus = AccountStatus.Active,
            CreatedAt = DateTime.UtcNow
        };

        await users.InsertOneAsync(user);

        return (
            true,
            201,
            new
            {
                message = "User created successfully",
                user = new
                {
                    id = user.UserId,
                    nic = user.NIC,
                    name = user.Name,
                    email = user.Email,
                    role = user.Role,
                    status = user.AccountStatus
                }
            });
    }

    /// Retrieves Backoffice and Grid Operator accounts with optional filters.
    public async Task<List<User>> GetUsersAsync(
        string? role,
        string? status)
    {
        var users = _mongoDbService.GetUsersCollection();

        var filters = new List<FilterDefinition<User>>();

        filters.Add(
            Builders<User>.Filter.In(
                x => x.Role,
                new[]
                {
                    UserRole.Backoffice,
                    UserRole.GridOperator
                }));

        if (!string.IsNullOrWhiteSpace(role))
        {
            filters.Add(
                Builders<User>.Filter.Eq(
                    x => x.Role,
                    role.Trim().ToUpperInvariant()));
        }

        if (!string.IsNullOrWhiteSpace(status))
        {
            filters.Add(
                Builders<User>.Filter.Eq(
                    x => x.AccountStatus,
                    status.Trim().ToUpperInvariant()));
        }

        var filter = Builders<User>.Filter.And(filters);

        return await users.Find(filter).ToListAsync();
    }

    /// Updates an existing Backoffice or Grid Operator account.
    public async Task<(bool Success, int StatusCode, object Response)>
        UpdateUserAsync(
            string userId,
            UpdateUserRequest request)
    {
        var users = _mongoDbService.GetUsersCollection();

        var user = await users
            .Find(x => x.UserId == userId)
            .FirstOrDefaultAsync();

        if (user == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "User not found."
                });
        }

        string role = request.Role.Trim().ToUpperInvariant();

        if (role != UserRole.Backoffice &&
            role != UserRole.GridOperator)
        {
            return (
                false,
                400,
                new
                {
                    message = "Invalid user role."
                });
        }

        bool duplicateEmail = await users
            .Find(x =>
                x.Email == request.Email.Trim().ToLowerInvariant() &&
                x.UserId != userId)
            .AnyAsync();

        if (duplicateEmail)
        {
            return (
                false,
                409,
                new
                {
                    message = "Email already exists."
                });
        }

        var update = Builders<User>.Update
            .Set(x => x.Name, request.Name.Trim())
            .Set(x => x.Email, request.Email.Trim().ToLowerInvariant())
            .Set(x => x.Phone, request.Phone.Trim())
            .Set(x => x.Address, request.Address.Trim())
            .Set(x => x.Role, role);

        await users.UpdateOneAsync(
            x => x.UserId == userId,
            update);

        return (
            true,
            200,
            new
            {
                message = "User updated successfully"
            });
    }

    /// Deactivates a Backoffice or Grid Operator account.
    public async Task<(bool Success, int StatusCode, object Response)>
        DeactivateUserAsync(string userId)
    {
        var users = _mongoDbService.GetUsersCollection();

        var user = await users
            .Find(x => x.UserId == userId)
            .FirstOrDefaultAsync();

        if (user == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "User not found."
                });
        }

        var update = Builders<User>.Update
            .Set(x => x.AccountStatus, AccountStatus.Inactive);

        await users.UpdateOneAsync(
            x => x.UserId == userId,
            update);

        return (
            true,
            200,
            new
            {
                message = "User deactivated successfully",
                status = AccountStatus.Inactive
            });
    }

    /// Registers a new Solar Prosumer account.
    public async Task<(bool Success, int StatusCode, object Response)>
        RegisterProsumerAsync(
            RegisterProsumerRequest request)
    {
        var users = _mongoDbService.GetUsersCollection();

        string nic = request.Nic.Trim();
        string email = request.Email.Trim().ToLowerInvariant();

        bool nicExists = await users
            .Find(x => x.NIC == nic)
            .AnyAsync();

        if (nicExists)
        {
            return (
                false,
                409,
                new
                {
                    message = "NIC already registered."
                });
        }

        bool emailExists = await users
            .Find(x => x.Email == email)
            .AnyAsync();

        if (emailExists)
        {
            return (
                false,
                409,
                new
                {
                    message = "Email already registered."
                });
        }

        var user = new User
        {
            UserId = await GenerateUserIdAsync(),
            NIC = nic,
            Name = request.Name.Trim(),
            Email = email,
            Phone = request.Phone.Trim(),
            Address = request.Address.Trim(),
            PasswordHash = _passwordService.HashPassword(request.Password),
            Role = UserRole.Prosumer,
            AccountStatus = AccountStatus.Active,
            CreatedAt = DateTime.UtcNow
        };

        await users.InsertOneAsync(user);

        return (
            true,
            201,
            new
            {
                message = "Prosumer registration successful",
                prosumer = new
                {
                    nic = user.NIC,
                    name = user.Name,
                    email = user.Email,
                    status = user.AccountStatus
                }
            });
    }

    /// Retrieves a Prosumer profile using the NIC.
    public async Task<User?> GetProsumerAsync(string nic)
    {
        var users = _mongoDbService.GetUsersCollection();

        return await users
            .Find(x =>
                x.NIC == nic &&
                x.Role == UserRole.Prosumer)
            .FirstOrDefaultAsync();
    }

    /// Updates a Prosumer's profile information.
    public async Task<(bool Success, int StatusCode, object Response)>
        UpdateProsumerAsync(
            string nic,
            UpdateProsumerRequest request)
    {
        var users = _mongoDbService.GetUsersCollection();

        var prosumer = await GetProsumerAsync(nic);

        if (prosumer == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "Prosumer not found."
                });
        }

        string email = request.Email.Trim().ToLowerInvariant();

        bool duplicateEmail = await users
            .Find(x =>
                x.Email == email &&
                x.NIC != nic)
            .AnyAsync();

        if (duplicateEmail)
        {
            return (
                false,
                409,
                new
                {
                    message = "Email already exists."
                });
        }

        var update = Builders<User>.Update
            .Set(x => x.Name, request.Name.Trim())
            .Set(x => x.Email, email)
            .Set(x => x.Phone, request.Phone.Trim())
            .Set(x => x.Address, request.Address.Trim());

        await users.UpdateOneAsync(
            x =>
                x.NIC == nic &&
                x.Role == UserRole.Prosumer,
            update);

        return (
            true,
            200,
            new
            {
                message = "Profile updated successfully"
            });
    }

    /// Changes a Prosumer account from ACTIVE to PENDING_DEACTIVATION and records the date and time of the request.
    public async Task<(bool Success, int StatusCode, object Response)>
        RequestProsumerDeactivationAsync(string nic)
    {
        var users = _mongoDbService.GetUsersCollection();

        var prosumer = await GetProsumerAsync(nic);

        if (prosumer == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "Prosumer not found."
                });
        }

        if (prosumer.AccountStatus != AccountStatus.Active)
        {
            return (
                false,
                409,
                new
                {
                    message = "Prosumer account is not active."
                });
        }

        var update = Builders<User>.Update
            .Set(
                x => x.AccountStatus,
                AccountStatus.PendingDeactivation)
            .Set(
                x => x.DeactivationRequestedAt,
                DateTime.UtcNow);

        await users.UpdateOneAsync(
            x =>
                x.NIC == nic &&
                x.Role == UserRole.Prosumer,
            update);

        return (
            true,
            200,
            new
            {
                message = "Deactivation request submitted",
                status = AccountStatus.PendingDeactivation
            });
    }

    /// Retrieves all Prosumer accounts waiting for Backoffice deactivation approval.
    public async Task<List<User>> GetDeactivationRequestsAsync()
    {
        var users = _mongoDbService.GetUsersCollection();

        return await users
            .Find(x =>
                x.Role == UserRole.Prosumer &&
                x.AccountStatus == AccountStatus.PendingDeactivation)
            .ToListAsync();
    }

    /// Approves a Prosumer deactivation request and changes the account to INACTIVE.
    public async Task<(bool Success, int StatusCode, object Response)>
        DeactivateProsumerAsync(string nic)
    {
        var users = _mongoDbService.GetUsersCollection();

        var prosumer = await GetProsumerAsync(nic);

        if (prosumer == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "Prosumer not found."
                });
        }

        if (prosumer.AccountStatus != AccountStatus.PendingDeactivation)
        {
            return (
                false,
                409,
                new
                {
                    message = "Prosumer does not have a pending deactivation request."
                });
        }

        // Clear the request timestamp because the pending request has now been approved and the account is inactive.
        var update = Builders<User>.Update
            .Set(
                x => x.AccountStatus,
                AccountStatus.Inactive)
            .Set(
                x => x.DeactivationRequestedAt,
                null);

        await users.UpdateOneAsync(
            x =>
                x.NIC == nic &&
                x.Role == UserRole.Prosumer,
            update);

        return (
            true,
            200,
            new
            {
                message = "Prosumer account deactivated",
                status = AccountStatus.Inactive
            });
    }

    /// Reactivates an inactive Prosumer account.
    public async Task<(bool Success, int StatusCode, object Response)>
        ReactivateProsumerAsync(string nic)
    {
        var users = _mongoDbService.GetUsersCollection();

        var prosumer = await GetProsumerAsync(nic);

        if (prosumer == null)
        {
            return (
                false,
                404,
                new
                {
                    message = "Prosumer not found."
                });
        }

        // Clear any previous deactivation request timestamp because the account is active again.
        var update = Builders<User>.Update
            .Set(
                x => x.AccountStatus,
                AccountStatus.Active)
            .Set(
                x => x.DeactivationRequestedAt,
                null);

        await users.UpdateOneAsync(
            x =>
                x.NIC == nic &&
                x.Role == UserRole.Prosumer,
            update);

        return (
            true,
            200,
            new
            {
                message = "Prosumer account reactivated",
                status = AccountStatus.Active
            });
    }

    /// Generates the next application-level user identifier.
    private async Task<string> GenerateUserIdAsync()
    {
        var users = _mongoDbService.GetUsersCollection();

        var latestUser = await users
            .Find(_ => true)
            .SortByDescending(x => x.CreatedAt)
            .FirstOrDefaultAsync();

        if (latestUser == null)
        {
            return "USER001";
        }

        if (!latestUser.UserId.StartsWith("USER"))
        {
            return $"USER{DateTime.UtcNow.Ticks}";
        }

        if (int.TryParse(
            latestUser.UserId.Replace("USER", ""),
            out int number))
        {
            return $"USER{(number + 1):D3}";
        }

        return $"USER{DateTime.UtcNow.Ticks}";
    }
}