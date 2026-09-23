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
}
