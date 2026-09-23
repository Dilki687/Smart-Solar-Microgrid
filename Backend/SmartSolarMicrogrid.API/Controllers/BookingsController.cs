// Provides API endpoints for managing energy booking slots and reservations.

using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SmartSolarMicrogrid.API.DTOs.Bookings;
using SmartSolarMicrogrid.API.Models;
using SmartSolarMicrogrid.API.Services;

namespace SmartSolarMicrogrid.API.Controllers;

[ApiController]
[Route("api/bookings")]
[Authorize]
public class BookingsController : ControllerBase
{
    private readonly BookingService _bookingService;

    // Initializes the bookings controller.
    public BookingsController(BookingService bookingService)
    {
        _bookingService = bookingService;
    }

    // Creates a new energy booking slot.
    [HttpPost("slots")]
    [Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
    public async Task<IActionResult> CreateBookingSlot(
        [FromBody] CreateBookingSlotRequest request)
    {
        // Create the booking slot through the service.
        var result = await _bookingService.CreateBookingSlotAsync(request);

        // Return the service result.
        return StatusCode(result.StatusCode, result.Response);
    }

    // Retrieves active booking slots.
    [HttpGet("slots")]
    [AllowAnonymous]
    public async Task<IActionResult> GetBookingSlots(
        [FromQuery] string? stationId = null)
    {
        // Retrieve booking slots through the service.
        var slots = await _bookingService.GetBookingSlotsAsync(stationId);

        // Return the booking slots.
        return Ok(slots);
    }

    // Creates a reservation for the authenticated prosumer.
    [HttpPost("reservations")]
    [Authorize(Roles = UserRole.Prosumer)]
    public async Task<IActionResult> CreateReservation(
        [FromBody] CreateReservationRequest request)
    {
        // Retrieve the authenticated user's identifier.
        var prosumerUserId =
            User.FindFirstValue(ClaimTypes.NameIdentifier);

        // Check whether the user identifier exists.
        if (string.IsNullOrWhiteSpace(prosumerUserId))
        {
            return Unauthorized(new
            {
                message = "User identity could not be determined."
            });
        }

        // Create the reservation through the booking service.
        var result = await _bookingService.CreateReservationAsync(
            request,
            prosumerUserId);

        // Return the service result.
        return StatusCode(result.StatusCode, result.Response);
    }
// Approves a pending reservation.
[HttpPost("reservations/{reservationId}/approve")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> ApproveReservation(
    string reservationId)
{
    // Approve the reservation through the booking service.
    var result = await _bookingService.ApproveReservationAsync(
        reservationId);

    // Return the service response.
    return StatusCode(
        result.StatusCode,
        result.Response);
}


// Rejects a pending reservation.
[HttpPost("reservations/{reservationId}/reject")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> RejectReservation(
    string reservationId,
    [FromBody] RejectReservationRequest request)
{
    // Reject the reservation through the booking service.
    var result = await _bookingService.RejectReservationAsync(
        reservationId,
        request.Reason);

    // Return the service response.
    return StatusCode(
        result.StatusCode,
        result.Response);
}
// Requests an update to an existing reservation.
[HttpPost("reservations/{reservationId}/request-update")]
[Authorize(Roles = UserRole.Prosumer)]
public async Task<IActionResult> RequestReservationUpdate(
    string reservationId,
    [FromBody] UpdateReservationRequest request)
{
    // Get the authenticated prosumer's user ID.
    var prosumerUserId =
        User.FindFirstValue(ClaimTypes.NameIdentifier);

    // Check whether the user ID exists.
    if (string.IsNullOrWhiteSpace(prosumerUserId))
    {
        return Unauthorized(new
        {
            message = "User identity could not be determined."
        });
    }

    // Submit the update request through the booking service.
    var result =
        await _bookingService.RequestReservationUpdateAsync(
            reservationId,
            request,
            prosumerUserId);

    // Return the service response.
    return StatusCode(result.StatusCode, result.Response);
}
// Approves a pending reservation change.
[HttpPost("reservations/{reservationId}/approve-change")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> ApproveReservationChange(
    string reservationId)
{
    // Approve the pending change through the booking service.
    var result =
        await _bookingService.ApproveReservationChangeAsync(
            reservationId);

    // Return the service response.
    return StatusCode(
        result.StatusCode,
        result.Response);
}
// Rejects a pending reservation change.
[HttpPost("reservations/{reservationId}/reject-change")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> RejectReservationChange(
    string reservationId,
    [FromBody] RejectReservationRequest request)
{
    // Reject the pending change through the booking service.
    var result =
        await _bookingService.RejectReservationChangeAsync(
            reservationId,
            request.Reason);

    // Return the service response.
    return StatusCode(
        result.StatusCode,
        result.Response);
}
[HttpPost("reservations/{reservationId}/cancel")]
[Authorize(Roles = "PROSUMER")]
public async Task<IActionResult> CancelReservation(
    string reservationId,
    [FromBody] RejectReservationRequest request)
{
    var prosumerUserId =
        User.FindFirst("userId")?.Value ??
        User.FindFirst(ClaimTypes.NameIdentifier)?.Value;

    if (string.IsNullOrWhiteSpace(prosumerUserId))
    {
        return Unauthorized(new
        {
            message = "User identity was not found."
        });
    }

    var result = await _bookingService.CancelReservationAsync(
        reservationId,
        prosumerUserId,
        request.Reason);

    return StatusCode(result.StatusCode, result.Response);
}
// Updates an existing booking slot.
// Accessible by Backoffice users and Grid Operators.
[HttpPut("slots/{slotId}")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> UpdateBookingSlot(
    string slotId,
    [FromBody] UpdateBookingSlotRequest request)
{
    // Validate the incoming request model.
    if (!ModelState.IsValid)
    {
        return ValidationProblem(ModelState);
    }

    // Update the booking slot through the booking service.
    var result = await _bookingService.UpdateBookingSlotAsync(
        slotId,
        request);

    // Return the service response with its corresponding status code.
    return StatusCode(
        result.StatusCode,
        result.Response);
}
// Deactivates a booking slot.
// Accessible by Backoffice users.
[HttpPatch("slots/{slotId}/deactivate")]
[Authorize(Roles = UserRole.Backoffice)]
public async Task<IActionResult> DeactivateBookingSlot(
    string slotId)
{
    // Deactivate the booking slot through the booking service.
    var result =
        await _bookingService.DeactivateBookingSlotAsync(slotId);

    // Return the service response with its corresponding status code.
    return StatusCode(
        result.StatusCode,
        result.Response);
}
// Retrieves reservations for Backoffice and Grid Operator users.
[HttpGet("reservations")]
[Authorize(Roles = UserRole.Backoffice + "," + UserRole.GridOperator)]
public async Task<IActionResult> GetReservations(
    [FromQuery] string? status = null)
{
    var reservations =
        await _bookingService.GetReservationsAsync(status);

    return Ok(new
    {
        reservations
    });
}
// Retrieves reservations belonging to the authenticated prosumer.
[HttpGet("reservations/my")]
[Authorize(Roles = UserRole.Prosumer)]
public async Task<IActionResult> GetMyReservations()
{
    var prosumerUserId =
        User.FindFirstValue(ClaimTypes.NameIdentifier);

    if (string.IsNullOrWhiteSpace(prosumerUserId))
    {
        return Unauthorized(new
        {
            message = "User identity could not be determined."
        });
    }

    var reservations =
        await _bookingService.GetMyReservationsAsync(
            prosumerUserId);

    return Ok(new
    {
        reservations
    });
}
}