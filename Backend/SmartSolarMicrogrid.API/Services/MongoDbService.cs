using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

/// Provides access to the MongoDB database used by the Smart Solar Microgrid API.
public class MongoDbService
{
    private readonly IMongoDatabase _database;

    /// Creates the MongoDB client and connects to the configured database.
    public MongoDbService(IConfiguration configuration)
    {
        var connectionString =
            configuration["MongoDb:ConnectionString"];

        var databaseName =
            configuration["MongoDb:DatabaseName"];

        if (string.IsNullOrWhiteSpace(connectionString))
        {
            throw new InvalidOperationException(
                "MongoDb:ConnectionString is not configured.");
        }

        if (string.IsNullOrWhiteSpace(databaseName))
        {
            throw new InvalidOperationException(
                "MongoDb:DatabaseName is not configured.");
        }

        var client = new MongoClient(connectionString);

        _database = client.GetDatabase(databaseName);

        CreateIndexes();
    }

    /// Returns the MongoDB database.
    public IMongoDatabase GetDatabase()
    {
        return _database;
    }

    /// Returns the Users collection.
    public IMongoCollection<User> GetUsersCollection()
    {
        return _database.GetCollection<User>("Users");
    }

    /// Returns the solar stations collection.
    public IMongoCollection<SolarStationInfo> GetStationsCollection()
    {
        return _database.GetCollection<SolarStationInfo>("SolarStations");
    }

    /// Creates unique indexes for users and stations.
    private void CreateIndexes()
    {
        var users = GetUsersCollection();

        var nicIndex = new CreateIndexModel<User>(
            Builders<User>.IndexKeys.Ascending(x => x.NIC),
            new CreateIndexOptions
            {
                Unique = true
            });

        var emailIndex = new CreateIndexModel<User>(
            Builders<User>.IndexKeys.Ascending(x => x.Email),
            new CreateIndexOptions
            {
                Unique = true
            });

        users.Indexes.CreateMany(
            new[]
            {
                nicIndex,
                emailIndex
            });

        var stations = GetStationsCollection();

        var stationIdIndex = new CreateIndexModel<SolarStationInfo>(
            Builders<SolarStationInfo>.IndexKeys.Ascending(x => x.StationId),
            new CreateIndexOptions
            {
                Unique = true
            });

        stations.Indexes.CreateOne(stationIdIndex);
    }
}
