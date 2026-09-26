// Handles energy booking slots and reservation operations.

using MongoDB.Driver;
using SmartSolarMicrogrid.API.DTOs.Bookings;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

public class BookingService
{
    private readonly MongoDbService _mongoDbService;

    // Initializes the booking service with the MongoDB service.
    public BookingService(MongoDbService mongoDbService)
    {
        _mongoDbService = mongoDbService;
    }

    // Creates a new energy booking slot.
    public async Task<(bool Success, int StatusCode, object Response)>
        CreateBookingSlotAsync(CreateBookingSlotRequest request)
    {
        // Validate the station identifier.
        if (string.IsNullOrWhiteSpace(request.StationId))
        {
            return (false, 400, new
            {
                message = "StationId is required."
            });
        }

        // Validate the booking time range.
        if (request.StartTime >= request.EndTime)
        {
            return (false, 400, new
            {
                message = "StartTime must be before EndTime."
            });
        }

        // Validate the capacity.
        if (request.TotalCapacity <= 0)
        {
            return (false, 400, new
            {
                message = "TotalCapacity must be greater than zero."
            });
        }

        // Check whether the station exists.
        var stationCollection = _mongoDbService.GetStationsCollection();

        var station = await stationCollection
            .Find(x => x.StationId == request.StationId)
            .FirstOrDefaultAsync();

        if (station == null)
        {
            return (false, 404, new
            {
                message = "Station not found."
            });
        }

        // Prevent creating slots for inactive stations.
        if (!string.Equals(station.Status, "ACTIVE",
                StringComparison.OrdinalIgnoreCase))
        {
            return (false, 400, new
            {
                message = "Booking slots can only be created for active stations."
            });
        }

        // Create the booking slot.
        var slot = new EnergyBookingSlot
        {
            SlotId = $"SLOT-{Guid.NewGuid():N}".ToUpperInvariant(),
            StationId = request.StationId,
            StartTime = request.StartTime.ToUniversalTime(),
            EndTime = request.EndTime.ToUniversalTime(),
            TotalCapacity = request.TotalCapacity,
            AvailableCapacity = request.TotalCapacity,
            IsActive = true,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        // Save the booking slot in MongoDB.
        var collection = _mongoDbService.GetBookingSlotsCollection();

        await collection.InsertOneAsync(slot);

        // Return the created slot.
        return (true, 201, slot);
    }

    // Retrieves active booking slots for a station.
    public async Task<List<EnergyBookingSlot>> GetBookingSlotsAsync(
        string? stationId = null)
    {
        // Start with active booking slots.
        var filter = Builders<EnergyBookingSlot>.Filter.Eq(
            x => x.IsActive,
            true);

        // Filter by station when a station identifier is provided.
        if (!string.IsNullOrWhiteSpace(stationId))
        {
            filter &= Builders<EnergyBookingSlot>.Filter.Eq(
                x => x.StationId,
                stationId);
        }

        // Retrieve slots from MongoDB.
        var collection = _mongoDbService.GetBookingSlotsCollection();

        return await collection
            .Find(filter)
            .SortBy(x => x.StartTime)
            .ToListAsync();
    }
    // Retrieves reservations for Backoffice and Grid Operator users.
public async Task<List<EnergyReservation>> GetReservationsAsync(
    string? status = null)
{
    var reservations =
        _mongoDbService.GetReservationsCollection();

    var filter =
        Builders<EnergyReservation>.Filter.Empty;

    if (!string.IsNullOrWhiteSpace(status) &&
        Enum.TryParse<BookingStatus>(
            status,
            true,
            out var bookingStatus))
    {
        filter &= Builders<EnergyReservation>.Filter.Eq(
            x => x.Status,
            bookingStatus);
    }

    return await reservations
        .Find(filter)
        .SortByDescending(x => x.CreatedAt)
        .ToListAsync();
}
// Retrieves reservations belonging to the authenticated prosumer.
public async Task<List<EnergyReservation>> GetMyReservationsAsync(
    string prosumerUserId)
{
    var reservations =
        _mongoDbService.GetReservationsCollection();

    return await reservations
        .Find(x => x.ProsumerUserId == prosumerUserId)
        .SortByDescending(x => x.CreatedAt)
        .ToListAsync();
}
    // Creates a pending reservation for a prosumer.
public async Task<(bool Success, int StatusCode, object Response)>
    CreateReservationAsync(
        CreateReservationRequest request,
        string prosumerUserId)
{
    // Validate the user identifier.
    if (string.IsNullOrWhiteSpace(prosumerUserId))
    {
        return (false, 400, new
        {
            message = "Prosumer user ID is required."
        });
    }

    // Validate the slot identifier.
    if (string.IsNullOrWhiteSpace(request.SlotId))
    {
        return (false, 400, new
        {
            message = "SlotId is required."
        });
    }

    // Validate the energy amount.
    if (request.EnergyAmountKwh <= 0)
    {
        return (false, 400, new
        {
            message = "EnergyAmountKwh must be greater than zero."
        });
    }

    // Validate the requested schedule.
    if (request.ScheduledStartTime >= request.ScheduledEndTime)
    {
        return (false, 400, new
        {
            message = "ScheduledStartTime must be before ScheduledEndTime."
        });
    }

    // Convert the requested start time to UTC.
    var requestedStartTime =
        request.ScheduledStartTime.ToUniversalTime();

    // Enforce the seven-day reservation rule.
    var currentTime = DateTime.UtcNow;
    var maximumBookingTime = currentTime.AddDays(7);

    if (requestedStartTime <= currentTime ||
        requestedStartTime > maximumBookingTime)
    {
        return (false, 400, new
        {
            message = "Reservations must be scheduled within the next 7 days."
        });
    }

    // Retrieve the requested booking slot.
    var slotCollection = _mongoDbService.GetBookingSlotsCollection();

    var slot = await slotCollection
        .Find(x => x.SlotId == request.SlotId && x.IsActive)
        .FirstOrDefaultAsync();

    // Check whether the slot exists.
    if (slot == null)
    {
        return (false, 404, new
        {
            message = "Active booking slot not found."
        });
    }

    // Confirm that the requested station matches the slot station.
    if (!string.IsNullOrWhiteSpace(request.StationId) &&
        request.StationId != slot.StationId)
    {
        return (false, 400, new
        {
            message = "StationId does not match the selected booking slot."
        });
    }

    // Confirm that the requested time matches the slot schedule.
    var requestedEndTime =
        request.ScheduledEndTime.ToUniversalTime();

    if (requestedStartTime != slot.StartTime ||
        requestedEndTime != slot.EndTime)
    {
        return (false, 400, new
        {
            message = "Reservation times must match the booking slot times."
        });
    }

    // Reserve one capacity unit atomically.
    var capacityFilter =
        Builders<EnergyBookingSlot>.Filter.And(
            Builders<EnergyBookingSlot>.Filter.Eq(
                x => x.SlotId,
                request.SlotId),
            Builders<EnergyBookingSlot>.Filter.Eq(
                x => x.IsActive,
                true),
            Builders<EnergyBookingSlot>.Filter.Gt(
                x => x.AvailableCapacity,
                0));

    var capacityUpdate =
        Builders<EnergyBookingSlot>.Update
            .Inc(x => x.AvailableCapacity, -1)
            .Set(x => x.UpdatedAt, DateTime.UtcNow);

    var updatedSlot = await slotCollection.FindOneAndUpdateAsync(
        capacityFilter,
        capacityUpdate,
        new FindOneAndUpdateOptions<EnergyBookingSlot>
        {
            ReturnDocument = ReturnDocument.After
        });

    // Stop if there is no available capacity.
    if (updatedSlot == null)
    {
        return (false, 409, new
        {
            message = "No available capacity for this booking slot."
        });
    }

    // Create the reservation as pending.
    var reservation = new EnergyReservation
    {
        ReservationId = $"RES-{Guid.NewGuid():N}"
            .ToUpperInvariant(),

        SlotId = slot.SlotId,
        StationId = slot.StationId,
        ProsumerUserId = prosumerUserId,

        ScheduledStartTime = slot.StartTime,
        ScheduledEndTime = slot.EndTime,

        EnergyAmountKwh = request.EnergyAmountKwh,
        Status = BookingStatus.PENDING,

        HasPendingChange = false,

        CreatedAt = DateTime.UtcNow,
        UpdatedAt = DateTime.UtcNow
    };

    // Save the reservation in MongoDB.
    var reservationCollection =
        _mongoDbService.GetReservationsCollection();

    try
    {
        await reservationCollection.InsertOneAsync(reservation);
    }
    catch
    {
        // Restore capacity if reservation creation fails.
        await slotCollection.UpdateOneAsync(
            x => x.SlotId == slot.SlotId,
            Builders<EnergyBookingSlot>.Update
                .Inc(x => x.AvailableCapacity, 1)
                .Set(x => x.UpdatedAt, DateTime.UtcNow));

        throw;
    }

    // Return the newly created reservation.
    return (true, 201, reservation);
}
// Approves a pending reservation.
public async Task<(bool Success, int StatusCode, object Response)>
    ApproveReservationAsync(string reservationId)
{
    // Retrieve the reservations collection.
    var reservations = _mongoDbService.GetReservationsCollection();

    // Retrieve the booking slots collection.
    var slots = _mongoDbService.GetBookingSlotsCollection();

    // Find the reservation by its application-level identifier.
    var reservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Check whether the reservation exists.
    if (reservation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Reservation not found."
            });
    }

    // Only pending reservations can be approved.
    if (reservation.Status != BookingStatus.PENDING)
    {
        return (
            false,
            400,
            new
            {
                message = "Only pending reservations can be approved."
            });
    }

    // Change the reservation status to confirmed.
    var update = Builders<EnergyReservation>.Update
        .Set(x => x.Status, BookingStatus.CONFIRMED)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Update the reservation in MongoDB.
    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.PENDING,
        update);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Return the updated reservation information.
    reservation.Status = BookingStatus.CONFIRMED;
    reservation.UpdatedAt = DateTime.UtcNow;

    return (
        true,
        200,
        new
        {
            message = "Reservation approved successfully.",
            reservation
        });
}


// Rejects a pending reservation and releases its capacity.
public async Task<(bool Success, int StatusCode, object Response)>
    RejectReservationAsync(
        string reservationId,
        string reason)
{
    // Retrieve the reservations collection.
    var reservations = _mongoDbService.GetReservationsCollection();

    // Retrieve the booking slots collection.
    var slots = _mongoDbService.GetBookingSlotsCollection();

    // Validate the rejection reason.
    if (string.IsNullOrWhiteSpace(reason))
    {
        return (
            false,
            400,
            new
            {
                message = "A rejection reason is required."
            });
    }

    // Find the reservation by its application-level identifier.
    var reservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Check whether the reservation exists.
    if (reservation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Reservation not found."
            });
    }

    // Only pending reservations can be rejected.
    if (reservation.Status != BookingStatus.PENDING)
    {
        return (
            false,
            400,
            new
            {
                message = "Only pending reservations can be rejected."
            });
    }

    // Mark the reservation as cancelled.
    var reservationUpdate = Builders<EnergyReservation>.Update
        .Set(x => x.Status, BookingStatus.CANCELLED)
        .Set(x => x.CancellationReason, reason.Trim())
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Update the reservation status.
    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.PENDING,
        reservationUpdate);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Release one capacity unit from the associated slot.
    var slotUpdate = Builders<EnergyBookingSlot>.Update
        .Inc(x => x.AvailableCapacity, 1)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Increase available capacity without exceeding total capacity.
    await slots.UpdateOneAsync(
        x =>
            x.SlotId == reservation.SlotId &&
            x.AvailableCapacity < x.TotalCapacity,
        slotUpdate);

    // Update the local response object.
    reservation.Status = BookingStatus.CANCELLED;
    reservation.CancellationReason = reason.Trim();
    reservation.UpdatedAt = DateTime.UtcNow;

    // Return the rejected reservation.
    return (
        true,
        200,
        new
        {
            message = "Reservation rejected successfully.",
            reservation
        });
}
// Requests an update to an existing reservation.
public async Task<(bool Success, int StatusCode, object Response)>
    RequestReservationUpdateAsync(
        string reservationId,
        UpdateReservationRequest request,
        string prosumerUserId)
{
    // Get the reservations collection.
    var reservations = _mongoDbService.GetReservationsCollection();

    // Get the booking slots collection.
    var slots = _mongoDbService.GetBookingSlotsCollection();

    // Find the existing reservation.
    var reservation = await reservations
        .Find(x =>
            x.ReservationId == reservationId &&
            x.ProsumerUserId == prosumerUserId)
        .FirstOrDefaultAsync();

    // Check whether the reservation exists.
    if (reservation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Reservation not found."
            }
        );
    }

    // Only confirmed reservations can be updated.
    if (reservation.Status != BookingStatus.CONFIRMED)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Only confirmed reservations can be updated."
            }
        );
    }

    // Check the 12-hour notice requirement.
    var minimumNoticeTime = DateTime.UtcNow.AddHours(12);

    if (reservation.ScheduledStartTime < minimumNoticeTime)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Updates must be requested at least 12 hours before the scheduled start time."
            }
        );
    }

    // Validate the requested energy amount.
    if (request.EnergyAmountKwh <= 0)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Energy amount must be greater than zero."
            }
        );
    }

    // Validate the requested schedule.
    if (request.ScheduledStartTime >= request.ScheduledEndTime)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Scheduled start time must be before the end time."
            }
        );
    }

    // Validate the new slot.
    var newSlot = await slots
        .Find(x =>
            x.SlotId == request.SlotId &&
            x.IsActive)
        .FirstOrDefaultAsync();

    // Check whether the new slot exists.
    if (newSlot == null)
    {
        return (
            false,
            404,
            new
            {
                message =
                    "The requested booking slot was not found."
            }
        );
    }

    // Ensure the requested station matches the selected slot.
    if (newSlot.StationId != reservation.StationId)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "The requested slot belongs to a different station."
            }
        );
    }

    // Ensure the requested times match the selected slot.
    if (request.ScheduledStartTime != newSlot.StartTime ||
        request.ScheduledEndTime != newSlot.EndTime)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Requested times must match the selected slot."
            }
        );
    }

    // Check whether the requested slot has available capacity.
    if (newSlot.SlotId != reservation.SlotId &&
        newSlot.AvailableCapacity <= 0)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "The requested booking slot has no available capacity."
            }
        );
    }

    // Store the requested changes without changing
    // the original booking.
    var update = Builders<EnergyReservation>.Update
        .Set(x => x.HasPendingChange, true)

        // NEW:
        // Record that the change request is waiting
        // for Backoffice/Grid Operator approval.
        .Set(x => x.ChangeRequestStatus, "PENDING")

        .Set(x => x.PendingSlotId, request.SlotId)

        .Set(x => x.PendingStationId, newSlot.StationId)

        .Set(
            x => x.PendingScheduledStartTime,
            request.ScheduledStartTime)

        .Set(
            x => x.PendingScheduledEndTime,
            request.ScheduledEndTime)

        .Set(
            x => x.PendingEnergyAmountKwh,
            request.EnergyAmountKwh)

        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Save the pending change.
    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.CONFIRMED &&
             x.UpdatedAt == reservation.UpdatedAt,
        update);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Return a successful response.
    return (
        true,
        200,
        new
        {
            message =
                "Reservation update request submitted successfully.",

            reservationId,

            hasPendingChange = true,

            changeRequestStatus = "PENDING"
        }
    );
}
// Approves a pending change to an existing reservation.
public async Task<(bool Success, int StatusCode, object Response)>
    ApproveReservationChangeAsync(string reservationId)
{
    // Get the reservations and slots collections.
    var reservations = _mongoDbService.GetReservationsCollection();
    var slots = _mongoDbService.GetBookingSlotsCollection();

    // Find the reservation.
    var reservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Check whether the reservation exists.
    if (reservation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Reservation not found."
            }
        );
    }

    // Check whether a pending change exists.
    if (reservation.Status != BookingStatus.CONFIRMED || !reservation.HasPendingChange ||
        string.IsNullOrWhiteSpace(reservation.PendingSlotId) ||
        reservation.PendingScheduledStartTime == null ||
        reservation.PendingScheduledEndTime == null ||
        reservation.PendingEnergyAmountKwh == null)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "No pending change exists for this reservation."
            }
        );
    }

    // Find the requested new slot.
    var newSlot = await slots
        .Find(x =>
            x.SlotId == reservation.PendingSlotId &&
            x.IsActive)
        .FirstOrDefaultAsync();

    // Check whether the new slot exists.
    if (newSlot == null)
    {
        return (
            false,
            404,
            new
            {
                message =
                    "The requested slot was not found."
            }
        );
    }

    // Check whether the requested slot has capacity.
    if (newSlot.SlotId != reservation.SlotId &&
        newSlot.AvailableCapacity <= 0)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "The requested slot has no available capacity."
            }
        );
    }

    // Check whether the reservation is moving to another slot.
    var slotChanged =
        newSlot.SlotId != reservation.SlotId;

    if (slotChanged)
    {
        // Reserve one capacity unit in the new slot.
        var reserveNewSlotFilter =
            Builders<EnergyBookingSlot>.Filter.And(
                Builders<EnergyBookingSlot>.Filter.Eq(
                    x => x.SlotId,
                    newSlot.SlotId),

                Builders<EnergyBookingSlot>.Filter.Gt(
                    x => x.AvailableCapacity,
                    0));

        var reserveNewSlotUpdate =
            Builders<EnergyBookingSlot>.Update
                .Inc(x => x.AvailableCapacity, -1)
                .Set(x => x.UpdatedAt, DateTime.UtcNow);

        var reserveNewSlotResult =
            await slots.UpdateOneAsync(
                reserveNewSlotFilter,
                reserveNewSlotUpdate);

        // Ensure the new slot was reserved successfully.
        if (reserveNewSlotResult.ModifiedCount == 0)
        {
            return (
                false,
                400,
                new
                {
                    message =
                        "The requested slot is no longer available."
                }
            );
        }

        // Release capacity in the original slot.
        await slots.UpdateOneAsync(
            x => x.SlotId == reservation.SlotId,
            Builders<EnergyBookingSlot>.Update
                .Inc(x => x.AvailableCapacity, 1)
                .Set(x => x.UpdatedAt, DateTime.UtcNow));
    }

    // Apply the pending change to the reservation.
    var update = Builders<EnergyReservation>.Update

        .Set(
            x => x.SlotId,
            reservation.PendingSlotId)

        .Set(
            x => x.StationId,
            reservation.PendingStationId)

        .Set(
            x => x.ScheduledStartTime,
            reservation.PendingScheduledStartTime.Value)

        .Set(
            x => x.ScheduledEndTime,
            reservation.PendingScheduledEndTime.Value)

        .Set(
            x => x.EnergyAmountKwh,
            reservation.PendingEnergyAmountKwh.Value)

        .Set(x => x.HasPendingChange, false)

        // NEW:
        // Tell the Prosumer that the change was approved.
        .Set(x => x.ChangeRequestStatus, "APPROVED")

        .Set(x => x.PendingSlotId, null)

        .Set(x => x.PendingStationId, null)

        .Set(x => x.PendingScheduledStartTime, null)

        .Set(x => x.PendingScheduledEndTime, null)

        .Set(x => x.PendingEnergyAmountKwh, null)

        .Set(x => x.Status, BookingStatus.CONFIRMED)

        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Save the updated reservation.
    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.CONFIRMED && x.HasPendingChange,
        update);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Retrieve the updated reservation.
    var updatedReservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Return the updated reservation.
    return (
        true,
        200,
        new
        {
            message =
                "Reservation change approved successfully.",

            reservation = updatedReservation
        }
    );
}
// Rejects a pending change to an existing reservation.
public async Task<(bool Success, int StatusCode, object Response)>
    RejectReservationChangeAsync(
        string reservationId,
        string reason)
{
    // Get the reservations collection.
    var reservations = _mongoDbService.GetReservationsCollection();

    // Validate the rejection reason.
    if (string.IsNullOrWhiteSpace(reason))
    {
        return (
            false,
            400,
            new
            {
                message = "A rejection reason is required."
            }
        );
    }

    // Find the reservation.
    var reservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Check whether the reservation exists.
    if (reservation == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Reservation not found."
            }
        );
    }

    // Check whether a pending change exists.
    if (!reservation.HasPendingChange)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "No pending change exists for this reservation."
            }
        );
    }

    // Reject the pending change and preserve
    // the original booking.
    var update = Builders<EnergyReservation>.Update

        .Set(x => x.HasPendingChange, false)

        // NEW:
        // Tell the Prosumer that the change was rejected.
        .Set(x => x.ChangeRequestStatus, "REJECTED")

        .Set(x => x.PendingSlotId, null)

        .Set(x => x.PendingStationId, null)

        .Set(x => x.PendingScheduledStartTime, null)

        .Set(x => x.PendingScheduledEndTime, null)

        .Set(x => x.PendingEnergyAmountKwh, null)

        // Store the rejection reason so the Prosumer
        // can see why the request was rejected.
        .Set(x => x.CancellationReason, reason.Trim())

        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Save the rejection.
    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.CONFIRMED && x.HasPendingChange,
        update);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Retrieve the updated reservation.
    var updatedReservation = await reservations
        .Find(x => x.ReservationId == reservationId)
        .FirstOrDefaultAsync();

    // Return the updated reservation.
    return (
        true,
        200,
        new
        {
            message =
                "Reservation change rejected successfully.",

            reservation = updatedReservation
        }
    );
}
// Cancels a confirmed reservation at least 12 hours before its start time.
public async Task<(bool Success, int StatusCode, object Response)>
    CancelReservationAsync(
        string reservationId,
        string prosumerUserId,
        string reason)
{
    var reservations = _mongoDbService.GetReservationsCollection();
    var slots = _mongoDbService.GetBookingSlotsCollection();

    if (string.IsNullOrWhiteSpace(reason))
    {
        return (
            false,
            400,
            new { message = "Cancellation reason is required." }
        );
    }

    var reservation = await reservations
        .Find(x =>
            x.ReservationId == reservationId &&
            x.ProsumerUserId == prosumerUserId)
        .FirstOrDefaultAsync();

    if (reservation == null)
    {
        return (
            false,
            404,
            new { message = "Reservation not found." }
        );
    }

    if (reservation.Status != BookingStatus.CONFIRMED)
    {
        return (
            false,
            400,
            new
            {
                message = "Only confirmed reservations can be cancelled."
            }
        );
    }

    var minimumNoticeTime = DateTime.UtcNow.AddHours(12);

    if (reservation.ScheduledStartTime < minimumNoticeTime)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Cancellations must be requested at least 12 hours before the scheduled start time."
            }
        );
    }

    var reservationUpdate = Builders<EnergyReservation>.Update
        .Set(x => x.Status, BookingStatus.CANCELLED)
        .Set(x => x.CancellationReason, reason.Trim())
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    var write = await reservations.UpdateOneAsync(
        x => x.ReservationId == reservationId && x.Status == BookingStatus.CONFIRMED &&
             x.SlotId == reservation.SlotId && x.UpdatedAt == reservation.UpdatedAt,
        reservationUpdate);
    if (write.MatchedCount == 0)
        return (false, 409, new { message = "Reservation changed. Refresh and try again." });

    // Restore one capacity unit to the original slot.
    var slotUpdate = Builders<EnergyBookingSlot>.Update
        .Inc(x => x.AvailableCapacity, 1)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    await slots.UpdateOneAsync(
        x =>
            x.SlotId == reservation.SlotId &&
            x.AvailableCapacity < x.TotalCapacity,
        slotUpdate);

    reservation.Status = BookingStatus.CANCELLED;
    reservation.CancellationReason = reason.Trim();
    reservation.UpdatedAt = DateTime.UtcNow;

    return (
        true,
        200,
        new
        {
            message = "Reservation cancelled successfully.",
            reservation
        }
    );
}
/// <summary>
/// Updates an existing booking slot while preserving existing reservations.
/// </summary>
public async Task<(bool Success, int StatusCode, object Response)>
    UpdateBookingSlotAsync(
        string slotId,
        UpdateBookingSlotRequest request)
{
    var slots = _mongoDbService.GetBookingSlotsCollection();
    var reservations = _mongoDbService.GetReservationsCollection();

    // Find the active booking slot.
    var slot = await slots
        .Find(x => x.SlotId == slotId && x.IsActive)
        .FirstOrDefaultAsync();

    if (slot == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Active booking slot not found."
            });
    }

    // Validate the booking time range.
    if (request.StartTime >= request.EndTime)
    {
        return (
            false,
            400,
            new
            {
                message = "Start time must be before end time."
            });
    }

    // Validate capacity.
    if (request.TotalCapacity <= 0)
    {
        return (
            false,
            400,
            new
            {
                message = "Total capacity must be greater than zero."
            });
    }

    // Convert incoming times to UTC.
    var requestedStartTime =
        request.StartTime.ToUniversalTime();

    var requestedEndTime =
        request.EndTime.ToUniversalTime();

    // Determine whether the schedule is changing.
    var scheduleChanged =
        requestedStartTime != slot.StartTime ||
        requestedEndTime != slot.EndTime;

    // Check for pending or confirmed reservations.
    var hasActiveReservations = await reservations
        .Find(x =>
            x.SlotId == slotId &&
            (x.Status == BookingStatus.PENDING ||
             x.Status == BookingStatus.CONFIRMED))
        .AnyAsync();

    // Prevent schedule changes when active reservations exist.
    if (scheduleChanged && hasActiveReservations)
    {
        return (
            false,
            409,
            new
            {
                message =
                    "Booking slot schedule cannot be changed because active reservations exist."
            });
    }

    // Calculate currently reserved capacity.
    var reservedCapacity =
        slot.TotalCapacity - slot.AvailableCapacity;

    // Prevent capacity from becoming lower than reserved capacity.
    if (request.TotalCapacity < reservedCapacity)
    {
        return (
            false,
            400,
            new
            {
                message =
                    "Total capacity cannot be lower than the currently reserved capacity."
            });
    }

    // Build the update.
    var update = Builders<EnergyBookingSlot>.Update
        .Set(x => x.StartTime, requestedStartTime)
        .Set(x => x.EndTime, requestedEndTime)
        .Set(x => x.TotalCapacity, request.TotalCapacity)
        .Set(
            x => x.AvailableCapacity,
            request.TotalCapacity - reservedCapacity)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    // Apply the update only to the active slot.
    var updateResult = await slots.UpdateOneAsync(
        x => x.SlotId == slotId && x.IsActive && x.TotalCapacity == slot.TotalCapacity &&
             x.AvailableCapacity == slot.AvailableCapacity && x.UpdatedAt == slot.UpdatedAt,
        update);

    // Confirm that the slot was updated.
    if (updateResult.ModifiedCount == 0)
    {
        return (
            false,
            409,
            new
            {
                message =
                    "The booking slot could not be updated."
            });
    }

    return (
        true,
        200,
        new
        {
            message = "Booking slot updated successfully.",
            slotId,
            startTime = requestedStartTime,
            endTime = requestedEndTime,
            totalCapacity = request.TotalCapacity,
            availableCapacity =
                request.TotalCapacity - reservedCapacity
        });
}

/// Deactivates a booking slot if it has no active reservations.
public async Task<(bool Success, int StatusCode, object Response)>
    DeactivateBookingSlotAsync(string slotId)
{
    var slots = _mongoDbService.GetBookingSlotsCollection();
    var reservations = _mongoDbService.GetReservationsCollection();

    var slot = await slots
        .Find(x => x.SlotId == slotId && x.IsActive)
        .FirstOrDefaultAsync();

    if (slot == null)
    {
        return (
            false,
            404,
            new
            {
                message = "Active booking slot not found."
            });
    }

    var hasActiveReservations = await reservations
        .Find(x =>
            x.SlotId == slotId &&
            (
                x.Status == BookingStatus.PENDING ||
                x.Status == BookingStatus.CONFIRMED
            ))
        .AnyAsync();

    if (hasActiveReservations)
    {
        return (
            false,
            409,
            new
            {
                message =
                    "Slot cannot be deactivated because it has active reservations."
            });
    }

    var update = Builders<EnergyBookingSlot>.Update
        .Set(x => x.IsActive, false)
        .Set(x => x.UpdatedAt, DateTime.UtcNow);

    await slots.UpdateOneAsync(
        x => x.SlotId == slotId && x.IsActive,
        update);

    return (
        true,
        200,
        new
        {
            message = "Booking slot deactivated successfully.",
            slotId = slotId,
            isActive = false
        });
}
}
