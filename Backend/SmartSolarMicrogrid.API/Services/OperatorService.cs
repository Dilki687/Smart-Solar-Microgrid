using MongoDB.Driver;
using SmartSolarMicrogrid.API.Models;

namespace SmartSolarMicrogrid.API.Services;

public class OperatorService(MongoDbService mongo)
{
    public async Task<(bool Success, int StatusCode, object Response)> DashboardAsync(string operatorId)
    {
        var user = await mongo.GetUsersCollection().Find(x => x.UserId == operatorId).FirstOrDefaultAsync();
        if (user?.Role != UserRole.GridOperator || user.AccountStatus != AccountStatus.Active)
            return (false, 403, new { message = "An active Grid Operator account is required." });
        var stations = await mongo.GetStationsCollection()
            .Find(x => x.Status == AccountStatus.Active)
            .ToListAsync();
        var ids = stations.Select(x => x.StationId).ToList();
        var reservations = await mongo.GetReservationsCollection()
            .Find(Builders<EnergyReservation>.Filter.In(x => x.StationId, ids))
            .SortBy(x => x.ScheduledStartTime).ToListAsync();
        // Sri Lankan operational day; all stored timestamps and returned instants remain UTC.
        var start = DateTime.UtcNow.AddMinutes(330).Date.AddMinutes(-330);
        var end = start.AddDays(1);
        var active = reservations.Where(x => x.Status is BookingStatus.PENDING or BookingStatus.CONFIRMED).ToList();
        var today = active.Where(x => x.ScheduledStartTime >= start && x.ScheduledStartTime < end).ToList();
        var upcoming = active.Where(x => x.ScheduledStartTime >= end).ToList();
        var slots = await mongo.GetBookingSlotsCollection()
            .Find(Builders<EnergyBookingSlot>.Filter.In(x => x.StationId, ids) &
                  Builders<EnergyBookingSlot>.Filter.Where(x => x.IsActive && x.EndTime > DateTime.UtcNow))
            .SortBy(x => x.StartTime).ToListAsync();
        return (true, 200, new
        {
            operatorName = user.Name, timeZone = "Asia/Colombo", todayBookings = today.Count,
            upcomingBookings = upcoming.Count, pendingBookings = active.Count(x => x.Status == BookingStatus.PENDING),
            completedBookings = reservations.Count(x => x.Status == BookingStatus.COMPLETED),
            today, upcoming, stations = stations.Select(x => new { x.StationId, x.Name, x.Status }), slots
        });
    }
}
