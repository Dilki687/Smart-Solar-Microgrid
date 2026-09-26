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

    /// Returns the energy booking slots collection.
    public IMongoCollection<EnergyBookingSlot> GetBookingSlotsCollection()
    {
        return _database.GetCollection<EnergyBookingSlot>(
            "EnergyBookingSlots");
    }

    /// Returns the energy reservations collection.
    public IMongoCollection<EnergyReservation> GetReservationsCollection()
    {
        return _database.GetCollection<EnergyReservation>(
            "EnergyReservations");
    }

    /// Creates unique indexes for users, stations, slots, and reservations.
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

        var bookingSlots = GetBookingSlotsCollection();

        var slotIdIndex = new CreateIndexModel<EnergyBookingSlot>(
            Builders<EnergyBookingSlot>.IndexKeys.Ascending(x => x.SlotId),
            new CreateIndexOptions
            {
                Unique = true
            });

        bookingSlots.Indexes.CreateOne(slotIdIndex);

        var reservations = GetReservationsCollection();

        var reservationIdIndex = new CreateIndexModel<EnergyReservation>(
            Builders<EnergyReservation>.IndexKeys.Ascending(
                x => x.ReservationId),
            new CreateIndexOptions
            {
                Unique = true
            });

        reservations.Indexes.CreateOne(reservationIdIndex);

        // Only documents with a transaction participate; legacy reservations need no migration.
        foreach (var field in new[] { "Transaction.TransactionId", "Transaction.QRToken" })
        {
            reservations.Indexes.CreateOne(new CreateIndexModel<EnergyReservation>(
                Builders<EnergyReservation>.IndexKeys.Ascending(field),
                new CreateIndexOptions<EnergyReservation>
                {
                    Unique = true,
                    PartialFilterExpression = Builders<EnergyReservation>.Filter.Type(field, MongoDB.Bson.BsonType.String)
                }));
        }
    }
}
