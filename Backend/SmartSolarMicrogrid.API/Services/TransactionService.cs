using System.Security.Cryptography;
using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

/// Authoritative QR lifecycle; all state transitions use conditional document writes.
public class TransactionService(MongoDbService mongo, StationService stations)
{
    private IMongoCollection<EnergyReservation> Reservations => mongo.GetReservationsCollection();
    private static (bool Success, int StatusCode, object Response) Error(int code, string message)
        => (false, code, new { message });

    public async Task<(bool Success, int StatusCode, object Response)> GenerateAsync(string reservationId, string userId)
    {
        var r = await Reservations.Find(x => x.ReservationId == reservationId).FirstOrDefaultAsync();
        if (r == null) return Error(404, "Reservation not found.");
        if (r.ProsumerUserId != userId) return Error(403, "This reservation belongs to another prosumer.");
        var validation = await ValidateAsync(r);
        if (validation != null) return validation.Value;

        var user = await mongo.GetUsersCollection().Find(x => x.UserId == userId).FirstOrDefaultAsync();
        var existing = r.Transaction;
        if (existing?.TransactionStatus == TransactionStates.Completed || existing?.QRStatus == TransactionStates.Used)
            return Error(409, "This reservation has already consumed its transaction.");
        if (existing != null && !IsUsableState(existing))
            return Error(409, "This QR transaction is invalid.");
        if (existing != null && Matches(r, existing) && existing.ExpiresAt > DateTime.UtcNow)
            return (true, 200, QrResponse(existing));

        var transaction = new EnergyTransaction
        {
            TransactionId = $"TRX-{Guid.NewGuid():N}".ToUpperInvariant(),
            ReservationId = r.ReservationId,
            ProsumerNIC = user!.NIC,
            StationId = r.StationId,
            SlotId = r.SlotId,
            ScheduledStartTime = r.ScheduledStartTime,
            ScheduledEndTime = r.ScheduledEndTime,
            EnergyAmountKwh = r.EnergyAmountKwh,
            QRToken = "TRX:" + Convert.ToHexString(RandomNumberGenerator.GetBytes(32)),
            GeneratedAt = DateTime.UtcNow,
            ExpiresAt = r.ScheduledEndTime
        };
        var filter = CurrentReservation(r) & Builders<EnergyReservation>.Filter.Eq(x => x.Transaction, existing);
        var saved = await Reservations.FindOneAndUpdateAsync(filter,
            Builders<EnergyReservation>.Update.Set(x => x.Transaction, transaction),
            new FindOneAndUpdateOptions<EnergyReservation> { ReturnDocument = ReturnDocument.After });
        if (saved == null)
        {
            // Concurrent generation must return the winner's QR, never create a second active QR.
            var winner = await Reservations.Find(x => x.ReservationId == reservationId).FirstOrDefaultAsync();
            if (winner?.Status == BookingStatus.CONFIRMED && !winner.HasPendingChange &&
                winner.Transaction is { } t && Matches(winner, t) && t.ExpiresAt > DateTime.UtcNow &&
                IsUsableState(t))
                return (true, 200, QrResponse(t));
            return Error(409, "Reservation changed. Refresh and try again.");
        }
        return (true, 201, QrResponse(transaction));
    }

    public async Task<(bool Success, int StatusCode, object Response)> VerifyAsync(string token, string operatorId)
    {
        token = token.Trim();
        if (token.Length != 68 || !token.StartsWith("TRX:", StringComparison.Ordinal))
            return Error(400, "Invalid QR code.");
        var r = await Reservations.Find(x => x.Transaction != null && x.Transaction.QRToken == token).FirstOrDefaultAsync();
        if (r?.Transaction == null) return Error(404, "QR transaction not found.");
        var validation = await ValidateAsync(r, operatorId);
        if (validation != null) return validation.Value;
        var t = r.Transaction;
        var stateError = ValidateTransaction(r, t);
        if (stateError != null) return stateError.Value;
        if (t.TransactionStatus == TransactionStates.Verified && t.VerifiedByOperatorId != operatorId)
            return Error(409, "This QR is already being handled by another operator.");

        var now = DateTime.UtcNow;
        var filter = CurrentReservation(r) & Builders<EnergyReservation>.Filter.Eq(x => x.Transaction, t);
        var update = Builders<EnergyReservation>.Update
            .Set(x => x.Transaction!.QRStatus, TransactionStates.Verified)
            .Set(x => x.Transaction!.TransactionStatus, TransactionStates.Verified)
            .Set(x => x.Transaction!.VerifiedAt, t.VerifiedAt ?? now)
            .Set(x => x.Transaction!.VerifiedByOperatorId, operatorId);
        var saved = await Reservations.FindOneAndUpdateAsync(filter, update,
            new FindOneAndUpdateOptions<EnergyReservation> { ReturnDocument = ReturnDocument.After });
        if (saved == null) return Error(409, "Transaction changed or was already used. Scan again.");
        return (true, 200, await DetailsAsync(saved));
    }

    public async Task<(bool Success, int StatusCode, object Response)> CompleteAsync(string transactionId, string operatorId)
    {
        var r = await Reservations.Find(x => x.Transaction != null && x.Transaction.TransactionId == transactionId).FirstOrDefaultAsync();
        if (r?.Transaction == null) return Error(404, "Transaction not found.");
        var validation = await ValidateAsync(r, operatorId);
        if (validation != null) return validation.Value;
        var t = r.Transaction;
        var stateError = ValidateTransaction(r, t);
        if (stateError != null) return stateError.Value;
        if (t.TransactionStatus != TransactionStates.Verified || t.QRStatus != TransactionStates.Verified)
            return Error(409, "Scan and verify the QR before completing the transfer.");
        if (t.VerifiedByOperatorId != operatorId)
            return Error(403, "Only the operator who verified this QR can complete it.");

        // One database operation: a concurrent request can never partially complete or win twice.
        var now = DateTime.UtcNow;
        var filter = CurrentReservation(r) & Builders<EnergyReservation>.Filter.Eq(x => x.Transaction, t);
        var update = Builders<EnergyReservation>.Update
            .Set(x => x.Status, BookingStatus.COMPLETED).Set(x => x.UpdatedAt, now)
            .Set(x => x.Transaction!.TransactionStatus, TransactionStates.Completed)
            .Set(x => x.Transaction!.QRStatus, TransactionStates.Used)
            .Set(x => x.Transaction!.CompletedAt, now)
            .Set(x => x.Transaction!.CompletedByOperatorId, operatorId);
        var saved = await Reservations.FindOneAndUpdateAsync(filter, update,
            new FindOneAndUpdateOptions<EnergyReservation> { ReturnDocument = ReturnDocument.After });
        if (saved == null) return Error(409, "Transaction already completed or reservation changed.");
        return (true, 200, await DetailsAsync(saved));
    }

    private async Task<(bool Success, int StatusCode, object Response)?> ValidateAsync(EnergyReservation r, string? operatorId = null)
    {
        var station = await stations.GetStationAsync(r.StationId);
        if (station == null) return Error(404, "Station not found.");
        if (operatorId != null)
        {
            var op = await mongo.GetUsersCollection().Find(x => x.UserId == operatorId).FirstOrDefaultAsync();
            if (op?.Role != UserRole.GridOperator || op.AccountStatus != AccountStatus.Active)
                return Error(403, "An active Grid Operator account is required.");
        }
        if (r.Status != BookingStatus.CONFIRMED)
            return Error(409, "Reservation must be confirmed and must not be cancelled or completed.");
        if (r.HasPendingChange) return Error(409, "Resolve the pending reservation change before energy transfer.");
        if (r.ScheduledEndTime <= DateTime.UtcNow) return Error(409, "The reservation QR has expired.");
        if (!string.Equals(station.Status, AccountStatus.Active, StringComparison.OrdinalIgnoreCase))
            return Error(409, "Station is inactive.");
        var user = await mongo.GetUsersCollection().Find(x => x.UserId == r.ProsumerUserId).FirstOrDefaultAsync();
        if (user?.Role != UserRole.Prosumer || user.AccountStatus != AccountStatus.Active)
            return Error(409, "Prosumer account is not active.");
        var slot = await mongo.GetBookingSlotsCollection().Find(x => x.SlotId == r.SlotId).FirstOrDefaultAsync();
        if (slot == null || !slot.IsActive || slot.StationId != r.StationId ||
            slot.StartTime != r.ScheduledStartTime || slot.EndTime != r.ScheduledEndTime)
            return Error(409, "Reservation no longer matches an active station slot.");
        return null;
    }

    private static (bool Success, int StatusCode, object Response)? ValidateTransaction(EnergyReservation r, EnergyTransaction t)
    {
        if (t.TransactionStatus == TransactionStates.Completed || t.QRStatus == TransactionStates.Used)
            return Error(409, "Transaction already completed. This QR has been used.");
        if (t.ExpiresAt <= DateTime.UtcNow) return Error(409, "This QR has expired.");
        if (!Matches(r, t)) return Error(409, "Reservation changed. Ask the prosumer to generate a new QR.");
        if (!IsUsableState(t))
            return Error(409, "This QR is invalid.");
        return null;
    }

    private static bool Matches(EnergyReservation r, EnergyTransaction t) =>
        t.ReservationId == r.ReservationId && t.StationId == r.StationId && t.SlotId == r.SlotId &&
        t.ScheduledStartTime == r.ScheduledStartTime && t.ScheduledEndTime == r.ScheduledEndTime &&
        t.EnergyAmountKwh == r.EnergyAmountKwh;

    private static bool IsUsableState(EnergyTransaction t) =>
        (t.TransactionStatus == TransactionStates.Created && t.QRStatus == TransactionStates.Active) ||
        (t.TransactionStatus == TransactionStates.Verified && t.QRStatus == TransactionStates.Verified);

    private static FilterDefinition<EnergyReservation> CurrentReservation(EnergyReservation r) =>
        Builders<EnergyReservation>.Filter.Where(x => x.ReservationId == r.ReservationId &&
            x.Status == BookingStatus.CONFIRMED && !x.HasPendingChange &&
            x.UpdatedAt == r.UpdatedAt && x.SlotId == r.SlotId && x.StationId == r.StationId &&
            x.ScheduledStartTime == r.ScheduledStartTime && x.ScheduledEndTime == r.ScheduledEndTime &&
            x.EnergyAmountKwh == r.EnergyAmountKwh);

    private static object QrResponse(EnergyTransaction t) => new
    {
        transactionId = t.TransactionId,
        reservationId = t.ReservationId,
        qrToken = t.QRToken,
        qrStatus = t.QRStatus,
        transactionStatus = t.TransactionStatus,
        generatedAt = t.GeneratedAt,
        expiresAt = t.ExpiresAt
    };

    private async Task<object> DetailsAsync(EnergyReservation r)
    {
        var t = r.Transaction!;
        var station = await stations.GetStationAsync(r.StationId);
        var prosumer = await mongo.GetUsersCollection().Find(x => x.UserId == r.ProsumerUserId).FirstOrDefaultAsync();
        return new
        {
            transactionId = t.TransactionId,
            reservationId = r.ReservationId,
            prosumerNIC = prosumer?.NIC ?? t.ProsumerNIC,
            prosumerName = prosumer?.Name,
            stationId = r.StationId,
            stationName = station?.Name,
            slotId = r.SlotId,
            scheduledStartTime = r.ScheduledStartTime,
            scheduledEndTime = r.ScheduledEndTime,
            energyAmountKwh = r.EnergyAmountKwh,
            reservationStatus = r.Status.ToString(),
            transactionStatus = t.TransactionStatus,
            qrStatus = t.QRStatus,
            verifiedAt = t.VerifiedAt,
            completedAt = t.CompletedAt
        };
    }
}
