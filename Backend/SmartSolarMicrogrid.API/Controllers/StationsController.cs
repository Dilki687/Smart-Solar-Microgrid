using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Stations;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

/// Solar station management endpoints.
[ApiController]
[Route("api/stations")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public class StationsController : ControllerBase
{
    private readonly StationService _stationService;

    /// Initializes the stations controller.
    public StationsController(StationService stationService)
    {
        _stationService = stationService;
    }

    /// Creates a new solar station.
    [HttpPost]
    public async Task<IActionResult> CreateStation(
        [FromBody] CreateStationRequest request)
    {
        if (!ModelState.IsValid)
        {
            return ValidationProblem(ModelState);
        }

        var result =
            await _stationService.CreateStationAsync(request);

        return StatusCode(
            result.StatusCode,
            result.Response);
    }

    /// Retrieves all solar stations with an optional status filter.
    [HttpGet]
    public async Task<IActionResult> GetStations(
        [FromQuery] string? status)
    {
        var stations =
            await _stationService.GetStationsAsync(status);

        var response = stations.Select(station => new
        {
            stationId = station.StationId,
            name = station.Name,
            address = station.Address,
            latitude = station.Latitude,
            longitude = station.Longitude,
            capacityKw = station.CapacityKw,
            status = station.Status,
            operatorUserId = station.OperatorUserId,
            createdAt = station.CreatedAt,
            updatedAt = station.UpdatedAt
        });

        return Ok(new
        {
            stations = response
        });
    }

    /// Retrieves a station by its application-level ID.
    [HttpGet("{stationId}")]
    public async Task<IActionResult> GetStation(
        string stationId)
    {
        var station =
            await _stationService.GetStationAsync(stationId);

        if (station == null)
        {
            return NotFound(new
            {
                message = "Station not found."
            });
        }

        return Ok(new
        {
            stationId = station.StationId,
            name = station.Name,
            address = station.Address,
            latitude = station.Latitude,
            longitude = station.Longitude,
            capacityKw = station.CapacityKw,
            status = station.Status,
            operatorUserId = station.OperatorUserId,
            createdAt = station.CreatedAt,
            updatedAt = station.UpdatedAt
        });
    }
}
