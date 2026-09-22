using System.Collections.Concurrent;

namespace SmartSolarMicrogrid.API.Services;

/// Maintains a list of JWT identifiers that have been logged out.
public class TokenBlacklistService
{
    private readonly ConcurrentDictionary<string, DateTime> _blacklistedTokens = new();

    /// Adds a JWT identifier to the logout blacklist.
    public void BlacklistToken(string tokenId, DateTime expiresAt)
    {
        _blacklistedTokens[tokenId] = expiresAt;
    }

    /// Checks whether a JWT identifier has been blacklisted.
    public bool IsBlacklisted(string tokenId)
    {
        if (!_blacklistedTokens.TryGetValue(tokenId, out var expiresAt))
        {
            return false;
        }

        if (expiresAt <= DateTime.UtcNow)
        {
            _blacklistedTokens.TryRemove(tokenId, out _);
            return false;
        }

        return true;
    }
}