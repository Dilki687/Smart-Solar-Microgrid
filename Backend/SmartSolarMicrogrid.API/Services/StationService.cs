using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.Stations;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

/// Handles solar station business logic and MongoDB operations.
public class StationService
{
    private readonly MongoDbService _mongoDbService;

    public StationService(MongoDbService mongoDbService)
    {
        _mongoDbService = mongoDbService;
    }

    /// Creates a new solar station.
    public async Task<(bool Success, int StatusCode, object Response)>
        CreateStationAsync(CreateStationRequest request)
    {
        var stations = _mongoDbService.GetStationsCollection();

        var stationId = await GenerateStationIdAsync();

        var station = new SolarStationInfo
        {
            StationId = stationId,
            Name = request.Name.Trim(),
            Address = request.Address.Trim(),
            Latitude = request.Latitude,
            Longitude = request.Longitude,
            CapacityKw = request.CapacityKw,
            Status = AccountStatus.Active,
            OperatorUserId = request.OperatorUserId.Trim(),
            CreatedAt = DateTime.UtcNow
        };

        await stations.InsertOneAsync(station);

        return (
            true,
            201,
            new
            {
                message = "Station created successfully",
                station = new
                {
                    stationId = station.StationId,
                    name = station.Name,
                    address = station.Address,
                    latitude = station.Latitude,
                    longitude = station.Longitude,
                    capacityKw = station.CapacityKw,
                    status = station.Status,
                    operatorUserId = station.OperatorUserId
                }
            });
    }

    /// Retrieves all solar stations.
    public async Task<List<SolarStationInfo>> GetStationsAsync(
        string? status)
    {
        var stations = _mongoDbService.GetStationsCollection();

        if (!string.IsNullOrWhiteSpace(status))
        {
            return await stations
                .Find(x => x.Status == status.Trim().ToUpperInvariant())
                .ToListAsync();
        }

        return await stations.Find(_ => true).ToListAsync();
    }

    /// Retrieves a station using its application-level ID.
    public async Task<SolarStationInfo?> GetStationAsync(
        string stationId)
    {
        var stations = _mongoDbService.GetStationsCollection();

        return await stations
            .Find(x => x.StationId == stationId)
            .FirstOrDefaultAsync();
    }
    
/// Updates an existing solar station.
public async Task<(bool Success, int StatusCode, object Response)>
    UpdateStationAsync(
        string stationId,
        UpdateStationRequest request)
{
    var stations = _mongoDbService.GetStationsCollection();

    var existingStation = await stations
        .Find(x => x.StationId == stationId)
        .FirstOrDefaultAsync();

    if (existingStation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Station not found."
            });
    }

    var update = Builders<SolarStationInfo>.Update
        .Set(x => x.Name, request.Name.Trim())
        .Set(x => x.Address, request.Address.Trim())
        .Set(x => x.Latitude, request.Latitude)
        .Set(x => x.Longitude, request.Longitude)
        .Set(x => x.CapacityKw, request.CapacityKw)
        .Set(x => x.OperatorUserId, request.OperatorUserId.Trim())
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    await stations.UpdateOneAsync(
        x => x.StationId == stationId,
        update);

    var updatedStation = await stations
        .Find(x => x.StationId == stationId)
        .FirstOrDefaultAsync();

    return (
        true,
        200,
        new
        {
            message = "Station updated successfully.",
            station = new
            {
                stationId = updatedStation!.StationId,
                name = updatedStation.Name,
                address = updatedStation.Address,
                latitude = updatedStation.Latitude,
                longitude = updatedStation.Longitude,
                capacityKw = updatedStation.CapacityKw,
                status = updatedStation.Status,
                operatorUserId = updatedStation.OperatorUserId,
                createdAt = updatedStation.CreatedAt,
                updatedAt = updatedStation.UpdatedAt
            }
        });
}


    /// Generates the next application-level station identifier.
    private async Task<string> GenerateStationIdAsync()
    {
        var stations = _mongoDbService.GetStationsCollection();

        var latestStation = await stations
            .Find(_ => true)
            .SortByDescending(x => x.CreatedAt)
            .FirstOrDefaultAsync();

        if (latestStation == null)
        {
            return "STATION001";
        }

        if (latestStation.StationId.StartsWith("STATION") &&
            int.TryParse(
                latestStation.StationId.Replace("STATION", ""),
                out int number))
        {
            return $"STATION{(number + 1):D3}";
        }

        return $"STATION{DateTime.UtcNow.Ticks}";
    }
   /// Deactivates a solar station after checking for active reservations.
/// Returns 409 Conflict if the station has pending or confirmed reservations.
public async Task<(bool Success, int StatusCode, object Response)>
    DeactivateStationAsync(string stationId)
{
    var stations = _mongoDbService.GetStationsCollection();
    var reservations = _mongoDbService.GetReservationsCollection();

    var station = await stations
        .Find(x => x.StationId == stationId)
        .FirstOrDefaultAsync();

    if (station == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Station not found."
            });
    }

    var activeReservations = await reservations
        .Find(x =>
            x.StationId == stationId &&
            (
                x.Status == BookingStatus.PENDING ||
                x.Status == BookingStatus.CONFIRMED
            ))
        .AnyAsync();

    if (activeReservations)
    {
        return (
            false,
            409,
            new
            {
                message =
                    "Station cannot be deactivated because it has active reservations."
            });
    }

    var update = Builders<SolarStationInfo>.Update
        .Set(x => x.Status, AccountStatus.Inactive)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    await stations.UpdateOneAsync(
        x => x.StationId == stationId,
        update);

    return (
        true,
        200,
        new
        {
            message = "Station deactivated successfully.",
            stationId = stationId,
            status = AccountStatus.Inactive
        });
}
/// Retrieves active solar stations within the specified radius.
public async Task<List<NearbyStationResponse>>
    GetNearbyStationsAsync(
        double latitude,
        double longitude,
        double radiusKm)
{
    var stationsCollection =
        _mongoDbService.GetStationsCollection();

    var slotsCollection =
        _mongoDbService.GetBookingSlotsCollection();

    var stations = await stationsCollection
        .Find(x => x.Status == AccountStatus.Active)
        .ToListAsync();

    var activeSlots = await slotsCollection
        .Find(x => x.IsActive)
        .ToListAsync();

    var nearbyStations = stations
        .Select(station =>
        {
            var distanceKm = CalculateDistanceKm(
                latitude,
                longitude,
                station.Latitude,
                station.Longitude);

            var availableSlots = activeSlots
                .Count(slot =>
                    slot.StationId == station.StationId &&
                    slot.AvailableCapacity > 0);

            return new NearbyStationResponse
            {
                NodeId = station.StationId,
                Name = station.Name,
                Latitude = station.Latitude,
                Longitude = station.Longitude,
                DistanceKm = Math.Round(distanceKm, 2),
                AvailableSlots = availableSlots,
                Status = station.Status
            };
        })
        .Where(x => x.DistanceKm <= radiusKm)
        .OrderBy(x => x.DistanceKm)
        .ToList();

    return nearbyStations;
}
/// Calculates the distance between two GPS coordinates using the Haversine formula.
private static double CalculateDistanceKm(
    double latitude1,
    double longitude1,
    double latitude2,
    double longitude2)
{
    const double earthRadiusKm = 6371;

    var latitudeDifference =
        DegreesToRadians(latitude2 - latitude1);

    var longitudeDifference =
        DegreesToRadians(longitude2 - longitude1);

    var a =
        Math.Sin(latitudeDifference / 2) *
        Math.Sin(latitudeDifference / 2) +
        Math.Cos(DegreesToRadians(latitude1)) *
        Math.Cos(DegreesToRadians(latitude2)) *
        Math.Sin(longitudeDifference / 2) *
        Math.Sin(longitudeDifference / 2);

    var c = 2 * Math.Atan2(
        Math.Sqrt(a),
        Math.Sqrt(1 - a));

    return earthRadiusKm * c;
}

/// Converts degrees to radians.
private static double DegreesToRadians(double degrees)
{
    return degrees * Math.PI / 180;
}
}
