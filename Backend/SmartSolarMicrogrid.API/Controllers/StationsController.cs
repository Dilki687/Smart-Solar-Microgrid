using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Stations;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

/// Solar station management endpoints.
[ApiController]
[Route("api/stations")]
[Authorize]
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
    [Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
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

/// Updates an existing solar station.
[HttpPut("{stationId}")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> UpdateStation(
    string stationId,
    [FromBody] UpdateStationRequest request)
{
    if (!ModelState.IsValid)
    {
        return ValidationProblem(ModelState);
    }

    var result =
        await _stationService.UpdateStationAsync(
            stationId,
            request);

    return StatusCode(
        result.StatusCode,
        result.Response);
}
/// Deactivates a solar station if it has no active reservations.
[HttpPatch("{stationId}/deactivate")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> DeactivateStation(
    string stationId)
{
    var result =
        await _stationService.DeactivateStationAsync(
            stationId);

    return StatusCode(
        result.StatusCode,
        result.Response);
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
    // Retrieves active solar stations near the user's location.
[HttpGet("/api/nodes/nearby")]
[AllowAnonymous]
public async Task<IActionResult> GetNearbyStations(
    [FromQuery] double latitude,
    [FromQuery] double longitude,
    [FromQuery] double radiusKm = 10)
{
    // Validate the search radius.
    if (radiusKm <= 0)
    {
        return BadRequest(new
        {
            message = "Radius must be greater than zero."
        });
    }

    // Retrieve nearby stations through the station service.
    var stations =
        await _stationService.GetNearbyStationsAsync(
            latitude,
            longitude,
            radiusKm);

    // Return the nearby stations.
    return Ok(new
    {
        stations
    });
}
}
